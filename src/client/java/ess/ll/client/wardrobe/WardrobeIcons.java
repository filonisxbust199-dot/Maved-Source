package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import ess.ll.client.wardrobe.Wardrobe.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Wardrobe models drawn straight into the GUI as live tiles: the idle clip plays, quads are projected
 * orthographically, sorted back to front and lit by a fixed key light. Anchored parts sit where they would on a
 * player, so clothing keeps its shape.
 */
public final class WardrobeIcons {
    private static final float[][] PART_ORIGIN={{0,0,0},{0,0,0},{-5,2,0},{5,2,0},{-1.9f,12,0},{1.9f,12,0},{0,0,0}};
    private static final Vector3f LIGHT=new Vector3f(-.35f,-.75f,-.55f).normalize();
    private static final Map<String,float[]> FRAMES=new HashMap<>();
    private static final Quaternionf ROT=new Quaternionf();

    private WardrobeIcons() {}

    public static float time() { return (System.nanoTime()%(1L<<42))/1e9f; }

    /** Draws an item's model centred on (cx, cy) inside a size x size square; false while it is still loading. */
    public static boolean draw(GuiGraphicsExtractor g,Item item,float cx,float cy,float size,float yaw,float pitch,float alpha) {
        WardrobeModel model=Wardrobe.model(item);
        if(model==null || alpha<=.004f || size<=1) return model!=null;
        var cache=WardrobeAnimator.Cache.of(-1,item.id(),true);
        float now=time();
        cache.clips.want(model,"idle",now);
        float[] pose=cache.pose.compute(model,cache.clips,now,null);
        float[] frame=FRAMES.computeIfAbsent(item.id(),k->frame(model));
        float scale=size*.5f/Math.max(.02f,frame[3]);
        Matrix4f view=new Matrix4f().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw)).scale(scale).translate(-frame[0],-frame[1],-frame[2]);
        List<Quad> quads=new ArrayList<>();
        project(model,pose,view,cx,cy,alpha,quads);
        quads.sort((a,b)->Float.compare(b.depth,a.depth));
        var texture=Minecraft.getInstance().getTextureManager().getTexture(model.texture);
        var p=new Matrix3x2f(g.pose());
        var clip=g.scissorStack.peek();
        var bounds=new ScreenRectangle((int)Math.floor(cx-size),(int)Math.floor(cy-size),(int)Math.ceil(size*2)+1,(int)Math.ceil(size*2)+1).transformMaxBounds(p);
        if(clip!=null) bounds=bounds.intersection(clip);
        if(bounds!=null && !quads.isEmpty())
            g.guiRenderState.addGuiElement(new Mesh(p,quads,TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),clip,bounds));
        return true;
    }
    /** Centre and half-extent of the rest pose. */
    private static float[] frame(WardrobeModel model) {
        float[] min={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE},max={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        Matrix4f[] bones=bones(model,null,new Matrix4f());
        Vector3f v=new Vector3f();
        for(int q=0;q<model.quadCount;q++) {
            for(int k=0;k<4;k++) {
                int i=q*12+k*3;
                bones[model.quadBone[q]].transformPosition(model.quadPos[i],model.quadPos[i+1],model.quadPos[i+2],v);
                min[0]=Math.min(min[0],v.x); min[1]=Math.min(min[1],v.y); min[2]=Math.min(min[2],v.z);
                max[0]=Math.max(max[0],v.x); max[1]=Math.max(max[1],v.y); max[2]=Math.max(max[2],v.z);
            }
        }
        if(min[0]>max[0]) return new float[]{0,0,0,.5f};
        float half=0;
        for(int a=0;a<3;a++) half=Math.max(half,(max[a]-min[a])/2);
        return new float[]{(min[0]+max[0])/2,(min[1]+max[1])/2,(min[2]+max[2])/2,half*1.08f};
    }
    /** Bone matrices with every anchored group placed where that body part is on a standing player. */
    private static Matrix4f[] bones(WardrobeModel model,float[] pose,Matrix4f view) {
        int n=model.bones();
        Matrix4f[] out=new Matrix4f[n];
        for(int b=0;b<n;b++) {
            int parent=model.parent[b];
            Matrix4f m;
            if(parent>=0) m=new Matrix4f(out[parent]);
            else {
                float[] o=PART_ORIGIN[model.group[b]];
                m=new Matrix4f(view).translate(o[0]/16,o[1]/16,o[2]/16);
            }
            int i=b*9;
            boolean anchoredRoot=parent<0 && model.anchor[b]>=0;
            float ox=anchoredRoot ? 0 : model.pivot[b*3],oy=anchoredRoot ? 0 : model.pivot[b*3+1],oz=anchoredRoot ? 0 : model.pivot[b*3+2];
            float rx=pose==null ? model.rotation[b*3] : pose[i],ry=pose==null ? model.rotation[b*3+1] : pose[i+1],rz=pose==null ? model.rotation[b*3+2] : pose[i+2];
            m.translate((ox+(pose==null ? 0 : pose[i+3]))/16,(oy+(pose==null ? 0 : pose[i+4]))/16,(oz+(pose==null ? 0 : pose[i+5]))/16);
            if(rx!=0 || ry!=0 || rz!=0) m.rotate(ROT.rotationZYX(rz,ry,rx));
            if(pose!=null && (pose[i+6]!=1 || pose[i+7]!=1 || pose[i+8]!=1)) m.scale(pose[i+6],pose[i+7],pose[i+8]);
            out[b]=m;
        }
        return out;
    }
    private static void project(WardrobeModel model,float[] pose,Matrix4f view,float cx,float cy,float alpha,List<Quad> out) {
        Matrix4f[] bones=bones(model,pose,view);
        Vector3f v=new Vector3f(),n=new Vector3f();
        for(int q=0;q<model.quadCount;q++) {
            int b=model.quadBone[q];
            if(pose!=null && pose[b*9+6]==0 && pose[b*9+7]==0) continue;
            Matrix4f m=bones[b];
            m.transformDirection(model.quadNormal[q*3],model.quadNormal[q*3+1],model.quadNormal[q*3+2],n);
            if(n.lengthSquared()>0) n.normalize();
            int flags=model.quadFlags[q];
            boolean glow=(flags&WardrobeModel.GLOW)!=0,translucent=(flags&WardrobeModel.TRANSLUCENT)!=0;
            // Models are cut out and single sided in the world; back faces stay hidden here too.
            if(n.z>.02f && !translucent) continue;
            float[] xy=new float[8],uv=new float[8];
            float depth=-Float.MAX_VALUE;
            for(int k=0;k<4;k++) {
                int i=q*12+k*3;
                m.transformPosition(model.quadPos[i],model.quadPos[i+1],model.quadPos[i+2],v);
                xy[k*2]=cx+v.x; xy[k*2+1]=cy+v.y; depth=Math.max(depth,v.z);
                uv[k*2]=model.quadUv[q*8+k*2]; uv[k*2+1]=model.quadUv[q*8+k*2+1];
            }
            float light=glow ? 1 : Math.min(1,.5f+.55f*Math.max(0,n.dot(LIGHT))+.1f*Math.max(0,-n.z));
            int c=Math.round(255*light),a=Math.round(255*alpha*(translucent ? .7f : 1));
            out.add(new Quad(xy,uv,depth,a<<24|c<<16|c<<8|c));
        }
    }

    private record Quad(float[] xy,float[] uv,float depth,int color) {}
    private record Mesh(Matrix3x2fc pose,List<Quad> quads,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }
        public void buildVertices(VertexConsumer out) {
            for(Quad q:quads) for(int k=0;k<4;k++)
                out.addVertexWith2DPose(pose,q.xy()[k*2],q.xy()[k*2+1]).setUv(q.uv()[k*2],q.uv()[k*2+1]).setColor(q.color());
        }
    }
}
