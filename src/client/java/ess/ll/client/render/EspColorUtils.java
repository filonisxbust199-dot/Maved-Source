package ess.ll.client.render;

public final class EspColorUtils {
    private static int defaultOverlayColor() {
        return -402653185;
    }

    public static int ensureOverlayAlphaBit(int var0) {
        int var1 = var0 != 0 ? var0 : defaultOverlayColor();
        return var1 | 134217728;
    }

    private EspColorUtils() {
    }

    public static int packOverlayFlags(int var0, boolean var1, int var2) {
        int var3 = (var1 ? 224 : 160) | var0 & 7;
        return var3 << 24 | var2 & 16777215;
    }
}
