package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.WorldFx;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

/**
 * Camera motion blur from velocity: every pixel's world position is rebuilt from the depth buffer, projected with
 * last frame's camera, and the colour is smeared along the screen path between the two, as a camera shutter
 * would. Turning and moving both blur, near things more than far ones when walking; the sky blurs with turns
 * only. It runs after the world and before the hand, so the hand and the HUD stay sharp.
 */
public final class MotionBlur {
    public static final SliderSetting STRENGTH=new SliderSetting("Сила",.1f,3,.05f,1);
    public static final SliderSetting SAMPLES=new SliderSetting("Качество",4,48,1,16);
    public static final SliderSetting MAX_LENGTH=new SliderSetting("Макс. размытие",1,30,1,10);
    public static final Setting[] SETTINGS={STRENGTH,SAMPLES,MAX_LENGTH};

    private static final BindGroupLayout LAYOUT=BindGroupLayout.builder()
            .withUniform("MotionBlur",UniformType.UNIFORM_BUFFER)
            .withUniform("SceneSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("DepthSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/motion_blur"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/chams_blur_composite"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/motion_blur"))
            .withBindGroupLayout(LAYOUT).withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))
            .withCull(false).withVertexBinding(0,ess.ll.client.render.targetfx.internal.RenderPipelineLayouts.vertexFormat)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    /** A camera step longer than this between frames is a teleport, not motion, and is not blurred. */
    private static final double TELEPORT=8;
    private static final Matrix4f PREV_PROJECTION=new Matrix4f(), PREV_VIEW=new Matrix4f();
    private static Vec3 prevCamera;
    private static TextureTarget scene;
    private MotionBlur() {}

    /** Registers the pipeline before the shaders load. */
    public static void initialize() {}
    private static boolean enabled() { return ClientModules.MOTION_BLUR!=null && ClientModules.MOTION_BLUR.enabled(); }

    /** After the level, before the hand. */
    public static void render() {
        var mc=Minecraft.getInstance();
        if(!enabled() || mc.level==null || !WorldFx.captured()) { release(); return; }
        Matrix4f projection=WorldFx.projection(), view=WorldFx.view();
        Vec3 camera=mc.gameRenderer.mainCamera().position();
        try {
            if(prevCamera!=null && camera.distanceTo(prevCamera)<TELEPORT) blur(mc,projection,view,camera);
        } catch(RuntimeException e) {
            ClientModules.MOTION_BLUR.setEnabled(false);
            release();
            System.err.println("[ESS] MotionBlur disabled after render failure: "+e);
            return;
        }
        PREV_PROJECTION.set(projection); PREV_VIEW.set(view); prevCamera=camera;
    }

    private static void blur(Minecraft mc,Matrix4f projection,Matrix4f view,Vec3 camera) {
        var main=mc.gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        scene=WorldFx.target(scene,"ESS motion blur scene",main.width,main.height);
        WorldFx.copyColor(main,scene);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var data=WorldFx.upload(Std140Builder.onStack(stack,288)
                    .putMat4f(new Matrix4f(projection).invert()).putMat4f(new Matrix4f(view).invert())
                    .putMat4f(PREV_PROJECTION).putMat4f(PREV_VIEW)
                    .putVec4((float)(camera.x-prevCamera.x),(float)(camera.y-prevCamera.y),(float)(camera.z-prevCamera.z),STRENGTH.get())
                    .putVec4(Math.round(SAMPLES.get()),MAX_LENGTH.get()/100f,main.width,main.height).get());
            WorldFx.draw("ESS motion blur",main,PIPELINE,"MotionBlur",data,
                    "SceneSampler",scene.getColorTextureView(),FilterMode.LINEAR,
                    "DepthSampler",main.getDepthTextureView(),FilterMode.NEAREST);
        }
    }

    /** Off: the copy of the frame is freed, and the next frame starts without a previous camera. */
    public static void release() {
        prevCamera=null;
        if(scene!=null) { scene.destroyBuffers(); scene=null; }
    }
}
