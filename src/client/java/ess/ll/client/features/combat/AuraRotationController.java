package ess.ll.client.features.combat;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public final class AuraRotationController implements MinecraftAccess {
    private final AttackAura aura;
    private final Map<String, AuraRotationStrategy> strategies;
    private float smoothedFrameDelta = 1.0F;
    private int raycastTick;
    private boolean cachedRaycast;
    private static final int INITIAL_RAYCAST_TICK = Integer.MIN_VALUE;
    private static final int INVALID_RAYCAST_TICK = Integer.MIN_VALUE;
    private static final double PI = Math.PI;

    private boolean hasWorld() {
        return mc.player != null && mc.level != null;
    }

    public void reset() {
        this.smoothedFrameDelta = 1.0F;
        this.raycastTick = INVALID_RAYCAST_TICK;
        this.strategies.values().forEach(AuraRotationStrategy::reset);
    }

    public void updateTick(int priority) {
        this.update(AuraRotationUpdateMode.TICK, false, priority);
    }

    private void update(AuraRotationUpdateMode mode, boolean snap, int priority) {
        if (this.hasWorld()) {
            LivingEntity target = AttackAura.target;
            if (target != null) {
                AuraRotationStrategy strategy = this.activeStrategy();
                if (strategy != null && strategy.updateModes().contains(mode)) {
                    boolean fixedRate = mode == AuraRotationUpdateMode.FIXED_60;
                    if (fixedRate && RotationManager.isBusy(priority)) {
                        strategy.onRotationBlocked();
                    } else {
                        AuraRotationController.RotationContext context = this.createContext(target, snap, mode);
                        AuraRotationController.RotationStep step = this.defaultRotation(context);
                        strategy.calculateRotation(context, step);
                        boolean applied = this.applyRotation(step, priority);
                        if (fixedRate) {
                            strategy.onRotationApplied(applied);
                        }
                    }
                }
            }
        }
    }

    public AuraRotationController(AttackAura attackAura) {
        this.raycastTick = INITIAL_RAYCAST_TICK;
        this.cachedRaycast = false;
        this.aura = attackAura;
        this.strategies = this.createStrategies();
    }

    private AuraRotationController.RotationStep defaultRotation(AuraRotationController.RotationContext context) {
        AuraRotationController.RotationStep step = new AuraRotationController.RotationStep(context.currentYaw() + context.yawDelta(), Mth.clamp(context.currentPitch() + context.pitchDelta(), -89.9F, 89.9F));
        step.setReturnSpeeds(context.defaultReturnSpeed(), context.defaultReturnSpeed());
        return step;
    }

    public Rotation currentRotation() {
        AuraRotationStrategy auraRotationStrategy = this.activeStrategy();
        return auraRotationStrategy == null ? null : auraRotationStrategy.currentRotation(this.aura);
    }

    /**
     * Run an arbitrary block/crystal aim through Aura's selected rotation strategy.
     */
    public boolean rotateTo(Vector2f aim, LivingEntity target, int priority) {
        if (!this.hasWorld() || aim == null || target == null)
            return false;
        AuraRotationStrategy strategy = this.activeStrategy();
        if (strategy == null)
            return false;
        float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
        float yawDelta = Mth.wrapDegrees(aim.x - yaw);
        float pitchDelta = Mth.wrapDegrees(aim.y - pitch);
        float frame = this.smoothFrameDelta();
        float returnSpeed = (20.0F + GuiHitTestUtils.randomBetween(4.0F, 10.0F)) * frame;
        var context = new RotationContext(this.aura, target, yaw, pitch, aim.x, aim.y, yawDelta, pitchDelta, mc.player.isFallFlying(), false, false, target.getBoundingBox(), (float) (System.currentTimeMillis() % 300L / 300.0 * 2.0 * PI), frame, returnSpeed, AuraRotationUpdateMode.RENDER);
        var result = this.defaultRotation(context);
        strategy.calculateRotation(context, result);
        return this.applyRotation(result, priority);
    }

    public void updateRender(boolean snap, int priority, boolean highFps) {
        this.update(highFps ? AuraRotationUpdateMode.HIGH_FPS : AuraRotationUpdateMode.RENDER, snap, priority);
    }

    private AuraRotationStrategy activeStrategy() {
        String value = this.aura.getRotationMode().getValue();
        return this.strategies.get(value);
    }

    private float smoothFrameDelta() {
        float gameTimeDeltaTicks = mc.getDeltaTracker().getGameTimeDeltaTicks();
        if (!Float.isFinite(gameTimeDeltaTicks)) {
            gameTimeDeltaTicks = 1.0F;
        }
        gameTimeDeltaTicks = Mth.clamp(gameTimeDeltaTicks, 0.05F, 2.0F);
        this.smoothedFrameDelta = Mth.lerp(0.35F, this.smoothedFrameDelta, gameTimeDeltaTicks);
        return this.smoothedFrameDelta;
    }

    private boolean applyRotation(AuraRotationController.RotationStep step, int priority) {
        float yaw = step.getYaw();
        float pitch = Mth.clamp(step.getPitch(), -89.9F, 89.9F);
        return RotationManager.rotate(new Rotation(yaw, pitch), step.getYawSpeed(), step.getPitchSpeed(), step.getReturnYawSpeed(), step.getReturnPitchSpeed(), priority, step.isVisible());
    }

    private Map<String, AuraRotationStrategy> createStrategies() {
        Map<String, AuraRotationStrategy> linkedHashMap = new LinkedHashMap<>();
        for (AuraRotationStrategy auraRotationStrategy : List.of(new GrimAuraRotation(), new MatrixAuraRotation(), new MatrixSnapAuraRotation(), new LegitAuraRotation(), new SmoothAuraRotation(), new UniversalAuraRotation())) {
            linkedHashMap.put(auraRotationStrategy.name(), auraRotationStrategy);
        }
        return Collections.unmodifiableMap(linkedHashMap);
    }

    private boolean cachedRaycast(float yaw, float pitch, LivingEntity target, AABB bounds) {
        int tick = mc.player.tickCount;
        if (tick != this.raycastTick) {
            this.raycastTick = tick;
            this.cachedRaycast = RaycastUtils.isLookingAt(yaw, pitch, 3.0, target, false, bounds);
        }
        return this.cachedRaycast;
    }

    public void updateFixedRate(int priority) {
        this.update(AuraRotationUpdateMode.FIXED_60, false, priority);
    }

    private AuraRotationController.RotationContext createContext(LivingEntity target, boolean snap, AuraRotationUpdateMode mode) {
        Vec3 aimDirection = this.aura.getAimPointSelector().aimDirection(this.aura, target);
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(aimDirection.z, aimDirection.x)) - 90.0);
        float targetPitch = (float) Mth.wrapDegrees(Math.toDegrees(-Math.atan2(aimDirection.y, Math.hypot(aimDirection.x, aimDirection.z))));
        float yawDelta = Mth.clamp(Mth.wrapDegrees(targetYaw - currentYaw), -180.0F, 180.0F);
        float pitchDelta = Mth.clamp(Mth.wrapDegrees(targetPitch - currentPitch), -90.0F, 90.0F);
        AABB boundingBox = target.getBoundingBox();
        boolean fallFlying = mc.player.isFallFlying();
        boolean lookingAtTarget = this.cachedRaycast(currentYaw, currentPitch, target, boundingBox);
        float frameDelta = this.smoothFrameDelta();
        float returnSpeed = (20.0F + GuiHitTestUtils.randomBetween(4.0F, 10.0F)) * frameDelta;
        float oscillationAngle = (float) (System.currentTimeMillis() % 300L / 300.0 * 2.0 * PI);
        return new AuraRotationController.RotationContext(this.aura, target, currentYaw, currentPitch, targetYaw, targetPitch, yawDelta, pitchDelta, fallFlying, lookingAtTarget, snap, boundingBox, oscillationAngle, frameDelta, returnSpeed, mode);
    }

    public record RotationContext(AttackAura aura, LivingEntity target, float currentYaw, float currentPitch, float rawYaw, float rawPitch, float yawDelta, float pitchDelta, boolean isGliding, boolean rayTrace, boolean snap, AABB targetBox, float angle, float frameDelta, float defaultReturnSpeed, AuraRotationUpdateMode pass) {
    }

    public static final class RotationStep {
        private float yaw;
        private float pitch;
        private float yawSpeed;
        private float pitchSpeed;
        private float returnYawSpeed;
        private float returnPitchSpeed;
        private boolean visible;

        public void setSpeeds(float yawSpeed, float pitchSpeed) {
            this.yawSpeed = yawSpeed;
            this.pitchSpeed = pitchSpeed;
        }

        public float getYaw() {
            return this.yaw;
        }

        public boolean isVisible() {
            return this.visible;
        }

        public float getPitch() {
            return this.pitch;
        }

        public float getReturnYawSpeed() {
            return this.returnYawSpeed;
        }

        public void setReturnPitchSpeed(float returnPitchSpeed) {
            this.returnPitchSpeed = returnPitchSpeed;
        }

        public float getYawSpeed() {
            return this.yawSpeed;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public void useUnlimitedSpeeds() {
            this.setSpeeds(9999.0F, 9999.0F);
            this.setReturnSpeeds(9999.0F, 9999.0F);
        }

        public float getPitchSpeed() {
            return this.pitchSpeed;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setReturnYawSpeed(float returnYawSpeed) {
            this.returnYawSpeed = returnYawSpeed;
        }

        public void setReturnSpeeds(float yawSpeed, float pitchSpeed) {
            this.returnYawSpeed = yawSpeed;
            this.returnPitchSpeed = pitchSpeed;
        }

        public float getReturnPitchSpeed() {
            return this.returnPitchSpeed;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public RotationStep(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public void setYawSpeed(float yawSpeed) {
            this.yawSpeed = yawSpeed;
        }

        public void setPitchSpeed(float pitchSpeed) {
            this.pitchSpeed = pitchSpeed;
        }
    }
}
