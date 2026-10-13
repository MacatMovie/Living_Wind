#!/usr/bin/env python3
"""Patch a USER-SUPPLIED Complementary Reimagined r5.9.3 + EP 1.10.5 pack.

This script does not distribute any shader assets; it writes a modified PRIVATE
copy of the shader pack that the user already owns. This is deliberately strict
about file fingerprints/anchors: unknown shader versions fail instead of
silently corrupting shader GLSL.

Usage: python make_livingwind_shader.py "input shader.zip" "output shader.zip"
"""
from __future__ import annotations
import argparse
from pathlib import Path
import zipfile

TARGET = "shaders/lib/materials/materialMethods/wavingBlocks.glsl"
MARKER = "// LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)"
# GLSL code authored for Living Wind. No shader source from Complementary copied.
INSERT = r'''// LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)
// Registered dynamically by Living Wind through Euphoria 1.10.5's uniform API.
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
    // The integrated wind clock comes from Java. Adjusting speed by scaling
    // frameTimeCounter would instead snap wave phase on every gust.
    vec3 wave = GetRawWave(pos, livingWindTime * waveSpeed * WAVING_SPEED);
    float amplitude = WAVING_I * mix(1.0, WAVING_I_RAIN_MULT * 0.01, rainFactor);
    amplitude *= clamp(0.08 + 0.86 * strength + 0.25 * gust, 0.05, 2.8);
    amplitude *= clamp(livingWindAmplitude, 0.0, 3.0);

    vec2 direction = vec2(livingWindDirectionX, livingWindDirectionZ);
    float dirLen = length(direction);
    if (dirLen > 0.0001) {
        direction /= dirLen;
        vec2 crossWind = vec2(-direction.y, direction.x);
        // Preserve small irregular movement while most displacement follows
        // the real, synchronized wind direction.
        float along = wave.x * 0.6 + wave.z * 0.4;
        vec2 aligned = direction * along + crossWind * wave.y * 0.25;
        wave.xz = mix(wave.xz, aligned, 0.80);
    }
    return wave * amplitude;
}
// END LIVING WIND OPTIONAL SHADER BRIDGE

'''


def patch(data: bytes) -> bytes:
    text = data.decode('utf-8')
    if MARKER in text:
        raise ValueError('Shader is already patched with the Living Wind bridge')
    anchor = 'void DoWave_Foliage(inout vec3 playerPos, vec3 worldPos, float waveMult) {'
    if text.count(anchor) != 1:
        raise ValueError('Expected exactly one DoWave_Foliage: unsupported shader version')
    # Change ONLY the two main vegetation animation functions, leaving
    # lava/water/other material animation untouched.
    for fn in ('DoWave_Foliage', 'DoWave_Leaves'):
        start = text.index('void ' + fn + '(')
        end = text.find('\nvoid ', start + 5)
        if end == -1: raise ValueError('Could not isolate ' + fn)
        old = text[start:end]
        target = 'vec3 wave = GetWave(worldPos, 170.0);'
        if old.count(target) != 1:
            raise ValueError('Waving function incompatible: ' + fn)
        new = old.replace(target, 'vec3 wave = LivingWindGetWave(worldPos, 170.0);', 1)
        text = text[:start] + new + text[end:]
    text = text.replace(anchor, INSERT + anchor, 1)
    return text.encode('utf-8')


def make(input_path: Path, output_path: Path):
    if input_path.resolve() == output_path.resolve():
        raise ValueError('Input and output must be different (original left untouched)')
    if not zipfile.is_zipfile(input_path):
        raise ValueError('Input must be a ZIP shader pack')
    with zipfile.ZipFile(input_path) as src:
        files = [item for item in src.infolist() if not item.is_dir()]
        matches = [x.filename for x in files if x.filename.endswith(TARGET)]
        if len(matches) != 1:
            raise ValueError('Expected one patched Euphoria foliage source (this is not the EP preset?)')
        target = matches[0]
        prefix = target[: -len(TARGET)]
        if 'Euphoria' not in '\n'.join(x.filename for x in files):
            # Version info can be contained only inside the source name.
            print('Note: no explicit Euphoria marker in filenames; verifying GLSL anchors instead')
        raw = src.read(target).decode('utf-8')
        if 'MCWIND_INTERNAL' not in raw or 'GetRawWave' not in raw:
            raise ValueError('Requires Euphoria r5.9.3 patched wavingBlocks.glsl')
        patched = patch(src.read(target))
        # Flatten an enclosing directory so Iris can read shaders/ from the ZIP root.
        with zipfile.ZipFile(output_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=7) as out:
            for item in files:
                relative = item.filename[len(prefix):] if item.filename.startswith(prefix) else item.filename
                if not relative or relative.startswith('../'):
                    continue
                content = patched if item.filename == target else src.read(item)
                out.writestr(relative, content)
    with zipfile.ZipFile(output_path) as check:
        if check.testzip() is not None:
            raise RuntimeError('Output ZIP failed integrity check')
        result = check.read(TARGET).decode('utf-8')
        assert MARKER in result and result.count('LivingWindGetWave(worldPos, 170.0)') == 2
    return output_path

if __name__ == '__main__':
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('input_zip', type=Path)
    cli.add_argument('output_zip', type=Path)
    args = cli.parse_args()
    print('Created:',make(args.input_zip, args.output_zip))
