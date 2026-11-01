package ess.ll.client.event;

import net.minecraft.client.DeltaTracker;

public class FrameUpdateEvent extends Event {
    public static final FrameUpdateEvent INSTANCE = new FrameUpdateEvent();
    private DeltaTracker deltaTracker;

    public void fire(DeltaTracker deltaTracker) {
        this.deltaTracker = deltaTracker;
        super.dispatch();
    }

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public void setDeltaTracker(DeltaTracker deltaTracker) {
        this.deltaTracker = deltaTracker;
    }
}
