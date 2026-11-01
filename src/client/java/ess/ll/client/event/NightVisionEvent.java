package ess.ll.client.event;

public class NightVisionEvent extends Event {
    public static final NightVisionEvent INSTANCE = new NightVisionEvent();

    public boolean fire() {
        if (!this.hasListeners()) {
            return false;
        }
        super.dispatch();
        return this.consumeCancelled();
    }
}
