package ess.ll.client.event;

public class ToggleStateEvent extends Event {
    public static final ToggleStateEvent INSTANCE = new ToggleStateEvent();
    private Object source;
    private ToggleStateEvent.State state;
    private ToggleStateEvent.Kind kind;

    public ToggleStateEvent.State getState() {
        return this.state;
    }

    public void setSource(Object object) {
        this.source = object;
    }

    public Object getSource() {
        return this.source;
    }

    public void setKind(ToggleStateEvent.Kind kind) {
        this.kind = kind;
    }

    public void fire(Object object, ToggleStateEvent.State state, ToggleStateEvent.Kind kind) {
        this.source = object;
        this.state = state;
        this.kind = kind;
        super.dispatch();
    }

    public void setState(ToggleStateEvent.State state) {
        this.state = state;
    }

    public ToggleStateEvent.Kind getKind() {
        return this.kind;
    }

    public enum Kind {
        MODULE, CHECKBOX
    }

    public enum State {
        DISABLE, ENABLE
    }
}
