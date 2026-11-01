package ess.ll.client.event;

@FunctionalInterface
public interface EventListener {
    void fire(Event event);
}
