package com.macatmovie.livingwind.client.shader;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Runtime-only Complementary GLSL integration, with cautious repair of ZIPs
 * and extracted shader sources edited by Living Wind v1.3.4/v1.3.5.
 *
 * NEVER writes a shaderpatch to a user's installed shaderpack. Only restoration
 * of a byte-for-byte verified historic backup may change an installed pack.
 * The runtime hook executes after Iris/Oculus reads a shader file, but before
 * parsing #includes and compiling the shader. The original input stays on disk.
 */
public final class LivingWindShaderBridge {
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


    private static final Set<String> REPORTED_RUNTIME_FILES = ConcurrentHashMap.newKeySet();
    private static volatile Path gameDirectory;

    /** Can be set during client initialization, but ZIPs may load before then. */
    public static void setGameDirectory(Path game) {
        if (game != null) gameDirectory = game.toAbsolutePath().normalize();
    }

    private static final long MAX_RESTORE_ENTRY_BYTES = 256L * 1024 * 1024;

    private LivingWindShaderBridge() {}

    /** Called by optional Iris/Oculus IncludeGraph mixins after each source read. */
    public static String patchLoadedSource(Path path, String source) {
        if (path == null || source == null || source.length() > MAX_SHADER_BYTES) return source;
        String normalized = path.toString().replace('\\', '/');
        boolean wave = normalized.endsWith(TARGET);
        boolean storm = normalized.endsWith(STORM);
        if (!wave && !storm) return source;
        if (isDisabled(resolveConfigDirectory(path))) return source;
        byte[] patched = wave ? patchSource(source.getBytes(StandardCharsets.UTF_8))
                              : patchStormSource(source.getBytes(StandardCharsets.UTF_8));
        if (patched == null) return source;
        if (REPORTED_RUNTIME_FILES.add(normalized)) {
            System.out.println("[Living Wind] Runtime-only Complementary wind shader: " + normalized);
        }
        return new String(patched, StandardCharsets.UTF_8);
    }

    private static Path findShaderpacksRoot(Path path) {
        for (Path parent = path.toAbsolutePath(); parent != null; parent = parent.getParent()) {
            if (parent.getFileName() != null && parent.getFileName().toString().equalsIgnoreCase("shaderpacks"))
                return parent;
        }
        return null;
    }

    private static Path resolveConfigDirectory(Path shaderFile) {
        // Unpacked packs have a normal OS path containing shaderpacks.
        Path shaderpacks = findShaderpacksRoot(shaderFile);
        if (shaderpacks != null) return shaderpacks.getParent().resolve("config");

        // ZIP file systems expose virtual paths such as /shaders/lib/... which
        // do NOT contain the host's shaderpacks folder. Ask Minecraft for its
        // actual game directory, including launchers with custom instances.
        Path directory = gameDirectory;
        if (directory == null) {
            try {
                Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
                Object instance = mc.getMethod("getInstance").invoke(null);
                Object file = mc.getField("gameDirectory").get(instance);
                if (file instanceof java.io.File game) {
                    directory = game.toPath().toAbsolutePath().normalize();
                    gameDirectory = directory;
                }
            } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                // Unit tests/nonstandard bootstrap: use the instance working dir.
            }
        }
        if (directory == null) directory = Path.of(System.getProperty("user.dir", "."));
        return directory.resolve("config");
    }

    // Retain the old config key to honor existing user installations.
    private static boolean isDisabled(Path config) {
        if (config == null) return false;
        Path file = config.resolve("livingwind-client.toml");
        try {
            if (Files.isRegularFile(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.strip().matches("(?i)^autoPatchEuphoriaShaders\\s*=\\s*false\\s*(?:#.*)?$"))
                        return true;
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    /**
     * Repair only archives/files demonstrably modified by the old auto-patcher.
     * If anything differs from its exact expected output, leave it untouched.
     * Backups are retained after recovery. Safe to call more than once.
     */
    public static synchronized void recoverPreviouslyPatchedPacks(Path shaderpacks) {
        if (shaderpacks == null || !Files.isDirectory(shaderpacks) || Files.isSymbolicLink(shaderpacks)) return;
        Path game = shaderpacks.getParent();
        if (game == null) return;
        setGameDirectory(game);
        Path backups = game.resolve("config/livingwind/shader-backups");
        if (!Files.isDirectory(backups) || Files.isSymbolicLink(backups)) return;
        try (DirectoryStream<Path> packs = Files.newDirectoryStream(shaderpacks)) {
            for (Path pack : packs) {
                if (Files.isSymbolicLink(pack)) continue;
                try {
                    if (Files.isRegularFile(pack, LinkOption.NOFOLLOW_LINKS)
                        && pack.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        restoreZipIfSafe(pack, backups);
                    } else if (Files.isDirectory(pack, LinkOption.NOFOLLOW_LINKS)) {
                        restoreFolderIfSafe(pack, backups);
                    }
                } catch (Exception ex) {
                    System.err.println("[Living Wind] Shader backup recovery skipped " + pack.getFileName() + ": " + ex);
                }
            }
        } catch (IOException ex) {
            System.err.println("[Living Wind] Cannot inspect shaderpacks for legacy recovery: " + ex);
        }
    }

    private static void restoreZipIfSafe(Path zip, Path backups) throws IOException {
        if (Files.size(zip) > MAX_ZIP_BYTES) return;
        String prefix = shortHash(zip.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8)) + "-";
        String originalCurrentHash = null;
        Path selected = null;
        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(backups, prefix + "*.zip")) {
            for (Path backup : candidates) {
                if (!Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(backup)
                    || Files.size(backup) > MAX_ZIP_BYTES) continue;
                String filename = backup.getFileName().toString();
                String suffix = filename.substring(prefix.length(), filename.length() - 4);
                if (!suffix.matches("[0-9a-f]{20}") || !sha256(backup).startsWith(suffix)) continue;
                String hashBeforeVerification = sha256(zip);
                if (!isOnlyLegacyZipPatch(zip, backup) || !sha256(zip).equals(hashBeforeVerification)) continue;
                if (selected != null) {
                    System.err.println("[Living Wind] Several backups match " + zip.getFileName()
                        + "; not choosing between them automatically.");
                    return;
                }
                selected = backup;
                originalCurrentHash = hashBeforeVerification;
            }
        }
        if (selected == null) {
            if (hasLegacyShaderMarkers(zip))
                System.err.println("[Living Wind] Cannot safely restore modified shader ZIP " + zip.getFileName()
                    + ". No byte-verified matching backup was found. Reinstall the original shaderpack"
                    + " (backups are retained in config/livingwind/shader-backups).");
            return;
        }
        Path temp = Files.createTempFile(zip.getParent(), ".livingwind-recover-", ".tmp");
        try {
            Files.copy(selected, temp, StandardCopyOption.REPLACE_EXISTING);
            if (!sha256(temp).equals(sha256(selected)) || !sha256(zip).equals(originalCurrentHash)) return;
            replaceAtomically(temp, zip);
            System.out.println("[Living Wind] Restored original shader ZIP " + zip.getFileName()
                + " from verified v1.3.x backup " + selected.getFileName());
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static boolean hasLegacyShaderMarkers(Path zip) {
        try (ZipFile src = new ZipFile(zip.toFile())) {
            if (src.size() > MAX_ENTRIES) return false;
            for (Enumeration<? extends ZipEntry> entries = src.entries(); entries.hasMoreElements();) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || (!e.getName().endsWith(TARGET) && !e.getName().endsWith(STORM))
                    || e.getSize() < 0 || e.getSize() > MAX_SHADER_BYTES) continue;
                try (InputStream in = src.getInputStream(e)) {
                    String source = new String(in.readNBytes((int) MAX_SHADER_BYTES + 1), StandardCharsets.UTF_8);
                    if (source.contains(MARKER) || source.contains(STORM_MARKER)) return true;
                }
            }
        } catch (IOException ex) {
            System.err.println("[Living Wind] Cannot inspect shader markers in " + zip.getFileName() + ": " + ex);
        }
        return false;
    }

    /** Compare every entry, not just shader markers: preserve all user modifications. */
    private static boolean isOnlyLegacyZipPatch(Path current, Path backup) throws IOException {
        try (ZipFile a = new ZipFile(current.toFile());
             ZipFile b = new ZipFile(backup.toFile(), ZipFile.OPEN_READ, Charset.forName("IBM850"))) {
            if (a.size() != b.size() || a.size() > MAX_ENTRIES || !Objects.equals(a.getComment(), b.getComment()))
                return false;
            Map<String, ZipEntry> actual = new HashMap<>();
            Set<String> names = new HashSet<>();
            for (Enumeration<? extends ZipEntry> entries = a.entries(); entries.hasMoreElements();) {
                ZipEntry e = entries.nextElement();
                if (!names.add(e.getName())) return false;
                actual.put(e.getName(), e);
            }
            boolean changed = false;
            long processed = 0;
            names.clear();
            for (Enumeration<? extends ZipEntry> entries = b.entries(); entries.hasMoreElements();) {
                ZipEntry original = entries.nextElement();
                if (!names.add(original.getName())) return false;
                ZipEntry modified = actual.get(original.getName());
                if (modified == null || original.isDirectory() != modified.isDirectory()
                    || original.getTime() != modified.getTime()) return false;
                boolean wave = original.getName().endsWith(TARGET);
                boolean storm = original.getName().endsWith(STORM);
                if ((wave || storm) && !original.isDirectory()) {
                    if (original.getSize() < 0 || original.getSize() > MAX_SHADER_BYTES
                        || modified.getSize() < 0 || modified.getSize() > MAX_SHADER_BYTES) return false;
                    byte[] originalBytes;
                    try (InputStream in = b.getInputStream(original)) { originalBytes = in.readNBytes((int) MAX_SHADER_BYTES + 1); }
                    if (originalBytes.length > MAX_SHADER_BYTES) return false;
                    byte[] expected = wave ? patchSource(originalBytes) : patchStormSource(originalBytes);
                    if (expected != null) {
                        if (modified.getMethod() != ZipEntry.DEFLATED || modified.getSize() != expected.length
                            || !equalsStreamBytes(a.getInputStream(modified), expected)) return false;
                        changed = true;
                        processed += expected.length;
                        continue;
                    }
                }
                if (original.getMethod() != modified.getMethod()
                    || !Objects.equals(original.getComment(), modified.getComment())
                    || !Arrays.equals(original.getExtra(), modified.getExtra())) return false;
                if (original.getSize() < 0 || modified.getSize() != original.getSize()
                    || original.getSize() > MAX_RESTORE_ENTRY_BYTES - processed) return false;
                processed += original.getSize();
                if (!streamsEqual(b.getInputStream(original), a.getInputStream(modified), original.getSize())) return false;
            }
            return changed;
        }
    }

    private static boolean equalsStreamBytes(InputStream source, byte[] expected) throws IOException {
        try (InputStream input = source) {
            return Arrays.equals(input.readNBytes(expected.length + 1), expected);
        }
    }

    private static boolean streamsEqual(InputStream first, InputStream second, long expectedLength) throws IOException {
        try (InputStream a = first; InputStream b = second) {
            byte[] x = new byte[8192], y = new byte[8192];
            long read = 0;
            while (true) {
                int n = a.read(x);
                if (n < 0) return read == expectedLength && b.read() < 0;
                read += n;
                if (read > expectedLength || b.readNBytes(y, 0, n) != n
                    || !Arrays.equals(x, 0, n, y, 0, n)) return false;
            }
        }
    }

    private static void restoreFolderIfSafe(Path pack, Path backups) throws IOException {
        Path root = pack;
        if (!Files.isRegularFile(root.resolve(TARGET)) || !Files.isRegularFile(root.resolve(STORM))) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(pack)) {
                int found = 0;
                for (Path child : children) {
                    if (!Files.isSymbolicLink(child) && Files.isDirectory(child)
                        && Files.isRegularFile(child.resolve(TARGET)) && Files.isRegularFile(child.resolve(STORM))) {
                        root = child;
                        found++;
                    }
                }
                if (found != 1) return;
            }
        }
        for (Path file : new Path[]{root.resolve(TARGET), root.resolve(STORM)}) {
            if (hasSymlinkBetween(pack, file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.size(file) > MAX_SHADER_BYTES) continue;
            byte[] current = Files.readAllBytes(file);
            if (!new String(current, StandardCharsets.UTF_8).contains(file.endsWith("wavingBlocks.glsl") ? MARKER : STORM_MARKER))
                continue;
            String prefix = shortHash(file.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8)) + "-";
            Path selected = null;
            try (DirectoryStream<Path> candidates = Files.newDirectoryStream(backups, prefix + "*.glsl")) {
                for (Path backup : candidates) {
                    if (!Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(backup)
                        || Files.size(backup) > MAX_SHADER_BYTES) continue;
                    String filename = backup.getFileName().toString();
                    String suffix = filename.substring(prefix.length(), filename.length() - 5);
                    if (!suffix.matches("[0-9a-f]{20}") || !sha256(backup).startsWith(suffix)) continue;
                    byte[] original = Files.readAllBytes(backup);
                    byte[] expected = file.getFileName().toString().equals("wavingBlocks.glsl")
                        ? patchSource(original) : patchStormSource(original);
                    if (expected == null || !Arrays.equals(expected, current)) continue;
                    if (selected != null) {
                        System.err.println("[Living Wind] Ambiguous GLSL backups for " + file + "; not restoring.");
                        selected = null;
                        break;
                    }
                    selected = backup;
                }
            }
            if (selected == null) {
                System.err.println("[Living Wind] Cannot safely restore modified shader GLSL " + file
                    + ". No verified original backup matches; reinstall the shaderpack if necessary.");
            } else {
                Path temp = Files.createTempFile(file.getParent(), ".livingwind-recover-", ".tmp");
                try {
                    Files.copy(selected, temp, StandardCopyOption.REPLACE_EXISTING);
                    if (!Arrays.equals(current, Files.readAllBytes(file))
                        || !sha256(temp).equals(sha256(selected))) continue;
                    replaceAtomically(temp, file);
                    System.out.println("[Living Wind] Restored original shader GLSL " + file
                        + " from verified legacy backup " + selected.getFileName());
                } finally { Files.deleteIfExists(temp); }
            }
        }
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


    private static boolean hasSymlinkBetween(Path root, Path file) {
        for (Path cursor = file; cursor != null; cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) return true;
            if (cursor.equals(root)) return false;
        }
        return true;
    }

    private static void replaceAtomically(Path temp, Path destination) throws IOException {
        try {
            Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String shortHash(byte[] bytes) {
        return HexFormat.of().formatHex(digest(bytes), 0, 10);
    }
    private static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest(bytes)); }
    private static String sha256(Path file) throws IOException {
        MessageDigest hash = createDigest();
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] chunk = new byte[32768];
            int n;
            while ((n = stream.read(chunk)) != -1) hash.update(chunk, 0, n);
        }
        return HexFormat.of().formatHex(hash.digest());
    }
    private static byte[] digest(byte[] bytes) { return createDigest().digest(bytes); }
    private static MessageDigest createDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }
}
