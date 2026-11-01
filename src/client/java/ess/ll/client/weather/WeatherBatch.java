package ess.ll.client.weather;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Zenith's Glowboost: a frame batch of weather geometry kept as camera-relative floats (lines, flat and gradient
 * quads, textured oriented quads, discs and glow sprites), flushed in one go. On 26.3 it is flushed into the main
 * target inside the level's main pass with four pipelines (translucent or additive, plain or textured); depth is
 * tested (reversed: GREATER_THAN_OR_EQUAL) and never written. Lines become thin camera-facing quads.
 */
public final class WeatherBatch {
    private static final Identifier identifier = Identifier.fromNamespaceAndPath("ess-best", "textures/weather/glow.png");
    private static final float CONST_floatValue = 1.7F;
    private double doubleValue;
    private double doubleValue2;
    private double doubleValue3;
    private double doubleValue4;
    private double doubleValue5;
    private double doubleValue6;
    private double doubleValue7;
    private double doubleValue8;
    private double doubleValue9;
    private double doubleValue10;
    private double doubleValue11;
    private double doubleValue12;
    private float floatValue;
    private boolean flag;
    private float[] floatValues = new float[24576];
    private int[] intValues = new int[4096];
    private int intValue;
    private float[] floatValues2 = new float[12288];
    private int[] intValues2 = new int[2048];
    private int intValue2;
    private float[] floatValues3 = new float[16384];
    private int[] intValues3 = new int[4096];
    private int intValue3;
    private float[] floatValues4 = new float[16384];
    private int[] intValues4 = new int[4096];
    private int intValue4;
    private float[] floatValues5 = new float[12288];
    private int[] intValues5 = new int[1024];
    private int intValue5;
    private float[] floatValues6 = new float[24576];
    private int[] intValues6 = new int[8192];
    private int intValue6;
    private float[] floatValues7 = new float[12288];
    private int[] intValues7 = new int[1024];
    private int intValue7;
    private float[] floatValues8 = new float[20480];
    private int[] intValues8 = new int[4096];
    private int intValue8;
    private float[] floatValues9 = new float[49152];
    private float[] floatValues10 = new float[16384];
    private int[] intValues9 = new int[4096];
    private int[] intValues10 = new int[4096];
    private int intValue9;
    private final Identifier[] identifiers = new Identifier[8];
    private int intValue10;
    private int intValue11;

    public void beginFrame(Camera camera, float var2) {
        Vec3 position2 = camera.position();
        this.doubleValue = position2.x;
        this.doubleValue2 = position2.y;
        this.doubleValue3 = position2.z;
        this.setRotation(camera.xRot(), camera.yRot());
        this.floatValue = Math.max(0.0F, var2);
        this.flag = false;
        this.reset();
    }

    public void beginWithoutCamera(float var1, float var2, float var3) {
        this.doubleValue = 0.0;
        this.doubleValue2 = 0.0;
        this.doubleValue3 = 0.0;
        this.setRotation(var1, var2);
        this.floatValue = Math.max(0.0F, var3);
        this.flag = false;
        this.reset();
    }

    private void setRotation(float var1, float var2) {
        float var3 = var1 * (float) (Math.PI / 180.0);
        float var4 = -var2 * (float) (Math.PI / 180.0);
        float var5 = Mth.cos(var3);
        this.doubleValue10 = Mth.sin(var4) * var5;
        this.doubleValue11 = -Mth.sin(var3);
        this.doubleValue12 = Mth.cos(var4) * var5;
        double var6 = -this.doubleValue12;
        double var8 = this.doubleValue10;
        double var10 = Math.sqrt(var6 * var6 + var8 * var8);
        if (var10 < 1.0E-6) {
            var6 = 1.0;
            var8 = 0.0;
            var10 = 1.0;
        }
        this.doubleValue4 = var6 / var10;
        this.doubleValue5 = 0.0;
        this.doubleValue6 = var8 / var10;
        this.doubleValue7 = this.doubleValue5 * this.doubleValue12 - this.doubleValue6 * this.doubleValue11;
        this.doubleValue8 = this.doubleValue6 * this.doubleValue10 - this.doubleValue4 * this.doubleValue12;
        this.doubleValue9 = this.doubleValue4 * this.doubleValue11 - this.doubleValue5 * this.doubleValue10;
    }

    private void reset() {
        this.intValue = 0;
        this.intValue2 = 0;
        this.intValue3 = 0;
        this.intValue4 = 0;
        this.intValue5 = 0;
        this.intValue6 = 0;
        this.intValue7 = 0;
        this.intValue8 = 0;
        this.intValue9 = 0;
        this.intValue10 = 0;
        this.intValue11 = 0;
        Arrays.fill(this.identifiers, null);
    }

    public double getDoubleValue4() {
        return this.doubleValue4;
    }

    public double getDoubleValue5() {
        return this.doubleValue5;
    }

    public double getDoubleValue6() {
        return this.doubleValue6;
    }

    public double getDoubleValue7() {
        return this.doubleValue7;
    }

    public double getDoubleValue8() {
        return this.doubleValue8;
    }

    public double getDoubleValue9() {
        return this.doubleValue9;
    }

    public double getDoubleValue10() {
        return this.doubleValue10;
    }

    public double getDoubleValue11() {
        return this.doubleValue11;
    }

    public double getDoubleValue12() {
        return this.doubleValue12;
    }

    public double getDoubleValue() {
        return this.doubleValue;
    }

    public double getDoubleValue2() {
        return this.doubleValue2;
    }

    public double getDoubleValue3() {
        return this.doubleValue3;
    }

    public float getFloatValue() {
        return this.floatValue;
    }

    public void setLineAlpha(float var1) {
        this.floatValue = Math.max(0.0F, var1);
    }

    public void setFlag(boolean flag2) {
        this.flag = flag2;
    }

    public boolean isEmpty() {
        return this.intValue == 0 && this.intValue2 == 0 && this.intValue3 == 0 && this.intValue4 == 0 && this.intValue5 == 0 && this.intValue7 == 0 && this.intValue8 == 0 && this.intValue9 == 0;
    }

    public void addLine(double var1, double var3, double var5, double var7, double var9, double var11, int var13) {
        if (var13 >>> 24 != 0) {
            float var14 = (float) (var1 - this.doubleValue);
            float var15 = (float) (var3 - this.doubleValue2);
            float var16 = (float) (var5 - this.doubleValue3);
            float var17 = (float) (var7 - this.doubleValue);
            float var18 = (float) (var9 - this.doubleValue2);
            float var19 = (float) (var11 - this.doubleValue3);
            if (this.floatValue > 0.05F) {
                if ((this.intValue2 + 1) * 6 > this.floatValues2.length) {
                    this.floatValues2 = Arrays.copyOf(this.floatValues2, this.floatValues2.length * 2);
                }
                if (this.intValue2 >= this.intValues2.length) {
                    this.intValues2 = Arrays.copyOf(this.intValues2, this.intValues2.length * 2);
                }
                int var20 = this.intValue2 * 6;
                this.floatValues2[var20] = var14;
                this.floatValues2[var20 + 1] = var15;
                this.floatValues2[var20 + 2] = var16;
                this.floatValues2[var20 + 3] = var17;
                this.floatValues2[var20 + 4] = var18;
                this.floatValues2[var20 + 5] = var19;
                this.intValues2[this.intValue2++] = WeatherBatch.fade(var13, 0.2F + 0.1F * this.floatValue);
            }
            if ((this.intValue + 1) * 6 > this.floatValues.length) {
                this.floatValues = Arrays.copyOf(this.floatValues, this.floatValues.length * 2);
            }
            if (this.intValue >= this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intValues.length * 2);
            }
            int var21 = this.intValue * 6;
            this.floatValues[var21] = var14;
            this.floatValues[var21 + 1] = var15;
            this.floatValues[var21 + 2] = var16;
            this.floatValues[var21 + 3] = var17;
            this.floatValues[var21 + 4] = var18;
            this.floatValues[var21 + 5] = var19;
            this.intValues[this.intValue++] = var13;
        }
    }

    public void addFlatQuad(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, double var17, double var19, double var21, double var23, int var25) {
        if (var25 >>> 24 != 0) {
            if ((this.intValue5 + 1) * 12 > this.floatValues5.length) {
                this.floatValues5 = Arrays.copyOf(this.floatValues5, this.floatValues5.length * 2);
            }
            if (this.intValue5 >= this.intValues5.length) {
                this.intValues5 = Arrays.copyOf(this.intValues5, this.intValues5.length * 2);
            }
            this.intValue5 = this.writeQuad(this.floatValues5, this.intValues5, this.intValue5, var1, var3, var5, var7, var9, var11, var13, var15, var17, var19, var21, var23, var25);
        }
    }

    public void addGradientQuad(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, double var17, double var19, double var21, double var23, int var25) {
        if (var25 >>> 24 != 0) {
            if ((this.intValue7 + 1) * 12 > this.floatValues7.length) {
                this.floatValues7 = Arrays.copyOf(this.floatValues7, this.floatValues7.length * 2);
            }
            if (this.intValue7 >= this.intValues7.length) {
                this.intValues7 = Arrays.copyOf(this.intValues7, this.intValues7.length * 2);
            }
            this.intValue7 = this.writeQuad(this.floatValues7, this.intValues7, this.intValue7, var1, var3, var5, var7, var9, var11, var13, var15, var17, var19, var21, var23, var25);
        }
    }

    public void addTexturedQuad(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, double var17, double var19, double var21, double var23, int var25, int var26, int var27, int var28) {
        if ((var25 | var26 | var27 | var28) >>> 24 != 0) {
            if ((this.intValue6 + 1) * 12 > this.floatValues6.length) {
                this.floatValues6 = Arrays.copyOf(this.floatValues6, this.floatValues6.length * 2);
            }
            if ((this.intValue6 + 1) * 4 > this.intValues6.length) {
                this.intValues6 = Arrays.copyOf(this.intValues6, this.intValues6.length * 2);
            }
            int var29 = this.intValue6 * 12;
            this.floatValues6[var29] = (float) (var1 - this.doubleValue);
            this.floatValues6[var29 + 1] = (float) (var3 - this.doubleValue2);
            this.floatValues6[var29 + 2] = (float) (var5 - this.doubleValue3);
            this.floatValues6[var29 + 3] = (float) (var7 - this.doubleValue);
            this.floatValues6[var29 + 4] = (float) (var9 - this.doubleValue2);
            this.floatValues6[var29 + 5] = (float) (var11 - this.doubleValue3);
            this.floatValues6[var29 + 6] = (float) (var13 - this.doubleValue);
            this.floatValues6[var29 + 7] = (float) (var15 - this.doubleValue2);
            this.floatValues6[var29 + 8] = (float) (var17 - this.doubleValue3);
            this.floatValues6[var29 + 9] = (float) (var19 - this.doubleValue);
            this.floatValues6[var29 + 10] = (float) (var21 - this.doubleValue2);
            this.floatValues6[var29 + 11] = (float) (var23 - this.doubleValue3);
            int var30 = this.intValue6 * 4;
            this.intValues6[var30] = var25;
            this.intValues6[var30 + 1] = var26;
            this.intValues6[var30 + 2] = var27;
            this.intValues6[var30 + 3] = var28;
            this.intValue6++;
        }
    }

    private int writeQuad(float[] var1, int[] var2, int var3, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, double var20, double var22, double var24, double var26, int var28) {
        int var29 = var3 * 12;
        var1[var29] = (float) (var4 - this.doubleValue);
        var1[var29 + 1] = (float) (var6 - this.doubleValue2);
        var1[var29 + 2] = (float) (var8 - this.doubleValue3);
        var1[var29 + 3] = (float) (var10 - this.doubleValue);
        var1[var29 + 4] = (float) (var12 - this.doubleValue2);
        var1[var29 + 5] = (float) (var14 - this.doubleValue3);
        var1[var29 + 6] = (float) (var16 - this.doubleValue);
        var1[var29 + 7] = (float) (var18 - this.doubleValue2);
        var1[var29 + 8] = (float) (var20 - this.doubleValue3);
        var1[var29 + 9] = (float) (var22 - this.doubleValue);
        var1[var29 + 10] = (float) (var24 - this.doubleValue2);
        var1[var29 + 11] = (float) (var26 - this.doubleValue3);
        var2[var3] = var28;
        return var3 + 1;
    }

    public void addSpark(double var1, double var3, double var5, double var7, int var9, long var10, float var12, int var13, boolean var14) {
        if (var13 >>> 24 != 0 && !(var7 <= 5.0E-4)) {
            int var15 = Math.max(5, var9);
            double var16 = 0.0;
            double var18 = 0.0;
            for (int var20 = 0; var20 <= var15; var20++) {
                int var21 = var20 % var15;
                double var22 = var21 * (Math.PI * 2) / var15;
                double var24 = 1.0 - var12 * 0.5 + var12 * hashFloat(var10, var21, 11);
                double var26 = var1 + Math.cos(var22) * var7 * var24;
                double var28 = var5 + Math.sin(var22) * var7 * var24;
                if (var20 > 0) {
                    if (var14) {
                        this.addGradientQuad(var1, var3, var5, var16, var3, var18, var26, var3, var28, var1, var3, var5, var13);
                    } else {
                        this.addFlatQuad(var1, var3, var5, var16, var3, var18, var26, var3, var28, var1, var3, var5, var13);
                    }
                }
                var16 = var26;
                var18 = var28;
            }
        }
    }

    public void addBeam(double var1, double var3, double var5, double var7, double var9, int var11, int var12, boolean var13) {
        if (var12 >>> 24 != 0 && !(var7 <= 5.0E-4)) {
            int var14 = Math.max(6, var11);
            double var15 = Math.max(0.0, var7 - var9 * 0.5);
            double var17 = var7 + var9 * 0.5;
            double var19 = 1.0;
            double var21 = 0.0;
            for (int var23 = 1; var23 <= var14; var23++) {
                double var24 = var23 * (Math.PI * 2) / var14;
                double var26 = Math.cos(var24);
                double var28 = Math.sin(var24);
                if (var13) {
                    this.addGradientQuad(var1 + var19 * var15, var3, var5 + var21 * var15, var1 + var26 * var15, var3, var5 + var28 * var15, var1 + var26 * var17, var3, var5 + var28 * var17, var1 + var19 * var17, var3, var5 + var21 * var17, var12);
                } else {
                    this.addFlatQuad(var1 + var19 * var15, var3, var5 + var21 * var15, var1 + var26 * var15, var3, var5 + var28 * var15, var1 + var26 * var17, var3, var5 + var28 * var17, var1 + var19 * var17, var3, var5 + var21 * var17, var12);
                }
                var19 = var26;
                var21 = var28;
            }
        }
    }

    public void addDisc(double var1, double var3, double var5, float var7, float var8, int var9) {
        if (var9 >>> 24 != 0 && !(var7 <= 0.0F)) {
            if ((this.intValue8 + 1) * 5 > this.floatValues8.length) {
                this.floatValues8 = Arrays.copyOf(this.floatValues8, this.floatValues8.length * 2);
            }
            if (this.intValue8 >= this.intValues8.length) {
                this.intValues8 = Arrays.copyOf(this.intValues8, this.intValues8.length * 2);
            }
            int var10 = this.intValue8 * 5;
            this.floatValues8[var10] = (float) (var1 - this.doubleValue);
            this.floatValues8[var10 + 1] = (float) (var3 - this.doubleValue2);
            this.floatValues8[var10 + 2] = (float) (var5 - this.doubleValue3);
            this.floatValues8[var10 + 3] = var7;
            this.floatValues8[var10 + 4] = var8;
            this.intValues8[this.intValue8++] = var9;
        }
    }

    public void useTexture(Identifier identifier) {
        for (int var2 = 0; var2 < this.intValue10; var2++) {
            if (this.identifiers[var2] == identifier) {
                this.intValue11 = var2;
                return;
            }
        }
        if (this.intValue10 == this.identifiers.length) {
            this.intValue11 = this.intValue10 - 1;
            this.identifiers[this.intValue11] = identifier;
        } else {
            this.identifiers[this.intValue10] = identifier;
            this.intValue11 = this.intValue10++;
        }
    }

    public void addGlowQuad(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, double var17, double var19, double var21, double var23, float var25, float var26, float var27, float var28, int var29) {
        if (var29 >>> 24 != 0) {
            this.growQuads();
            int var30 = this.intValue9 * 12;
            this.floatValues9[var30] = (float) (var1 - this.doubleValue);
            this.floatValues9[var30 + 1] = (float) (var3 - this.doubleValue2);
            this.floatValues9[var30 + 2] = (float) (var5 - this.doubleValue3);
            this.floatValues9[var30 + 3] = (float) (var7 - this.doubleValue);
            this.floatValues9[var30 + 4] = (float) (var9 - this.doubleValue2);
            this.floatValues9[var30 + 5] = (float) (var11 - this.doubleValue3);
            this.floatValues9[var30 + 6] = (float) (var13 - this.doubleValue);
            this.floatValues9[var30 + 7] = (float) (var15 - this.doubleValue2);
            this.floatValues9[var30 + 8] = (float) (var17 - this.doubleValue3);
            this.floatValues9[var30 + 9] = (float) (var19 - this.doubleValue);
            this.floatValues9[var30 + 10] = (float) (var21 - this.doubleValue2);
            this.floatValues9[var30 + 11] = (float) (var23 - this.doubleValue3);
            this.putUvColor(var25, var26, var27, var28, var29);
        }
    }

    public void addOrientedQuad(double var1, double var3, double var5, double var7, double var9, double var11, double var13, float var15, float var16, float var17, float var18, int var19) {
        double var20 = var7 * var13;
        double var22 = var9 * var13;
        double var24 = -var9 * var11;
        double var26 = var7 * var11;
        this.addGlowQuad(var1 - var24 + var20, var3, var5 - var26 + var22, var1 + var24 + var20, var3, var5 + var26 + var22, var1 + var24 - var20, var3, var5 + var26 - var22, var1 - var24 - var20, var3, var5 - var26 - var22, var15, var16, var17, var18, var19);
    }

    private void growQuads() {
        if ((this.intValue9 + 1) * 12 > this.floatValues9.length) {
            this.floatValues9 = Arrays.copyOf(this.floatValues9, this.floatValues9.length * 2);
            this.floatValues10 = Arrays.copyOf(this.floatValues10, this.floatValues10.length * 2);
            this.intValues9 = Arrays.copyOf(this.intValues9, this.intValues9.length * 2);
            this.intValues10 = Arrays.copyOf(this.intValues10, this.intValues10.length * 2);
        }
    }

    private void putUvColor(float var1, float var2, float var3, float var4, int var5) {
        int var6 = this.intValue9 * 4;
        this.floatValues10[var6] = var1;
        this.floatValues10[var6 + 1] = var2;
        this.floatValues10[var6 + 2] = var3;
        this.floatValues10[var6 + 3] = var4;
        this.intValues10[this.intValue9] = this.intValue11;
        this.intValues9[this.intValue9++] = var5;
    }

    public void addCameraQuad(double var1, double var3, double var5, double var7, double var9, double var11, float var13, float var14, float var15, float var16, float var17, float var18, int var19) {
        if (var19 >>> 24 != 0 && !(var13 <= 0.0F) && !(var14 <= 0.0F)) {
            double var20 = Math.sqrt(var7 * var7 + var9 * var9 + var11 * var11);
            if (!(var20 < 1.0E-6)) {
                double var22 = var7 / var20 * var13;
                double var24 = var9 / var20 * var13;
                double var26 = var11 / var20 * var13;
                double var28 = var9 / var20 * this.doubleValue12 - var11 / var20 * this.doubleValue11;
                double var30 = var11 / var20 * this.doubleValue10 - var7 / var20 * this.doubleValue12;
                double var32 = var7 / var20 * this.doubleValue11 - var9 / var20 * this.doubleValue10;
                double var34 = Math.sqrt(var28 * var28 + var30 * var30 + var32 * var32);
                if (!(var34 < 1.0E-4)) {
                    double var36 = var28 / var34 * var14;
                    double var38 = var30 / var34 * var14;
                    double var40 = var32 / var34 * var14;
                    this.growQuads();
                    int var42 = this.intValue9 * 12;
                    float var43 = (float) (var1 - this.doubleValue);
                    float var44 = (float) (var3 - this.doubleValue2);
                    float var45 = (float) (var5 - this.doubleValue3);
                    this.floatValues9[var42] = (float) (var43 - var36 + var22);
                    this.floatValues9[var42 + 1] = (float) (var44 - var38 + var24);
                    this.floatValues9[var42 + 2] = (float) (var45 - var40 + var26);
                    this.floatValues9[var42 + 3] = (float) (var43 + var36 + var22);
                    this.floatValues9[var42 + 4] = (float) (var44 + var38 + var24);
                    this.floatValues9[var42 + 5] = (float) (var45 + var40 + var26);
                    this.floatValues9[var42 + 6] = (float) (var43 + var36 - var22);
                    this.floatValues9[var42 + 7] = (float) (var44 + var38 - var24);
                    this.floatValues9[var42 + 8] = (float) (var45 + var40 - var26);
                    this.floatValues9[var42 + 9] = (float) (var43 - var36 - var22);
                    this.floatValues9[var42 + 10] = (float) (var44 - var38 - var24);
                    this.floatValues9[var42 + 11] = (float) (var45 - var40 - var26);
                    this.putUvColor(var15, var16, var17, var18, var19);
                }
            }
        }
    }

    public void addGlow(double var1, double var3, double var5, float var7, int var8, float var9) {
        if (!(this.floatValue <= 0.02F) && !(var7 < 0.001F)) {
            float var10 = var9 * Math.min(1.5F, this.floatValue);
            if (!(var10 < 0.01F)) {
                this.putGlow(var1, var3, var5, var7 * 2.1F, WeatherBatch.fade(var8, Math.min(1.0F, var10 * 0.22F)));
                this.putGlow(var1, var3, var5, var7, WeatherBatch.fade(var8, Math.min(1.0F, var10 * 0.55F)));
            }
        }
    }

    public void addBrightGlow(double var1, double var3, double var5, float var7, int var8, float var9) {
        if (!(var7 < 0.001F) && !(var9 < 0.002F)) {
            int var10 = WeatherBatch.fade(var8, Math.min(1.0F, var9));
            if (var10 >>> 24 != 0) {
                if ((this.intValue4 + 1) * 4 > this.floatValues4.length) {
                    this.floatValues4 = Arrays.copyOf(this.floatValues4, this.floatValues4.length * 2);
                }
                if (this.intValue4 >= this.intValues4.length) {
                    this.intValues4 = Arrays.copyOf(this.intValues4, this.intValues4.length * 2);
                }
                int var11 = this.intValue4 * 4;
                this.floatValues4[var11] = (float) (var1 - this.doubleValue);
                this.floatValues4[var11 + 1] = (float) (var3 - this.doubleValue2);
                this.floatValues4[var11 + 2] = (float) (var5 - this.doubleValue3);
                this.floatValues4[var11 + 3] = var7;
                this.intValues4[this.intValue4++] = var10;
            }
        }
    }

    private void putGlow(double var1, double var3, double var5, float var7, int var8) {
        if (var8 >>> 24 != 0) {
            if ((this.intValue3 + 1) * 4 > this.floatValues3.length) {
                this.floatValues3 = Arrays.copyOf(this.floatValues3, this.floatValues3.length * 2);
            }
            if (this.intValue3 >= this.intValues3.length) {
                this.intValues3 = Arrays.copyOf(this.intValues3, this.intValues3.length * 2);
            }
            int var9 = this.intValue3 * 4;
            this.floatValues3[var9] = (float) (var1 - this.doubleValue);
            this.floatValues3[var9 + 1] = (float) (var3 - this.doubleValue2);
            this.floatValues3[var9 + 2] = (float) (var5 - this.doubleValue3);
            this.floatValues3[var9 + 3] = var7;
            this.intValues3[this.intValue3++] = var8;
        }
    }

    /**
     * Draws everything batched this frame with the view rotation (positions are camera-relative), then clears.
     */
    public void draw(Matrix4f view) {
        if (this.isEmpty()) {
            return;
        }
        try {
            BufferBuilder plain = WeatherDraw.begin(false);
            this.drawGradientQuads(plain, this.floatValues5, this.intValues5, this.intValue5);
            this.drawFlatQuads(plain);
            this.drawDiscs(plain);
            WeatherDraw.draw("weather quads", plain, WeatherDraw.TRANSLUCENT, null, view);
            BufferBuilder glowQuads = WeatherDraw.begin(false);
            this.drawGradientQuads(glowQuads, this.floatValues7, this.intValues7, this.intValue7);
            this.drawLines(glowQuads);
            WeatherDraw.draw("weather glow quads", glowQuads, WeatherDraw.ADDITIVE, null, view);
            int start = 0;
            while (start < this.intValue9) {
                int texture = this.intValues10[start];
                int end = start + 1;
                while (end < this.intValue9 && this.intValues10[end] == texture) {
                    end++;
                }
                Identifier id = this.identifiers[texture];
                BufferBuilder textured = WeatherDraw.begin(id != null);
                this.drawTexturedRange(textured, start, end, id != null);
                WeatherDraw.draw("weather sprites", textured, id != null ? WeatherDraw.TEXTURED : WeatherDraw.TRANSLUCENT, id, view);
                start = end;
            }
            BufferBuilder soft = WeatherDraw.begin(true);
            this.drawGlowSprites(soft, this.floatValues4, this.intValues4, this.intValue4);
            WeatherDraw.draw("weather soft glow", soft, WeatherDraw.TEXTURED, identifier, view);
            BufferBuilder bright = WeatherDraw.begin(true);
            this.drawGlowSprites(bright, this.floatValues3, this.intValues3, this.intValue3);
            WeatherDraw.draw("weather glow", bright, WeatherDraw.TEXTURED_ADDITIVE, identifier, view);
        } finally {
            this.reset();
        }
    }

    private void drawFlatQuads(BufferBuilder bufferBuilder) {
        for (int var4 = 0; var4 < this.intValue6; var4++) {
            int var5 = var4 * 12;
            int var6 = var4 * 4;
            bufferBuilder.addVertex(this.floatValues6[var5], this.floatValues6[var5 + 1], this.floatValues6[var5 + 2]).setColor(this.intValues6[var6]);
            bufferBuilder.addVertex(this.floatValues6[var5 + 3], this.floatValues6[var5 + 4], this.floatValues6[var5 + 5]).setColor(this.intValues6[var6 + 1]);
            bufferBuilder.addVertex(this.floatValues6[var5 + 6], this.floatValues6[var5 + 7], this.floatValues6[var5 + 8]).setColor(this.intValues6[var6 + 2]);
            bufferBuilder.addVertex(this.floatValues6[var5 + 9], this.floatValues6[var5 + 10], this.floatValues6[var5 + 11]).setColor(this.intValues6[var6 + 3]);
        }
    }

    private void drawGradientQuads(BufferBuilder bufferBuilder, float[] var2, int[] var3, int var4) {
        for (int var7 = 0; var7 < var4; var7++) {
            int var8 = var7 * 12;
            int var9 = var3[var7];
            bufferBuilder.addVertex(var2[var8], var2[var8 + 1], var2[var8 + 2]).setColor(var9);
            bufferBuilder.addVertex(var2[var8 + 3], var2[var8 + 4], var2[var8 + 5]).setColor(var9);
            bufferBuilder.addVertex(var2[var8 + 6], var2[var8 + 7], var2[var8 + 8]).setColor(var9);
            bufferBuilder.addVertex(var2[var8 + 9], var2[var8 + 10], var2[var8 + 11]).setColor(var9);
        }
    }

    private void drawTexturedRange(BufferBuilder bufferBuilder, int var2, int var3, boolean var5) {
        for (int var8 = var2; var8 < var3; var8++) {
            int var9 = var8 * 12;
            int var10 = this.intValues9[var8];
            if (var5) {
                int var11 = var8 * 4;
                float var12 = this.floatValues10[var11];
                float var13 = this.floatValues10[var11 + 1];
                float var14 = this.floatValues10[var11 + 2];
                float var15 = this.floatValues10[var11 + 3];
                bufferBuilder.addVertex(this.floatValues9[var9], this.floatValues9[var9 + 1], this.floatValues9[var9 + 2]).setUv(var12, var13).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 3], this.floatValues9[var9 + 4], this.floatValues9[var9 + 5]).setUv(var14, var13).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 6], this.floatValues9[var9 + 7], this.floatValues9[var9 + 8]).setUv(var14, var15).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 9], this.floatValues9[var9 + 10], this.floatValues9[var9 + 11]).setUv(var12, var15).setColor(var10);
            } else {
                bufferBuilder.addVertex(this.floatValues9[var9], this.floatValues9[var9 + 1], this.floatValues9[var9 + 2]).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 3], this.floatValues9[var9 + 4], this.floatValues9[var9 + 5]).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 6], this.floatValues9[var9 + 7], this.floatValues9[var9 + 8]).setColor(var10);
                bufferBuilder.addVertex(this.floatValues9[var9 + 9], this.floatValues9[var9 + 10], this.floatValues9[var9 + 11]).setColor(var10);
            }
        }
    }

    /**
     * Flat discs facing the camera (rotated squares), batched by addDisc.
     */
    private void drawDiscs(BufferBuilder bufferBuilder) {
        for (int var4 = 0; var4 < this.intValue8; var4++) {
            int var5 = var4 * 5;
            float var6 = this.floatValues8[var5];
            float var7 = this.floatValues8[var5 + 1];
            float var8 = this.floatValues8[var5 + 2];
            float var9 = this.floatValues8[var5 + 3];
            float var10 = this.floatValues8[var5 + 4];
            int var11 = this.intValues8[var4];
            float var12 = Mth.cos(var10) * var9;
            float var13 = Mth.sin(var10) * var9;
            float var14 = (float) (this.doubleValue4 * var12 + this.doubleValue7 * var13);
            float var15 = (float) (this.doubleValue5 * var12 + this.doubleValue8 * var13);
            float var16 = (float) (this.doubleValue6 * var12 + this.doubleValue9 * var13);
            float var17 = (float) (this.doubleValue7 * var12 - this.doubleValue4 * var13);
            float var18 = (float) (this.doubleValue8 * var12 - this.doubleValue5 * var13);
            float var19 = (float) (this.doubleValue9 * var12 - this.doubleValue6 * var13);
            bufferBuilder.addVertex(var6 + var14, var7 + var15, var8 + var16).setColor(var11);
            bufferBuilder.addVertex(var6 + var17, var7 + var18, var8 + var19).setColor(var11);
            bufferBuilder.addVertex(var6 - var14, var7 - var15, var8 - var16).setColor(var11);
            bufferBuilder.addVertex(var6 - var17, var7 - var18, var8 - var19).setColor(var11);
        }
    }

    /**
     * Camera-facing glow sprites of the glow texture (soft: translucent, bright: additive).
     */
    private void drawGlowSprites(BufferBuilder bufferBuilder, float[] values, int[] colors, int count) {
        for (int var4 = 0; var4 < count; var4++) {
            int var5 = var4 * 4;
            float var6 = values[var5];
            float var7 = values[var5 + 1];
            float var8 = values[var5 + 2];
            float var9 = values[var5 + 3];
            int var10 = colors[var4];
            float var11 = (float) (this.doubleValue4 * var9);
            float var12 = (float) (this.doubleValue5 * var9);
            float var13 = (float) (this.doubleValue6 * var9);
            float var14 = (float) (this.doubleValue7 * var9);
            float var15 = (float) (this.doubleValue8 * var9);
            float var16 = (float) (this.doubleValue9 * var9);
            bufferBuilder.addVertex(var6 + var11 - var14, var7 + var12 - var15, var8 + var13 - var16).setUv(0.0F, 1.0F).setColor(var10);
            bufferBuilder.addVertex(var6 - var11 - var14, var7 - var12 - var15, var8 - var13 - var16).setUv(1.0F, 1.0F).setColor(var10);
            bufferBuilder.addVertex(var6 - var11 + var14, var7 - var12 + var15, var8 - var13 + var16).setUv(1.0F, 0.0F).setColor(var10);
            bufferBuilder.addVertex(var6 + var11 + var14, var7 + var12 + var15, var8 + var13 + var16).setUv(0.0F, 0.0F).setColor(var10);
        }
    }

    /**
     * Lines: the wide soft underlay first, then the thin core, as quads facing the camera.
     */
    private void drawLines(BufferBuilder bufferBuilder) {
        this.drawLineLayer(bufferBuilder, this.floatValues2, this.intValues2, this.intValue2, 0.016F);
        this.drawLineLayer(bufferBuilder, this.floatValues, this.intValues, this.intValue, 0.005F);
    }

    private void drawLineLayer(BufferBuilder bufferBuilder, float[] var2, int[] var3, int var4, float width) {
        for (int var8 = 0; var8 < var4; var8++) {
            int var9 = var8 * 6;
            float x0 = var2[var9], y0 = var2[var9 + 1], z0 = var2[var9 + 2];
            float x1 = var2[var9 + 3], y1 = var2[var9 + 4], z1 = var2[var9 + 5];
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            // Side vector: across the line and across the view ray to its middle; width grows with distance.
            float mx = (x0 + x1) * 0.5F, my = (y0 + y1) * 0.5F, mz = (z0 + z1) * 0.5F;
            float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
            float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (len < 1.0E-6F) {
                continue;
            }
            float w = width * Math.max(1.0F, (float) Math.sqrt(mx * mx + my * my + mz * mz) * 0.35F) / len;
            sx *= w;
            sy *= w;
            sz *= w;
            int color = var3[var8];
            bufferBuilder.addVertex(x0 - sx, y0 - sy, z0 - sz).setColor(color);
            bufferBuilder.addVertex(x0 + sx, y0 + sy, z0 + sz).setColor(color);
            bufferBuilder.addVertex(x1 + sx, y1 + sy, z1 + sz).setColor(color);
            bufferBuilder.addVertex(x1 - sx, y1 - sy, z1 - sz).setColor(color);
        }
    }

    /**
     * Colour with its alpha scaled (Zenith ColorUtils).
     */
    public static int fade(int color, float alpha) {
        int a = Math.round((color >>> 24 & 0xFF) * alpha);
        return Math.clamp(a, 0, 255) << 24 | color & 0xFFFFFF;
    }

    public static float easeOut(float var0) {
        float var1 = 1.0F - var0;
        return 1.0F - var1 * var1 * var1;
    }

    public static float fadeInOut(float var0) {
        return fadeEdges(var0, 0.1F, 0.45F);
    }

    public static float fadeEdges(float var0, float var1, float var2) {
        float var3 = var0 < var1 ? var0 / var1 : 1.0F;
        float var4 = var0 > var2 ? (1.0F - var0) / (1.0F - var2) : 1.0F;
        return var3 * var4 * var4;
    }

    public static float hashFloat(long var0, int var2, int var3) {
        long var4 = var0 * -7046029254386353131L + var2 * -4658895280553007687L + var3 * -7723592293110705685L;
        var4 ^= var4 >>> 30;
        var4 *= -4658895280553007687L;
        var4 ^= var4 >>> 27;
        var4 *= -7723592293110705685L;
        var4 ^= var4 >>> 31;
        return (float) (var4 >>> 40) / 1.6777216E7F;
    }
}
