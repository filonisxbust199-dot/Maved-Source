package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import ess.ll.client.render.WorldFx;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/** GPU half of ESS's BlockOverlay: its face shaders, uniforms and six animated face quads. */
final class BlockOverlayGpu {
    private static final Identifier SURFACE=Identifier.fromNamespaceAndPath("ess-best","core/blockoverlay_surface");
    private static final Identifier SOLAR=Identifier.fromNamespaceAndPath("ess-best","core/blockoverlay_solar");
    private static final BindGroupLayout SURFACE_LAYOUT=BindGroupLayout.builder()
            .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
            .withUniform("Projection",UniformType.UNIFORM_BUFFER)
            .withUniform("OverlayParams",UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout SOLAR_LAYOUT=BindGroupLayout.builder()
            .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
            .withUniform("Projection",UniformType.UNIFORM_BUFFER)
            .withUniform("SolarParams",UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline SURFACE_PIPE=surface("surface",false,false);
    private static final RenderPipeline PATTERN_PIPE=surface("pattern",true,false);
    private static final RenderPipeline GLOW_PIPE=surface("glow",false,true);
    private static final RenderPipeline SOLAR_PIPE=RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/blockoverlay_solar"))
            .withVertexShader(SOLAR).withFragmentShader(SOLAR).withBindGroupLayout(SOLAR_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build());
    private BlockOverlayGpu() {}

    private static RenderPipeline surface(String name,boolean pattern,boolean glow) {
        var builder=RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/blockoverlay_"+name))
                .withVertexShader(SURFACE).withFragmentShader(SURFACE).withBindGroupLayout(SURFACE_LAYOUT)
                .withColorTargetState(new ColorTargetState(glow
                        ? new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE) : BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(glow ? CompareOp.ALWAYS_PASS : CompareOp.GREATER_THAN_OR_EQUAL,false))
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false);
        if(pattern) builder.withShaderDefine("OVERLAY_PATTERN");
        if(glow) builder.withShaderDefine("OVERLAY_GLOW");
        return RenderPipelines.register(builder.build());
    }

    static void draw(double[] bounds,Vec3 camera,float[] faceWeights,int style,float time,float scale,
                     int primary,int secondary,float opacity,float width,float glow,float cornerLength) {
        boolean solar=style<0;
        ByteBuffer vertices=ByteBuffer.allocateDirect(6*6*(solar?16:24)).order(ByteOrder.nativeOrder());
        for(int face=0;face<6;face++) {
            float weight=faceWeights[face];
            if(weight<=.015f) continue;
            double[][] quad=face(bounds,face,!solar && style>=5);
            if(solar) {
                int color=Math.clamp(Math.round(opacity*weight*255),0,255)<<24;
                putSolar(vertices,quad[0],camera,color); putSolar(vertices,quad[1],camera,color); putSolar(vertices,quad[2],camera,color);
                putSolar(vertices,quad[0],camera,color); putSolar(vertices,quad[2],camera,color); putSolar(vertices,quad[3],camera,color);
            } else {
                double uLen=distance(quad[0],quad[1]),vLen=distance(quad[0],quad[3]);
                double longest=Math.max(uLen,vLen);
                if(longest<1e-6) continue;
                float aspectU=Math.max(1,Math.round((float)(uLen/longest*255)))/255f;
                float aspectV=Math.max(1,Math.round((float)(vLen/longest*255)))/255f;
                float pu=style>=5?.16f/aspectU:0,pv=style>=5?.16f/aspectV:0;
                putSurface(vertices,quad,camera,-pu,-pv,aspectU,aspectV,weight);
                putSurface(vertices,quad,camera,1+pu,-pv,aspectU,aspectV,weight);
                putSurface(vertices,quad,camera,1+pu,1+pv,aspectU,aspectV,weight);
                putSurface(vertices,quad,camera,-pu,-pv,aspectU,aspectV,weight);
                putSurface(vertices,quad,camera,1+pu,1+pv,aspectU,aspectV,weight);
                putSurface(vertices,quad,camera,-pu,1+pv,aspectU,aspectV,weight);
            }
        }
        int count=vertices.position()/(solar?16:24);
        if(count==0) return;
        vertices.flip();
        var mc=Minecraft.getInstance();
        var target=mc.gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(target)) return;
        var device=RenderSystem.getDevice();
        var encoder=device.createCommandEncoder();
        var gpu=encoder.transientMemory().uploadGpu(vertices,4,32);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var transform=RenderSystem.getDynamicUniforms().writeTransform(WorldFx.view(),new Vector4f(1),new Vector3f(),new Matrix4f());
            var projection=WorldFx.upload(Std140Builder.onStack(stack,64).putMat4f(WorldFx.projection()).get());
            var settings=solar
                    ? WorldFx.upload(Std140Builder.onStack(stack,64)
                        .putVec4((float)(camera.x%1024),(float)(camera.y%1024),(float)(camera.z%1024),time)
                        .putVec4(r(primary),g(primary),b(primary),1)
                        .putVec4(r(secondary),g(secondary),b(secondary),1)
                        .putVec4(r(lift(primary,.55f)),g(lift(primary,.55f)),b(lift(primary,.55f)),scale).get())
                    : WorldFx.upload(Std140Builder.onStack(stack,64)
                        .putVec4(r(primary),g(primary),b(primary),opacity)
                        .putVec4(r(secondary),g(secondary),b(secondary),glow)
                        .putVec4(time,scale,style,width)
                        .putVec4(cornerLength,(float)((bounds[0]+bounds[3])*.5-camera.x),
                                (float)((bounds[1]+bounds[4])*.5-camera.y),(float)((bounds[2]+bounds[5])*.5-camera.z)).get());
            try(var pass=encoder.createRenderPass(()->"ESS BlockOverlay",
                    target.getColorTextureView(),Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(solar?SOLAR_PIPE:style>=5?SURFACE_PIPE:PATTERN_PIPE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms",transform);
                pass.setUniform("Projection",projection);
                pass.setUniform(solar?"SolarParams":"OverlayParams",settings);
                pass.setVertexBuffer(0,gpu);
                pass.draw(count,1,0,0);
                if(!solar && style>=5 && glow>0) {
                    pass.setPipeline(RenderSystem.getCompiledPipeline(GLOW_PIPE));
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms",transform);
                    pass.setUniform("Projection",projection);
                    pass.setUniform("OverlayParams",settings);
                    pass.setVertexBuffer(0,gpu);
                    pass.draw(count,1,0,0);
                }
            }
        }
    }

    private static void putSolar(ByteBuffer out,double[] p,Vec3 cam,int color) {
        out.putFloat((float)(p[0]-cam.x)).putFloat((float)(p[1]-cam.y)).putFloat((float)(p[2]-cam.z));
        out.put((byte)(color>>16)).put((byte)(color>>8)).put((byte)color).put((byte)(color>>24));
    }
    private static void putSurface(ByteBuffer out,double[][] q,Vec3 cam,float u,float v,float aspectU,float aspectV,float weight) {
        double x=q[0][0]+(q[1][0]-q[0][0])*u+(q[3][0]-q[0][0])*v;
        double y=q[0][1]+(q[1][1]-q[0][1])*u+(q[3][1]-q[0][1])*v;
        double z=q[0][2]+(q[1][2]-q[0][2])*u+(q[3][2]-q[0][2])*v;
        out.putFloat((float)(x-cam.x)).putFloat((float)(y-cam.y)).putFloat((float)(z-cam.z));
        out.putFloat(u).putFloat(v);
        out.put((byte)Math.round(aspectU*255)).put((byte)Math.round(aspectV*255)).put((byte)255)
                .put((byte)Math.clamp(Math.round(weight*255),0,255));
    }
    private static double distance(double[] a,double[] b) {
        double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2]; return Math.sqrt(x*x+y*y+z*z);
    }
    private static double[][] face(double[] b,int direction,boolean padded) {
        double e=padded?.003:0;
        double x0=b[0]-e,y0=b[1]-e,z0=b[2]-e,x1=b[3]+e,y1=b[4]+e,z1=b[5]+e;
        e=padded?0:.003;
        return switch (direction) {
            case 0 -> new double[][]{{x0,y0-e,z0},{x1,y0-e,z0},{x1,y0-e,z1},{x0,y0-e,z1}}; // DOWN
            case 1 -> new double[][]{{x0,y1+e,z0},{x1,y1+e,z0},{x1,y1+e,z1},{x0,y1+e,z1}}; // UP
            case 2 -> new double[][]{{x0,y0,z0-e},{x1,y0,z0-e},{x1,y1,z0-e},{x0,y1,z0-e}}; // NORTH
            case 3 -> new double[][]{{x0,y0,z1+e},{x1,y0,z1+e},{x1,y1,z1+e},{x0,y1,z1+e}}; // SOUTH
            case 4 -> new double[][]{{x0-e,y0,z0},{x0-e,y0,z1},{x0-e,y1,z1},{x0-e,y1,z0}}; // WEST
            default -> new double[][]{{x1+e,y0,z0},{x1+e,y0,z1},{x1+e,y1,z1},{x1+e,y1,z0}}; // EAST
        };
    }
    private static float r(int c) { return (c>>16&255)/255f; }
    private static float g(int c) { return (c>>8&255)/255f; }
    private static float b(int c) { return (c&255)/255f; }
    private static int lift(int c,float t) {
        int r=(int)((c>>16&255)+(255-(c>>16&255))*t);
        int g=(int)((c>>8&255)+(255-(c>>8&255))*t);
        int b=(int)((c&255)+(255-(c&255))*t);
        return r<<16|g<<8|b;
    }
}
