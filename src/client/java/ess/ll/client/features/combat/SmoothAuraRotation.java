package ess.ll.client.features.combat;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.util.RandomUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import ess.ll.client.util.RotationMath;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SmoothAuraRotation implements AuraRotationStrategy {
    private final SmoothAuraRotation.RotationTarget pendingTarget = new SmoothAuraRotation.RotationTarget();
    private float yawVelocity;
    private float pitchVelocity;
    private int pauseTicks;
    private float wobbleYaw;
    private float wobblePitch;
    private float smoothedDeltaYaw;
    private float smoothedDeltaPitch;
    private boolean modelActive;
    private RotationModelRepository modelRepository;
    private AttackAura aura;
    private LivingEntity target;

    @Override
    public String name() {
        return "Smooth";
    }

    private boolean tryModelRotation() {
        if (mc.player != null && this.modelRepository != null && this.modelRepository.isLoaded()) {
            float yRot = mc.player.getYRot();
            float xRot = mc.player.getXRot();
            float yaw = FreeLookController.getYaw();
            float pitch = FreeLookController.getPitch();
            float var5 = Mth.wrapDegrees(yaw - yRot);
            float var6 = pitch - xRot;
            if (Math.abs(var5) <= 5.0F && Math.abs(var6) <= 5.0F) {
                this.modelRepository.clearHistory();
                return true;
            }
            Vec3 position2 = mc.player.position();
            Vec3 deltaMovement = mc.player.getDeltaMovement();
            float attackStrengthScale = mc.player.getAttackStrengthScale(1.5F);
            RotationModelSnapshot rotationModelSnapshot = RotationModelSnapshot.fallbackReset(mc.player, yRot, xRot, yaw, pitch, position2, deltaMovement, attackStrengthScale, mc.player.onGround(), mc.player.isFallFlying(), mc.player.isSprinting(), mc.player.isUsingItem(), mc.options.keyJump.isDown(), (float) mc.player.fallDistance, mc.player.tickCount);
            RotationModelRepository.RotationPrediction var11 = this.modelRepository.predict(rotationModelSnapshot);
            if (var11 == null) {
                return true;
            }
            float var12 = clampToward(Mth.clamp(var11.yawStep(), -180.0F, 180.0F), var5);
            float var13 = clampToward(Mth.clamp(var11.pitchStep(), -90.0F, 90.0F), var6);
            mc.player.setYRot(yRot + RotationMath.snapToGcd(var12));
            mc.player.setXRot(Mth.clamp(xRot + RotationMath.snapToGcd(var13), -90.0F, 90.0F));
            return false;
        } else {
            return true;
        }
    }

    private static float clampToward(float var0, float var1) {
        return var1 >= 0.0F ? Mth.clamp(var0, 0.0F, var1) : Mth.clamp(var0, var1, 0.0F);
    }

    private void maybePause(boolean var1, float var2, float var3) {
        if (!var1 && this.pauseTicks <= 0 && RandomUtils.nextFloat() < 0.03F + (var2 + var3) * 0.5F * 0.05F) {
            this.pauseTicks = RandomUtils.nextInt(1, 4);
            this.wobbleYaw = RandomUtils.nextFloat((-1.9F), 1.9F) * var2;
            this.wobblePitch = RandomUtils.nextFloat((-1.2F), 1.2F) * var3;
        }
        if (var1) {
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
            this.pauseTicks = 0;
            this.wobbleYaw = 0.0F;
            this.wobblePitch = 0.0F;
        }
        if (this.pauseTicks > 0) {
            this.pauseTicks--;
        } else {
            this.wobbleYaw = this.wobbleYaw * 0.45F;
            this.wobblePitch = this.wobblePitch * 0.45F;
        }
    }

    private void rotate(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        this.aura = var1.aura();
        RotationModelRepository rotationModelRepository = this.aura.getModelRepository();
        LivingEntity entity = var1.target();
        if (entity != this.target) {
            this.target = entity;
            rotationModelRepository.resetSession();
            this.resetState();
        }
        this.modelRepository = rotationModelRepository;
        RotationManager.setResetCondition(this::tryModelRotation);
        RotationModelSnapshot rotationModelSnapshot = RotationModelSnapshot.from(entity.getId(), var1);
        RotationModelRepository.RotationPrediction var6 = rotationModelRepository.predict(rotationModelSnapshot);
        if (var6 == null) {
            this.pendingTarget.set(mc.player.getYRot(), mc.player.getXRot());
            this.pendingTarget.apply(var2);
        } else {
            float yRot = mc.player.getYRot();
            float xRot = mc.player.getXRot();
            boolean var9 = var1.rayTrace();
            float var10 = var9 ? 0.0F : this.aura.getCriticalPredictionOffset();
            float var11 = var9 ? 0.0F : this.aura.getAimPredictionOffset();
            float var12 = this.dampenBySpeed(Mth.clamp(var6.yawStep(), -180.0F, 180.0F), Math.abs(Mth.wrapDegrees(var1.rawYaw() - yRot)), 25.0F);
            float var13 = this.dampenBySpeed(Mth.clamp(var6.pitchStep(), -90.0F, 90.0F), Math.abs(var1.rawPitch() - xRot), 15.0F);
            float var14 = Math.abs(Mth.wrapDegrees(var1.rawYaw() - yRot)) + Math.abs(var1.rawPitch() - xRot);
            float var15;
            float var16;
            if (this.modelActive && !(var14 > 12.0F)) {
                this.smoothedDeltaYaw = 0.2F * this.smoothedDeltaYaw + 0.8F * var12;
                this.smoothedDeltaPitch = 0.2F * this.smoothedDeltaPitch + 0.8F * var13;
                var15 = this.smoothedDeltaYaw;
                var16 = this.smoothedDeltaPitch;
            } else {
                this.smoothedDeltaYaw = var12;
                this.smoothedDeltaPitch = var13;
                this.modelActive = true;
                var15 = var12;
                var16 = var13;
            }
            float var17 = Mth.clamp(Mth.wrapDegrees(var1.rawYaw() - yRot), -6.5F * var10, 6.5F * var10);
            float var18 = Mth.clamp(var1.rawPitch() - xRot, -4.5F * var11, 4.5F * var11);
            this.addWobble(var10, var11);
            this.maybePause(var9, var10, var11);
            float var19 = var15 + var17 + this.yawVelocity + this.wobbleYaw;
            float var20 = var16 + var18 + this.pitchVelocity + this.wobblePitch;
            this.pendingTarget.set(yRot + var19, Mth.clamp(xRot + var20, -89.9F, 89.9F), var6.turnSpeedYaw(), var6.turnSpeedPitch(), var6.reverbSpeedYaw(), var6.reverbSpeedPitch());
            this.pendingTarget.apply(var2);
        }
    }

    private float dampenBySpeed(float var1, float var2, float var3) {
        float var4 = Math.min(1.0F, var2 / var3);
        return var1 * (1.0F - 0.35F * (1.0F - var4 * var4));
    }

    @Override
    public Set<AuraRotationUpdateMode> updateModes() {
        return EnumSet.of(AuraRotationUpdateMode.TICK, AuraRotationUpdateMode.RENDER);
    }

    @Override
    public Rotation currentRotation(AttackAura attackAura) {
        return null;
    }

    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        if (var1.pass() == AuraRotationUpdateMode.TICK) {
            this.rotate(var1, var2);
        } else {
            this.pendingTarget.apply(var2);
        }
    }

    private void addWobble(float var1, float var2) {
        float var3 = RandomUtils.nextFloat((-0.8F), 0.8F) * var1;
        float var4 = RandomUtils.nextFloat((-0.55F), 0.55F) * var2;
        this.yawVelocity = this.yawVelocity * 0.72F + var3 * 0.28F;
        this.pitchVelocity = this.pitchVelocity * 0.72F + var4 * 0.28F;
    }

    @Override
    public void reset() {
        this.resetState();
        this.target = null;
        RotationManager.clearResetCondition();
        if (this.aura != null) {
            this.aura.getModelRepository().resetSession();
        }
    }

    private void resetState() {
        this.pendingTarget.reset();
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
        this.pauseTicks = 0;
        this.wobbleYaw = 0.0F;
        this.wobblePitch = 0.0F;
        this.smoothedDeltaYaw = 0.0F;
        this.smoothedDeltaPitch = 0.0F;
        this.modelActive = false;
    }

    private static final class RotationTarget {
        private float yaw;
        private float pitch;
        private float yawLimit;
        private float pitchLimit;
        private float yawOffset;
        private float pitchOffset;
        private boolean active;

        void set(float var1, float var2) {
            this.set(var1, var2, 0.0F, 0.0F, 0.0F, 0.0F);
        }

        private RotationTarget() {
            this.yawLimit = 9999.0F;
            this.pitchLimit = 9999.0F;
            this.yawOffset = 9999.0F;
            this.pitchOffset = 9999.0F;
        }

        void apply(AuraRotationController.RotationStep var1) {
            if (this.active) {
                var1.setYaw(this.yaw);
                var1.setPitch(this.pitch);
                var1.setSpeeds(this.yawLimit, this.pitchLimit);
                var1.setReturnSpeeds(this.yawOffset, this.pitchOffset);
                var1.setVisible(false);
            }
        }

        void set(float var1, float var2, float var3, float var4, float var5, float var6) {
            this.yaw = var1;
            this.pitch = var2;
            this.yawLimit = var3;
            this.pitchLimit = var4;
            this.yawOffset = var5;
            this.pitchOffset = var6;
            this.active = true;
        }

        void reset() {
            this.yaw = 0.0F;
            this.pitch = 0.0F;
            this.yawLimit = 9999.0F;
            this.pitchLimit = 9999.0F;
            this.yawOffset = 9999.0F;
            this.pitchOffset = 9999.0F;
            this.active = false;
        }
    }
}
