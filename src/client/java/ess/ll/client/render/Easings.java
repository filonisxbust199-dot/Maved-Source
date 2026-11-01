package ess.ll.client.render;

public final class Easings {
    public static final EasingFunction LINEAR = var0 -> var0;
    public static final EasingFunction QUAD_IN = powerIn(2);
    public static final EasingFunction CLAMPED_BACK = clampedBackCurve(1.0);
    public static final EasingFunction QUAD_OUT = powerOut(2);
    public static final EasingFunction QUAD_IN_OUT = powerInOut(2.0);
    public static final EasingFunction CUBIC_IN = powerIn(3);
    public static final EasingFunction CUBIC_OUT = powerOut(3);
    public static final EasingFunction CUBIC_IN_OUT = powerInOut(3.0);
    public static final EasingFunction QUART_IN = powerIn(4);
    public static final EasingFunction QUART_OUT = powerOut(4);
    public static final EasingFunction QUART_IN_OUT = powerInOut(4.0);
    public static final EasingFunction QUINT_IN = powerIn(5);
    public static final EasingFunction QUINT_OUT = powerOut(5);
    public static final EasingFunction QUINT_IN_OUT = powerInOut(5.0);
    public static final EasingFunction SINE_IN = var0 -> 1.0 - Math.cos(var0 * Easings.HALF_COSINE_PI / 2.0);
    public static final EasingFunction SINE_OUT = var0 -> Math.sin(var0 * Easings.SINE_PI / 2.0);
    public static final EasingFunction SINE_IN_OUT = var0 -> -(Math.cos(Easings.COSINE_PI * var0) - 1.0) / 2.0;
    public static final EasingFunction CIRC_IN = var0 -> 1.0 - Math.sqrt(1.0 - Math.pow(var0, 2.0));
    public static final EasingFunction CIRC_OUT = var0 -> Math.sqrt(1.0 - Math.pow(var0 - 1.0, 2.0));
    public static final EasingFunction CIRC_IN_OUT = var0 -> var0 < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * var0, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow((-2.0) * var0 + 2.0, 2.0)) + 1.0) / 2.0;
    public static final EasingFunction ELASTIC_IN = var0 -> var0 != 0.0 && var0 != 1.0 ? Math.pow((-2.0), 10.0 * var0 - 10.0) * Math.sin((var0 * 10.0 - 10.75) * Easings.ELASTIC_IN_PERIOD) : var0;
    public static final EasingFunction ELASTIC_OUT = var0 -> var0 != 0.0 && var0 != 1.0 ? Math.pow(2.0, -10.0 * var0) * Math.sin((var0 * 10.0 - 0.75) * Easings.ELASTIC_OUT_PERIOD) + 1.0 : var0;

    public static final EasingFunction ELASTIC_IN_OUT = var0 -> {
        if (var0 != 0.0 && var0 != 1.0) {
            return var0 < 0.5 ? -(Math.pow(2.0, 20.0 * var0 - 10.0) * Math.sin((20.0 * var0 - 11.125) * Easings.ELASTIC_IN_OUT_PERIOD)) / 2.0 : Math.pow(2.0, -20.0 * var0 + 10.0) * Math.sin((20.0 * var0 - 11.125) * Easings.ELASTIC_OUT_IN_PERIOD) / 2.0 + 1.0;
        } else {
            return var0;
        }
    };

    public static final EasingFunction EXPO_IN = var0 -> var0 != 0.0 ? Math.pow(2.0, 10.0 * var0 - 10.0) : var0;
    public static final EasingFunction EXPO_OUT = var0 -> var0 != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * var0) : var0;

    public static final EasingFunction EXPO_IN_OUT = var0 -> {
        if (var0 != 0.0 && var0 != 1.0) {
            return var0 < 0.5 ? Math.pow(2.0, 20.0 * var0 - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * var0 + 10.0)) / 2.0;
        } else {
            return var0;
        }
    };

    public static final EasingFunction BACK_IN = var0 -> 2.70158 * Math.pow(var0, 3.0) - 1.70158 * Math.pow(var0, 2.0);
    public static final EasingFunction BACK_OUT = var0 -> 1.0 + 2.70158 * Math.pow(var0 - 1.0, 3.0) + 1.70158 * Math.pow(var0 - 1.0, 2.0);
    public static final EasingFunction BACK_IN_OUT = var0 -> var0 < 0.5 ? Math.pow(2.0 * var0, 2.0) * (7.189819 * var0 - 2.5949095) / 2.0 : (Math.pow(2.0 * var0 - 2.0, 2.0) * (3.5949095 * (var0 * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;

    public static final EasingFunction BOUNCE_OUT = var0 -> {
        double var2 = 7.5625;
        double var4 = 2.75;
        if (var0 < 1.0 / var4) {
            return var2 * Math.pow(var0, 2.0);
        } else if (var0 < 2.0 / var4) {
            return var2 * Math.pow(var0 - 1.5 / var4, 2.0) + 0.75;
        } else {
            return var0 < 2.5 / var4 ? var2 * Math.pow(var0 - 2.25 / var4, 2.0) + 0.9375 : var2 * Math.pow(var0 - 2.625 / var4, 2.0) + 0.984375;
        }
    };

    public static final EasingFunction BOUNCE_IN = var0 -> 1.0 - BOUNCE_OUT.apply(1.0 - var0);
    public static final EasingFunction BOUNCE_IN_OUT = var0 -> var0 < 0.5 ? (1.0 - BOUNCE_OUT.apply(1.0 - 2.0 * var0)) / 2.0 : (1.0 + BOUNCE_OUT.apply(2.0 * var0 - 1.0)) / 2.0;
    private static final double ELASTIC_IN_OUT_PERIOD = Math.PI * 4.0 / 9.0;
    private static final double ELASTIC_OUT_IN_PERIOD = Math.PI * 4.0 / 9.0;
    private static final double ELASTIC_OUT_PERIOD = Math.PI * 2.0 / 3.0;
    private static final double ELASTIC_IN_PERIOD = Math.PI * 2.0 / 3.0;
    private static final double COSINE_PI = Math.PI;
    private static final double SINE_PI = Math.PI;
    private static final double HALF_COSINE_PI = Math.PI;

    public static EasingFunction powerIn(double var0) {
        return var2 -> Math.pow(var2, var0);
    }

    private Easings() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static EasingFunction powerOut(int var0) {
        return powerOut((double) var0);
    }

    public static EasingFunction clampedBackCurve(double var0) {
        double var2 = var0;
        float var4 = 5.0F;
        return var3 -> Math.max(0.0, 1.0 + var4 * Math.pow(var2 - 1.0, 3.0) + var3 * Math.pow(var2 - 1.0, 2.0));
    }

    public static EasingFunction powerInOut(double var0) {
        return var2 -> var2 < 0.5 ? Math.pow(2.0, var0 - 1.0) * Math.pow(var2, var0) : 1.0 - Math.pow((-2.0) * var2 + 2.0, var0) / 2.0;
    }

    public static EasingFunction powerOut(double var0) {
        return var2 -> 1.0 - Math.pow(1.0 - var2, var0);
    }

    public static EasingFunction powerIn(int var0) {
        return powerIn((double) var0);
    }
}
