package ess.ll.client.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** Camera-relative model capture and the original ESS ghost/emissive passes, adapted to 26.3. */
public final class GhostModelBatch implements VertexConsumer {
    private static final int CAPACITY=262144, STRIDE=24;
    private static final BindGroupLayout LAYOUT=BindGroupLayout.builder()
            .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
            .withUniform("Projection",UniformType.UNIFORM_BUFFER).build();
    private static final Identifier VERTEX=Identifier.fromNamespaceAndPath("ess-best","core/model_ghost");
    private static final RenderPipeline NORMAL=pipeline("normal","model_ghost",BlendFunction.TRANSLUCENT,true,false);
    private static final RenderPipeline NO_DEPTH=pipeline("no_depth","model_ghost",BlendFunction.TRANSLUCENT,false,false);
    private static final RenderPipeline GLOW=pipeline("glow","model_ghost_glow",BlendFunction.ADDITIVE,true,true);
    private static final RenderPipeline GLOW_NO_DEPTH=pipeline("glow_no_depth","model_ghost_glow",BlendFunction.ADDITIVE,false,true);
    private static final RenderPipeline FLAT=pipeline("flat","model_ghost_flat",BlendFunction.ADDITIVE,true,true);
    private static final RenderPipeline FLAT_NO_DEPTH=pipeline("flat_no_depth","model_ghost_flat",BlendFunction.ADDITIVE,false,true);
    private static final RenderPipeline FLAT_ALPHA=pipeline("flat_alpha","model_ghost_flat",BlendFunction.TRANSLUCENT,true,true);
    private static final RenderPipeline FLAT_ALPHA_NO_DEPTH=pipeline("flat_alpha_no_depth","model_ghost_flat",BlendFunction.TRANSLUCENT,false,true);
    private static final RenderPipeline MASK=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/chams_mask_depth"))
            .withVertexShader(VERTEX)
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/model_ghost_flat"))
            .withBindGroupLayout(LAYOUT).withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,true))
            .withCull(false).withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    private static final RenderPipeline SOUL=pipeline("soul","model_ghost_soul",BlendFunction.TRANSLUCENT,true,false);
    private static RenderPipeline pipeline(String name,String fragment,BlendFunction blend,boolean depth,boolean twoSided) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/model_ghost_"+name))
                .withVertexShader(VERTEX)
                .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/"+fragment))
                .withBindGroupLayout(LAYOUT).withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(depth?CompareOp.GREATER_THAN_OR_EQUAL:CompareOp.ALWAYS_PASS,false))
                .withCull(!twoSided).withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    }

    private final ByteBuffer staging=ByteBuffer.allocateDirect(CAPACITY*STRIDE).order(ByteOrder.nativeOrder());
    private GpuBuffer gpu;
    private int vertices,lastVertex;

    public void begin() { staging.clear(); vertices=0; lastVertex=-1; }
    public boolean empty() { return vertices==0; }
    public void close() { if(gpu!=null) { gpu.close(); gpu=null; } }

    public void flush(Identifier texture,float glow,boolean throughWalls,boolean emissive) {
        flush(texture,glow,throughWalls,emissive,false,0);
    }
    public void flush(Identifier texture,float glow,boolean throughWalls,boolean emissive,boolean soul,float soulTime) {
        if(vertices==0) return;
        if(vertices%4!=0) { begin(); return; }
        var mc=Minecraft.getInstance();
        var target=mc.gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(target)) { begin(); return; }
        var skin=mc.getTextureManager().getTexture(texture);
        staging.flip();
        var device=RenderSystem.getDevice();
        if(gpu==null || gpu.isClosed()) gpu=device.createBuffer(()->"ESS model ghost batch",40,(long)CAPACITY*STRIDE);
        device.createCommandEncoder().writeToBuffer(gpu.slice(0,staging.remaining()),staging);
        int indices=vertices/4*6;
        var sequence=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexBuffer=sequence.getBuffer(indices);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var projection=WorldFx.upload(Std140Builder.onStack(stack,64).putMat4f(WorldFx.projection()).get());
            if(!emissive) {
                var modulation=soul?new Vector4f(soulTime,(float)mc.gameRenderer.mainCamera().position().x,
                        (float)mc.gameRenderer.mainCamera().position().y,(float)mc.gameRenderer.mainCamera().position().z):new Vector4f(1);
                draw(soul?SOUL:throughWalls?NO_DEPTH:NORMAL,skin,projection,modulation,indexBuffer,sequence,indices,target);
            }
            if(emissive || glow>.001f) {
                float strength=Math.min(glow,1.5f);
                draw(throughWalls?GLOW_NO_DEPTH:GLOW,skin,projection,new Vector4f(strength),indexBuffer,sequence,indices,target);
            }
        } finally { begin(); }
    }
    public void flushFlat(Identifier texture,float strength,boolean throughWalls) {
        flushFlat(texture,strength,throughWalls,true);
    }
    public void flushFlat(Identifier texture,float strength,boolean throughWalls,boolean additive) {
        flushFlatTo(texture,strength,throughWalls,additive,null);
    }
    /** Opaque cutout pass into a colour+depth mask, with the model's nearest depth retained. */
    public void flushMaskTo(Identifier texture,RenderTarget mask) {
        if(vertices==0) return;
        if(vertices%4!=0) { begin(); return; }
        var mc=Minecraft.getInstance();
        if(mask==null || mask.getColorTextureView()==null || mask.getDepthTextureView()==null || !WorldFx.usable(mc.gameRenderer.mainRenderTarget())) { begin(); return; }
        var skin=mc.getTextureManager().getTexture(texture); staging.flip();
        var device=RenderSystem.getDevice();
        if(gpu==null || gpu.isClosed()) gpu=device.createBuffer(()->"ESS model ghost batch",40,(long)CAPACITY*STRIDE);
        device.createCommandEncoder().writeToBuffer(gpu.slice(0,staging.remaining()),staging);
        int indices=vertices/4*6; var sequence=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var projection=WorldFx.upload(Std140Builder.onStack(stack,64).putMat4f(WorldFx.projection()).get());
            draw(MASK,skin,projection,new Vector4f(1),sequence.getBuffer(indices),sequence,indices,mask);
        } finally { begin(); }
    }
    /** Draw a silhouette into an off-screen colour target while testing against the world depth. */
    public void flushFlatTo(Identifier texture,float strength,boolean throughWalls,boolean additive,RenderTarget colorTarget) {
        if(vertices==0) return;
        // Flat uses the same capture and index buffer; the dedicated pass is selected below.
        if(vertices%4!=0) { begin(); return; }
        var mc=Minecraft.getInstance(); var scene=mc.gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(scene)) { begin(); return; }
        var target=colorTarget==null?scene:colorTarget;
        var skin=mc.getTextureManager().getTexture(texture); staging.flip();
        var device=RenderSystem.getDevice();
        if(gpu==null || gpu.isClosed()) gpu=device.createBuffer(()->"ESS model ghost batch",40,(long)CAPACITY*STRIDE);
        device.createCommandEncoder().writeToBuffer(gpu.slice(0,staging.remaining()),staging);
        int indices=vertices/4*6; var sequence=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var projection=WorldFx.upload(Std140Builder.onStack(stack,64).putMat4f(WorldFx.projection()).get());
            draw(additive?(throughWalls?FLAT_NO_DEPTH:FLAT):(throughWalls?FLAT_ALPHA_NO_DEPTH:FLAT_ALPHA),skin,projection,new Vector4f(Math.min(strength,1.5f)),
                    sequence.getBuffer(indices),sequence,indices,target,scene);
        } finally { begin(); }
    }
    private void draw(RenderPipeline pipeline,net.minecraft.client.renderer.texture.AbstractTexture texture,
                      com.mojang.renderpearl.api.buffers.GpuBufferSlice projection,Vector4f color,GpuBuffer indexBuffer,
                      RenderSystem.AutoStorageIndexBuffer sequence,int count,com.mojang.blaze3d.pipeline.RenderTarget target) {
        draw(pipeline,texture,projection,color,indexBuffer,sequence,count,target,target);
    }
    private void draw(RenderPipeline pipeline,net.minecraft.client.renderer.texture.AbstractTexture texture,
                      com.mojang.renderpearl.api.buffers.GpuBufferSlice projection,Vector4f color,GpuBuffer indexBuffer,
                      RenderSystem.AutoStorageIndexBuffer sequence,int count,RenderTarget colorTarget,RenderTarget depthTarget) {
        var transforms=RenderSystem.getDynamicUniforms().writeTransform(WorldFx.view(),color,new Vector3f(),new Matrix4f());
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"ESS ghost model",
                colorTarget.getColorTextureView(),Optional.empty(),depthTarget.getDepthTextureView(),OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms",transforms);
            pass.setUniform("Projection",projection);
            pass.setUniform("Sampler0",texture.getTextureView(),texture.getSampler());
            pass.setVertexBuffer(0,gpu.slice());
            pass.setIndexBuffer(indexBuffer,sequence.type());
            pass.drawIndexed(count,1,0,0,0);
        }
    }

    @Override public VertexConsumer addVertex(float x,float y,float z) {
        if(vertices>=CAPACITY) throw new IllegalStateException("Ghost model batch overflow");
        lastVertex=staging.position();
        staging.putFloat(x).putFloat(y).putFloat(z).putFloat(0).putFloat(0).putInt(-1);
        vertices++;
        return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a) {
        if(lastVertex>=0) {
            staging.put(lastVertex+20,(byte)r).put(lastVertex+21,(byte)g)
                    .put(lastVertex+22,(byte)b).put(lastVertex+23,(byte)a);
        }
        return this;
    }
    @Override public VertexConsumer setColor(int color) {
        return setColor(color>>16&255,color>>8&255,color&255,color>>>24);
    }
    @Override public VertexConsumer setUv(float u,float v) {
        if(lastVertex>=0) { staging.putFloat(lastVertex+12,u); staging.putFloat(lastVertex+16,v); }
        return this;
    }
    @Override public VertexConsumer setUv1(int u,int v) { return this; }
    @Override public VertexConsumer setUv2(int u,int v) { return this; }
    @Override public VertexConsumer setUv3(float u,float v) { return this; }
    @Override public VertexConsumer setNormal(float x,float y,float z) { return this; }
    @Override public VertexConsumer setLineWidth(float width) { return this; }
}
