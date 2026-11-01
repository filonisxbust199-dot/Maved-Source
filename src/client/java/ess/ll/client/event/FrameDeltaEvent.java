package ess.ll.client.event;

public class FrameDeltaEvent extends Event {
    public static final FrameDeltaEvent INSTANCE = new FrameDeltaEvent();
    private float delta = 1.0F;

    public void setDelta(float delta) {
        this.delta = delta;
    }

    public void fire(float delta) {
        this.delta = delta;
        super.dispatch();
    }

    public float getDelta() {
        return this.delta;
    }
}
