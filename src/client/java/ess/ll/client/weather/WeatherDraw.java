package ess.ll.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Immediate weather geometry in the main target: camera-relative quads drawn with the view rotation as the model
 * view and the level's projection, depth-tested against the finished frame and never writing depth.
 */
final class WeatherDraw {
    private static final BlendFunction ADD=new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE,BlendFactor.ZERO,BlendFactor.ONE);
    static final RenderPipeline TRANSLUCENT=colored("translucent",BlendFunction.TRANSLUCENT);
    static final RenderPipeline ADDITIVE=colored("additive",ADD);
    static final RenderPipeline TEXTURED=textured("textured",BlendFunction.TRANSLUCENT);
    static final RenderPipeline TEXTURED_ADDITIVE=textured("textured_additive",ADD);
    private static ByteBufferBuilder bytes;

    private WeatherDraw() {}

    private static RenderPipeline colored(String name,BlendFunction blend) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/weather_"+name))
                .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                .withCull(false).build());
    }
    private static RenderPipeline textured(String name,BlendFunction blend) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/weather_"+name))
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                .withCull(false).build());
    }

    static BufferBuilder begin(boolean textured) {
        if(bytes==null) bytes=new ByteBufferBuilder(1<<20);
        return new BufferBuilder(bytes,PrimitiveTopology.QUADS,textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
    }

    static void draw(String label,BufferBuilder builder,RenderPipeline pipeline,Identifier texture,Matrix4f view) {
        var mesh=builder.build();
        if(mesh==null) return;
        try(mesh) {
            var state=mesh.drawState();
            int indices=state.indexCount();
            var sequence=RenderSystem.getSequentialBuffer(state.primitiveTopology());
            var indexBuffer=sequence.getBuffer(indices);
            var device=RenderSystem.getDevice();
            var target=Minecraft.getInstance().gameRenderer.mainRenderTarget();
            // Fetched before the pass: a texture loaded on first use uploads, which a running pass does not allow.
            var tex=texture!=null ? Minecraft.getInstance().getTextureManager().getTexture(texture) : null;
            try(var vertices=device.createBuffer(()->"ESS "+label,32,mesh.vertexBuffer())) {
                var transforms=RenderSystem.getDynamicUniforms().writeTransform(view,new Vector4f(1,1,1,1),new Vector3f(),new Matrix4f());
                try(var pass=device.createCommandEncoder().createRenderPass(()->"ESS "+label,target.getColorTextureView(),
                        Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
                    pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms",transforms);
                    if(tex!=null) pass.setUniform("Sampler0",tex.getTextureView(),tex.getSampler());
                    pass.setVertexBuffer(0,vertices.slice());
                    pass.setIndexBuffer(indexBuffer,sequence.type());
                    pass.drawIndexed(indices,1,0,0,0);
                }
            }
        }
    }
}
