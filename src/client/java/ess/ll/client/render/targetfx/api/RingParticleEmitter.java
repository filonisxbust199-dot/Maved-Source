package ess.ll.client.render.targetfx.api;

public final class RingParticleEmitter {
   private static final float currentValue8 = 1.65F;
   private static final float currentValue12 = 0.15F;
   private static final float currentValue5 = 0.5F;
   private static final float currentValue = 0.7F;
   private static final float currentValue11 = 0.2F;
   private static final float currentValue13 = 1.7F;
   private static final double currentValue10 = 1.5;
   private static final float currentValue7 = 255.0F;
   private static final float currentValue6 = 0.08F;
   private static final float currentValue3 = 255.0F;
   private static final float currentValue14 = 0.22F;
   private static final float currentValue15 = 1.75F;
   private static final double currentValue9 = 1.5;
   private static final double currentValue2 = 30.0;
   private static final float currentValue4 = 1.08F;

   public static void updateState(float amount, float amount2, double amount3, float amount4, float amount5, int value, RingParticleEmitter.dVYgkAAicLJAPZh8 instance) {
      float amount6 = amount * currentValue8;
      float amount7 = amount2 - currentValue12;
      float amount8 = Math.max(currentValue5, currentValue - currentValue11 * amount4);

      for (int index = 0; index < 360; index += 2) {
         double angleRadians = Math.toRadians(index + amount3);
         float amount9 = (float)(Math.cos(angleRadians) * amount6 * amount8);
         float amount10 = (float)(Math.sin(angleRadians) * amount6 * amount8);

         for (int index2 = 0; index2 < value; index2++) {
            float angleRadians2 = amount7 / currentValue13 + amount7 / 2.0F * (float)Math.cos(Math.toRadians(amount3 / currentValue10 + index2 * 2));
            int value2 = Math.clamp((long)((int)(currentValue7 * amount4 * ((float)index2 / value) * currentValue6)), 0, 255);
            instance.emitParticle(amount9, angleRadians2, amount10, amount5, value2);
         }
      }

      int value3 = Math.clamp((long)((int)(currentValue3 * amount4 * currentValue14)), 0, 255);

      for (int index3 = 0; index3 < 360; index3 += 2) {
         double angleRadians3 = Math.toRadians(index3 + amount3);
         float amount11 = (float)(Math.cos(angleRadians3) * amount6 * amount8);
         float amount12 = (float)(Math.sin(angleRadians3) * amount6 * amount8);
         float angleRadians4 = amount7 / currentValue15 + amount7 / 2.0F * (float)Math.cos(Math.toRadians(amount3 / currentValue9 + currentValue2));
         instance.emitParticle(amount11, angleRadians4, amount12, amount5 * currentValue4, value3);
      }
   }

   private RingParticleEmitter() {
   }

   @FunctionalInterface
   public interface dVYgkAAicLJAPZh8 {
      void emitParticle(float amount13, float amount14, float amount15, float amount16, int value4);
   }
}
