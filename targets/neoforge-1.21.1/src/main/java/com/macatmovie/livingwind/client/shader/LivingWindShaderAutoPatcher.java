package com.macatmovie.livingwind.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * User-local, opt-out, conservative auto-patcher for Complementary Reimagined
 * r5.9.3 with or without Euphoria Patches 1.10.5. Supports foliage and
 * Nether Storm using the shader packs already installed by the player.
 *
 * Important: this changes an *installed* shader archive, not a shader embedded
 * in or distributed with Living Wind. Full byte-exact backups are made before
 * replacing ZIPs. Unrecognized versions and packs are left untouched.
 *
 * Invoked during client startup and after Euphoria regenerates a pack.
 * Uses only Java NIO: no Euphoria or Iris linkage at class-loading time.
 */
public final class LivingWindShaderAutoPatcher {
    private static final String TARGET = "shaders/lib/materials/materialMethods/wavingBlocks.glsl";
    private static final String STORM = "shaders/lib/atmospherics/netherStorm.glsl";
    private static final String STORM_MARKER = "// LIVING WIND NETHER STORM DRIFT (v0.3.0)";
    private static final String LEGACY_MARKER = "// LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)";
    private static final String LEGACY_END = "// END LIVING WIND OPTIONAL SHADER BRIDGE";
    private static final String STORM_INSERT = """
        // LIVING WIND NETHER STORM DRIFT (v0.3.0)
        // The original storm turbulence remains unchanged; only its spatial noise
        // sampling is advected by a continuously integrated horizontal wind offset.
        uniform float livingWindStormActive;
        uniform float livingWindStormOffsetX;
        uniform float livingWindStormOffsetZ;
        // END LIVING WIND NETHER STORM DRIFT
        """;

    private static final String MARKER = "// LIVING WIND OPTIONAL SHADER BRIDGE (v0.3.0)";
    private static final long MAX_ZIP_BYTES = 128L * 1024 * 1024;
    private static final long MAX_SHADER_BYTES = 1024L * 1024;
    private static final int MAX_ENTRIES = 8192;
    private static final String INSERT = """
    // LIVING WIND OPTIONAL SHADER BRIDGE (v0.3.0)
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
    """ + "\n";

    private LivingWindShaderAutoPatcher() {}

    public static void patchInstalledPacks(Path shaderpacks) {
        if (shaderpacks == null || !Files.isDirectory(shaderpacks)) return;
        Path config = shaderpacks.getParent().resolve("config");
        if (isDisabled(config)) return;
        Path backups = config.resolve("livingwind").resolve("shader-backups");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(shaderpacks)) {
            for (Path pack : stream) {
                try {
                    if (Files.isSymbolicLink(pack)) continue;
                    if (Files.isRegularFile(pack) && pack.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        patchZip(pack, backups);
                    } else if (Files.isDirectory(pack)) {
                        patchFolder(pack, backups);
                    }
                } catch (Exception ex) {
                    System.err.println("[Living Wind] Auto shader patch skipped " + pack.getFileName() + ": " + ex);
                }
            }
        } catch (IOException ex) {
            System.err.println("[Living Wind] Could not examine shaderpacks: " + ex);
        }
    }

    // The TOML value is read without constructing NeoForge's client config: the
    // Euphoria bootstrap runs before NeoForge has loaded the config specs.
    private static boolean isDisabled(Path config) {
        Path file = config.resolve("livingwind-client.toml");
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.strip().matches("(?i)^autoPatchEuphoriaShaders\\s*=\\s*false\\s*(?:#.*)?$"))
                        return true;
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    /** Atomically modify both GLSL files, with a complete original ZIP backup. */
    public static boolean patchZip(Path zip, Path backups) throws IOException {
        if (Files.size(zip) > MAX_ZIP_BYTES) return false;
        java.util.Map<String, byte[]> replacements = new java.util.HashMap<>();
        try (ZipFile src = new ZipFile(zip.toFile(), ZipFile.OPEN_READ, Charset.forName("IBM850"))) {
            if (src.size() > MAX_ENTRIES) return false;
            for (String target : new String[]{TARGET, STORM}) {
                ZipEntry matching = null;
                for (var entries = src.entries(); entries.hasMoreElements();) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(target)) {
                        if (matching != null) return false; // Ambiguous nested shader packs
                        matching = entry;
                    }
                }
                if (matching == null || matching.getSize() < 1 || matching.getSize() > MAX_SHADER_BYTES) return false;
                byte[] input;
                try (InputStream in = src.getInputStream(matching)) {
                    input = in.readNBytes((int) MAX_SHADER_BYTES + 1);
                }
                if (input.length > MAX_SHADER_BYTES) return false;
                byte[] replacement = target.equals(TARGET) ? patchSource(input) : patchStormSource(input);
                if (replacement == null) {
                    String existing = new String(input, StandardCharsets.UTF_8);
                    if (!existing.contains(target.equals(TARGET) ? MARKER : STORM_MARKER)) return false;
                } else replacements.put(matching.getName(), replacement);
            }
        }
        if (replacements.isEmpty()) return false;
        Files.createDirectories(backups);
        String location = shortHash(zip.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
        Path backup = backups.resolve(location + "-" + sha256(zip).substring(0, 20) + ".zip");
        if (!Files.exists(backup)) Files.copy(zip, backup);

        Path temp = Files.createTempFile(zip.getParent(), ".livingwind-", ".tmp");
        try {
            try (ZipFile src = new ZipFile(zip.toFile(), ZipFile.OPEN_READ, Charset.forName("IBM850"));
                 OutputStream fileOut = Files.newOutputStream(temp);
                 ZipOutputStream out = new ZipOutputStream(fileOut)) {
                if (src.getComment() != null) out.setComment(src.getComment());
                for (var entries = src.entries(); entries.hasMoreElements();) {
                    ZipEntry entry = entries.nextElement();
                    byte[] replacement = replacements.get(entry.getName());
                    ZipEntry replacementEntry = replacement == null ? new ZipEntry(entry) : new ZipEntry(entry.getName());
                    if (replacement != null) {
                        replacementEntry.setMethod(ZipEntry.DEFLATED);
                        replacementEntry.setTime(entry.getTime());
                    }
                    out.putNextEntry(replacementEntry);
                    if (!entry.isDirectory()) {
                        if (replacement != null) out.write(replacement);
                        else try (InputStream in = src.getInputStream(entry)) { in.transferTo(out); }
                    }
                    out.closeEntry();
                }
            }
            try (ZipFile validated = new ZipFile(temp.toFile())) {
                if (validated.size() < 2) throw new IOException("Patched shader archive is empty");
                for (var replace : replacements.entrySet()) {
                    ZipEntry entry = validated.getEntry(replace.getKey());
                    if (entry == null) throw new IOException("Patched GLSL entry missing");
                    try (InputStream in = validated.getInputStream(entry)) {
                        if (!java.util.Arrays.equals(in.readAllBytes(), replace.getValue()))
                            throw new IOException("Patched GLSL mismatch");
                    }
                }
            }
            replaceAtomically(temp, zip);
            System.out.println("[Living Wind] Updated Complementary shader " + zip.getFileName()
                + " (backup: " + backup.getFileName() + ")");
            return true;
        } finally { Files.deleteIfExists(temp); }
    }

    private static void patchFolder(Path pack, Path backups) throws IOException {
        Path root = pack;
        if (!Files.isRegularFile(root.resolve(TARGET)) || !Files.isRegularFile(root.resolve(STORM))) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(pack)) {
                int candidates = 0;
                for (Path child : children) {
                    if (Files.isDirectory(child) && !Files.isSymbolicLink(child)
                        && Files.isRegularFile(child.resolve(TARGET)) && Files.isRegularFile(child.resolve(STORM))) {
                        root = child; candidates++;
                    }
                }
                if (candidates != 1) return;
            }
        }
        Path wave = root.resolve(TARGET), storm = root.resolve(STORM);
        if (Files.isSymbolicLink(wave) || Files.isSymbolicLink(storm)) return;
        if (Files.size(wave) > MAX_SHADER_BYTES || Files.size(storm) > MAX_SHADER_BYTES) return;
        byte[] originalW = Files.readAllBytes(wave), originalS = Files.readAllBytes(storm);
        byte[] resultW = patchSource(originalW), resultS = patchStormSource(originalS);
        if (resultW == null && !new String(originalW, StandardCharsets.UTF_8).contains(MARKER)) return;
        if (resultS == null && !new String(originalS, StandardCharsets.UTF_8).contains(STORM_MARKER)) return;
        if (resultW == null && resultS == null) return;
        Files.createDirectories(backups);
        for (Path originalFile : new Path[]{wave, storm}) {
            byte[] bytes = originalFile.equals(wave) ? originalW : originalS;
            Path backup = backups.resolve(shortHash(originalFile.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8))
                + "-" + sha256(bytes).substring(0, 20) + ".glsl");
            if (!Files.exists(backup)) Files.copy(originalFile, backup);
        }
        if (resultW != null) replaceFile(wave, resultW);
        if (resultS != null) replaceFile(storm, resultS);
        System.out.println("[Living Wind] Updated Complementary shader folder " + pack.getFileName());
    }

    private static void replaceFile(Path file, byte[] data) throws IOException {
        Path temp = Files.createTempFile(file.getParent(), ".livingwind-", ".tmp");
        try { Files.write(temp, data); replaceAtomically(temp, file); }
        finally { Files.deleteIfExists(temp); }
    }

    /** Both plain Complementary and Euphoria use this identical foliage interface. */
    public static byte[] patchSource(byte[] original) {
        String shader = new String(original, StandardCharsets.UTF_8);
        if (shader.contains(MARKER)) return null;
        if (!shader.contains("vec3 GetRawWave(in vec3 pos, float wind)")) return null;
        String anchor = "void DoWave_Foliage(inout vec3 playerPos, vec3 worldPos, float waveMult) {";
        if (count(shader, anchor) != 1) return null;
        if (shader.contains(LEGACY_MARKER)) {
            // Upgrade our previous Euphoria patch in place, without restoring the
            // player's original shader ZIP or touching Euphoria's other changes.
            int start = shader.indexOf(LEGACY_MARKER);
            int end = shader.indexOf(LEGACY_END, start);
            if (end < 0 || shader.indexOf(LEGACY_MARKER, start + 1) >= 0) return null;
            end += LEGACY_END.length();
            shader = shader.substring(0, start) + INSERT.stripTrailing() + shader.substring(end);
            return shader.getBytes(StandardCharsets.UTF_8);
        }
        for (String fn : new String[]{"DoWave_Foliage", "DoWave_Leaves"}) {
            int start = shader.indexOf("void " + fn + "(");
            if (start < 0) return null;
            int end = shader.indexOf("\nvoid ", start + 5);
            if (end < 0) return null;
            String block = shader.substring(start, end);
            String needle = "vec3 wave = GetWave(worldPos, 170.0);";
            if (count(block, needle) != 1) return null;
            shader = shader.substring(0, start) + block.replace(needle,
                "vec3 wave = LivingWindGetWave(worldPos, 170.0);") + shader.substring(end);
        }
        return shader.replace(anchor, INSERT + anchor).getBytes(StandardCharsets.UTF_8);
    }

    /** Moves the noise sampling only; never alters storm density/height or MCWind. */
    public static byte[] patchStormSource(byte[] original) {
        String shader = new String(original, StandardCharsets.UTF_8);
        if (shader.contains(STORM_MARKER) || !shader.contains("vec4 GetNetherStorm(")) return null;
        String anchor = "vec4 GetNetherStorm(";
        String coord = "tracePosM.y += tracePosM.x;";
        if (count(shader, anchor) != 1 || count(shader, coord) != 1 || !shader.contains("Noise3D(tracePosM")) return null;
        // No additional raymarch iterations. Preserve EP's MCWind-specific turbulence.
        shader = shader.replace(anchor, STORM_INSERT + "\n" + anchor);
        shader = shader.replace(coord, coord + "\n        // Wind moves entire fog formations in world-space X/Z, preserving turbulence."
            + "\n        tracePosM.xz -= vec2(livingWindStormOffsetX, livingWindStormOffsetZ)"
            + " * (livingWindStormActive > 0.5 ? 0.001 : 0.0);");
        return shader.getBytes(StandardCharsets.UTF_8);
    }

    private static int count(String s, String part) {
        int n=0, index=0;
        while ((index=s.indexOf(part, index)) >= 0) { n++; index+=part.length(); }
        return n;
    }

    private static void replaceAtomically(Path temp, Path destination) throws IOException {
        try {
            Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String shortHash(byte[] b) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(b);
            return HexFormat.of().formatHex(digest, 0, 10);
        } catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }

    private static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest(bytes)); }
    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = createDigest();
            byte[] chunk = new byte[32768];
            int n;
            while ((n=in.read(chunk)) != -1) md.update(chunk, 0, n);
            return HexFormat.of().formatHex(md.digest());
        }
    }
    private static byte[] digest(byte[] bytes) { return createDigest().digest(bytes); }
    private static MessageDigest createDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }
}
