package ess.ll.client.render;

public class SmoothAnimation {
    private long startTimeMillis;
    private double durationMillis;
    private double startValue;
    private double targetValue;
    private double value;
    private double previousValue;
    private EasingFunction easing = Easings.LINEAR;
    private Runnable onComplete;

    public SmoothAnimation withEasing(EasingFunction easing) {
        this.easing = easing;
        return this;
    }

    public double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public SmoothAnimation withPreviousValue(double previousValue) {
        this.previousValue = previousValue;
        return this;
    }

    public boolean shouldKeepCurrentAnimation(boolean keepRunning, double target) {
        return keepRunning && this.isRunning() && (target == this.getStartValue() || target == this.getTargetValue() || target == this.getValue());
    }

    public double getProgress() {
        return this.getDurationMillis() <= 0.0 ? 1.0 : (System.currentTimeMillis() - this.getStartTimeMillis()) / this.getDurationMillis();
    }

    public boolean isRunning() {
        return !this.isComplete();
    }

    public double getPreviousValue() {
        return this.previousValue;
    }

    public SmoothAnimation withDurationMillis(double durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    public SmoothAnimation withValue(double value) {
        this.value = value;
        return this;
    }

    public SmoothAnimation animateTo(double target, double durationSeconds, boolean keepRunning) {
        return this.animateTo(target, durationSeconds, Easings.LINEAR, keepRunning);
    }

    public void snapTo(double target) {
        this.animateTo(target, 0.0);
        this.update();
        this.withValue(target);
    }

    public SmoothAnimation animateTo(double target, double durationSeconds, EasingFunction easingFunction) {
        return this.animateTo(target, durationSeconds, easingFunction, false);
    }

    public double getValue() {
        return this.value;
    }

    public double getStartValue() {
        return this.startValue;
    }

    public double getTargetValue() {
        return this.targetValue;
    }

    public boolean update() {
        this.withPreviousValue(this.getValue());
        boolean running = this.isRunning();
        if (running) {
            this.withValue(this.lerp(this.getStartValue(), this.getTargetValue(), this.getEasing().apply(this.getProgress())));
        } else {
            this.withStartTimeMillis(0L);
            this.withValue(this.getTargetValue());
            if (this.onComplete != null) {
                this.onComplete.run();
                this.onComplete = null;
            }
        }
        return running;
    }

    public SmoothAnimation withStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
        return this;
    }

    public EasingFunction getEasing() {
        return this.easing;
    }

    public Runnable getOnComplete() {
        return this.onComplete;
    }

    public SmoothAnimation animateTo(double target, double durationSeconds) {
        return this.animateTo(target, durationSeconds, Easings.LINEAR, false);
    }

    public double getDurationMillis() {
        return this.durationMillis;
    }

    public SmoothAnimation animateTo(double target, double durationSeconds, EasingFunction easingFunction, boolean keepRunning) {
        if (!this.shouldKeepCurrentAnimation(keepRunning, target)) {
            this.withEasing(easingFunction).withDurationMillis(durationSeconds * 1000.0).withStartTimeMillis(System.currentTimeMillis()).withStartValue(this.getValue()).withTargetValue(target);
        }
        return this;
    }

    public float getPreviousFloatValue() {
        return (float) this.getPreviousValue();
    }

    public SmoothAnimation withOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public boolean isComplete() {
        return this.getProgress() >= 1.0;
    }

    public SmoothAnimation withStartValue(double startValue) {
        this.startValue = startValue;
        return this;
    }

    public float getFloatValue() {
        return (float) this.getValue();
    }

    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    public SmoothAnimation withTargetValue(double targetValue) {
        this.targetValue = targetValue;
        return this;
    }
}
