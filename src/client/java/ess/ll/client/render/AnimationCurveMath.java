package ess.ll.client.render;

public final class AnimationCurveMath {
    private static final double SINE_IN_PI = Math.PI;
    private static final double SINE_OUT_PI = Math.PI;
    private static final double SINE_IN_OUT_PI = Math.PI;
    private static final double ELASTIC_IN_PERIOD = Math.PI * 2.0 / 3.0;
    private static final double ELASTIC_OUT_PERIOD = Math.PI * 2.0 / 3.0;
    private static final double ELASTIC_IN_OUT_PERIOD = Math.PI * 4.0 / 9.0;

    public static double expoInOut(double var0) {
        if (var0 == 0.0) {
            return 0.0;
        } else if (var0 == 1.0) {
            return 1.0;
        } else {
            return var0 < 0.5 ? Math.pow(2.0, 20.0 * var0 - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * var0 + 10.0)) / 2.0;
        }
    }

    public static double quadInOut(double var0) {
        return var0 < 0.5 ? 2.0 * var0 * var0 : 1.0 - Math.pow((-2.0) * var0 + 2.0, 2.0) / 2.0;
    }

    public static double expoOut(double var0) {
        return var0 == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * var0);
    }

    public static double cubicIn(double var0) {
        return var0 * var0 * var0;
    }

    public static double circInOut(double var0) {
        return var0 < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * var0, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow((-2.0) * var0 + 2.0, 2.0)) + 1.0) / 2.0;
    }

    public static double quartInOut(double var0) {
        return var0 < 0.5 ? 8.0 * var0 * var0 * var0 * var0 : 1.0 - Math.pow((-2.0) * var0 + 2.0, 4.0) / 2.0;
    }

    public static double sineInOut(double var0) {
        return -(Math.cos(SINE_IN_OUT_PI * var0) - 1.0) / 2.0;
    }

    public static double backIn(double var0) {
        double var2 = 1.70158F;
        double var4 = var2 + 1.0;
        return var4 * var0 * var0 * var0 - var2 * var0 * var0;
    }

    public static double quartOut(double var0) {
        return 1.0 - Math.pow(1.0 - var0, 4.0);
    }

    public static double sineOut(double var0) {
        return Math.sin(var0 * SINE_OUT_PI / 2.0);
    }

    public static double quadOut(double var0) {
        return 1.0 - (1.0 - var0) * (1.0 - var0);
    }

    public static double quintIn(double var0) {
        return var0 * var0 * var0 * var0 * var0;
    }

    public static double backInOut(double var0) {
        double var2 = 1.70158F;
        double var4 = var2 * 1.525F;
        return var0 < 0.5 ? Math.pow(2.0 * var0, 2.0) * ((var4 + 1.0) * 2.0 * var0 - var4) / 2.0 : (Math.pow(2.0 * var0 - 2.0, 2.0) * ((var4 + 1.0) * (var0 * 2.0 - 2.0) + var4) + 2.0) / 2.0;
    }

    public static double quintOut(double var0) {
        return 1.0 - Math.pow(1.0 - var0, 5.0);
    }

    public static double expoIn(double var0) {
        return var0 == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * var0 - 10.0);
    }

    private AnimationCurveMath() {
    }

    public static double bounceInOut(double var0) {
        return var0 < 0.5 ? (1.0 - bounceOut(1.0 - 2.0 * var0)) / 2.0 : (1.0 + bounceOut(2.0 * var0 - 1.0)) / 2.0;
    }

    public static double bounceIn(double var0) {
        return 1.0 - bounceOut(1.0 - var0);
    }

    public static double quadIn(double var0) {
        return var0 * var0;
    }

    public static double bounceOut(double var0) {
        double var2 = 7.5625;
        double var4 = 2.75;
        if (var0 < 1.0 / var4) {
            return var2 * var0 * var0;
        } else if (var0 < 2.0 / var4) {
            var0 -= 1.5 / var4;
            return var2 * var0 * var0 + 0.75;
        } else if (var0 < 2.5 / var4) {
            var0 -= 2.25 / var4;
            return var2 * var0 * var0 + 0.9375;
        } else {
            var0 -= 2.625 / var4;
            return var2 * var0 * var0 + 0.984375;
        }
    }

    public static double quartIn(double var0) {
        return var0 * var0 * var0 * var0;
    }

    public static double backOut(double var0) {
        double var2 = 1.70158F;
        double var4 = var2 + 1.0;
        return 1.0 + var4 * Math.pow(var0 - 1.0, 3.0) + var2 * Math.pow(var0 - 1.0, 2.0);
    }

    public static double cubicInOut(double var0) {
        return var0 < 0.5 ? 4.0 * var0 * var0 * var0 : 1.0 - Math.pow((-2.0) * var0 + 2.0, 3.0) / 2.0;
    }

    public static double cubicOut(double var0) {
        return 1.0 - Math.pow(1.0 - var0, 3.0);
    }

    public static double elasticOut(double var0) {
        double var2 = ELASTIC_OUT_PERIOD;
        if (var0 == 0.0) {
            return 0.0;
        } else {
            return var0 == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * var0) * Math.sin((var0 * 10.0 - 0.75) * var2) + 1.0;
        }
    }

    public static double circIn(double var0) {
        return 1.0 - Math.sqrt(1.0 - Math.pow(var0, 2.0));
    }

    public static double circOut(double var0) {
        return Math.sqrt(1.0 - Math.pow(var0 - 1.0, 2.0));
    }

    public static double elasticInOut(double var0) {
        double var2 = ELASTIC_IN_OUT_PERIOD;
        if (var0 == 0.0) {
            return 0.0;
        } else if (var0 == 1.0) {
            return 1.0;
        } else {
            return var0 < 0.5 ? -(Math.pow(2.0, 20.0 * var0 - 10.0) * Math.sin((20.0 * var0 - 11.125) * var2)) / 2.0 : Math.pow(2.0, -20.0 * var0 + 10.0) * Math.sin((20.0 * var0 - 11.125) * var2) / 2.0 + 1.0;
        }
    }

    public static double quintInOut(double var0) {
        return var0 < 0.5 ? 16.0 * var0 * var0 * var0 * var0 * var0 : 1.0 - Math.pow((-2.0) * var0 + 2.0, 5.0) / 2.0;
    }

    public static double elasticIn(double var0) {
        double var2 = ELASTIC_IN_PERIOD;
        if (var0 == 0.0) {
            return 0.0;
        } else {
            return var0 == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * var0 - 10.0) * Math.sin((var0 * 10.0 - 10.75) * var2);
        }
    }

    public static double smoothstep(double var0) {
        return var0 * var0 * (3.0 - 2.0 * var0);
    }

    public static double sineIn(double var0) {
        return 1.0 - Math.cos(var0 * SINE_IN_PI / 2.0);
    }
}
