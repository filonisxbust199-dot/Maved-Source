package ess.ll.client.features.combat;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.util.Rotation;
import java.util.EnumSet;
import java.util.Set;

public interface AuraRotationStrategy extends MinecraftAccess {
    void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2);

    default Set<AuraRotationUpdateMode> updateModes() {
        return EnumSet.of(AuraRotationUpdateMode.RENDER);
    }

    default Rotation currentRotation(AttackAura attackAura) {
        return null;
    }

    default void onRotationApplied(boolean var1) {
    }

    default void reset() {
    }

    default void onRotationBlocked() {
    }

    String name();
}
