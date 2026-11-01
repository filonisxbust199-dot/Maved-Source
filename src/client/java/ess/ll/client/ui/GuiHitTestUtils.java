package ess.ll.client.ui;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.RaycastUtils;
import com.mojang.blaze3d.platform.Window;
import java.util.Random;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class GuiHitTestUtils implements MinecraftAccess {
   public static final Random RANDOM = new Random();
   private static final double ROUND_SCALE = 100.0;
   private static final double ROUND_DIVISOR = 100.0;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static int toFramebufferScale(int value) {
      Window window = mc.getWindow();
      return value * window.getGuiScale() / 2;
   }

   public static double difference(double a, double b) {
      return a - b;
   }

   private GuiHitTestUtils() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static double roundToStep(double value, double step) {
      double rounded = Math.round(value / step) * step;
      return Math.round(rounded * ROUND_SCALE) / ROUND_DIVISOR;
   }

   public static Vec3 lerpTo(Vec3 to, Vec3 from, float delta) {
      return new Vec3(lerpTo(to.x(), from.x(), delta), lerpTo(to.y(), from.y(), delta), lerpTo(to.z(), from.z(), delta));
   }

   public static boolean canSee(Vec3 target) {
      if (mc.player == null) {
         return false;
      }

      Vec3 eyePosition = mc.player.getEyePosition();
      return RaycastUtils.clip(eyePosition, target, Block.COLLIDER, Fluid.NONE).getType() == Type.MISS;
   }

   public static boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
   }

   public static double clamp(double min, double max, double value) {
      return Math.clamp(value, min, max);
   }

   public static Vec2 toFramebuffer(float x, float y) {
      return new Vec2(x * mc.getWindow().getGuiScale() / 2.0F, y * mc.getWindow().getGuiScale() / 2.0F);
   }

   public static double lerp(double from, double to, double delta) {
      return lerpTo(to, from, delta);
   }

   public static float randomBetween(float min, float max) {
      return min + RANDOM.nextFloat() * (max - min);
   }

   public static int lerp(int from, int to, double delta) {
      return (int)lerpTo(to, from, delta);
   }

   public static float clamp(float min, float max, float value) {
      return Math.clamp(value, min, max);
   }

   public static double lerpTo(double to, double from, double delta) {
      return from + (to - from) * delta;
   }

   public static float invertInRange(float value, float min, float max) {
      value = Math.clamp(value, min, max);
      return min + (max - value);
   }

   public static float lerp(float from, float to, double delta) {
      return (float)lerpTo(to, from, delta);
   }

   public static int clamp(int min, int max, int value) {
      return Math.clamp(value, min, max);
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = difference(x2, x1);
      double dy = difference(y2, y1);
      double dz = difference(z2, z1);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static float clamp01(float value) {
      return clamp(0.0F, 1.0F, value);
   }

   public static double clamp01(double value) {
      return clamp(0.0, 1.0, value);
   }

   public static double distance(Vec3 from, Vec3 to) {
      double dx = difference(to.x, from.x);
      double dy = difference(to.y, from.y);
      double dz = difference(to.z, from.z);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static boolean isInside(int mouseX, int mouseY, float x, float y, float width, float height) {
      return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
   }

   public static boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
   }
}
