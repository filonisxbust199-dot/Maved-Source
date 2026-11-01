package ess.ll.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ess.ll.client.module.ClientModules;
import ess.ll.client.module.render.FogAtmosphere;
import ess.ll.client.module.render.ShaderSky;
import ess.ll.client.module.render.CustomCrystal;
import ess.ll.client.render.targetfx.internal.ClientIdentifiers;
import ess.ll.client.render.targetfx.internal.GpuUniformBufferHelper;
import ess.ll.client.render.targetfx.internal.RenderPipelineLayouts;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Full-screen world effects (FogAtmosphere, WorldTweaks weather): the frame's projection and view
 * rotation captured from LevelRenderer, pipeline and pass helpers, and a guard that switches a module off
 * instead of crashing the game when its shader fails.
 */
public final class WorldFx {
    private static final Matrix4f PROJECTION=new Matrix4f(), VIEW=new Matrix4f();
    private static boolean captured;
    private WorldFx() {}

    public static void capture(CameraRenderState camera) {
        if(camera.projectionMatrix==null || camera.viewRotationMatrix==null) { captured=false; return; }
        PROJECTION.set(camera.projectionMatrix); VIEW.set(camera.viewRotationMatrix);
        captured=true;
    }
    public static boolean captured() { return captured; }
    /** The level's projection and view rotation of this frame (copies). */
    public static Matrix4f projection() { return new Matrix4f(PROJECTION); }
    public static Matrix4f view() { return new Matrix4f(VIEW); }
    /** Client tick: GPU targets of modules that are off are freed. */
    public static void tick(Minecraft mc) {
        if(!ClientModules.FOG_ATMOSPHERE.enabled()) FogAtmosphere.release();
        if(!ClientModules.WORLD_TWEAKS.enabled()) ess.ll.client.weather.WeatherFx.release();
    }
    /** Start of the main pass, over the sky and before the terrain. */
    public static void beforeTerrain() {
        if(!captured || Minecraft.getInstance().level==null) return;
        guard(ClientModules.CUSTOM_CRYSTAL,CustomCrystal::renderTick);
        guard(ClientModules.WORLD_TWEAKS,ess.ll.client.weather.WeatherFx::sky);
        guard(ClientModules.SHADER_SKY,ShaderSky::render);
    }
    /** Main pass, before entity outlines: depth is final for opaque geometry. */
    public static void renderWorld() {
        if(!captured || Minecraft.getInstance().level==null) return;
        var delta=Minecraft.getInstance().getDeltaTracker();
        guard(ClientModules.FOG_ATMOSPHERE,()->FogAtmosphere.render(PROJECTION,VIEW,delta));
        guard(ClientModules.WORLD_TWEAKS,ess.ll.client.weather.WeatherFx::afterLevel);
    }
    private static void guard(ClientModules.Module module,Runnable effect) {
        if(!module.enabled()) return;
        try { effect.run(); }
        catch(RuntimeException e) {
            module.setEnabled(false);
            System.err.println("[ESS] "+module.name+" disabled: "+e);
            var player=Minecraft.getInstance().player;
            if(player!=null) player.sendSystemMessage(Component.literal("§c[Maved] "+module.name+" выключен: ошибка шейдера"));
        }
    }

    /** Full-screen triangle pipeline on the dimasik shaders; samplers are bound by name. */
    public static RenderPipeline pipeline(String fragment,Optional<BlendFunction> blend,String uniform,String... samplers) {
        var layout=RenderPipelineLayouts.getRenderPipelineLayouts6()
                .getRenderPipelineLayouts("dimasik/"+fragment)
                .getRenderPipelineLayouts11(ClientIdentifiers.clientResource("hands_fullscreen"))
                .getRenderPipelineLayouts4(ClientIdentifiers.clientResource(fragment))
                .getRenderPipelineLayouts5(CompareOp.ALWAYS_PASS)
                .getRenderPipelineLayouts9(false)
                .getRenderPipelineLayouts3(false);
        layout=blend.isPresent() ? layout.getRenderPipelineLayouts7(blend.get()) : layout.getRenderPipelineLayouts10();
        layout=layout.getRenderPipelineLayouts8(RenderPipelineLayouts.vertexFormat,PrimitiveTopology.TRIANGLES)
                .getRenderPipelineLayouts2(uniform,UniformType.UNIFORM_BUFFER);
        for(String sampler:samplers) layout=layout.getRenderPipelineLayouts12(sampler);
        return layout.getRenderPipeline();
    }
    /** A colour-only target of the given size, created or resized as needed. */
    public static TextureTarget target(TextureTarget target,String label,int width,int height) {
        width=Math.max(1,width); height=Math.max(1,height);
        if(target==null) return new TextureTarget(label,width,height,GpuFormat.RGBA8_UNORM,null);
        if(target.width!=width || target.height!=height) target.resize(width,height);
        return target;
    }
    public static boolean usable(RenderTarget target) {
        return target!=null && target.getColorTexture()!=null && target.getDepthTexture()!=null && !target.getDepthTexture().isClosed()
                && target.getColorTextureView()!=null && target.getDepthTextureView()!=null;
    }
    /** Copies the colour of from into to (same size or smaller). */
    public static void copyColor(RenderTarget from,RenderTarget to) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(from.getColorTexture(),to.getColorTexture(),0,0,0,0,0,
                Math.min(from.width,to.width),Math.min(from.height,to.height));
    }
    public static GpuBufferSlice upload(ByteBuffer data) { return GpuUniformBufferHelper.getGpuBufferSlice2(data); }

    /** One full-screen draw into target; samplers are name / view pairs, linear or nearest filtered. */
    public static void draw(String label,RenderTarget target,RenderPipeline pipeline,String uniform,GpuBufferSlice data,Object... samplers) {
        try(RenderPass pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->label,target.getColorTextureView(),Optional.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            pass.setUniform(uniform,data);
            for(int i=0;i+2<samplers.length;i+=3)
                pass.setUniform((String)samplers[i],(GpuTextureView)samplers[i+1],RenderSystem.getSamplerCache().getClampToEdge((FilterMode)samplers[i+2]));
            pass.draw(3,1,0,0);
        }
    }
}
