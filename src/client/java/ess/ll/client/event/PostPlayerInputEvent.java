package ess.ll.client.event;

public class PostPlayerInputEvent extends PlayerMovementEvent {
    public static final PostPlayerInputEvent INSTANCE = new PostPlayerInputEvent();

    @Override
    public boolean isPostInput() {
        return true;
    }

    @Override
    public boolean isPreInput() {
        return false;
    }
}
