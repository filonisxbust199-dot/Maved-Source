package ess.ll.client.resource;

import ess.ll.client.render.EncryptedTextureUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EncryptedAssetArchive {
    private static final Map<String, String> ASSET_ALIASES = new HashMap<>();
    private static final Map<String, String> RESOURCE_ALIASES = new HashMap<>();
    private static final Set<String> ENCRYPTED_ASSETS = new HashSet<>();
    private static final List<String[]> ALIAS_ENTRIES = new ArrayList<>();

    public static boolean hasAliases() {
        return !ALIAS_ENTRIES.isEmpty();
    }

    public static boolean isEncryptedAsset(String var0, String var1) {
        return ENCRYPTED_ASSETS.contains(assetKey(var0, var1));
    }

    private static void loadIndex() {
        String var0 = "/assets/hex/0.bin";
        try {
            label77: {
                try (InputStream resourceAsStream = EncryptedAssetArchive.class.getResourceAsStream(var0)) {
                    if (resourceAsStream != null) {
                        String var3;
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(EncryptedTextureUtils.decodeStream(resourceAsStream), StandardCharsets.UTF_8))) {
                            while ((var3 = reader.readLine()) != null) {
                                parseIndexEntry(var3);
                            }
                            break label77;
                        }
                    }
                }
                return;
            }
        } catch (Throwable throwable) {
            ASSET_ALIASES.clear();
            RESOURCE_ALIASES.clear();
            ENCRYPTED_ASSETS.clear();
            ALIAS_ENTRIES.clear();
        }
    }

    private EncryptedAssetArchive() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static void parseIndexEntry(String var0) {
        int var1 = var0.indexOf(9);
        if (var1 > 0 && var1 != var0.length() - 1) {
            String var2 = var0.substring(0, var1);
            String var3 = var0.substring(var1 + 1);
            String[] var4 = splitAssetPath(var2);
            String[] var5 = splitAssetPath(var3);
            if (var4 != null && var5 != null) {
                RESOURCE_ALIASES.put("/" + var2, "/" + var3);
                ENCRYPTED_ASSETS.add(assetKey(var5[0], var5[1]));
                if (var4[0].equals(var5[0]) && isValidPath(var4[0]) && isValidPath(var4[1]) && isValidPath(var5[1])) {
                    ASSET_ALIASES.put(assetKey(var4[0], var4[1]), var5[1]);
                    ALIAS_ENTRIES.add(new String[] { var4[0], var4[1], var5[1] });
                }
            }
        }
    }

    public static List<String[]> getAliases() {
        return Collections.unmodifiableList(ALIAS_ENTRIES);
    }

    private static String assetKey(String var0, String var1) {
        return var0 + ":" + var1;
    }

    static {
        loadIndex();
    }

    private static boolean isValidPath(String var0) {
        for (int var1 = 0; var1 < var0.length(); var1++) {
            char var2 = var0.charAt(var1);
            boolean var3 = var2 >= 'a' && var2 <= 'z' || var2 >= '0' && var2 <= '9' || var2 == '/' || var2 == '.' || var2 == '_' || var2 == '-';
            if (!var3) {
                return false;
            }
        }
        return true;
    }

    public static String resolveResourcePath(String var0) {
        String var1 = RESOURCE_ALIASES.get(var0);
        return var1 == null ? var0 : var1;
    }

    private static String[] splitAssetPath(String var0) {
        if (!var0.startsWith("assets/")) {
            return null;
        }
        int var1 = var0.indexOf(47, "assets/".length());
        return var1 >= 0 && var1 != var0.length() - 1 ? new String[] { var0.substring("assets/".length(), var1), var0.substring(var1 + 1) } : null;
    }

    public static String resolveAssetPath(String var0, String var1) {
        return ASSET_ALIASES.get(assetKey(var0, var1));
    }
}
