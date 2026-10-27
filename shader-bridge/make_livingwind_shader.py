#!/usr/bin/env python3
"""Create a PRIVATE Living Wind-compatible copy of an owned shader pack.

Supports Complementary Reimagined r5.9.3, with or without Euphoria Patches
1.10.5, including packs modified by Living Wind's previous foliage bridge.
No source shader assets are bundled with this script.

Usage: python make_livingwind_shader.py input.zip output.zip
In normal installations Living Wind patches supported shader packs automatically;
this offline tool is only for testing/manual recovery.
"""
from pathlib import Path
import argparse
import zipfile

WAVE = 'shaders/lib/materials/materialMethods/wavingBlocks.glsl'
STORM = 'shaders/lib/atmospherics/netherStorm.glsl'
OLD = '// LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)'
WAVE_MARKER = '// LIVING WIND OPTIONAL SHADER BRIDGE (v0.3.0)'
STORM_MARKER = '// LIVING WIND NETHER STORM DRIFT (v0.3.0)'
END = '// END LIVING WIND OPTIONAL SHADER BRIDGE'
INSERT_WAVE = r'''// LIVING WIND OPTIONAL SHADER BRIDGE (v0.3.0)
// Registered dynamically through Iris/Oculus or Euphoria uniform hooks.
// When Living Wind isn't connected, use the original Complementary waving.
uniform float livingWindActive;
uniform float livingWindStrength;
uniform float livingWindGust;
uniform float livingWindDirectionX;
uniform float livingWindDirectionZ;
uniform float livingWindAmplitude;
uniform float livingWindTime;

vec3 LivingWindGetWave(in vec3 pos, float waveSpeed) {
    if (livingWindActive < 0.5) return GetWave(pos, waveSpeed);
    float strength = clamp(livingWindStrength, 0.0, 2.5);
    float gust = clamp(livingWindGust, 0.0, 1.5);
    vec3 wave = GetRawWave(pos, livingWindTime * waveSpeed * WAVING_SPEED);
    float amplitude = WAVING_I * mix(1.0, WAVING_I_RAIN_MULT * 0.01, rainFactor);
    amplitude *= clamp(0.08 + 0.86 * strength + 0.25 * gust, 0.05, 2.8);
    amplitude *= clamp(livingWindAmplitude, 0.0, 3.0);
    vec2 direction = vec2(livingWindDirectionX, livingWindDirectionZ);
    float dirLen = length(direction);
    if (dirLen > 0.0001) {
        direction /= dirLen;
        vec2 crossWind = vec2(-direction.y, direction.x);
        float along = wave.x * 0.6 + wave.z * 0.4;
        vec2 aligned = direction * along + crossWind * wave.y * 0.25;
        wave.xz = mix(wave.xz, aligned, 0.80);
    }
    return wave * amplitude;
}
// END LIVING WIND OPTIONAL SHADER BRIDGE

'''
INSERT_STORM = '''// LIVING WIND NETHER STORM DRIFT (v0.3.0)
// Preserve the original storm turbulence, only translate its noise sampling.
uniform float livingWindStormActive;
uniform float livingWindStormOffsetX;
uniform float livingWindStormOffsetZ;
// END LIVING WIND NETHER STORM DRIFT

'''


def patch_wave(source: str) -> str:
    if WAVE_MARKER in source:
        return source
    anchor = 'void DoWave_Foliage(inout vec3 playerPos, vec3 worldPos, float waveMult) {'
    if source.count(anchor) != 1 or 'vec3 GetRawWave(in vec3 pos, float wind)' not in source:
        raise ValueError('Unsupported foliage GLSL version')
    if OLD in source:
        start = source.index(OLD)
        end = source.find(END, start)
        if end < 0:
            raise ValueError('Incomplete legacy Living Wind bridge')
        return source[:start] + INSERT_WAVE.rstrip() + source[end+len(END):]
    for fn in ('DoWave_Foliage', 'DoWave_Leaves'):
        start = source.find('void ' + fn + '(')
        end = source.find('\nvoid ', start + 5)
        if start < 0 or end < 0:
            raise ValueError('Unsupported waving function: ' + fn)
        block = source[start:end]
        target = 'vec3 wave = GetWave(worldPos, 170.0);'
        if block.count(target) != 1:
            raise ValueError('Unsupported ' + fn + ' waving call')
        source = source[:start] + block.replace(target, 'vec3 wave = LivingWindGetWave(worldPos, 170.0);') + source[end:]
    return source.replace(anchor, INSERT_WAVE + anchor, 1)


def patch_storm(source: str) -> str:
    if STORM_MARKER in source:
        return source
    coord = 'tracePosM.y += tracePosM.x;'
    if source.count('vec4 GetNetherStorm(') != 1 or source.count(coord) != 1 or 'Noise3D(tracePosM' not in source:
        raise ValueError('Unsupported Nether Storm GLSL version')
    source = source.replace('vec4 GetNetherStorm(', INSERT_STORM + 'vec4 GetNetherStorm(', 1)
    source = source.replace(coord, coord + '''
        // Advect fog through world-space X/Z, without modifying native turbulence.
        tracePosM.xz -= vec2(livingWindStormOffsetX, livingWindStormOffsetZ) *
            (livingWindStormActive > 0.5 ? 0.001 : 0.0);''', 1)
    return source


def make(input_path: Path, output_path: Path) -> Path:
    if input_path.resolve() == output_path.resolve():
        raise ValueError('Input and output must be different. Keep the original unmodified.')
    with zipfile.ZipFile(input_path) as src:
        names = src.namelist()
        if len(names) > 8192:
            raise ValueError('Too many ZIP entries')
        matches = {}
        for target in (WAVE, STORM):
            candidates = [n for n in names if n.endswith(target)]
            if len(candidates) != 1:
                raise ValueError(f'Expected exactly one {target}')
            matches[target] = candidates[0]
        updates = {
            matches[WAVE]: patch_wave(src.read(matches[WAVE]).decode('utf-8')).encode('utf-8'),
            matches[STORM]: patch_storm(src.read(matches[STORM]).decode('utf-8')).encode('utf-8'),
        }
        prefix = matches[WAVE][:-len(WAVE)]
        # Preserve input entries, including presets/configuration and EP/MCWind.
        with zipfile.ZipFile(output_path, 'w', compression=zipfile.ZIP_DEFLATED) as out:
            for item in src.infolist():
                if item.is_dir():
                    continue
                filename = item.filename[len(prefix):] if item.filename.startswith(prefix) else item.filename
                if not filename or filename.startswith('../') or filename.startswith('/'):
                    continue
                out.writestr(filename, updates.get(item.filename, src.read(item)))
    with zipfile.ZipFile(output_path) as check:
        if check.testzip() is not None or WAVE_MARKER.encode() not in check.read(WAVE) or STORM_MARKER.encode() not in check.read(STORM):
            raise RuntimeError('Output verification failed')
    return output_path


if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('input_zip', type=Path)
    ap.add_argument('output_zip', type=Path)
    a = ap.parse_args()
    print('Created:', make(a.input_zip, a.output_zip))
