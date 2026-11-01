package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws wardrobe models (from Zenith's BoneMatrices): bone matrices from a pose, then the quads of one body part
 * group in up to four passes — solid, translucent, glowing (full bright) and shine (glass highlights and sparkles
 * through the energy swirl type).
 */
public final class WardrobeRenderer {
    private enum Pass { SOLID, TRANSLUCENT, GLOW, SHINE }
    public static final int FULL_BRIGHT=0xF000F0;
    private static Matrix4f[] bone=new Matrix4f[0];
    private static Matrix3f[] boneNormal=new Matrix3f[0];
    private static final Quaternionf ROT=new Quaternionf();
    private static final Vector3f V=new Vector3f(),N=new Vector3f();
    private static final Vector3f KEY_LIGHT=new Vector3f(-.35f,.78f,.52f).normalize(),FILL_LIGHT=new Vector3f(.55f,.35f,.76f).normalize();
    private static final Vector3f[] CORNER={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};
    private static final float[] CORNER_UV=new float[8];
    private static final Vector3f FACE=new Vector3f(),TILT=new Vector3f(),VIEW=new Vector3f(),HALF=new Vector3f(),P=new Vector3f();
    private static final Map<Identifier,RenderType> SWIRL=new HashMap<>();
    private static final float[] GX=new float[25],GY=new float[25],GZ=new float[25],GU=new float[25],GV=new float[25];
    private static final int[] GC=new int[25];

    private WardrobeRenderer() {}

    /**
     * Draws a model group on top of the pose stack. pose may be null (rest pose); detail cubes only when near;
     * glow only when the item's rarity glows; tint is ARGB (-1 none); rootsOnly skips child bones.
     */
    public static void draw(WardrobeModel model,float[] pose,int group,PoseStack stack,SubmitNodeCollector collector,int light,int overlay,
                            boolean detail,boolean glow,int tint,boolean rootsOnly) {
        if(model==null || model.quadCount==0 || collector==null || model.texture==null || !model.hasGroup(group)) return;
        float[] p=pose==null ? null : pose.clone();
        submit(model,p,group,stack,collector,light,overlay,detail,Pass.SOLID,tint,rootsOnly);
        if(model.hasFlag(WardrobeModel.TRANSLUCENT)) submit(model,p,group,stack,collector,light,overlay,detail,Pass.TRANSLUCENT,tint,rootsOnly);
        if(glow && model.hasFlag(WardrobeModel.GLOW)) submit(model,p,group,stack,collector,light,overlay,detail,Pass.GLOW,tint,rootsOnly);
        if(model.hasFlag(WardrobeModel.GLASS) || model.hasFlag(WardrobeModel.SPARKLE)) submit(model,p,group,stack,collector,light,overlay,detail,Pass.SHINE,tint,rootsOnly);
    }
    private static void submit(WardrobeModel model,float[] pose,int group,PoseStack stack,SubmitNodeCollector collector,int light,int overlay,
                               boolean detail,Pass pass,int tint,boolean rootsOnly) {
        var queue=switch (pass) { case TRANSLUCENT -> collector.order(1); case SHINE -> collector.order(2); default -> collector; };
        queue.submitCustomGeometry(stack,type(model,pass,tint),(entry,vc)->emit(model,pose,group,entry.pose(),entry.normal(),vc,light,overlay,detail,pass,tint,rootsOnly));
    }
    private static RenderType type(WardrobeModel model,Pass pass,int tint) {
        return switch (pass) {
            case SOLID -> tint>>>24<255 ? RenderTypes.entityTranslucent(model.texture) : RenderTypes.entityCutout(model.texture);
            case TRANSLUCENT -> RenderTypes.entityTranslucent(model.texture);
            case GLOW -> RenderTypes.eyes(model.texture);
            case SHINE -> SWIRL.computeIfAbsent(model.texture,t->RenderTypes.energySwirl(t,0,0));
        };
    }

    private static void emit(WardrobeModel model,float[] pose,int group,Matrix4f base,Matrix3f baseNormal,VertexConsumer vc,int light,int overlay,
                             boolean detail,Pass pass,int tint,boolean rootsOnly) {
        int bones=model.bones();
        ensure(bones);
        for(int b=0;b<bones;b++) {
            if(model.group[b]!=group) continue;
            Matrix4f m=bone[b];
            Matrix3f n=boneNormal[b];
            int parent=model.parent[b];
            if(parent>=0) { m.set(bone[parent]); n.set(boneNormal[parent]); }
            else { m.set(base); n.set(baseNormal); }
            int i=b*9;
            float rx=pose==null ? model.rotation[b*3] : pose[i],ry=pose==null ? model.rotation[b*3+1] : pose[i+1],rz=pose==null ? model.rotation[b*3+2] : pose[i+2];
            float px=pose==null ? 0 : pose[i+3],py=pose==null ? 0 : pose[i+4],pz=pose==null ? 0 : pose[i+5];
            float sx=pose==null ? 1 : pose[i+6],sy=pose==null ? 1 : pose[i+7],sz=pose==null ? 1 : pose[i+8];
            // An anchored root sits on the body part's own origin; the part's transform is already on the stack.
            boolean anchoredRoot=parent<0 && model.anchor[b]>=0;
            float ox=anchoredRoot ? 0 : model.pivot[b*3],oy=anchoredRoot ? 0 : model.pivot[b*3+1],oz=anchoredRoot ? 0 : model.pivot[b*3+2];
            m.translate((ox+px)/16,(oy+py)/16,(oz+pz)/16);
            if(rx!=0 || ry!=0 || rz!=0) { ROT.rotationZYX(rz,ry,rx); m.rotate(ROT); n.rotate(ROT); }
            if(sx!=1 || sy!=1 || sz!=1) m.scale(sx,sy,sz);
        }
        for(int q=model.groupStart(group),end=model.groupEnd(group);q<end;q++) {
            int b=model.quadBone[q];
            if(model.group[b]!=group || rootsOnly && model.parent[b]>=0) continue;
            int flags=model.quadFlags[q];
            if(!detail && (flags&WardrobeModel.DETAIL)!=0) continue;
            boolean translucent=(flags&WardrobeModel.TRANSLUCENT)!=0,glowing=(flags&WardrobeModel.GLOW)!=0;
            switch (pass) {
                case SOLID -> { if(translucent) continue; }
                case TRANSLUCENT -> { if(!translucent) continue; }
                case GLOW -> { if(!glowing) continue; }
                case SHINE -> { if((flags&(WardrobeModel.GLASS|WardrobeModel.SPARKLE))==0) continue; }
            }
            // A bone scaled to zero (hidden by its clip) draws nothing.
            if(pose!=null && pose[b*9+6]==0 && pose[b*9+7]==0) continue;
            Matrix4f m=bone[b];
            FACE.set(model.quadNormal[q*3],model.quadNormal[q*3+1],model.quadNormal[q*3+2]);
            boneNormal[b].transform(FACE);
            if(FACE.lengthSquared()>1e-8f) FACE.normalize(); else FACE.set(0,1,0);
            int faceLight=!glowing && pass!=Pass.GLOW ? light : FULL_BRIGHT;
            int color=pass==Pass.GLOW ? -1 : tint;
            boolean glass=(flags&WardrobeModel.GLASS)!=0;
            if(pass==Pass.SHINE || pass==Pass.TRANSLUCENT && glass) {
                for(int k=0;k<4;k++) {
                    int at=q*12+k*3;
                    CORNER[k].set(model.quadPos[at],model.quadPos[at+1],model.quadPos[at+2]);
                    m.transformPosition(CORNER[k]);
                    CORNER_UV[k*2]=model.quadUv[q*8+k*2];
                    CORNER_UV[k*2+1]=model.quadUv[q*8+k*2+1];
                }
                if(pass==Pass.SHINE && !glass) sparkle(vc,q,tint,overlay,light);
                else glass(vc,pass==Pass.SHINE,overlay,faceLight,tint);
            } else for(int k=0;k<4;k++) {
                int at=q*12+k*3;
                V.set(model.quadPos[at],model.quadPos[at+1],model.quadPos[at+2]);
                m.transformPosition(V);
                vc.addVertex(V.x,V.y,V.z,color,model.quadUv[q*8+k*2],model.quadUv[q*8+k*2+1],overlay,faceLight,FACE.x,FACE.y,FACE.z);
            }
        }
    }

    /** Bone matrix of a bone in model space (the rest pose when pose is null), for attaching things to it. */
    public static void boneMatrix(WardrobeModel model,float[] pose,int b,Matrix4f out) {
        int[] chain=new int[model.bones()];
        int len=0;
        for(int i=b;i>=0 && len<chain.length;i=model.parent[i]) chain[len++]=i;
        for(int c=len-1;c>=0;c--) {
            int i=chain[c],at=i*9;
            boolean anchoredRoot=model.parent[i]<0 && model.anchor[i]>=0;
            float ox=anchoredRoot ? 0 : model.pivot[i*3],oy=anchoredRoot ? 0 : model.pivot[i*3+1],oz=anchoredRoot ? 0 : model.pivot[i*3+2];
            out.translate((ox+(pose==null ? 0 : pose[at+3]))/16,(oy+(pose==null ? 0 : pose[at+4]))/16,(oz+(pose==null ? 0 : pose[at+5]))/16);
            out.rotateZYX(pose==null ? model.rotation[i*3+2] : pose[at+2],pose==null ? model.rotation[i*3+1] : pose[at+1],pose==null ? model.rotation[i*3] : pose[at]);
            if(pose!=null) out.scale(pose[at+6],pose[at+7],pose[at+8]);
        }
    }

    /** Light level 0..1 of a packed light value, darkened at night outdoors. */
    private static float brightness(int light) {
        if(light==FULL_BRIGHT) return 1;
        float block=(light>>4&15)/15f,sky=(light>>20&15)/15f;
        var level=Minecraft.getInstance().level;
        if(level!=null) sky*=1-level.getSkyDarken()/11f*.75f;
        return Math.max(.15f,Math.min(1,Math.max(block,sky)));
    }
    private static float pow(float base,int exp) {
        float r=1,b=base;
        while(exp>0) { if((exp&1)!=0) r*=b; b*=b; exp>>=1; }
        return r;
    }
    private static float specular(Vector3f normal,Vector3f light,Vector3f view,float power) {
        HALF.set(light).add(view);
        if(HALF.lengthSquared()<1e-8f) return 0;
        HALF.normalize();
        return pow(Math.max(0,normal.dot(HALF)),(int)power);
    }
    private static void viewOf(Vector3f point,Vector3f out) {
        out.set(point).negate();
        if(out.lengthSquared()>1e-8f) out.normalize(); else out.set(0,0,1);
    }
    private static void lerp(float u,float v,Vector3f out) {
        float ax=CORNER[0].x+(CORNER[1].x-CORNER[0].x)*u,ay=CORNER[0].y+(CORNER[1].y-CORNER[0].y)*u,az=CORNER[0].z+(CORNER[1].z-CORNER[0].z)*u;
        float bx=CORNER[3].x+(CORNER[2].x-CORNER[3].x)*u,by=CORNER[3].y+(CORNER[2].y-CORNER[3].y)*u,bz=CORNER[3].z+(CORNER[2].z-CORNER[3].z)*u;
        out.set(ax+(bx-ax)*v,ay+(by-ay)*v,az+(bz-az)*v);
    }
    private static float uv(int k,float u,float v) {
        float a=CORNER_UV[k]+(CORNER_UV[2+k]-CORNER_UV[k])*u,b=CORNER_UV[6+k]+(CORNER_UV[4+k]-CORNER_UV[6+k])*u;
        return a+(b-a)*v;
    }
    /**
     * Glass: the face split into a small grid so the highlight follows the view. As the translucent pass it is a
     * faint fresnel tint; as the shine pass, specular highlights of two lights in the swirl type.
     */
    private static void glass(VertexConsumer vc,boolean shine,int overlay,int light,int tint) {
        float size=Math.max(CORNER[0].distance(CORNER[1]),CORNER[1].distance(CORNER[2]));
        int cells=Math.max(1,Math.min(4,(int)Math.ceil(size/.06f))),row=cells+1;
        float alpha=(tint>>>24&255)/255f,bright=brightness(light);
        for(int y=0;y<=cells;y++) for(int x=0;x<=cells;x++) {
            float u=(float)x/cells,v=(float)y/cells;
            lerp(u,v,P);
            viewOf(P,VIEW);
            float fresnel=pow(1-Math.abs(FACE.dot(VIEW)),5);
            int i=y*row+x;
            GX[i]=P.x; GY[i]=P.y; GZ[i]=P.z; GU[i]=uv(0,u,v); GV[i]=uv(1,u,v);
            if(shine) {
                HALF.set(KEY_LIGHT).add(VIEW);
                float key=0;
                if(HALF.lengthSquared()>=1e-8f) { HALF.normalize(); float d=Math.max(0,FACE.dot(HALF)); key=pow(d,60)*.85f+pow(d,10)*.12f; }
                float fill=specular(FACE,FILL_LIGHT,VIEW,40)*.35f;
                int c=(int)(Math.min(1,(key+fill+fresnel*.15f)*alpha*bright)*255);
                GC[i]=0xff000000|c<<16|c<<8|c;
            } else GC[i]=(int)(Math.min(1,.05f+fresnel*.2f)*alpha*255)<<24|0xffffff;
        }
        int faceLight=shine ? FULL_BRIGHT : light;
        for(int y=0;y<cells;y++) for(int x=0;x<cells;x++) for(int k=0;k<4;k++) {
            int i=(y+(k>=2 ? 1 : 0))*row+x+(k==1 || k==2 ? 1 : 0);
            vc.addVertex(GX[i],GY[i],GZ[i],GC[i],GU[i],GV[i],overlay,faceLight,FACE.x,FACE.y,FACE.z);
        }
    }
    /** Sparkle: each face catches the light at its own random tilt, so jewels glint as they turn. */
    private static void sparkle(VertexConsumer vc,int quad,int tint,int overlay,int light) {
        int h=quad*-1640531535;
        h^=h>>>15; h*=-2048144777; h^=h>>>13;
        TILT.set(FACE).add(((h&255)/255f-.5f)*.9f,((h>>>8&255)/255f-.5f)*.9f,((h>>>16&255)/255f-.5f)*.9f);
        if(TILT.lengthSquared()<1e-8f) return;
        TILT.normalize();
        P.set(CORNER[0]).add(CORNER[1]).add(CORNER[2]).add(CORNER[3]).mul(.25f);
        viewOf(P,VIEW);
        float glint=specular(TILT,KEY_LIGHT,VIEW,16)*.9f+specular(TILT,FILL_LIGHT,VIEW,16)*.45f;
        float value=Math.min(1,glint*((tint>>>24&255)/255f)*brightness(light));
        if(value<.02f) return;
        int c=(int)(value*255),color=0xff000000|c<<16|c<<8|c;
        for(int k=0;k<4;k++) {
            Vector3f corner=CORNER[k];
            vc.addVertex(P.x+(corner.x-P.x)*1.12f+FACE.x*.0012f,P.y+(corner.y-P.y)*1.12f+FACE.y*.0012f,P.z+(corner.z-P.z)*1.12f+FACE.z*.0012f,
                    color,CORNER_UV[k*2],CORNER_UV[k*2+1],overlay,FULL_BRIGHT,FACE.x,FACE.y,FACE.z);
        }
    }
    private static void ensure(int n) {
        if(bone.length>=n) return;
        Matrix4f[] m=new Matrix4f[n];
        Matrix3f[] nm=new Matrix3f[n];
        System.arraycopy(bone,0,m,0,bone.length);
        System.arraycopy(boneNormal,0,nm,0,boneNormal.length);
        for(int i=bone.length;i<n;i++) { m[i]=new Matrix4f(); nm[i]=new Matrix3f(); }
        bone=m; boneNormal=nm;
    }
}
