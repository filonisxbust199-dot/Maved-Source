package ess.ll.client.render;

import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WorldBoxRenderer {
    public static void drawBlockBox(Vec3 vector, PoseStack poseStack, Camera camera, Frustum frustum, WorldVertexCollector worldVertexCollector) {
        drawBlockBox(vector, poseStack, camera, frustum, worldVertexCollector, -1);
    }

    public static void drawBlockBox(Vec3 vector, PoseStack poseStack, Camera camera, Frustum frustum, WorldVertexCollector worldVertexCollector, int var5) {
        drawBox(new AABB(vector, vector.add(1.0, 1.0, 1.0)), poseStack, camera, frustum, worldVertexCollector, var5);
    }

    private WorldBoxRenderer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void drawBox(AABB bounds, PoseStack poseStack, Camera camera, Frustum frustum, WorldVertexCollector worldVertexCollector, int var5) {
        if (frustum.isVisible(bounds)) {
            poseStack.pushPose();
            Vec3 position2 = camera.position();
            if (worldVertexCollector.getBuffer(EffectRenderPipelines.LINES) instanceof BaritoneVertexConsumerAccess var8) {
                Matrix4f matrix = poseStack.last().pose();
                float var10 = (float) (bounds.minX - position2.x);
                float var11 = (float) (bounds.minY - position2.y);
                float var12 = (float) (bounds.minZ - position2.z);
                float var13 = (float) (bounds.maxX - position2.x);
                float var14 = (float) (bounds.maxY - position2.y);
                float var15 = (float) (bounds.maxZ - position2.z);
                var8.coloredVertex(matrix, var10, var11, var12, var5);
                var8.coloredVertex(matrix, var13, var11, var12, var5);
                var8.coloredVertex(matrix, var13, var11, var15, var5);
                var8.coloredVertex(matrix, var10, var11, var15, var5);
                var8.coloredVertex(matrix, var10, var11, var12, var5);
                var8.coloredVertex(matrix, var10, var14, var12, var5);
                var8.coloredVertex(matrix, var13, var14, var12, var5);
                var8.coloredVertex(matrix, var13, var11, var12, var5);
                var8.coloredVertex(matrix, var13, var11, var12, var5);
                var8.coloredVertex(matrix, var13, var14, var12, var5);
                var8.coloredVertex(matrix, var13, var14, var15, var5);
                var8.coloredVertex(matrix, var13, var11, var15, var5);
                var8.coloredVertex(matrix, var10, var11, var15, var5);
                var8.coloredVertex(matrix, var13, var11, var15, var5);
                var8.coloredVertex(matrix, var13, var14, var15, var5);
                var8.coloredVertex(matrix, var10, var14, var15, var5);
                var8.coloredVertex(matrix, var10, var11, var12, var5);
                var8.coloredVertex(matrix, var10, var11, var15, var5);
                var8.coloredVertex(matrix, var10, var14, var15, var5);
                var8.coloredVertex(matrix, var10, var14, var12, var5);
                var8.coloredVertex(matrix, var10, var14, var12, var5);
                var8.coloredVertex(matrix, var10, var14, var15, var5);
                var8.coloredVertex(matrix, var13, var14, var15, var5);
                var8.coloredVertex(matrix, var13, var14, var12, var5);
                poseStack.popPose();
            } else {
                poseStack.popPose();
            }
        }
    }
}
