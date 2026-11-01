package ess.ll.client.features.combat;

import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.module.combat.AttackAura;

public interface CriticalHitStrategy {
    default boolean shouldAllowFallbackAttack(AttackAura attackAura) {
        return false;
    }

    default int ticksUntilCritical(AttackAura attackAura) {
        return -1;
    }

    void update(AttackAura attackAura);

    default void reset() {
    }

    default void modifyInput(PlayerInputEvent event) {
    }

    boolean isApplicable(AttackAura attackAura);
}
