package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.internal.StringConcatSupport;
import ess.ll.client.render.targetfx.model.ThemeColorRole;
import ess.ll.client.render.targetfx.ClickGuiRenderer;
import java.awt.Color;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
public final class ColorMath {
   private static final long PULSE_PERIOD_MILLIS = 10000L;
   private static final float RAINBOW_TIME_DIVISOR = 8.0F;
   private static final float FULL_HUE_CYCLE = 360.0F;
   private static final float RAINBOW_SATURATION = 0.85F;
   private static final int OPAQUE_ALPHA_MASK = -16777216;
   private static final float COLOR_INDEX_STEP = 16.0F;
   private static final int RGB_MASK = 16777215;
   private static final float PULSE_PERIOD_FLOAT = 10000.0F;
   private static final float COSINE_CENTER = 0.5F;
   private static final float COSINE_AMPLITUDE = 0.5F;
   private static final double PI = Math.PI;
   private static final double COSINE_FREQUENCY = 2.0;
   private static final long HUE_CYCLE_STEPS = 360L;
   private static final float HALF_HUE_CYCLE = 180.0F;
   private static final float CHANNEL_MAX = 255.0F;
   private static final float SATURATION_BOOST = 1.5F;
   private static final String HEX_HASH_PREFIX = "#";
   private static final String HEX_PREFIX_LOWER = "0x";
   private static final String HEX_PREFIX_UPPER = "0X";
   private static final double CHANNEL_MAX_DOUBLE = 255.0;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static int alphaChannel(int value) {
      return value >> 24 & 0xFF;
   }

   public static Vector4f getVector4f(int value2) {
      float amount = (value2 >> 24 & 0xFF) / CHANNEL_MAX;
      float amount2 = (value2 >> 16 & 0xFF) / CHANNEL_MAX;
      float amount3 = (value2 >> 8 & 0xFF) / CHANNEL_MAX;
      float amount4 = (value2 & 0xFF) / CHANNEL_MAX;
      return new Vector4f(amount2, amount3, amount4, amount);
   }

   public static int greenChannel(int value3) {
      return value3 >> 8 & 0xFF;
   }

   public static int animatedGradientColor(int value4, int value5, int value6, int value7) {
      int nowMillis = (int)((System.currentTimeMillis() / value7 + value6) % HUE_CYCLE_STEPS);
      nowMillis = (nowMillis > 180 ? 360 - nowMillis : nowMillis) + 180;
      int value8 = blendRgba(value4, value5, Mth.clamp(nowMillis / HALF_HUE_CYCLE - 1.0F, 0.0F, 1.0F));
      float[] instance = normalizedRgba(value8);
      float[] instance2 = Color.RGBtoHSB(
         (int)(instance[0] * CHANNEL_MAX), (int)(instance[1] * CHANNEL_MAX), (int)(instance[2] * CHANNEL_MAX), null
      );
      instance2[1] *= SATURATION_BOOST;
      instance2[1] = Math.min(instance2[1], 1.0F);
      return Color.HSBtoRGB(instance2[0], instance2[1], instance2[2]);
   }

   public static float[] normalizedRgba(int value9) {
      return new float[]{
         (value9 >> 16 & 0xFF) / CHANNEL_MAX,
         (value9 >> 8 & 0xFF) / CHANNEL_MAX,
         (value9 & 0xFF) / CHANNEL_MAX,
         (value9 >> 24 & 0xFF) / CHANNEL_MAX
      };
   }

   public static int interpolateRgba(int value10, int value11, float amount5) {
      Vector4f vector4f = getVector4f(value10).mul(CHANNEL_MAX);
      Vector4f vector4f2 = getVector4f(value11).mul(CHANNEL_MAX);
      return packRgba(
         (int)SettingAnimationMath.interpolateLinear(vector4f.x, vector4f2.x, amount5),
         (int)SettingAnimationMath.interpolateLinear(vector4f.y, vector4f2.y, amount5),
         (int)SettingAnimationMath.interpolateLinear(vector4f.z, vector4f2.z, amount5),
         SettingAnimationMath.interpolateLinear(vector4f.w, vector4f2.w, amount5)
      );
   }

   private ColorMath() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static int packRgba(int value12, int value13, int value14, double amount6) {
      return (int)amount6 << 24 | value12 << 16 | value13 << 8 | value14;
   }

   public static int themeGradientColor(int value15) {
      if (ClickGuiRenderer.checkCondition2()) {
         float nowMillis2 = ((float)System.currentTimeMillis() / RAINBOW_TIME_DIVISOR + value15 * 16) % FULL_HUE_CYCLE / FULL_HUE_CYCLE;
         if (nowMillis2 < 0.0F) {
            nowMillis2++;
         }

         return Color.HSBtoRGB(nowMillis2, RAINBOW_SATURATION, 1.0F) | OPAQUE_ALPHA_MASK;
      } else {
         return pulsingThemeColor(
            ClickGuiRenderer.calculateValue3(ThemeColorRole.MAIN),
            ClickGuiRenderer.calculateValue3(ThemeColorRole.MAIN_TWO),
            value15 * COLOR_INDEX_STEP / FULL_HUE_CYCLE,
            255
         );
      }
   }

   public static int parseHexColor(String text) {
      String text2 = text.trim();
      if (text2.startsWith(HEX_HASH_PREFIX)) {
         text2 = text2.substring(1);
      }

      if (text2.startsWith(HEX_PREFIX_LOWER) || text2.startsWith(HEX_PREFIX_UPPER)) {
         text2 = text2.substring(2);
      }

      if (text2.length() == 3 || text2.length() == 4) {
         char instance3 = text2.charAt(0);
         char instance4 = text2.charAt(1);
         char instance5 = text2.charAt(2);
         char instance6 = text2.length() == 4 ? text2.charAt(3) : 70;
         text2 = StringConcatSupport.concat135(instance3, instance3, instance4, instance4, instance5, instance5, instance6, instance6);
      }

      if (text2.length() == 6) {
         int value16 = Integer.parseInt(text2.substring(0, 2), 16);
         int value17 = Integer.parseInt(text2.substring(2, 4), 16);
         int value18 = Integer.parseInt(text2.substring(4, 6), 16);
         return packRgba(value16, value17, value18, CHANNEL_MAX_DOUBLE);
      } else if (text2.length() == 8) {
         int value19 = Integer.parseInt(text2.substring(0, 2), 16);
         int value20 = Integer.parseInt(text2.substring(2, 4), 16);
         int value21 = Integer.parseInt(text2.substring(4, 6), 16);
         int value22 = Integer.parseInt(text2.substring(6, 8), 16);
         return packRgba(value19, value20, value21, value22);
      } else {
         throw new IllegalArgumentException(StringConcatSupport.concat136(text));
      }
   }

   public static int withAlpha(int value23, float amount7) {
      int value24 = Math.min(255, Math.max(0, (int)(amount7 * CHANNEL_MAX)));
      return packRgba(redChannel(value23), greenChannel(value23), blueChannel(value23), value24);
   }

   public static int pulsingThemeColor(int value25, int value26, float amount8, int value27) {
      if ((value25 & RGB_MASK) == (value26 & RGB_MASK)) {
         return packRgba(redChannel(value25), greenChannel(value25), blueChannel(value25), value27);
      } else {
         float nowMillis3 = ((float)(System.currentTimeMillis() % PULSE_PERIOD_MILLIS) / PULSE_PERIOD_FLOAT + amount8) % 1.0F;
         if (nowMillis3 < 0.0F) {
            nowMillis3++;
         }

         float amount9 = COSINE_CENTER - COSINE_AMPLITUDE * (float)Math.cos(nowMillis3 * PI * COSINE_FREQUENCY);
         int value28 = blendRgba(value25, value26, amount9);
         return packRgba(redChannel(value28), greenChannel(value28), blueChannel(value28), value27);
      }
   }

   public static int blueChannel(int value29) {
      return value29 & 0xFF;
   }

   public static int redChannel(int value30) {
      return value30 >> 16 & 0xFF;
   }

   public static int opaqueRgb(int value31, int value32, int value33) {
      return OPAQUE_ALPHA_MASK | value31 << 16 | value32 << 8 | value33;
   }

   public static Component getComponent(String text3) {
      MutableComponent mutableComponent = Component.empty();

      for (int index = 0; index < text3.length(); index++) {
         char instance7 = text3.charAt(index);
         MutableComponent mutableComponent2 = Component.literal(String.valueOf(instance7)).setStyle(Style.EMPTY.withColor(new Color(themeGradientColor(index)).getRGB()));
         mutableComponent.append(mutableComponent2);
      }

      return mutableComponent;
   }

   public static int blendRgba(int value34, int value35, float amount10) {
      float[] instance8 = normalizedRgba(value34);
      float[] instance9 = normalizedRgba(value35);
      return packRgba(
         (int)SettingAnimationMath.interpolateLinear(instance8[0] * CHANNEL_MAX, instance9[0] * CHANNEL_MAX, amount10),
         (int)SettingAnimationMath.interpolateLinear(instance8[1] * CHANNEL_MAX, instance9[1] * CHANNEL_MAX, amount10),
         (int)SettingAnimationMath.interpolateLinear(instance8[2] * CHANNEL_MAX, instance9[2] * CHANNEL_MAX, amount10),
         (int)SettingAnimationMath.interpolateLinear(instance8[3] * CHANNEL_MAX, instance9[3] * CHANNEL_MAX, amount10)
      );
   }

   public static int scaleRgba(int value36, float amount11) {
      int value37 = (int)(redChannel(value36) * amount11);
      int value38 = (int)(greenChannel(value36) * amount11);
      int value39 = (int)(blueChannel(value36) * amount11);
      int value40 = (int)(alphaChannel(value36) * amount11);
      return packRgba(value37, value38, value39, value40);
   }
}
