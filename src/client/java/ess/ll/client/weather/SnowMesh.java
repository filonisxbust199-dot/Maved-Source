package ess.ll.client.weather;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import ess.ll.client.render.targetfx.internal.GpuUniformBufferHelper;
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

/**
 * Zenith's SnowShader: the snow cover around the camera as one mesh drawn with the snow shader (grain, crust,
 * sparkle, wet sheen). SnowCover hands it quads only when the cover changed; the mesh stays on the GPU and is
 * drawn every frame. Positions are kept relative to an anchor that moves in 24-block steps, so the floats stay
 * small while the mesh does not need a rebuild for every step the camera takes.
 */
final class SnowMesh {
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/weather_snow"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/weather_snow"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/weather_snow"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("SnowData",UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,true))
            .withCull(false).build());

    private static boolean enabled=true;
    private static float[] quads=new float[6144];
    private static int[] colors=new int[2048];
    private static int count;
    /** Collecting a new mesh this frame / a mesh is on the GPU. */
    private static boolean collecting, built;
    private static boolean anchored;
    private static double anchorX, anchorY, anchorZ, keptX, keptY, keptZ, meshX, meshY, meshZ;
    private static double cameraX, cameraY, cameraZ;
    private static GpuBuffer vertices;
    private static int indexCount;
    private static ByteBufferBuilder bytes;
    private static final float[] sunDir={.3f,.9f,.3f}, sunColor={1,.98f,.94f}, skyColor={.52f,.56f,.62f};
    private static float wetness=.3f, sheen=1;
    private static final float GRAIN_SCALE=24, GRAIN_STRENGTH=1, CRUST=.6f, SPARKLE=.19f;

    private SnowMesh() {}

    static boolean isFlag() { return enabled; }
    /** Dev check: indices of the mesh on the GPU. */
    static int indices() { return indexCount; }
    /** The snow shader is available (Zenith checked the program compiled). */
    static boolean isAvailable() { return enabled; }
    static boolean isFlag3() { return built; }

    /** Per frame: camera, light of the weather channel and wetness. */
    static void updateLighting(Vec3 camera,WeatherChannel channel,float wet) {
        cameraX=camera.x; cameraY=camera.y; cameraZ=camera.z;
        float length=(float)Math.sqrt(channel.floatValues4[0]*channel.floatValues4[0]+channel.floatValues4[1]*channel.floatValues4[1]+channel.floatValues4[2]*channel.floatValues4[2]);
        if(length<1e-5f) length=1;
        for(int i=0;i<3;i++) sunDir[i]=channel.floatValues4[i]/length;
        System.arraycopy(channel.floatValues5,0,sunColor,0,3);
        skyColor[0]=(channel.floatValues[0]+channel.floatValues2[0])*.5f+.16f;
        skyColor[1]=(channel.floatValues[1]+channel.floatValues2[1])*.5f+.16f;
        skyColor[2]=(channel.floatValues[2]+channel.floatValues2[2])*.5f+.17f;
        wetness=wet;
    }
    /** A new mesh starts: quads that follow replace the one on the GPU when the frame draws. */
    static void beginMesh() {
        anchor();
        count=0;
        collecting=true;
        built=false;
    }
    private static void anchor() {
        double x=Math.floor(cameraX), y=Math.floor(cameraY), z=Math.floor(cameraZ);
        if(!anchored || Math.abs(x-keptX)>24 || Math.abs(y-keptY)>24 || Math.abs(z-keptZ)>24) {
            keptX=x; keptY=y; keptZ=z;
            anchored=true;
        }
        anchorX=keptX; anchorY=keptY; anchorZ=keptZ;
    }
    static void addQuad(double x0,double y0,double z0,double x1,double y1,double z1,double x2,double y2,double z2,
                          double x3,double y3,double z3,int c0,int c1,int c2,int c3) {
        if(!collecting) beginMesh();
        if((count+1)*12>quads.length) {
            quads=java.util.Arrays.copyOf(quads,quads.length*2);
            colors=java.util.Arrays.copyOf(colors,colors.length*2);
        }
        int q=count*12;
        quads[q]=(float)(x0-anchorX); quads[q+1]=(float)(y0-anchorY); quads[q+2]=(float)(z0-anchorZ);
        quads[q+3]=(float)(x1-anchorX); quads[q+4]=(float)(y1-anchorY); quads[q+5]=(float)(z1-anchorZ);
        quads[q+6]=(float)(x2-anchorX); quads[q+7]=(float)(y2-anchorY); quads[q+8]=(float)(z2-anchorZ);
        quads[q+9]=(float)(x3-anchorX); quads[q+10]=(float)(y3-anchorY); quads[q+11]=(float)(z3-anchorZ);
        int c=count*4;
        colors[c]=c0; colors[c+1]=c1; colors[c+2]=c2; colors[c+3]=c3;
        count++;
    }
    /** Drops the mesh (snow off or gone). */
    static void clear() {
        count=0;
        collecting=false;
        built=false;
        if(vertices!=null) { vertices.close(); vertices=null; }
        indexCount=0;
    }
    static void setSheen(float value) { sheen=Math.clamp(value,0,2.5f); }

    /** Uploads a mesh collected this frame, then draws the one on the GPU with the frame's view rotation. */
    static void render(Matrix4f view) {
        if(collecting) upload();
        if(vertices==null || indexCount==0) return;
        var device=RenderSystem.getDevice();
        var target=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        float dx=(float)(meshX-cameraX), dy=(float)(meshY-cameraY), dz=(float)(meshZ-cameraZ);
        Matrix4f modelView=new Matrix4f(view).translate(dx,dy,dz);
        var sequence=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexBuffer=sequence.getBuffer(indexCount);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var data=GpuUniformBufferHelper.getGpuBufferSlice2(Std140Builder.onStack(stack,176)
                    .putMat4f(new Matrix4f().translation(dx,dy,dz))
                    .putVec4((float)wrap(cameraX),(float)wrap(cameraY),(float)wrap(cameraZ),0)
                    .putVec4(sunDir[0],sunDir[1],sunDir[2],0)
                    .putVec4(sunColor[0],sunColor[1],sunColor[2],0)
                    .putVec4(skyColor[0],skyColor[1],skyColor[2],0)
                    .putVec4(GRAIN_SCALE,256*GRAIN_SCALE,GRAIN_STRENGTH,CRUST)
                    .putVec4(SPARKLE,wetness,sheen,0).get());
            var transforms=RenderSystem.getDynamicUniforms().writeTransform(modelView,new Vector4f(1,1,1,1),new Vector3f(),new Matrix4f());
            try(var pass=device.createCommandEncoder().createRenderPass(()->"ESS snow cover",target.getColorTextureView(),
                    Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms",transforms);
                pass.setUniform("SnowData",data);
                pass.setVertexBuffer(0,vertices.slice());
                pass.setIndexBuffer(indexBuffer,sequence.type());
                pass.drawIndexed(indexCount,1,0,0,0);
            }
        }
    }
    private static void upload() {
        collecting=false;
        if(vertices!=null) { vertices.close(); vertices=null; }
        indexCount=0;
        if(count==0) { built=true; return; }
        if(bytes==null) bytes=new ByteBufferBuilder(1<<20);
        BufferBuilder builder=new BufferBuilder(bytes,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(int i=0;i<count;i++) for(int v=0;v<4;v++) {
            int q=i*12+v*3;
            builder.addVertex(quads[q],quads[q+1],quads[q+2]).setColor(colors[i*4+v]);
        }
        var mesh=builder.build();
        if(mesh==null) return;
        try(mesh) {
            vertices=RenderSystem.getDevice().createBuffer(()->"ESS snow cover",32,mesh.vertexBuffer());
            indexCount=mesh.drawState().indexCount();
        }
        meshX=anchorX; meshY=anchorY; meshZ=anchorZ;
        built=true;
        count=0;
    }
    private static double wrap(double v) { return v-Math.floor(v/256.0)*256.0; }
}
