package ess.ll.client.util;

import ess.ll.client.integration.MinecraftAccess;
public final class RotationMath implements MinecraftAccess {
   private static final double MOUSE_STEP_FACTOR = 0.15;
   private static final double SENSITIVITY_SCALE = 0.6F;
   private static final double SENSITIVITY_OFFSET = 0.2F;
   private static final double SENSITIVITY_POWER = 3.0;
   private static final double SENSITIVITY_MULTIPLIER = 8.0;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static float snapToGcd(float angle) {
      return gcdSteps(angle) * gcd();
   }

   public static float gcdSteps(float angle) {
      return Math.round(angle / gcd());
   }

   private RotationMath() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static float gcd() {
      return (float)(sensitivityFactor() * MOUSE_STEP_FACTOR);
   }

   public static float sensitivityFactor() {
      double sensitivity = mc.options.sensitivity().get();
      return (float)(Math.pow(sensitivity * SENSITIVITY_SCALE + SENSITIVITY_OFFSET, SENSITIVITY_POWER) * SENSITIVITY_MULTIPLIER);
   }
}
