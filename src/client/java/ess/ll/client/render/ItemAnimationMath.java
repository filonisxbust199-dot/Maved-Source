package ess.ll.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class ItemAnimationMath {
    private static final Quaternionf rotationScratch = new Quaternionf();
    private static final float X_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float Y_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float Z_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float PIVOT_X_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float PIVOT_Y_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float PIVOT_Z_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    public static void rotateAroundY(PoseStack poseStack, double var1, double var3, double var5, double var7) {
        poseStack.rotateAround(rotationScratch.rotationY((float) var1 * PIVOT_Y_DEGREES_TO_RADIANS), (float) var3, (float) var5, (float) var7);
    }

    public static void rotateAroundZ(PoseStack poseStack, double var1, double var3, double var5, double var7) {
        poseStack.rotateAround(rotationScratch.rotationZ((float) var1 * PIVOT_Z_DEGREES_TO_RADIANS), (float) var3, (float) var5, (float) var7);
    }

    public static double ceil(double var0) {
        return Math.ceil(var0);
    }

    public static void rotateAroundX(PoseStack poseStack, double var1, double var3, double var5, double var7) {
        poseStack.rotateAround(rotationScratch.rotationX((float) var1 * PIVOT_X_DEGREES_TO_RADIANS), (float) var3, (float) var5, (float) var7);
    }

    public static void rotateY(PoseStack poseStack, double var1) {
        poseStack.rotate(rotationScratch.rotationY((float) var1 * Y_DEGREES_TO_RADIANS));
    }

    public static void rotateX(PoseStack poseStack, double var1) {
        poseStack.rotate(rotationScratch.rotationX((float) var1 * X_DEGREES_TO_RADIANS));
    }

    public static double abs(double var0) {
        return Math.abs(var0);
    }

    private ItemAnimationMath() {
    }

    public static void shear(PoseStack poseStack, double var1, double var3, double var5) {
        Matrix4f matrix = new Matrix4f(1.0F, (float) var1, (float) var1, 0.0F, (float) var3, 1.0F, (float) var3, 0.0F, (float) var5, (float) var5, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        poseStack.last().pose().mul(matrix);
    }

    public static void scale(PoseStack poseStack, double var1, double var3, double var5) {
        poseStack.scale((float) var1, (float) var3, (float) var5);
    }

    public static void translateZ(PoseStack poseStack, double var1) {
        poseStack.translate(0.0, 0.0, var1);
    }

    public static void popPose(PoseStack poseStack) {
        poseStack.popPose();
    }

    public static double pow(double var0, double var2) {
        return Math.pow(var0, var2);
    }

    public static double clamp(double var0, double var2, double var4) {
        return Math.clamp(var0, var2, var4);
    }

    public static void translate(PoseStack poseStack, double var1, double var3, double var5) {
        poseStack.translate(var1, var3, var5);
    }

    public static void pushPose(PoseStack poseStack) {
        poseStack.pushPose();
    }

    public static double round(double var0) {
        return Math.round(var0);
    }

    public static void rotateZ(PoseStack poseStack, double var1) {
        poseStack.rotate(rotationScratch.rotationZ((float) var1 * Z_DEGREES_TO_RADIANS));
    }

    public static double cos(double var0) {
        return Math.cos(var0);
    }

    public static double sin(double var0) {
        return Math.sin(var0);
    }

    public static void translateY(PoseStack poseStack, double var1) {
        poseStack.translate(0.0, var1, 0.0);
    }

    public static double lerp(double var0, double var2, double var4) {
        return Mth.lerp(var0, var2, var4);
    }

    public static void translateX(PoseStack poseStack, double var1) {
        poseStack.translate(var1, 0.0, 0.0);
    }

    public static double floor(double var0) {
        return Math.floor(var0);
    }
}
