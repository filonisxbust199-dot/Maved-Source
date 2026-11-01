package ess.ll.client.features.combat;

import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.util.Rotation;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.Mth;

public final class MatrixSnapAuraRotation implements AuraRotationStrategy {
    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        var2.setSpeeds(9999.0F, 9999.0F);
        var2.setReturnSpeeds(9999.0F, 9999.0F);
        AttackAura attackAura = var1.aura();
        boolean sx = Math.abs(attackAura.targetDistance) <= attackAura.effectiveAttackRange();
        int sxx = attackDelay(attackAura);
        if (sx && sxx <= 5) {
            int rotation = Math.max(1, sxx - 2);
            var2.setYaw(var1.currentYaw() + Mth.wrapDegrees(var1.rawYaw() - var1.currentYaw()) / rotation);
            var2.setPitch(Mth.clamp(var1.currentPitch() + Mth.wrapDegrees(var1.rawPitch() - var1.currentPitch()) / rotation, -89.9F, 89.9F));
        } else {
            Rotation rotation = Rotation.freeLook();
            var2.setYaw((float) (rotation.getYaw() + Math.random() * 3.0));
            var2.setPitch((float) (rotation.getPitch() + Math.random() * 3.0));
        }
    }

    @Override
    public String name() {
        return "MatrixSnap";
    }

    @Override
    public Set<AuraRotationUpdateMode> updateModes() {
        return EnumSet.of(AuraRotationUpdateMode.TICK);
    }

    private static int attackDelay(AttackAura attackAura) {
        int var1 = attackAura.attackDelayTicks();
        if (attackAura.getAttackOptions().isSelected("Только криты") && !attackAura.canAttackWithCriticalPolicy()) {
            int var2 = attackAura.predictedCriticalTick();
            return var2 < 0 ? Integer.MAX_VALUE : Math.max(var1, var2);
        } else {
            return var1;
        }
    }
}
