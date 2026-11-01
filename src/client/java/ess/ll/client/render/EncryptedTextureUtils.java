package ess.ll.client.render;

import ess.ll.client.resource.ResourceByteCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class EncryptedTextureUtils {
    private static final byte[] TEXTURE_HEADER = new byte[] { 72, 88, 84, 88, 1 };

    private EncryptedTextureUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static InputStream decodeStream(InputStream input) throws IOException {
        if (input == null) {
            return null;
        }
        PushbackInputStream input2 = new PushbackInputStream(input, TEXTURE_HEADER.length);
        byte[] var2 = new byte[TEXTURE_HEADER.length];
        int var3 = input2.readNBytes(var2, 0, var2.length);
        if (var3 == TEXTURE_HEADER.length && hasTextureHeader(var2)) {
            byte[] var4;
            try (PushbackInputStream input3 = input2) {
                var4 = input3.readAllBytes();
            }
            return new ByteArrayInputStream(ResourceByteCodec.decode(var4));
        } else {
            if (var3 > 0) {
                input2.unread(var2, 0, var3);
            }
            return input2;
        }
    }

    public static byte[] decodeBytes(byte[] var0) {
        if (var0 != null && var0.length != 0) {
            int var1 = hasTextureHeader(var0) ? TEXTURE_HEADER.length : 0;
            return ResourceByteCodec.decode(var0, var1, var0.length - var1);
        } else {
            throw new IllegalArgumentException("Encrypted texture bytes are empty");
        }
    }

    public static boolean isEncryptedPath(String var0) {
        return var0 != null && var0.toLowerCase(Locale.ROOT).endsWith(".crypt");
    }

    public static Path encodeFile(Path path, Path path2) throws IOException {
        byte[] var2 = Files.readAllBytes(path);
        byte[] var3 = encodeBytes(var2);
        Path parent = path2.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path2, var3);
        return path2;
    }

    public static Path encodeFile(Path path) throws IOException {
        return encodeFile(path, encryptedPath(path));
    }

    public static boolean hasTextureHeader(byte[] var0) {
        if (var0 != null && var0.length >= TEXTURE_HEADER.length) {
            for (int var1 = 0; var1 < TEXTURE_HEADER.length; var1++) {
                if (var0[var1] != TEXTURE_HEADER[var1]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static Path encryptedPath(Path path) {
        String var1 = path.getFileName().toString();
        if (var1.endsWith(".crypt")) {
            return path.resolveSibling(var1);
        }
        int var2 = var1.lastIndexOf(46);
        if (var2 > 0) {
            var1 = var1.substring(0, var2) + ".crypt";
        } else {
            var1 = var1 + ".crypt";
        }
        return path.resolveSibling(var1);
    }

    public static byte[] encodeBytes(byte[] var0) {
        if (var0 != null && var0.length != 0) {
            byte[] var1 = ResourceByteCodec.encode(var0);
            byte[] var2 = new byte[TEXTURE_HEADER.length + var1.length];
            System.arraycopy(TEXTURE_HEADER, 0, var2, 0, TEXTURE_HEADER.length);
            System.arraycopy(var1, 0, var2, TEXTURE_HEADER.length, var1.length);
            return var2;
        } else {
            throw new IllegalArgumentException("Source texture bytes are empty");
        }
    }
}
