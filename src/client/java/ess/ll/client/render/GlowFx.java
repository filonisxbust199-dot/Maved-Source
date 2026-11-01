package ess.ll.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import ess.ll.client.render.targetfx.internal.GpuResourceScope;
import ess.ll.client.render.targetfx.internal.ScreenProjectionMath;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Glowing world effects shared by Trails, HitBubbles and BlockOverlay: lines with a white-hot core, spreading
 * rings, dots, bands and fills, all drawn by the glow_fx shader in one pass after water and ice, so
 * those never cover them while solid blocks still do. Each module adds a painter that lays its quads out for
 * the frame; positions are taken relative to the camera.
 */
public final class GlowFx {
    public static final int LINE=0,RING=1,BURST=2,DOT=3,SHEET=4,FILL=5;
    /** Lays out a module's quads for this frame. */
    public interface Painter { void paint(GlowFx fx); }

    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/glow_fx"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/glow_fx"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/glow_fx"))
            .withVertexBinding(0,DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
            .withCull(false).build());
    private static final List<Painter> PAINTERS=new ArrayList<>();
    private static boolean registered;

    /** Camera position and the partial tick of this frame. */
    public final Vec3 eye;
    public final float partial;
    private final BufferBuilder out;
    private final Matrix4f view;
    private final int clock;
    private final Vector3f point=new Vector3f();
    private int quads;

    private GlowFx(BufferBuilder out,Matrix4f view,Vec3 eye,float partial) {
        this.out=out; this.view=view; this.eye=eye; this.partial=partial;
        clock=(int)(System.currentTimeMillis()%4000*1000/4000);
    }

    public static void add(Painter painter) {
        if(!registered) {
            registered=true;
            LevelRenderEvents.END_MAIN.register(context->draw());
        }
        PAINTERS.add(painter);
    }

    private static void draw() {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null) return;
        var builder=GpuResourceScope.getGpuResourceScope().getBufferBuilder(PrimitiveTopology.QUADS,DefaultVertexFormat.ENTITY);
        GlowFx fx=new GlowFx(builder,ScreenProjectionMath.viewMatrixCopy(),mc.gameRenderer.mainCamera().position(),
                mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        for(Painter painter:PAINTERS) {
            try { painter.paint(fx); } catch(RuntimeException e) { System.err.println("[ESS] Glow effect failed: "+e); }
        }
        if(fx.quads==0) return;
        var mesh=builder.build();
        if(mesh==null) return;
        try(mesh) {
            var state=mesh.drawState();
            int indices=state.indexCount();
            var sequence=RenderSystem.getSequentialBuffer(state.primitiveTopology());
            var indexBuffer=sequence.getBuffer(indices);
            var device=RenderSystem.getDevice();
            try(var vertices=device.createBuffer(()->"ESS glow fx",32,mesh.vertexBuffer())) {
                var target=mc.gameRenderer.mainRenderTarget();
                var transforms=RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(),new Vector4f(1,1,1,1),new Vector3f(),new Matrix4f());
                try(var pass=device.createCommandEncoder().createRenderPass(()->"ESS glow fx",target.getColorTextureView(),
                        Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
                    pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms",transforms);
                    pass.setVertexBuffer(0,vertices.slice());
                    pass.setIndexBuffer(indexBuffer,sequence.type());
                    pass.drawIndexed(indices,1,0,0,0);
                }
            }
        } catch(RuntimeException e) {
            System.err.println("[ESS] Glow fx draw failed: "+e);
        }
    }

    /** One corner: colour alpha is the strength; glow and the parameter go in thousandths. */
    public void vertex(double x,double y,double z,int rgb,float strength,float u,float v,int mode,float glow,float param) {
        view.transformPosition((float)(x-eye.x),(float)(y-eye.y),(float)(z-eye.z),point);
        int a=Math.round(Math.clamp(strength,0,1)*255);
        out.addVertex(point.x,point.y,point.z,a<<24|rgb&0xffffff,u,v,mode|clock<<16,
                Math.round(Math.clamp(glow,0,1)*1000)|Math.round(Math.clamp(param,0,1)*1000)<<16,0,1,0);
    }
    public void quadDone() { quads++; }

    /**
     * A line from a to b facing the camera. Width is the core's half width; with glow the quad grows so the
     * glow has room around it. The line is stretched by its core at both ends so joined lines close at corners.
     */
    public void line(Vec3 a,Vec3 b,float width,int rgb,float strengthA,float strengthB,float glow) {
        Vec3 dir=b.subtract(a);
        double length=dir.length();
        if(length<1e-5) return;
        dir=dir.scale(1/length);
        a=a.subtract(dir.scale(width)); b=b.add(dir.scale(width));
        Vec3 side=dir.cross(a.add(b).scale(.5).subtract(eye));
        if(side.lengthSqr()<1e-10) return;
        float core=glow>.01f ? .25f : .9f;
        side=side.normalize().scale(width/core);
        vertex(a.x-side.x,a.y-side.y,a.z-side.z,rgb,strengthA,0,-1,LINE,glow,core);
        vertex(a.x+side.x,a.y+side.y,a.z+side.z,rgb,strengthA,0,1,LINE,glow,core);
        vertex(b.x+side.x,b.y+side.y,b.z+side.z,rgb,strengthB,1,1,LINE,glow,core);
        vertex(b.x-side.x,b.y-side.y,b.z-side.z,rgb,strengthB,1,-1,LINE,glow,core);
        quadDone();
    }

    /**
     * A smooth line through points facing the camera: each point has its own half width, colour and strength,
     * and its side is taken from the neighbours, so bends have no gaps.
     */
    public void strip(List<Vec3> points,float[] widths,int[] colors,float[] strengths,float glow) {
        int n=points.size();
        if(n<2) return;
        float core=glow>.01f ? .25f : .9f;
        Vec3[] sides=new Vec3[n];
        for(int i=0;i<n;i++) {
            Vec3 tangent=points.get(Math.min(n-1,i+1)).subtract(points.get(Math.max(0,i-1)));
            Vec3 side=tangent.cross(points.get(i).subtract(eye));
            sides[i]=side.lengthSqr()<1e-12 ? null : side.normalize().scale(widths[i]/core);
        }
        for(int i=0;i+1<n;i++) {
            Vec3 a=points.get(i),b=points.get(i+1),sa=sides[i],sb=sides[i+1];
            if(sa==null || sb==null || strengths[i]<=.003f && strengths[i+1]<=.003f) continue;
            vertex(a.x-sa.x,a.y-sa.y,a.z-sa.z,colors[i],strengths[i],0,-1,LINE,glow,core);
            vertex(a.x+sa.x,a.y+sa.y,a.z+sa.z,colors[i],strengths[i],0,1,LINE,glow,core);
            vertex(b.x+sb.x,b.y+sb.y,b.z+sb.z,colors[i+1],strengths[i+1],1,1,LINE,glow,core);
            vertex(b.x-sb.x,b.y-sb.y,b.z-sb.z,colors[i+1],strengths[i+1],1,-1,LINE,glow,core);
            quadDone();
        }
    }

    /** A square sprite facing the camera around a centre: a ring, a burst or a dot. */
    public void sprite(Vec3 centre,float radius,int mode,int rgb,float strength,float glow,float param) {
        var camera=Minecraft.getInstance().gameRenderer.mainCamera();
        Quaternionf rotation=new Quaternionf(camera.rotation());
        Vector3f right=rotation.transform(new Vector3f(radius,0,0)),up=rotation.transform(new Vector3f(0,radius,0));
        float[][] corners={{-1,-1},{1,-1},{1,1},{-1,1}};
        for(float[] c:corners)
            vertex(centre.x+right.x*c[0]+up.x*c[1],centre.y+right.y*c[0]+up.y*c[1],centre.z+right.z*c[0]+up.z*c[1],rgb,strength,c[0],c[1],mode,glow,param);
        quadDone();
    }

    /** World point to GUI coordinates; null behind the camera or off screen. */
    public static Vector2f screen(Vec3 point) { return screen(point,false); }
    /** World point to GUI coordinates; with clamp off-screen points are kept (still null behind the camera). */
    public static Vector2f screen(Vec3 point,boolean anywhere) {
        var mc=Minecraft.getInstance();
        var camera=mc.gameRenderer.mainCamera();
        Vec3 offset=point.subtract(camera.position());
        Vector3f view=new Vector3f((float)offset.x,(float)offset.y,(float)offset.z).rotate(new Quaternionf(camera.rotation()).conjugate());
        if(view.z>=-.05f) return null;
        float h=mc.getWindow().getGuiScaledHeight(),w=mc.getWindow().getGuiScaledWidth();
        float f=(float)(1/Math.tan(Math.toRadians(camera.getFov())/2));
        float x=w/2+view.x*f*h/(-2*view.z),y=h/2-view.y*f*h/(-2*view.z);
        if(!Float.isFinite(x) || !Float.isFinite(y)) return null;
        return anywhere || x>=0 && x<=w && y>=0 && y<=h ? new Vector2f(x,y) : null;
    }
}
