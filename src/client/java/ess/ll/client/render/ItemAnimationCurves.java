package ess.ll.client.render;

public final class ItemAnimationCurves {
    public static double swingBezier(double var0) {
        double var2 = var0 * var0;
        double sx = var2 * var0;
        return 3.0 * var0 * (1.0 - var0) * (1.0 - var0) * 0.44 + 3.0 * var2 * (1.0 - var0) * 1.0 + sx;
    }

    public static double recoveryBezier(double var0) {
        double var2 = var0 * var0;
        double var4 = var2 * var0;
        return 3.0 * var0 * (1.0 - var0) * (1.0 - var0) * 0.44 + 3.0 * var2 * (1.0 - var0) * 0.94 + var4;
    }

    private ItemAnimationCurves() {
    }
}
