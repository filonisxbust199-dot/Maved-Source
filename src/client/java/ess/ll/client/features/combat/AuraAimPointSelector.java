package ess.ll.client.features.combat;

import ess.ll.client.core.EntityAimPointSampler;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.integration.OptionalModules.ElytraTarget;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.ModuleManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class AuraAimPointSelector implements MinecraftAccess {
    private volatile AuraAimPointSelector.AimMode mode = AuraAimPointSelector.AimMode.NONE;
    private volatile Vec3 predictedPosition = null;
    private final EntityAimPointSampler pointSampler = new EntityAimPointSampler();

    public void selectMode(ElytraTarget elytraTarget, AttackAura attackAura, LivingEntity entity) {
        if (entity == null) {
            this.mode = AuraAimPointSelector.AimMode.NONE;
            this.predictedPosition = null;
        } else if (mc.player != null) {
            boolean fallFlying = mc.player.isFallFlying();
            if (fallFlying && elytraTarget.isEnabled() && elytraTarget.aimAtPredicted.getValue() && elytraTarget.predictedPosition != null) {
                this.predictedPosition = elytraTarget.predictedPosition;
                this.mode = AuraAimPointSelector.AimMode.ELYTRA_RESOLVE;
            } else {
                boolean sx = fallFlying && entity.isFallFlying() && elytraTarget.isEnabled() && elytraTarget.unusedFlag.getValue() && attackAura.elytraTargetStable && !ModuleManager.INSTANCE.freeCam().isEnabled();
                attackAura.chasingElytraTarget = sx;
                if (sx) {
                    this.mode = AuraAimPointSelector.AimMode.ELYTRA_CHASE;
                } else if (fallFlying) {
                    this.mode = AuraAimPointSelector.AimMode.GLIDING_UP;
                } else {
                    if (!GuiHitTestUtils.canSee(entity.getEyePosition())) {
                        Vec3 closestVisiblePoint = this.pointSampler.getClosestVisiblePoint();
                        if (closestVisiblePoint != null) {
                            this.mode = AuraAimPointSelector.AimMode.BEST_POINT;
                            return;
                        }
                    }
                    this.mode = attackAura.getRotationMode().is("Matrix") ? AuraAimPointSelector.AimMode.RANDOM_POINT : AuraAimPointSelector.AimMode.STANDARD;
                }
            }
        }
    }

    public void updateSamples(LivingEntity entity) {
        if (entity == null) {
            this.mode = AuraAimPointSelector.AimMode.NONE;
            this.predictedPosition = null;
        } else {
            if (entity.getBbWidth() < 3.0F && entity.getBbHeight() < 3.0F) {
                this.pointSampler.update(entity);
            }
        }
    }

    private Vec3 centerPoint(LivingEntity entity, Vec3 vector) {
        return vector.add(0.0, entity.getBbHeight() / 2.0F, 0.0);
    }

    public Vec3 aimDirection(AttackAura attackAura, LivingEntity entity) {
        if (this.mode == AuraAimPointSelector.AimMode.NONE) {
            return Vec3.ZERO;
        }
        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 position2 = entity.position();
        return switch (this.mode) {
            case NONE ->
                Vec3.ZERO;
            case STANDARD ->
                this.centerPoint(entity, position2).subtract(eyePosition);
            case BEST_POINT ->
                {
                    Vec3 closestVisiblePoint = this.pointSampler.getClosestVisiblePoint();
                    yield closestVisiblePoint == null ? entity.getEyePosition().subtract(eyePosition) : closestVisiblePoint.subtract(eyePosition);
                }
            case RANDOM_POINT ->
                {
                    Vec3 vector = this.pointSampler.aimPoint(entity, eyePosition, attackAura.effectiveAttackRange());
                    yield vector == null ? this.centerPoint(entity, position2).subtract(eyePosition) : vector.subtract(eyePosition);
                }
            case ELYTRA_RESOLVE ->
                this.predictedPosition.subtract(eyePosition);
            case ELYTRA_CHASE ->
                attackAura.predictedAimPoint.subtract(eyePosition);
            case GLIDING_UP ->
                {
                    float bbHeight = entity.getBbHeight();
                    double var6 = Math.max(0.1, bbHeight * 0.25F);
                    boolean fallFlying = mc.player.isFallFlying();
                    yield fallFlying && entity.isFallFlying() ? new Vec3(position2.x, position2.y, position2.z).subtract(eyePosition) : new Vec3(position2.x, position2.y + bbHeight - var6, position2.z).subtract(eyePosition);
                }
        };
    }

    public String getModeName() {
        return this.mode.name();
    }

    private enum AimMode {
        NONE,
        STANDARD,
        BEST_POINT,
        RANDOM_POINT,
        ELYTRA_RESOLVE,
        ELYTRA_CHASE,
        GLIDING_UP
    }
}
