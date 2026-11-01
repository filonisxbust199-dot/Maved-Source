package ess.ll.client.features.combat;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.storage.RotationDerivativeHistory;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import ess.ll.client.util.RotationMath;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class UniversalAuraRotation implements AuraRotationStrategy {
    private final RotationDerivativeHistory derivatives = new RotationDerivativeHistory();
    private final String modelName;
    private final Supplier<OnnxRotationModel> modelSupplier;
    private AttackAura aura;
    private LivingEntity target;
    private float[] lastRotation;
    private boolean hasRotation;
    private int rotationAge;
    private float speedScale = 1.0F;
    private double lastDistance;
    private boolean wasLooking;
    private boolean sessionActive;
    private boolean wasAttacking;

    private float[] predictRotation() {
        OnnxRotationModel onnxRotationModel = this.resolveModel();
        if (mc.player != null && onnxRotationModel != null && onnxRotationModel.isLoaded()) {
            float yRot = mc.player.getYRot();
            float xRot = mc.player.getXRot();
            float yaw = FreeLookController.getYaw();
            float pitch = FreeLookController.getPitch();
            float sxxxxx = Mth.wrapDegrees(yaw - yRot);
            float sxxxxxx = pitch - xRot;
            if (Math.abs(sxxxxx) <= 5.0F && Math.abs(sxxxxxx) <= 5.0F) {
                this.wasLooking = false;
                onnxRotationModel.resetState();
                this.derivatives.reset();
                return null;
            }
            if (!this.wasLooking) {
                this.wasLooking = true;
                onnxRotationModel.resetState();
                this.derivatives.reset();
            }
            RotationDerivativeHistory.Derivatives derivatives = this.derivatives.derive(yRot, xRot, sxxxxx, sxxxxxx);
            Vec3 deltaMovement = mc.player.getDeltaMovement();
            double sxxxxxxxxx = Math.hypot(deltaMovement.x, deltaMovement.z);
            float[] sxxxxxxxxxx = RotationModelFeatures.buildFeatures(yRot, xRot, yaw, pitch, derivatives.velYaw(), derivatives.velPitch(), derivatives.accelYaw(), derivatives.accelPitch(), derivatives.jerkYaw(), derivatives.jerkPitch(), 0.0, sxxxxxxxxx, this.lastDistance, false, derivatives.errVelYaw(), derivatives.errVelPitch());
            float[] sxxxxxxxxxxx = this.predictFeatures(sxxxxxxxxxx, sxxxxx, sxxxxxx, xRot, false, true, false);
            float sxxxxxxxxxxxx = yRot + RotationMath.snapToGcd(sxxxxxxxxxxx[0]);
            float sxxxxxxxxxxxxx = Mth.clamp(xRot + RotationMath.snapToGcd(sxxxxxxxxxxx[1]), -89.9F, 89.9F);
            this.derivatives.push(derivatives, yRot, xRot, sxxxxx, sxxxxxx);
            return new float[] { sxxxxxxxxxxxx, sxxxxxxxxxxxxx };
        } else {
            this.wasLooking = false;
            this.derivatives.reset();
            return null;
        }
    }

    @Override
    public String name() {
        return this.modelName;
    }

    private static float yawOf(float[] var0) {
        return Float.isFinite(var0[0]) ? Mth.clamp(var0[0], -180.0F, 180.0F) : 0.0F;
    }

    @Override
    public void reset() {
        if (this.aura != null) {
            this.resetModel();
        }
        this.target = null;
        this.wasLooking = false;
        this.sessionActive = false;
        this.wasAttacking = false;
        RotationManager.clearFrameResetCondition();
        this.clearCachedRotation();
        this.resetHistory();
    }

    private static float approach(float var0, float var1, float var2, float var3) {
        float var4 = Mth.clamp(var1, -var2, var2);
        return var0 + (var4 - var0) * var3;
    }

    private void deactivate() {
        this.sessionActive = false;
        this.resetModel();
        this.clearCachedRotation();
        this.resetHistory();
    }

    public UniversalAuraRotation(String var1, Supplier<OnnxRotationModel> supplier) {
        this.lastDistance = 3.0;
        this.modelName = var1;
        this.modelSupplier = supplier;
    }

    private boolean snapToPrediction() {
        float[] var1 = this.predictRotation();
        if (var1 != null && mc.player != null) {
            mc.player.setYRot(var1[0]);
            mc.player.setXRot(var1[1]);
            return false;
        } else {
            return true;
        }
    }

    private void resetModel() {
        OnnxRotationModel onnxRotationModel = this.resolveModel();
        if (onnxRotationModel != null) {
            onnxRotationModel.resetState();
        }
    }

    private float[] predictFeatures(float[] var1, float var2, float var3, float var4, boolean var5, boolean var6, boolean var7) {
        float[] var8 = this.resolveModel().predict(var1);
        float var9 = yawOf(var8);
        float var10 = pitchOf(var8);
        float var11 = var9;
        float var12 = var10;
        if (this.aura.isAimAssistEnabled() && !var5 && var6) {
            var11 = approach(var11, var2, 180.0F, this.aura.yawAssistStrength());
            var12 = approach(var12, var3, 90.0F, this.aura.pitchAssistStrength());
        }
        if (var7 && !var5 && this.aura.shouldUseMaceFall()) {
            var11 = Mth.clamp(var2, -60.0F, 60.0F);
            var12 = Mth.clamp(var3, -60.0F, 60.0F);
        }
        var12 = Mth.clamp(var12, -89.9F - var4, 89.9F - var4);
        return new float[] { var11, var12, var9, var10 };
    }

    private void switchTarget(LivingEntity entity) {
        this.target = entity;
        this.resetModel();
        this.clearCachedRotation();
        this.resetHistory();
    }

    @Override
    public Rotation currentRotation(AttackAura attackAura) {
        if (this.aura == null) {
            this.aura = attackAura;
        }
        RotationManager.setFrameResetCondition(this::snapToPrediction);
        return null;
    }

    private static void applyRotation(AuraRotationController.RotationStep var0, float var1, float var2) {
        var0.setYaw(var1);
        var0.setPitch(var2);
        var0.setSpeeds(0.0F, 0.0F);
        var0.setReturnSpeeds(0.0F, 0.0F);
        var0.setVisible(false);
    }

    private void clearCachedRotation() {
        this.lastRotation = null;
        this.hasRotation = false;
        this.rotationAge = 0;
    }

    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        if (var1.pass() == AuraRotationUpdateMode.FIXED_60) {
            if (mc.player != null) {
                this.aura = var1.aura();
                LivingEntity entity = var1.target();
                if (entity != null) {
                    if (entity != this.target) {
                        this.switchTarget(entity);
                    }
                    if (this.sessionActive) {
                        this.deactivate();
                    }
                    float yRot = mc.player.getYRot();
                    float xRot = mc.player.getXRot();
                    float var6 = Mth.wrapDegrees(var1.rawYaw() - yRot);
                    float var7 = var1.rawPitch() - xRot;
                    boolean var8 = this.derivatives.hasRotation();
                    RotationDerivativeHistory.Derivatives derivatives = this.derivatives.derive(yRot, xRot, var6, var7);
                    Vec3 vector = ModuleManager.INSTANCE.absentModule().isActive() ? Vec3.ZERO : mc.player.getDeltaMovement();
                    Vec3 deltaMovement = entity.getDeltaMovement();
                    double var12 = Math.hypot(vector.x, vector.z);
                    double var14 = Math.hypot(deltaMovement.x, deltaMovement.z);
                    this.lastDistance = this.aura.distanceToTarget(entity);
                    float attackStrengthScale = mc.player.getAttackStrengthScale(0.0F);
                    if (var8 && this.speedScale - attackStrengthScale > 0.55F && attackStrengthScale < 0.3F) {
                        boolean var20 = true;
                    } else {
                        boolean var10000 = false;
                    }
                    float[] var18 = RotationModelFeatures.buildFeatures(yRot, xRot, var1.rawYaw(), var1.rawPitch(), derivatives.velYaw(), derivatives.velPitch(), derivatives.accelYaw(), derivatives.accelPitch(), derivatives.jerkYaw(), derivatives.jerkPitch(), var14, var12, this.lastDistance, var1.rayTrace(), derivatives.errVelYaw(), derivatives.errVelPitch());
                    {
                        float[] var19 = this.stepToward(var2, var18, yRot, xRot, var6, var7, var1.rayTrace());
                        if (var19 != null) {
                            var1.rayTrace();
                        }
                    }
                    this.derivatives.push(derivatives, yRot, xRot, var6, var7);
                    this.speedScale = attackStrengthScale;
                }
            }
        }
    }

    private static float pitchOf(float[] var0) {
        return Float.isFinite(var0[1]) ? Mth.clamp(var0[1], -90.0F, 90.0F) : 0.0F;
    }

    private OnnxRotationModel resolveModel() {
        if (this.modelSupplier != null) {
            return this.modelSupplier.get();
        } else {
            return this.aura == null ? null : this.aura.getRotationModel();
        }
    }

    @Override
    public Set<AuraRotationUpdateMode> updateModes() {
        return EnumSet.of(AuraRotationUpdateMode.FIXED_60);
    }

    @Override
    public void onRotationBlocked() {
        this.sessionActive = true;
    }

    private void resetHistory() {
        this.derivatives.reset();
        this.speedScale = 1.0F;
    }

    public UniversalAuraRotation() {
        this("Universal", null);
    }

    private float[] stepToward(AuraRotationController.RotationStep var1, float[] var2, float var3, float var4, float var5, float var6, boolean var7) {
        OnnxRotationModel onnxRotationModel = this.resolveModel();
        if (onnxRotationModel != null && onnxRotationModel.isLoaded()) {
            RotationManager.setFrameResetCondition(this::snapToPrediction);
            this.wasLooking = false;
            float[] var9 = this.predictFeatures(var2, var5, var6, var4, var7, this.aura.isWithinAssistWindow(), true);
            var1.setYaw(var3 + var9[0]);
            var1.setPitch(var4 + var9[1]);
            var1.setSpeeds(9999.0F, 9999.0F);
            var1.setReturnSpeeds(180.0F, 180.0F);
            var1.setVisible(false);
            return var9;
        } else {
            applyRotation(var1, var3, var4);
            return null;
        }
    }

    public boolean isWasAttacking() {
        return this.wasAttacking;
    }

    @Override
    public void onRotationApplied(boolean var1) {
        this.wasAttacking = var1;
    }
}
