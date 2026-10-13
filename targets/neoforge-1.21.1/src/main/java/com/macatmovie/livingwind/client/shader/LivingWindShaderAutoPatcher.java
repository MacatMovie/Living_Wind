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
 * User-local, opt-out, conservative auto-patcher for the exact Euphoria
 * r5.9.3 / 1.10.5 foliage GLSL the Living Wind bridge was tested against.
 *
 * Important: this changes an *installed* shader archive, not a shader embedded
 * in or distributed with Living Wind. Full byte-exact backups are made before
 * replacing ZIPs. Unrecognized versions and packs are left untouched.
 *
 * Invoked after Euphoria's startup scanner and after Euphoria generates a pack.
 * Uses only Java NIO: no Euphoria or Iris linkage at class-loading time.
 */
public final class LivingWindShaderAutoPatcher {
    private static final String TARGET = "shaders/lib/materials/materialMethods/wavingBlocks.glsl";
    private static final String MARKER = "// LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)";
    private static final long MAX_ZIP_BYTES = 128L * 1024 * 1024;
    private static final long MAX_SHADER_BYTES = 1024L * 1024;
    private static final int MAX_ENTRIES = 8192;
    private static final String INSERT = """
    // LIVING WIND OPTIONAL SHADER BRIDGE (v0.2.9)
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

    public static boolean patchZip(Path zip, Path backups) throws IOException {
        if (Files.size(zip) > MAX_ZIP_BYTES) return false;
        String targetName = null;
        byte[] patchedBytes;
        try (ZipFile original = new ZipFile(zip.toFile(), ZipFile.OPEN_READ, Charset.forName("IBM850"))) {
            int matches = 0;
            for (var entries = original.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(TARGET) && !entry.isDirectory()) {
                    targetName = entry.getName();
                    matches++;
                }
            }
            if (matches != 1 || original.size() > MAX_ENTRIES) return false;
            ZipEntry target = original.getEntry(targetName);
            if (target.getSize() < 1 || target.getSize() > MAX_SHADER_BYTES) return false;
            byte[] source;
            try (InputStream in = original.getInputStream(target)) {
                source = in.readNBytes((int) MAX_SHADER_BYTES + 1);
            }
            if (source.length > MAX_SHADER_BYTES) return false;
            patchedBytes = patchSource(source);
            if (patchedBytes == null) return false;
        }

        // Reserve a complete byte-exact local backup BEFORE altering the source.
        Files.createDirectories(backups);
        String hash = sha256(zip);
        String location = shortHash(zip.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
        Path backup = backups.resolve(location + "-" + hash.substring(0, 20) + ".zip");
        if (!Files.exists(backup)) Files.copy(zip, backup);

        Path temp = Files.createTempFile(zip.getParent(), ".livingwind-", ".tmp");
        try {
            try (ZipFile src = new ZipFile(zip.toFile(), ZipFile.OPEN_READ, Charset.forName("IBM850"));
                 OutputStream fileOut = Files.newOutputStream(temp);
                 ZipOutputStream out = new ZipOutputStream(fileOut)) {
                String comment = src.getComment();
                if (comment != null) out.setComment(comment);
                for (var entries = src.entries(); entries.hasMoreElements();) {
                    ZipEntry originalEntry = entries.nextElement();
                    ZipEntry newEntry;
                    if (originalEntry.getName().equals(targetName)) {
                        // Fresh DEFLATED entry: an originally STORED GLSL entry
                        // can change its size/CRC after the insertion.
                        newEntry = new ZipEntry(originalEntry.getName());
                        newEntry.setMethod(ZipEntry.DEFLATED);
                        newEntry.setTime(originalEntry.getTime());
                    } else {
                        newEntry = new ZipEntry(originalEntry);
                    }
                    out.putNextEntry(newEntry);
                    if (!originalEntry.isDirectory()) {
                        if (originalEntry.getName().equals(targetName)) {
                            out.write(patchedBytes);
                        } else {
                            try (InputStream stream = src.getInputStream(originalEntry)) {
                                stream.transferTo(out);
                            }
                        }
                    }
                    out.closeEntry();
                }
            }
            // Verify that the generated archive actually contains our code.
            try (ZipFile result = new ZipFile(temp.toFile())) {
                ZipEntry entry = result.getEntry(targetName);
                if (entry == null) throw new IOException("Waving shader disappeared during patch");
                String content;
                try (InputStream in = result.getInputStream(entry)) {
                    content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (!content.contains(MARKER) || count(content, "LivingWindGetWave(worldPos, 170.0)") != 2)
                    throw new IOException("Shader output verification failed");
            }
            replaceAtomically(temp, zip);
            System.out.println("[Living Wind] Auto-patched Euphoria shader " + zip.getFileName()
                + " (original backed up to " + backup.getFileName() + ")");
            return true;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void patchFolder(Path pack, Path backups) throws IOException {
        // Folder shader packs can have a single outer wrapper directory.
        Path target = pack.resolve(TARGET);
        if (!Files.isRegularFile(target)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(pack)) {
                for (Path child : children) {
                    if (Files.isDirectory(child) && Files.isRegularFile(child.resolve(TARGET))) {
                        if (Files.isRegularFile(target)) return;
                        target = child.resolve(TARGET);
                    }
                }
            }
        }
        if (!Files.isRegularFile(target) || Files.isSymbolicLink(target) || Files.size(target) > MAX_SHADER_BYTES) return;
        byte[] source = Files.readAllBytes(target);
        byte[] patched = patchSource(source);
        if (patched == null) return;
        Files.createDirectories(backups);
        Path backup = backups.resolve(shortHash(target.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8))
            + "-" + sha256(source).substring(0, 20) + ".glsl");
        if (!Files.exists(backup)) Files.copy(target, backup);
        Path temp = Files.createTempFile(target.getParent(), ".livingwind-", ".tmp");
        try {
            Files.write(temp, patched);
            replaceAtomically(temp, target);
            System.out.println("[Living Wind] Auto-patched Euphoria shader folder " + pack.getFileName());
        } finally { Files.deleteIfExists(temp); }
    }

    public static byte[] patchSource(byte[] original) {
        String shader = new String(original, StandardCharsets.UTF_8);
        if (shader.contains(MARKER) || !shader.contains("MCWIND_INTERNAL")
            || !shader.contains("vec3 GetRawWave(in vec3 pos, float wind)")) return null;
        String anchor = "void DoWave_Foliage(inout vec3 playerPos, vec3 worldPos, float waveMult) {";
        if (count(shader, anchor) != 1) return null;
        for (String fn : new String[]{"DoWave_Foliage", "DoWave_Leaves"}) {
            String function = "void " + fn + "(";
            int start = shader.indexOf(function);
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
