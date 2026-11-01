package ess.ll.client.event;

public class TickEvent extends Event {
    public static final TickEvent INSTANCE = new TickEvent();
    private static int ticks;

    @Override
    public void dispatch() {
        ticks++;
        super.dispatch();
    }

    public static int getTicks() {
        return ticks;
    }
}
