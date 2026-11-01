package ess.ll.client.features.crystalv2;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CalculateFovUtil implements V2Access {
   public static float calculateFov(float cameraYaw, float cameraPitch, float targetYaw, float targetPitch) {
      Vec3 cameraDirection = getDirectionVector(cameraYaw, cameraPitch);
      Vec3 targetDirection = getDirectionVector(targetYaw, targetPitch);
      double dotProduct = cameraDirection.dot(targetDirection);
      dotProduct = Math.clamp(dotProduct, -1.0, 1.0);
      float angleRadians = (float)Math.acos(dotProduct);
      return (float)Math.toDegrees(angleRadians);
   }

   public static Vec3 getDirectionVector(float yaw, float pitch) {
      float yawRadians = (float)Math.toRadians(yaw);
      float pitchRadians = (float)Math.toRadians(pitch);
      float x = -Mth.cos(pitchRadians) * Mth.sin(yawRadians);
      float y = -Mth.sin(pitchRadians);
      float z = Mth.cos(pitchRadians) * Mth.cos(yawRadians);
      return new Vec3(x, y, z);
   }

   public static Rotation calculate(Vec3 fromVec, Vec3 toVec) {
      Vec3 diff = toVec.subtract(fromVec);
      double distance = Math.hypot(diff.x, diff.z);
      float yaw = (float)(Mth.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.atan2(diff.y, distance) * 180.0 / Math.PI));
      return new Rotation(yaw, pitch);
   }

   public static Rotation calculate(net.minecraft.world.entity.Entity entity) {
      return calculate(entity.getPosition(1.0F).add(0.0, entity.getEyeHeight(entity.getPose()), 0.0));
   }

   public static Rotation calculate(Vec3 toVec) {
      return calculate(mc.player.getPosition(1.0F).add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0), toVec);
   }

   public CalculateFovUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
