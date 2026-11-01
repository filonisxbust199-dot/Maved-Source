package ess.ll.client.weather;

public final class WeatherChannel {
    public static final int CONST_intValue = 0;
    public static final int CONST_intValue2 = 1;
    public static final int CONST_intValue3 = 2;
    public static final float CONST_floatValue = -1.0F;
    public final float[] floatValues;
    public final float[] floatValues2;
    public final float[] floatValues3;
    public final float[] floatValues4;
    public final float[] floatValues5;
    public final float[] floatValues6;
    public final float floatValue;
    public final float floatValue2;
    public final float floatValue3;
    public final float floatValue4;
    public final float floatValue5;
    public final float floatValue6;
    public final float floatValue7;
    public final float[] floatValues7;
    public final float[] floatValues8;
    public final float floatValue8;
    public final float floatValue9;
    public final float floatValue10;
    public final float floatValue11;
    public final float floatValue12;
    public final float[] floatValues9;
    public final float[] floatValues10;
    public final float floatValue13;
    public final int intValue;
    public final float floatValue14;
    public final float[] floatValues11;
    public final float floatValue15;
    public final float floatValue16;
    public final float floatValue17;
    public final int intValue2;
    public final float floatValue18;
    public final float floatValue19;
    public final float floatValue20;
    public final float floatValue21;
    public final int intValue3;
    public final float floatValue22;
    public final float floatValue23;
    public final int intValue4;
    public final int intValue5;
    public final float floatValue24;
    public final float floatValue25;
    public final float floatValue26;
    public final boolean flag;
    public final int intValue6;
    public final int intValue7;
    public final float floatValue27;
    public final int intValue8;
    public static final WeatherChannel INSTANCE = new WeatherChannel.Entry().withFog(0.098F, 0.116F, 0.142F, 0.178F, 0.202F, 0.232F, 0.062F, 0.07F, 0.084F).withSky(0.0F, 0.2F).withClouds(0.93F, 0.58F, 0.032F, 1.45F, 0.38F, 0.41F, 0.47F, 0.19F, 0.21F, 0.26F).withSnow(0.45F, 0.2F, 0.23F, 0.27F).withMist(0.22F, 0.8F, 0.86F, 1.05F).withLight(-13749704, 96.0F, 0.86F, 0.9F, 0.98F, 0.84F, 0.72F, -1.0F).withWind(1, 0.78F, 1.1F, 0.2F, 0.07F, -1766406440, 0.55F).withStorm(1.0F, -1710880240, -4927770, 1.0F, 0.6F, false).withFootprints(1342573072, 9000).withFrost(0.2F, -12959156).buildChannel();
    public static final WeatherChannel INSTANCE2 = new WeatherChannel.Entry().withFog(0.098F, 0.116F, 0.142F, 0.178F, 0.202F, 0.232F, 0.062F, 0.07F, 0.084F).withSky(0.0F, 0.2F).withClouds(0.93F, 0.58F, 0.032F, 1.45F, 0.38F, 0.41F, 0.47F, 0.19F, 0.21F, 0.26F).withSnow(0.45F, 0.2F, 0.23F, 0.27F).withMist(0.22F, 0.8F, 0.86F, 1.05F).withLight(-13749704, 96.0F, 0.86F, 0.9F, 0.98F, 0.84F, 0.72F, -1.0F).withWind(1, 0.78F, 1.1F, 0.2F, 0.07F, -1766406440, 0.55F).withStorm(1.0F, -1274672624, -4927770, 0.8F, 0.35F, false).withStars(0.0F).withFootprints(1342573072, 9000).withFrost(0.2F, -12959156).buildChannel();
    public static final WeatherChannel INSTANCE3 = new WeatherChannel.Entry().withFog(0.105F, 0.3F, 0.7F, 0.7F, 0.82F, 0.94F, 0.27F, 0.35F, 0.46F).withSky(0.03F, 0.85F).withClouds(0.26F, 0.1F, 0.02F, 1.05F, 1.0F, 0.995F, 0.98F, 0.64F, 0.72F, 0.84F).withSnow(0.26F, 0.66F, 0.78F, 0.92F).withRain(0.55F, 0.32F, 0.0F, 0.24F).withLight(-5716770, 150.0F, 0.96F, 0.99F, 1.06F, 1.06F, 0.72F, -1.0F).withWind(2, 0.42F, 0.32F, 0.1F, 0.05F, -924520710, 0.0F).withStorm(1.0F, 672143400, -2693388, 0.14F, 0.1F, true).withStars(0.0F).withFootprints(1679831104, 14000).withFrost(0.18F, -4336924).buildChannel();
    public static final WeatherChannel INSTANCE4 = new WeatherChannel.Entry().withFog(0.28F, 0.3F, 0.33F, 0.4F, 0.42F, 0.45F, 0.24F, 0.25F, 0.28F).withSky(0.008F, 0.35F).withClouds(0.75F, 0.3F, 0.02F, 1.5F, 0.52F, 0.54F, 0.56F, 0.36F, 0.37F, 0.4F).withSnow(1.0F, 0.44F, 0.46F, 0.49F).withLight(-8617336, 40.0F, 0.95F, 0.97F, 1.0F, 0.96F, 0.65F, -1.0F).withWind(0, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F).withStorm(0.5F, 772937760, -5652790, 0.38F, 0.18F, false).withFootprints(1007687712, 6000).withFrost(1.0F, -7564132).buildChannel();
    public static final float CONST_floatValue2 = -1.0F;

    private WeatherChannel(WeatherChannel.Entry entry) {
        this.floatValues = entry.floatValues;
        this.floatValues2 = entry.floatValues2;
        this.floatValues3 = entry.floatValues3;
        this.floatValues4 = entry.floatValues4;
        this.floatValues5 = entry.floatValues5;
        this.floatValues6 = entry.floatValues6;
        this.floatValue = entry.floatValue;
        this.floatValue2 = entry.floatValue2;
        this.floatValue3 = entry.floatValue3;
        this.floatValue4 = entry.floatValue4;
        this.floatValue5 = entry.floatValue5;
        this.floatValue6 = entry.floatValue6;
        this.floatValue7 = entry.floatValue7;
        this.floatValues7 = entry.floatValues7;
        this.floatValues8 = entry.floatValues8;
        this.floatValue8 = entry.floatValue8;
        this.floatValue9 = entry.floatValue9;
        this.floatValue10 = entry.floatValue10;
        this.floatValue11 = entry.floatValue11;
        this.floatValue12 = entry.floatValue12;
        this.floatValues9 = entry.floatValues9;
        this.floatValues10 = entry.floatValues10;
        this.floatValue13 = entry.floatValue13;
        this.intValue = entry.intValue;
        this.floatValue14 = entry.floatValue14;
        this.floatValues11 = entry.floatValues11;
        this.floatValue15 = entry.floatValue15;
        this.floatValue16 = entry.saturation;
        this.floatValue17 = entry.floatValue16;
        this.intValue2 = entry.intValue2;
        this.floatValue18 = entry.floatValue17;
        this.floatValue19 = entry.floatValue18;
        this.floatValue20 = entry.floatValue19;
        this.floatValue21 = entry.floatValue20;
        this.intValue3 = entry.intValue3;
        this.floatValue22 = entry.floatValue21;
        this.floatValue23 = entry.floatValue22;
        this.intValue4 = entry.intValue4;
        this.intValue5 = entry.intValue5;
        this.floatValue24 = entry.floatValue23;
        this.floatValue25 = entry.floatValue24;
        this.floatValue26 = entry.floatValue25;
        this.flag = entry.flag;
        this.intValue6 = entry.intValue6;
        this.intValue7 = entry.intValue7;
        this.floatValue27 = entry.floatValue26;
        this.intValue8 = entry.intValue8;
    }

    public WeatherChannel build(DayLight dayLight, WeatherChannel.Data data) {
        return this.build(dayLight, data, null);
    }

    public WeatherChannel build(DayLight dayLight, WeatherChannel.Data data, SkyPreset skyPreset) {
        WeatherChannel.Entry entry = new WeatherChannel.Entry();
        entry.floatValues = dayTint(this.floatValues, dayLight, dayLight.floatValue3, 0.45F);
        float var5 = 1.0F - Math.clamp(dayLight.floatValue2, 0.0F, 1.0F);
        entry.floatValues2 = lerpColor(dayTint(this.floatValues2, dayLight, 1.0F, 1.0F), dayTint(this.floatValues, dayLight, dayLight.floatValue3, 0.45F), var5 * 0.72F);
        entry.floatValues3 = dayTint(this.floatValues3, dayLight, dayLight.floatValue3, 0.7F);
        entry.floatValues4 = (float[]) dayLight.floatValues.clone();
        entry.floatValues5 = new float[] { dayLight.floatValue8, dayLight.floatValue9, dayLight.floatValue10 };
        entry.floatValues6 = (float[]) dayLight.floatValues2.clone();
        entry.floatValue = dayLight.floatValue7;
        float var6 = dayLight.flag ? Math.max(this.floatValue2, 0.018F) : this.floatValue2;
        entry.floatValue2 = var6 * dayLight.floatValue12;
        entry.floatValue3 = this.floatValue3 * dayLight.floatValue11 * dayLight.floatValue12;
        entry.floatValue4 = this.floatValue4;
        entry.floatValue5 = this.floatValue5;
        entry.floatValue6 = this.floatValue6;
        entry.floatValue7 = this.floatValue7;
        entry.floatValues7 = dayTint(this.floatValues7, dayLight, 1.0F, 1.0F);
        entry.floatValues8 = dayTint(this.floatValues8, dayLight, 1.0F, 1.0F);
        entry.floatValue8 = Math.min(1.2F, this.floatValue8 + dayLight.floatValue13);
        entry.floatValue9 = Math.min(1.2F, this.floatValue9 + dayLight.floatValue14);
        float var7 = 1.0F - Math.clamp(dayLight.floatValue2, 0.0F, 1.0F);
        entry.floatValue10 = this.floatValue10 * (0.2F + 0.8F * var7);
        entry.floatValue11 = this.floatValue11 * (0.12F + 0.88F * var7);
        entry.floatValue12 = Math.min(1.2F, this.floatValue12 * dayLight.floatValue15 * data.getFloatValue5() * (1.0F - var5 * 0.55F));
        entry.floatValues9 = dayTint(this.floatValues9, dayLight, 1.0F, 1.0F);
        entry.floatValues10 = this.floatValues10;
        entry.floatValue13 = data.isFlag() ? this.floatValue13 : 0.0F;
        entry.intValue = dayAdjustedColor(this.intValue, dayLight);
        entry.floatValue14 = data.getFloatValue6() > 0.0F ? data.getFloatValue6() : this.floatValue14;
        entry.floatValues11 = new float[] { this.floatValues11[0] * dayLight.floatValue4, this.floatValues11[1] * dayLight.floatValue5, this.floatValues11[2] * dayLight.floatValue6 };
        entry.floatValue15 = this.floatValue15 * dayLight.floatValue16;
        entry.saturation = this.floatValue16;
        entry.floatValue16 = dayLight.floatValue;
        entry.intValue2 = this.intValue2;
        entry.floatValue17 = this.floatValue18 * data.getFloatValue();
        entry.floatValue18 = this.floatValue19;
        entry.floatValue19 = this.floatValue20 * data.getFloatValue2();
        entry.floatValue20 = this.floatValue21 * data.getFloatValue2();
        entry.intValue3 = nightAdjustedColor(this.intValue3, dayLight);
        entry.floatValue21 = this.floatValue22;
        entry.floatValue22 = this.floatValue23;
        entry.intValue4 = this.intValue4;
        entry.intValue5 = dayAdjustedColor(this.intValue5, dayLight);
        entry.floatValue23 = Math.min(1.4F, this.floatValue24 * dayLight.floatValue17 * data.getFloatValue3());
        entry.floatValue24 = this.floatValue25 * dayLight.floatValue17 * data.getFloatValue3();
        entry.floatValue25 = this.floatValue26;
        entry.flag = this.flag;
        entry.intValue6 = this.intValue6;
        entry.intValue7 = this.intValue7;
        entry.floatValue26 = this.floatValue27 * data.getFloatValue4();
        entry.intValue8 = dayAdjustedColor(this.intValue8, dayLight);
        if (skyPreset != null) {
            applyPreset(entry, skyPreset.sample(dayLight.floatValue), dayLight, data);
        }
        entry.intValue = toneMapRgb(entry.floatValues2);
        return new WeatherChannel(entry);
    }

    private static void applyPreset(WeatherChannel.Entry entry, SkyPreset.Data data, DayLight dayLight, WeatherChannel.Data data2) {
        entry.floatValues = data.getFloatValues();
        entry.floatValues2 = data.getFloatValues2();
        entry.floatValues3 = data.getFloatValues3();
        entry.floatValues7 = data.getFloatValues4();
        entry.floatValues8 = data.getFloatValues5();
        entry.floatValue4 = data.getFloatValue2();
        entry.floatValue5 = data.getFloatValue3();
        entry.floatValue7 = data.getFloatValue4();
        entry.floatValue6 = data.getFloatValue5();
        entry.floatValue12 = Math.min(1.4F, data.getFloatValue6() * data2.getFloatValue5());
        entry.floatValues9 = data.getFloatValues6();
        entry.floatValue8 = data.getFloatValue7();
        entry.floatValue9 = data.getFloatValue8();
        entry.floatValue10 = data.getFloatValue9();
        entry.floatValue11 = data.getFloatValue10();
        entry.floatValue2 = data.getFloatValue11() * dayLight.floatValue12;
        entry.floatValue3 = data.getFloatValue12() * dayLight.floatValue12;
        entry.intValue = data.getIntValue();
        entry.floatValue15 = data.getFloatValue13();
        entry.floatValue13 = data.getFloatValue14();
    }

    private static float[] lerpColor(float[] var0, float[] var1, float var2) {
        return new float[] { var0[0] + (var1[0] - var0[0]) * var2, var0[1] + (var1[1] - var0[1]) * var2, var0[2] + (var1[2] - var0[2]) * var2 };
    }

    private static int toneMapRgb(float[] var0) {
        return 0xFF000000 | toneMapChannel(var0[0]) << 16 | toneMapChannel(var0[1]) << 8 | toneMapChannel(var0[2]);
    }

    private static int toneMapChannel(float var0) {
        float var1 = 1.0F - (float) Math.exp(-Math.max(var0, 0.0F) * 1.9F);
        return Math.clamp((long) ((int) (var1 * 255.0F + 0.5F)), 0, 255);
    }

    private static float[] dayTint(float[] var0, DayLight dayLight, float var2, float var3) {
        float var4 = dayLight.floatValue2 * var2;
        return new float[] { var0[0] * var4 * lerpFromOne(dayLight.floatValue4, var3), var0[1] * var4 * lerpFromOne(dayLight.floatValue5, var3), var0[2] * var4 * lerpFromOne(dayLight.floatValue6, var3) };
    }

    private static float lerpFromOne(float var0, float var1) {
        return 1.0F + (var0 - 1.0F) * var1;
    }

    private static int dayAdjustedColor(int var0, DayLight dayLight) {
        if (var0 == 0) {
            return 0;
        } else {
            float var2 = (float) Math.sqrt(dayLight.floatValue2);
            int var3 = var0 >>> 24 & 0xFF;
            int var4 = channel((var0 >> 16 & 0xFF) * var2 * dayLight.floatValue4);
            int var5 = channel((var0 >> 8 & 0xFF) * var2 * dayLight.floatValue5);
            int var6 = channel((var0 & 0xFF) * var2 * dayLight.floatValue6);
            return var3 << 24 | var4 << 16 | var5 << 8 | var6;
        }
    }

    private static int nightAdjustedColor(int var0, DayLight dayLight) {
        if (var0 == 0) {
            return 0;
        } else {
            float var2 = Math.max(0.88F, (float) Math.sqrt(dayLight.floatValue2));
            int var3 = var0 >>> 24 & 0xFF;
            int var4 = channel((var0 >> 16 & 0xFF) * var2 * halfStrength(dayLight.floatValue4));
            int var5 = channel((var0 >> 8 & 0xFF) * var2 * halfStrength(dayLight.floatValue5));
            int var6 = channel((var0 & 0xFF) * var2 * halfStrength(dayLight.floatValue6));
            return var3 << 24 | var4 << 16 | var5 << 8 | var6;
        }
    }

    private static float halfStrength(float var0) {
        return 1.0F + (var0 - 1.0F) * 0.5F;
    }

    private static int channel(float var0) {
        return Math.clamp((long) ((int) (var0 + 0.5F)), 0, 255);
    }

    public static final class Data {
        private final float floatValue;
        private final float floatValue2;
        private final float floatValue3;
        private final float floatValue4;
        private final float floatValue5;
        private final float floatValue6;
        private final boolean flag;

        public float floatValue() {
            return this.floatValue;
        }

        public float floatValue2() {
            return this.floatValue2;
        }

        public float floatValue3() {
            return this.floatValue3;
        }

        public float floatValue4() {
            return this.floatValue4;
        }

        public float floatValue5() {
            return this.floatValue5;
        }

        public float floatValue6() {
            return this.floatValue6;
        }

        public boolean flag() {
            return this.flag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Data other))
                return false;
            return java.util.Objects.equals(this.floatValue, other.floatValue) && java.util.Objects.equals(this.floatValue2, other.floatValue2) && java.util.Objects.equals(this.floatValue3, other.floatValue3) && java.util.Objects.equals(this.floatValue4, other.floatValue4) && java.util.Objects.equals(this.floatValue5, other.floatValue5) && java.util.Objects.equals(this.floatValue6, other.floatValue6) && java.util.Objects.equals(this.flag, other.flag);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(floatValue, floatValue2, floatValue3, floatValue4, floatValue5, floatValue6, flag);
        }

        @Override
        public String toString() {
            return "Data[" + "floatValue=" + this.floatValue + ", " + "floatValue2=" + this.floatValue2 + ", " + "floatValue3=" + this.floatValue3 + ", " + "floatValue4=" + this.floatValue4 + ", " + "floatValue5=" + this.floatValue5 + ", " + "floatValue6=" + this.floatValue6 + ", " + "flag=" + this.flag + "]";
        }

        public Data(float var1, float var2, float var3, float var4, float var5, float var6, boolean flag2) {
            this.floatValue = var1;
            this.floatValue2 = var2;
            this.floatValue3 = var3;
            this.floatValue4 = var4;
            this.floatValue5 = var5;
            this.floatValue6 = var6;
            this.flag = flag2;
        }

        public float getFloatValue() {
            return this.floatValue;
        }

        public float getFloatValue2() {
            return this.floatValue2;
        }

        public float getFloatValue3() {
            return this.floatValue3;
        }

        public float getFloatValue4() {
            return this.floatValue4;
        }

        public float getFloatValue5() {
            return this.floatValue5;
        }

        public float getFloatValue6() {
            return this.floatValue6;
        }

        public boolean isFlag() {
            return this.flag;
        }
    }

    private static final class Entry {
        private float[] floatValues = new float[] { 0.0F, 0.0F, 0.0F };
        private float[] floatValues2 = new float[] { 0.0F, 0.0F, 0.0F };
        private float[] floatValues3 = new float[] { 0.0F, 0.0F, 0.0F };
        private float[] floatValues4 = new float[] { 0.0F, 1.0F, 0.0F };
        private float[] floatValues5 = new float[] { 1.0F, 1.0F, 1.0F };
        private float[] floatValues6 = new float[] { 0.0F, 1.0F, 0.0F };
        private float floatValue = 1.0F;
        private float floatValue2;
        private float floatValue3;
        private float floatValue4;
        private float floatValue5;
        private float floatValue6 = 0.013F;
        private float floatValue7 = 1.2F;
        private float[] floatValues7 = new float[] { 1.0F, 1.0F, 1.0F };
        private float[] floatValues8 = new float[] { 0.5F, 0.5F, 0.5F };
        private float floatValue8;
        private float floatValue9;
        private float floatValue10;
        private float floatValue11;
        private float floatValue12;
        private float[] floatValues9 = new float[] { 0.5F, 0.5F, 0.5F };
        private float[] floatValues10 = new float[] { 0.85F, 0.9F, 1.0F };
        private float floatValue13;
        private int intValue = -8355712;
        private float floatValue14 = 128.0F;
        private float[] floatValues11 = new float[] { 1.0F, 1.0F, 1.0F };
        private float floatValue15 = 1.0F;
        private float saturation = 1.0F;
        private float floatValue16 = -1.0F;
        private int intValue2 = 0;
        private float floatValue17;
        private float floatValue18;
        private float floatValue19;
        private float floatValue20;
        private int intValue3;
        private float floatValue21;
        private float floatValue22;
        private int intValue4;
        private int intValue5;
        private float floatValue23;
        private float floatValue24;
        private float floatValue25 = 1.0F;
        private boolean flag;
        private int intValue6;
        private int intValue7;
        private float floatValue26;
        private int intValue8 = -8355712;

        private WeatherChannel.Entry withFog(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
            this.floatValues = new float[] { var1, var2, var3 };
            this.floatValues2 = new float[] { var4, var5, var6 };
            this.floatValues3 = new float[] { var7, var8, var9 };
            return this;
        }

        private WeatherChannel.Entry withSky(float var1, float var2) {
            this.floatValue2 = var1;
            this.floatValue3 = var2;
            return this;
        }

        private WeatherChannel.Entry withClouds(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
            this.floatValue4 = var1;
            this.floatValue5 = var2;
            this.floatValue6 = var3;
            this.floatValue7 = var4;
            this.floatValues7 = new float[] { var5, var6, var7 };
            this.floatValues8 = new float[] { var8, var9, var10 };
            return this;
        }

        private WeatherChannel.Entry withRain(float var1, float var2, float var3, float var4) {
            this.floatValue8 = var1;
            this.floatValue9 = var2;
            this.floatValue10 = var3;
            this.floatValue11 = var4;
            return this;
        }

        private WeatherChannel.Entry withSnow(float var1, float var2, float var3, float var4) {
            this.floatValue12 = var1;
            this.floatValues9 = new float[] { var2, var3, var4 };
            return this;
        }

        private WeatherChannel.Entry withMist(float var1, float var2, float var3, float var4) {
            this.floatValue13 = var1;
            this.floatValues10 = new float[] { var2, var3, var4 };
            return this;
        }

        private WeatherChannel.Entry withLight(int var1, float var2, float var3, float var4, float var5, float var6, float saturation2, float var8) {
            this.intValue = var1;
            this.floatValue14 = var2;
            this.floatValues11 = new float[] { var3, var4, var5 };
            this.floatValue15 = var6;
            this.saturation = saturation2;
            this.floatValue16 = var8;
            return this;
        }

        private WeatherChannel.Entry withWind(int var1, float var2, float var3, float var4, float var5, int var6, float var7) {
            this.intValue2 = var1;
            this.floatValue17 = var2;
            this.floatValue18 = var3;
            this.floatValue19 = var4;
            this.floatValue20 = var5;
            this.intValue3 = var6;
            this.floatValue21 = var7;
            return this;
        }

        private WeatherChannel.Entry withStorm(float var1, int var2, int var3, float var4, float var5, boolean flag2) {
            this.floatValue22 = var1;
            this.intValue4 = var2;
            this.intValue5 = var3;
            this.floatValue23 = var4;
            this.floatValue24 = var5;
            this.flag = flag2;
            return this;
        }

        private WeatherChannel.Entry withStars(float var1) {
            this.floatValue25 = var1;
            return this;
        }

        private WeatherChannel.Entry withFootprints(int var1, int var2) {
            this.intValue6 = var1;
            this.intValue7 = var2;
            return this;
        }

        private WeatherChannel.Entry withFrost(float var1, int var2) {
            this.floatValue26 = var1;
            this.intValue8 = var2;
            return this;
        }

        private WeatherChannel buildChannel() {
            return new WeatherChannel(this);
        }
    }
}
