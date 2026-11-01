package ess.ll.client.render;

public class AnimationUtils {
    private AnimationUtils.DirectionalCurve curve;
    private long durationMillis;
    private long startTimeMillis;
    private long lastUpdateMillis;
    private double startValue;
    private double targetValue;
    private double value;
    private boolean complete;

    public Number getNumberValue() {
        return this.value;
    }

    public void restart() {
        this.startTimeMillis = System.currentTimeMillis();
        this.startValue = this.value;
        this.complete = false;
    }

    public AnimationUtils.DirectionalCurve getCurve() {
        return this.curve;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public void setStartValue(double startValue2) {
        this.startValue = startValue2;
    }

    public void setComplete(boolean complete2) {
        this.complete = complete2;
    }

    @Override
    public int hashCode() {
        byte var1 = 59;
        int var2 = 1;
        long durationMillis = this.getDurationMillis();
        var2 = var2 * 59 + (int) (durationMillis >>> 32 ^ durationMillis);
        long startTimeMillis = this.getStartTimeMillis();
        var2 = var2 * 59 + (int) (startTimeMillis >>> 32 ^ startTimeMillis);
        long lastUpdateMillis = this.getLastUpdateMillis();
        var2 = var2 * 59 + (int) (lastUpdateMillis >>> 32 ^ lastUpdateMillis);
        long var9 = Double.doubleToLongBits(this.getStartValue());
        var2 = var2 * 59 + (int) (var9 >>> 32 ^ var9);
        long var11 = Double.doubleToLongBits(this.getTargetValue());
        var2 = var2 * 59 + (int) (var11 >>> 32 ^ var11);
        long var13 = Double.doubleToLongBits(this.getValue());
        var2 = var2 * 59 + (int) (var13 >>> 32 ^ var13);
        var2 = var2 * 59 + (this.isComplete() ? 79 : 97);
        AnimationUtils.DirectionalCurve curve = this.getCurve();
        return var2 * 59 + (curve == null ? 43 : curve.hashCode());
    }

    public AnimationUtils(AnimationUtils.DirectionalCurve curve2, long durationMillis2) {
        this.curve = curve2;
        this.durationMillis = durationMillis2;
        this.startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object var1) {
        if (var1 == this) {
            return true;
        } else if (!(var1 instanceof AnimationUtils var2)) {
            return false;
        } else {
            if (!var2.canEqual(this)) {
                return false;
            }
            if (this.getDurationMillis() != var2.getDurationMillis()) {
                return false;
            }
            if (this.getStartTimeMillis() != var2.getStartTimeMillis()) {
                return false;
            }
            if (this.getLastUpdateMillis() != var2.getLastUpdateMillis()) {
                return false;
            }
            if (Double.compare(this.getStartValue(), var2.getStartValue()) != 0) {
                return false;
            }
            if (Double.compare(this.getTargetValue(), var2.getTargetValue()) != 0) {
                return false;
            }
            if (Double.compare(this.getValue(), var2.getValue()) != 0) {
                return false;
            }
            if (this.isComplete() != var2.isComplete()) {
                return false;
            }
            AnimationUtils.DirectionalCurve curve = this.getCurve();
            AnimationUtils.DirectionalCurve curve2 = var2.getCurve();
            return curve == null ? curve2 == null : curve.equals(curve2);
        }
    }

    public AnimationUtils(long var1) {
        this(AnimationUtils.DirectionalCurve.LINEAR, var1);
    }

    public void setDurationMillis(long durationMillis2) {
        this.durationMillis = durationMillis2;
    }

    public void setTargetValue(double targetValue2) {
        this.targetValue = targetValue2;
    }

    public void setStartTimeMillis(long startTimeMillis2) {
        this.startTimeMillis = startTimeMillis2;
    }

    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    private static double clampProgress(double var0) {
        return Math.max(0.0, Math.min(1.0, var0));
    }

    public long getDurationMillis() {
        return this.durationMillis;
    }

    public void setValue(double value2) {
        this.value = value2;
    }

    public void update(double value2) {
        this.lastUpdateMillis = System.currentTimeMillis();
        if (this.durationMillis <= 0L) {
            this.targetValue = value2;
            this.value = value2;
            this.startValue = value2;
            this.startTimeMillis = this.lastUpdateMillis;
            this.complete = true;
        } else {
            if (Double.compare(this.targetValue, value2) != 0) {
                this.targetValue = value2;
                this.restart();
            }
            double progress = this.getProgress();
            this.complete = progress >= 1.0;
            if (this.complete) {
                this.value = value2;
            } else {
                boolean var5 = value2 < this.startValue;
                double var6 = this.curve.apply(progress, var5);
                this.value = this.startValue + (value2 - this.startValue) * var6;
            }
        }
    }

    @Override
    public String toString() {
        return "AnimationUtils(easing=" + this.getCurve() + ", duration=" + this.getDurationMillis() + ", startTime=" + this.getStartTimeMillis() + ", millis=" + this.getLastUpdateMillis() + ", startValue=" + this.getStartValue() + ", destinationValue=" + this.getTargetValue() + ", value=" + this.getValue() + ", finished=" + this.isComplete() + ")";
    }

    protected boolean canEqual(Object var1) {
        return var1 instanceof AnimationUtils;
    }

    public float getFloatValue() {
        return (float) this.value;
    }

    public long getLastUpdateMillis() {
        return this.lastUpdateMillis;
    }

    public double getTargetValue() {
        return this.targetValue;
    }

    public double getStartValue() {
        return this.startValue;
    }

    public void update(Number var1) {
        if (var1 != null) {
            this.update(var1.doubleValue());
        }
    }

    public void setCurve(AnimationUtils.DirectionalCurve curve2) {
        this.curve = curve2;
    }

    public void setLastUpdateMillis(long lastUpdateMillis2) {
        this.lastUpdateMillis = lastUpdateMillis2;
    }

    public double getValue() {
        return this.value;
    }

    public double getProgress() {
        return this.durationMillis <= 0L ? 1.0 : clampProgress((double) (System.currentTimeMillis() - this.startTimeMillis) / this.durationMillis);
    }

    public enum DirectionalCurve {
        LINEAR(Easings.LINEAR),
        QUAD(Easings.QUAD_IN, Easings.QUAD_OUT),
        CUBIC(Easings.CUBIC_IN, Easings.CUBIC_OUT),
        QUART(Easings.QUART_IN, Easings.QUART_OUT),
        QUINT(Easings.QUINT_IN, Easings.QUINT_OUT),
        SINE(Easings.SINE_IN, Easings.SINE_OUT),
        EXPO(Easings.EXPO_IN, Easings.EXPO_OUT),
        CIRC(Easings.CIRC_IN, Easings.CIRC_OUT),
        BACK(Easings.BACK_IN, Easings.BACK_OUT),
        ELASTIC(Easings.ELASTIC_IN, Easings.ELASTIC_OUT),
        BOUNCE(Easings.BOUNCE_IN, Easings.BOUNCE_OUT),
        EASE_IN_QUAD(Easings.QUAD_IN),
        EASE_OUT_QUAD(Easings.QUAD_OUT),
        EASE_IN_OUT_QUAD(Easings.QUAD_IN_OUT),
        EASE_IN_CUBIC(Easings.CUBIC_IN),
        EASE_OUT_CUBIC(Easings.CUBIC_OUT),
        EASE_IN_OUT_CUBIC(Easings.CUBIC_IN_OUT),
        EASE_IN_QUART(Easings.QUART_IN),
        EASE_OUT_QUART(Easings.QUART_OUT),
        EASE_IN_OUT_QUART(Easings.QUART_IN_OUT),
        EASE_IN_QUINT(Easings.QUINT_IN),
        EASE_OUT_QUINT(Easings.QUINT_OUT),
        EASE_IN_OUT_QUINT(Easings.QUINT_IN_OUT),
        EASE_IN_SINE(Easings.SINE_IN),
        EASE_OUT_SINE(Easings.SINE_OUT),
        EASE_IN_OUT_SINE(Easings.SINE_IN_OUT),
        EASE_IN_EXPO(Easings.EXPO_IN),
        EASE_OUT_EXPO(Easings.EXPO_OUT),
        EASE_IN_OUT_EXPO(Easings.EXPO_IN_OUT),
        EASE_IN_CIRC(Easings.CIRC_IN),
        EASE_OUT_CIRC(Easings.CIRC_OUT),
        EASE_IN_OUT_CIRC(Easings.CIRC_IN_OUT),
        EASE_IN_BACK(Easings.BACK_IN),
        EASE_OUT_BACK(Easings.BACK_OUT),
        EASE_IN_OUT_BACK(Easings.BACK_IN_OUT),
        EASE_IN_ELASTIC(Easings.ELASTIC_IN),
        EASE_OUT_ELASTIC(Easings.ELASTIC_OUT),
        EASE_IN_OUT_ELASTIC(Easings.ELASTIC_IN_OUT),
        EASE_IN_BOUNCE(Easings.BOUNCE_IN),
        EASE_OUT_BOUNCE(Easings.BOUNCE_OUT),
        EASE_IN_OUT_BOUNCE(Easings.BOUNCE_IN_OUT),
        SIGMOID(var0 -> {
            double var2 = 1.0 / (1.0 + Math.exp(-12.0 * (var0 - 0.5)));
            double var4 = 1.0 / (1.0 + Math.exp(6.0));
            double var6 = 1.0 / (1.0 + Math.exp(-6.0));
            return (var2 - var4) / (var6 - var4);
        }),
        SMOOTHSTEP(var0 -> var0 * var0 * (3.0 - 2.0 * var0)),
        SMOOTHERSTEP(var0 -> var0 * var0 * var0 * (var0 * (var0 * 6.0 - 15.0) + 10.0)),
        NOVA_EASING(var0 -> {
            float var2 = 1.3F;
            float var3 = var2 + 1.0F;
            return Math.max(0.0, 1.0 + var3 * Math.pow(var0 - 1.0, 3.0) + var2 * Math.pow(var0 - 1.0, 2.0));
        });

        private final EasingFunction increasingCurve;
        private final EasingFunction decreasingCurve;

        DirectionalCurve(EasingFunction easingFunction) {
            this(easingFunction, easingFunction);
        }

        DirectionalCurve(EasingFunction increasingCurve2, EasingFunction decreasingCurve2) {
            this.increasingCurve = increasingCurve2;
            this.decreasingCurve = decreasingCurve2;
        }

        public double apply(double var1, boolean var3) {
            double var4 = AnimationUtils.clampProgress(var1);
            return (var3 ? this.decreasingCurve : this.increasingCurve).apply(var4);
        }
    }
}
