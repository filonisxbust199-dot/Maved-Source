package ess.ll.client.module.render;

/** Marks client clock instances without replacing the objects receiving server updates. */
public interface ClientClockAccess {
    void ess$setOverworld(boolean overworld);
}
