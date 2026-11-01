package ess.ll.client.setting;

import net.minecraft.util.Mth;

public class SliderSetting extends Setting<Float> {
   private static final float DEGREES = 360.0F;
   private static final float DEGREES_OFFSET = 360.0F;
   private static final float DEGREES_MOD = 360.0F;

   public float cos() {
      return Mth.cos((float)Math.toRadians(this.getDegrees()));
   }

   public float getDegrees() {
      Float value = this.getValue();
      float raw = value == null ? 0.0F : value;
      return (raw % DEGREES + DEGREES_OFFSET) % DEGREES_MOD;
   }

   public float sin() {
      return Mth.sin((float)Math.toRadians(this.getDegrees()));
   }

   public SliderSetting(String text) {
      super(text);
   }
}
