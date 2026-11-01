package ess.ll.client.features.combat;

import ess.ll.client.util.RandomUtils;
import net.minecraft.util.Mth;

public final class LegitAuraRotation implements AuraRotationStrategy {
    private float accuracyDrift;

    @Override
    public void reset() {
        this.accuracyDrift = 0.0F;
    }

    private float yawSpeed(AuraRotationController.RotationContext var1) {
        return var1.rayTrace() ? 0.1F : (var1.isGliding() ? var1.aura().elytraYawSpeedPercent() : var1.aura().yawSpeedPercent()) / 100.0F;
    }

    private void updateDrift(boolean var1) {
        if (!var1) {
            this.accuracyDrift = this.accuracyDrift + 0.007F;
        } else if (this.accuracyDrift > 0.0F) {
            this.accuracyDrift = this.accuracyDrift - ((mc.player.isFallFlying() ? 0.03F : 0.007F) + RandomUtils.nextFloat() * 0.007F);
        }
        this.accuracyDrift = Mth.clamp(this.accuracyDrift, 0.0F, 1.0F);
    }

    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        if (mc.player != null) {
            float yRot = mc.player.getYRot();
            float xRot = mc.player.getXRot();
            this.updateDrift(var1.rayTrace());
            if (var1.rayTrace()) {
                var2.setYaw(yRot);
                var2.setPitch(xRot);
                var2.setSpeeds(this.yawSpeed(var1), this.pitchSpeed(var1));
                var2.setVisible(true);
            } else {
                float var5 = Mth.wrapDegrees(var1.rawYaw() - yRot);
                float var6 = var1.rawPitch() - xRot;
                float var7 = Mth.clamp(0.62F + this.accuracyDrift * 0.38F, 0.0F, 1.0F);
                float var8 = this.randomJitter(0.08F);
                float var9 = this.randomJitter(0.05F);
                var2.setYaw(yRot + var5 * var7 + var8);
                var2.setPitch(xRot + var6 * var7 + var9);
                var2.setSpeeds(this.yawSpeed(var1), this.pitchSpeed(var1));
                var2.setVisible(true);
            }
        }
    }

    private float randomJitter(float var1) {
        float var2 = var1 * (1.0F - this.accuracyDrift * 0.65F);
        return var2 > 0.0F ? RandomUtils.nextFloat(-var2, var2) : 0.0F;
    }

    private float pitchSpeed(AuraRotationController.RotationContext var1) {
        return var1.rayTrace() ? 0.1F : (var1.isGliding() ? var1.aura().elytraPitchSpeedPercent() : var1.aura().pitchSpeedPercent()) / 100.0F;
    }

    @Override
    public String name() {
        return "Legit";
    }
}
