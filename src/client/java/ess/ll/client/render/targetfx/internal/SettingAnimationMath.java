package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.api.MinecraftContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
public final class SettingAnimationMath {
   private static final double RADIANS_TO_DEGREES = 180.0 / Math.PI;
   private static final double HORIZONTAL_ANGLE_OFFSET = 90.0;
   private static final double VERTICAL_RADIANS_TO_DEGREES = 180.0 / Math.PI;
   private static final double TICKS_PER_SECOND = 20.0;
   private static final double RANDOM_JITTER_SCALE = 0.02F;
   private static final double RANDOM_POWER_RANGE = 0.7;
   private static final double RANDOM_WEIGHT_SCALE = 0.8;
   private static final double RANDOM_WEIGHT_OFFSET = 0.1;
   private static final double RANDOM_LOG_SCALE = 3.0;
   private static final double RANDOM_LOG_WEIGHT = 0.5;
   private static final double RANDOM_BASE_WEIGHT = 0.5;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static double frameDurationSeconds() {
      return MinecraftContext.MINECRAFT.getFps() > 0 ? 1.0 / MinecraftContext.MINECRAFT.getFps() : 1.0;
   }

   public static float interpolateByFrameTime(float amount, float amount2, float amount3) {
      return (float)(amount + (amount2 - amount) * Mth.clamp(frameDurationSeconds() * amount3, 0.0, 1.0));
   }

   private SettingAnimationMath() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static float randomizedInterpolation(float amount4, float amount5) {
      SecureRandom secureRandom = new SecureRandom();
      double amount6 = secureRandom.nextDouble();
      double amount7 = secureRandom.nextDouble();
      double amount8 = secureRandom.nextGaussian() * RANDOM_JITTER_SCALE;
      double amount9 = Math.pow(amount6, 1.0 + secureRandom.nextDouble() * RANDOM_POWER_RANGE);
      double amount10 = (amount7 * RANDOM_WEIGHT_SCALE + RANDOM_WEIGHT_OFFSET)
         * (Math.log1p(amount6 * RANDOM_LOG_SCALE) * RANDOM_LOG_WEIGHT + RANDOM_BASE_WEIGHT);
      return (float)(amount4 + (amount5 - amount4) * amount9 * amount10 + amount8);
   }

   public static Vec3 interpolatePosition(Vec3 position, Vec3 position2, float amount11) {
      return new Vec3(Mth.lerp(amount11, position.x, position2.x), Mth.lerp(amount11, position.y, position2.y), Mth.lerp(amount11, position.z, position2.z));
   }

   public static Vec2 anglesToPosition(Vec3 position3) {
      return anglesToCoordinates(position3.x, position3.y, position3.z);
   }

   public static Vec3 interpolatePositionByFrameTime(Vec3 position4, Vec3 position5, float amount12) {
      return new Vec3(
         interpolateByFrameTimeClamped((float)position4.x(), (float)position5.x(), amount12),
         interpolateByFrameTimeClamped((float)position4.y(), (float)position5.y(), amount12),
         interpolateByFrameTimeClamped((float)position4.z(), (float)position5.z(), amount12)
      );
   }

   public static double estimateEntityBps(Entity entity) {
      double x = entity.getX() - entity.xo;
      double y = entity.getY() - entity.yo;
      double z = entity.getZ() - entity.zo;
      double amount13 = Math.sqrt(x * x + y * y + z * z);
      return TICKS_PER_SECOND * amount13;
   }

   public static Vec3 clampPositionToBox(Vec3 position6, AABB aabb) {
      return new Vec3(Mth.clamp(position6.x(), aabb.minX, aabb.maxX), Mth.clamp(position6.y(), aabb.minY, aabb.maxY), Mth.clamp(position6.z(), aabb.minZ, aabb.maxZ));
   }

   public static double roundToStep(double amount14, double amount15) {
      double amount16 = Math.round(amount14 / amount15) * amount15;
      BigDecimal bigDecimal = new BigDecimal(amount16);
      bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
      return bigDecimal.doubleValue();
   }

   public static float interpolateLinear(float amount17, float amount18, float amount19) {
      return Mth.lerp(amount19, amount17, amount18);
   }

   public static Vec2 anglesToCoordinates(double amount20, double amount21, double amount22) {
      double x2 = amount20 - MinecraftContext.MINECRAFT.player.getX();
      double amount23 = amount21 - MinecraftContext.MINECRAFT.player.getEyeY();
      double z2 = amount22 - MinecraftContext.MINECRAFT.player.getZ();
      double amount24 = Mth.sqrt((float)(x2 * x2 + z2 * z2));
      float amount25 = (float)(Mth.atan2(z2, x2) * RADIANS_TO_DEGREES - HORIZONTAL_ANGLE_OFFSET);
      float amount26 = (float)(-Mth.atan2(amount23, amount24) * VERTICAL_RADIANS_TO_DEGREES);
      return new Vec2(amount25, amount26);
   }

   public static float interpolateByFrameTimeClamped(float amount27, float amount28, float amount29) {
      return (1.0F - Mth.clamp((float)(frameDurationSeconds() * amount29), 0.0F, 1.0F)) * amount27
         + Mth.clamp((float)(frameDurationSeconds() * amount29), 0.0F, 1.0F) * amount28;
   }
}
