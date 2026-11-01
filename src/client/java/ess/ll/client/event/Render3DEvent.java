package ess.ll.client.event;

import ess.ll.client.render.WorldVertexCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public class Render3DEvent extends Event {
    public static final Render3DEvent INSTANCE = new Render3DEvent();
    private WorldVertexCollector vertexCollector;
    private DeltaTracker deltaTracker;
    private PoseStack poseStack;
    private Matrix4f projection;
    private Frustum frustum;
    private Camera camera;

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public WorldVertexCollector getVertexCollector() {
        return this.vertexCollector;
    }

    public Frustum getFrustum() {
        return this.frustum;
    }

    public void setFrustum(Frustum frustum) {
        this.frustum = frustum;
    }

    public void fire(WorldVertexCollector worldVertexCollector, DeltaTracker deltaTracker, PoseStack poseStack, Matrix4f matrix, Frustum frustum, Camera camera) {
        this.vertexCollector = worldVertexCollector;
        this.deltaTracker = deltaTracker;
        this.poseStack = poseStack;
        this.projection = matrix;
        this.frustum = frustum;
        this.camera = camera;
        super.dispatch();
        this.vertexCollector.submit();
    }

    /**
     * Sends what subscribers draw from now on into another collector; returns the one it replaces.
     */
    public WorldVertexCollector swapCollector(WorldVertexCollector collector) {
        WorldVertexCollector previous = this.vertexCollector;
        this.vertexCollector = collector;
        return previous;
    }

    public void setProjection(Matrix4f matrix) {
        this.projection = matrix;
    }

    public void setDeltaTracker(DeltaTracker deltaTracker) {
        this.deltaTracker = deltaTracker;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Matrix4f getProjection() {
        return this.projection;
    }

    public void setPoseStack(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public void setVertexCollector(WorldVertexCollector worldVertexCollector) {
        this.vertexCollector = worldVertexCollector;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }
}
