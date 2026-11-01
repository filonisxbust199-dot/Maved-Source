package ess.ll.client.features.combat;

import net.minecraft.util.Mth;

public final class RotationModelFeatures {
    public static float normalizeYaw(float degrees) {
        return Mth.clamp(degrees / 180.0F, -1.0F, 1.0F);
    }

    private RotationModelFeatures() {
    }

    public static float[] buildFeatures(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float yawVelocity, float pitchVelocity, float yawAcceleration, float pitchAcceleration, float yawJerk, float pitchJerk, double targetSpeed, double playerSpeed, double targetDistance, boolean lookingAtTarget, float yawErrorVelocity, float pitchErrorVelocity) {
        float yawError = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchError = targetPitch - currentPitch;
        return new float[] { normalizeYaw(yawError), normalizePitch(pitchError), Mth.clamp(yawVelocity / 30.0F, -1.0F, 1.0F), Mth.clamp(pitchVelocity / 30.0F, -1.0F, 1.0F), Mth.clamp((float) ((targetSpeed + playerSpeed) / 2.0), 0.0F, 1.0F), Mth.clamp((float) (targetDistance / 8.0), 0.0F, 1.0F), Mth.clamp(yawAcceleration / 30.0F, -1.0F, 1.0F), Mth.clamp(pitchAcceleration / 30.0F, -1.0F, 1.0F), Mth.clamp(yawJerk / 30.0F, -1.0F, 1.0F), Mth.clamp(pitchJerk / 30.0F, -1.0F, 1.0F), lookingAtTarget ? 1.0F : 0.0F, Mth.clamp(yawErrorVelocity / 30.0F, -1.0F, 1.0F), Mth.clamp(pitchErrorVelocity / 30.0F, -1.0F, 1.0F) };
    }

    public static float normalizePitch(float degrees) {
        return Mth.clamp(degrees / 90.0F, -1.0F, 1.0F);
    }
}
