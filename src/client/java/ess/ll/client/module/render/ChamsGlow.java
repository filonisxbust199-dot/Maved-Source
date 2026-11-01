package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.render.GhostModelBatch;
import ess.ll.client.render.WorldFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** Chams body and armor depth masks, visibility filter, close/wide blur and composite. */
final class ChamsGlow {
    private static final BindGroupLayout VISIBLE_LAYOUT=BindGroupLayout.builder()
            .withUniform("ChamsVisibility",UniformType.UNIFORM_BUFFER)
            .withUniform("BodySampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BodyDepthSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("ArmorDepthSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("SceneDepthSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final BindGroupLayout BLUR_LAYOUT=BindGroupLayout.builder()
            .withUniform("ChamsGlow",UniformType.UNIFORM_BUFFER)
            .withUniform("InSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final BindGroupLayout COMPOSITE_LAYOUT=BindGroupLayout.builder()
            .withUniform("ChamsGlow",UniformType.UNIFORM_BUFFER)
            .withUniform("VisibleSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("CloseSampler",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("WideSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final RenderPipeline VISIBLE=post("visible","chams_visible",VISIBLE_LAYOUT,false);
    private static final RenderPipeline BLUR=post("blur","chams_glow_blur",BLUR_LAYOUT,false);
    private static final RenderPipeline COMPOSITE=post("composite","chams_glow_composite",COMPOSITE_LAYOUT,true);
    private static RenderPipeline post(String label,String fragment,BindGroupLayout layout,boolean blend) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/chams_glow_"+label))
                .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/chams_blur_composite"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/"+fragment))
                .withBindGroupLayout(layout)
                .withColorTargetState(blend?new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA):ColorTargetState.DEFAULT)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))
                .withCull(false).withVertexBinding(0,ess.ll.client.render.targetfx.internal.RenderPipelineLayouts.vertexFormat)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    }
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static TextureTarget body,armor,visible,closeHorizontal,closeBlurred,wideHorizontal,wideBlurred;
    private static boolean drawn;
    private ChamsGlow() {}
    static void initialize() { /* Register all pipelines before shader resource reload. */ }
    private static TextureTarget depthTarget(TextureTarget old,String name,int width,int height) {
        if(old==null) return new TextureTarget(name,width,height,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
        if(old.width!=width || old.height!=height) old.resize(width,height);
        return old;
    }
    static void begin() {
        drawn=false;
        var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        int w=main.width,h=main.height,halfW=(w+1)/2,halfH=(h+1)/2;
        body=depthTarget(body,"ESS Chams body",w,h);
        armor=depthTarget(armor,"ESS Chams armor",w,h);
        visible=WorldFx.target(visible,"ESS Chams visible",w,h);
        closeHorizontal=WorldFx.target(closeHorizontal,"ESS Chams close horizontal",halfW,halfH);
        closeBlurred=WorldFx.target(closeBlurred,"ESS Chams close",halfW,halfH);
        wideHorizontal=WorldFx.target(wideHorizontal,"ESS Chams wide horizontal",halfW,halfH);
        wideBlurred=WorldFx.target(wideBlurred,"ESS Chams wide",halfW,halfH);
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorAndDepthTextures(body.getColorTexture(),new Vector4f(0),body.getDepthTexture(),0);
        encoder.clearColorAndDepthTextures(armor.getColorTexture(),new Vector4f(0),armor.getDepthTexture(),0);
    }
    static void render(ChronoFrame frame,Vec3 camera,int color) {
        if(body==null) return;
        BATCH.begin(); frame.emit(camera,0xff000000|color,false,BATCH);
        if(BATCH.empty()) return;
        BATCH.flushMaskTo(frame.skin,body);
        if(!Chams.THROUGH_ARMOR.get()) ChronoArmor.renderMask(frame,camera,armor);
        drawn=true;
    }
    private static void blur(TextureTarget input,TextureTarget horizontal,TextureTarget output,float radius,int width,int height) {
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var u=WorldFx.upload(Std140Builder.onStack(stack,16).putVec4(1f/width,0,radius,0).get());
            WorldFx.draw("ESS Chams blur horizontal",horizontal,BLUR,"ChamsGlow",u,
                    "InSampler",input.getColorTextureView(),FilterMode.LINEAR);
            u=WorldFx.upload(Std140Builder.onStack(stack,16).putVec4(0,1f/height,radius,0).get());
            WorldFx.draw("ESS Chams blur vertical",output,BLUR,"ChamsGlow",u,
                    "InSampler",horizontal.getColorTextureView(),FilterMode.LINEAR);
        }
    }
    static void finish() {
        if(!drawn || body==null) return;
        var main=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var u=WorldFx.upload(Std140Builder.onStack(stack,16).putVec4(Chams.THROUGH_WALLS.get()?1:0,
                    Chams.THROUGH_ARMOR.get()?1:0,0,0).get());
            WorldFx.draw("ESS Chams visibility",visible,VISIBLE,"ChamsVisibility",u,
                    "BodySampler",body.getColorTextureView(),FilterMode.NEAREST,
                    "BodyDepthSampler",body.getDepthTextureView(),FilterMode.NEAREST,
                    "ArmorDepthSampler",armor.getDepthTextureView(),FilterMode.NEAREST,
                    "SceneDepthSampler",main.getDepthTextureView(),FilterMode.NEAREST);
            if(Chams.GLOW.get()) {
                float radius=Chams.GLOW_RADIUS.get();
                blur(visible,closeHorizontal,closeBlurred,Math.max(2,radius*.35f),main.width,main.height);
                blur(visible,wideHorizontal,wideBlurred,radius,main.width,main.height);
            }
            u=WorldFx.upload(Std140Builder.onStack(stack,16).putVec4(Chams.FILL_OPACITY.get()/100f,
                    Chams.GLOW.get()?Chams.GLOW_STRENGTH.get():0,0,0).get());
            WorldFx.draw("ESS Chams composite",main,COMPOSITE,"ChamsGlow",u,
                    "VisibleSampler",visible.getColorTextureView(),FilterMode.NEAREST,
                    "CloseSampler",Chams.GLOW.get()?closeBlurred.getColorTextureView():visible.getColorTextureView(),FilterMode.LINEAR,
                    "WideSampler",Chams.GLOW.get()?wideBlurred.getColorTextureView():visible.getColorTextureView(),FilterMode.LINEAR);
        }
    }
    static void release() {
        TextureTarget[] targets={body,armor,visible,closeHorizontal,closeBlurred,wideHorizontal,wideBlurred};
        for(TextureTarget target:targets) if(target!=null) target.destroyBuffers();
        body=armor=visible=closeHorizontal=closeBlurred=wideHorizontal=wideBlurred=null;
        BATCH.close();
    }
}
