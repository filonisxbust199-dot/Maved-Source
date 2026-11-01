package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.render.GhostModelBatch;
import ess.ll.client.render.WorldFx;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** Scene-sampled Chams blur masked by a depth-tested player silhouette. */
final class ChamsBlur {
    private static final BindGroupLayout LAYOUT=BindGroupLayout.builder()
            .withUniform("ChamsBlur",UniformType.UNIFORM_BUFFER)
            .withUniform("SceneSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("MaskSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/chams_blur_composite"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/chams_blur_composite"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/chams_blur_composite"))
            .withBindGroupLayout(LAYOUT).withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))
            .withCull(false).withVertexBinding(0,ess.ll.client.render.targetfx.internal.RenderPipelineLayouts.vertexFormat)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static TextureTarget mask,scene;
    private static boolean drawn;
    private ChamsBlur() {}
    static void initialize() { /* Register the pipeline before the shader resource reload. */ }
    static void begin() {
        drawn=false;
        var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        mask=WorldFx.target(mask,"ESS Chams mask",main.width,main.height);
        scene=WorldFx.target(scene,"ESS Chams scene",main.width,main.height);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(),new Vector4f(0));
    }
    static void render(ChronoFrame frame,Vec3 camera,int color,boolean throughWalls) {
        if(mask==null) return;
        BATCH.begin();
        frame.emit(camera,0xff000000|color&0xffffff,false,BATCH);
        if(BATCH.empty()) return;
        BATCH.flushFlatTo(frame.skin,1,throughWalls,false,mask);
        drawn=true;
    }
    static void finish() {
        if(!drawn || mask==null || scene==null) return;
        var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        WorldFx.copyColor(main,scene);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var settings=WorldFx.upload(Std140Builder.onStack(stack,16).putVec4(Chams.BLUR_RADIUS.get(),
                    Chams.BLUR_MIRROR.get()?1:0,Chams.BLUR_ALPHA.get()/100f,0).get());
            WorldFx.draw("ESS Chams blur",main,PIPELINE,"ChamsBlur",settings,
                    "SceneSampler",scene.getColorTextureView(),FilterMode.LINEAR,
                    "MaskSampler",mask.getColorTextureView(),FilterMode.LINEAR);
        }
    }
    static void release() {
        if(mask!=null) { mask.destroyBuffers(); mask=null; }
        if(scene!=null) { scene.destroyBuffers(); scene=null; }
        BATCH.close();
    }
}
