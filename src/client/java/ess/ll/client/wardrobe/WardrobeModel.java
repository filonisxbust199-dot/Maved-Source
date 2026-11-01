package ess.ll.client.wardrobe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A Zenith wardrobe model (assets/ess-best/wardrobe/models): bones with pivots, parents, an anchor on a player
 * body part and an optional physics driver; cubes flattened into quads (in blocks, sorted by the body part they
 * hang on); animation clips; particle effects; and the pet, weapon and character skin data.
 */
public final class WardrobeModel {
    /** The body part a root bone hangs on; FREE (6) means the model's own space (pets, held items). */
    public enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }
    public static final int FREE=6;
    public enum Driver { NONE, CAPE, LEGS_FRONT, LEGS_BACK, SPRING, SWAY, CLOTH, CLOCK }
    public enum When { ALWAYS, MOVING, IDLE, AIRBORNE, SNEAKING, SET_BONUS }
    public enum PetKind { WALKER, HOPPER, FLYER, FLOATER, CRAWLER }
    /** Quad flags. */
    public static final int GLOW=1,TRANSLUCENT=2,DETAIL=4,GLASS=8,SPARKLE=16,ARM_CORE=32;
    /** Vanilla parts a model hides. */
    public static final int HIDE_HAT=1,HIDE_JACKET=2,HIDE_SLEEVES=4,HIDE_PANTS=8,HIDE_CAPE=16;

    public record Effect(ParticleOptions particle,int bone,float x,float y,float z,float rate,float vx,float vy,float vz,float spread,When when) {}
    public record Pet(PetKind kind,float scale,float speed,float hover,String quirk,ParticleOptions accent,List<String> behaviors) {
        public boolean has(String behavior) { return behaviors.contains(behavior); }
    }
    public record Weapon(float gripX,float gripY,float gripZ,float angle,float roll,float scale,ParticleOptions trail,ParticleOptions hit,int arrowBone) {}
    /** Character skins: an idle pose per body part (radians, 6 x 3, or null), whether armour hides, aura effects. */
    public record Skin(float[] idlePose,boolean hideArmor,List<Effect> aura) {}

    public final String id;
    public final Identifier texture;
    public final int texWidth,texHeight;
    public final String[] boneNames;
    public final int[] parent;
    public final float[] pivot,rotation;
    /** Anchor of root bones (-1 otherwise), and the body part group of every bone (anchor of its root, or FREE). */
    public final int[] anchor,group;
    public final Driver[] driver;
    public final float[] driverArgs;
    public final int quadCount;
    /** Per quad: 4 corners (12 floats, blocks), uv (8 floats), normal (3 floats), bone, flags. */
    public final float[] quadPos,quadUv,quadNormal;
    public final int[] quadBone,quadFlags;
    public final Map<String,Clip> clips;
    public final List<Effect> effects;
    public final Pet pet;
    public final Weapon weapon;
    public final Skin skin;
    /** Beyond this distance (blocks) the "detail" cubes are skipped. */
    public final float lod;
    /** Bounds of the rest pose: min x, y, z, max x, y, z (blocks). */
    public final float[] bounds;
    public final int hides;
    /** Effects and the skin aura together. */
    public final List<Effect> allEffects;
    private final boolean[] hasGroup=new boolean[7];
    private final int[] groupStart=new int[7],groupEnd=new int[7];
    private final int allFlags;

    private WardrobeModel(String id,Identifier texture,int texWidth,int texHeight,String[] boneNames,int[] parent,float[] pivot,float[] rotation,
                          int[] anchor,int[] group,Driver[] driver,float[] driverArgs,float[] quadPos,float[] quadUv,float[] quadNormal,
                          int[] quadBone,int[] quadFlags,Map<String,Clip> clips,List<Effect> effects,Pet pet,Weapon weapon,Skin skin,
                          float lod,float[] bounds,int hides) {
        this.id=id; this.texture=texture; this.texWidth=texWidth; this.texHeight=texHeight;
        this.boneNames=boneNames; this.parent=parent; this.pivot=pivot; this.rotation=rotation; this.anchor=anchor; this.group=group;
        this.driver=driver; this.driverArgs=driverArgs; this.quadPos=quadPos; this.quadUv=quadUv; this.quadNormal=quadNormal;
        this.quadBone=quadBone; this.quadFlags=quadFlags; this.quadCount=quadBone.length; this.clips=clips; this.effects=effects;
        this.pet=pet; this.weapon=weapon; this.skin=skin; this.lod=lod; this.bounds=bounds; this.hides=hides;
        int flags=0,last=-1;
        boolean sorted=true;
        Arrays.fill(groupStart,-1);
        for(int q=0;q<quadBone.length;q++) {
            int g=group[quadBone[q]];
            hasGroup[g]=true;
            flags|=quadFlags[q];
            if(g<last) sorted=false;
            last=g;
            if(groupStart[g]<0) groupStart[g]=q;
            groupEnd[g]=q+1;
        }
        for(int g=0;g<=6;g++) {
            if(!sorted) { groupStart[g]=0; groupEnd[g]=quadBone.length; }
            else if(groupStart[g]<0) { groupStart[g]=0; groupEnd[g]=0; }
        }
        allFlags=flags;
        List<Effect> aura=skin==null ? null : skin.aura();
        if(aura==null || aura.isEmpty()) allEffects=effects;
        else if(effects.isEmpty()) allEffects=aura;
        else { List<Effect> all=new ArrayList<>(effects); all.addAll(aura); allEffects=all; }
    }

    public int bones() { return boneNames.length; }
    public int bone(String name) {
        for(int i=0;i<boneNames.length;i++) if(boneNames[i].equals(name)) return i;
        return -1;
    }
    public Clip clip(String name) { return clips.get(name); }
    public boolean hasGroup(int group) { return group>=0 && group<hasGroup.length && hasGroup[group]; }
    public boolean hasFlag(int flag) { return (allFlags&flag)!=0; }
    public int groupStart(int group) { return group>=0 && group<groupStart.length ? groupStart[group] : 0; }
    public int groupEnd(int group) { return group>=0 && group<groupEnd.length ? groupEnd[group] : quadCount; }

    /** An animation clip: per bone, keyframed rotation (degrees), position (pixels) and scale. */
    public static final class Clip {
        public final String name;
        public final float length;
        public final boolean loop;
        private final Track[][] tracks;
        Clip(String name,float length,boolean loop,Map<Integer,Track[]> tracks,int bones) {
            this.name=name; this.length=length; this.loop=loop; this.tracks=new Track[bones][];
            tracks.forEach((bone,t)->{ if(bone>=0 && bone<bones) this.tracks[bone]=t; });
        }
        /** Time inside the clip: wrapped when looping, clamped otherwise. */
        public float wrap(float time) {
            if(length<=0) return 0;
            if(!loop) return Math.min(Math.max(time,0),length);
            float t=time%length;
            return t<0 ? t+length : t;
        }
        public boolean animates(int bone) { return bone>=0 && bone<tracks.length && tracks[bone]!=null; }
        /** Fills out with rotation xyz (degrees), position xyz, scale xyz at the time. */
        public void sample(int bone,float time,float[] out) {
            out[0]=out[1]=out[2]=0; out[3]=out[4]=out[5]=0; out[6]=out[7]=out[8]=1;
            if(!animates(bone)) return;
            Track[] t=tracks[bone];
            if(t[0]!=null) t[0].sample(time,out,0,loop);
            if(t[1]!=null) t[1].sample(time,out,3,loop);
            if(t[2]!=null) t[2].sample(time,out,6,loop);
        }
    }
    /** Keyframes of one channel: smooth (Catmull-Rom) unless the bone asked for linear. */
    record Track(float[] times,float[] values,boolean smooth) {
        void sample(float time,float[] out,int at,boolean loop) {
            int last=times.length-1;
            if(last<0) return;
            if(time<=times[0] || last==0) { copy(0,out,at); return; }
            if(time>=times[last]) { copy(last,out,at); return; }
            int i=0;
            while(i<last && times[i+1]<time) i++;
            float a=times[i],b=times[i+1],t=b-a<=1e-6f ? 0 : (time-a)/(b-a);
            if(smooth) {
                int before=i-1>=0 ? i-1 : loop && last>=2 ? last-1 : 0;
                int after=i+2<=last ? i+2 : loop && last>=2 ? Math.min(i+2-last,last) : last;
                for(int k=0;k<3;k++) out[at+k]=catmullRom(values[before*3+k],values[i*3+k],values[(i+1)*3+k],values[after*3+k],t);
            } else for(int k=0;k<3;k++) {
                float v=values[i*3+k];
                out[at+k]=v+(values[(i+1)*3+k]-v)*t;
            }
        }
        private void copy(int i,float[] out,int at) { out[at]=values[i*3]; out[at+1]=values[i*3+1]; out[at+2]=values[i*3+2]; }
        private static float catmullRom(float p0,float p1,float p2,float p3,float t) {
            float t2=t*t,t3=t2*t;
            return .5f*(2*p1+(p2-p0)*t+(2*p0-5*p1+4*p2-p3)*t2+(3*p1-p0-3*p2+p3)*t3);
        }
    }

    // Parsing.
    private static final float[][] ANCHOR_PIVOT={{0,0,0},{0,0,0},{-5,2,0},{5,2,0},{-1.9f,12,0},{1.9f,12,0}};
    private static final String[] FACES={"down","up","west","north","east","south"};
    private static final float[][] FACE_NORMAL={{0,-1,0},{0,1,0},{-1,0,0},{0,0,-1},{1,0,0},{0,0,1}};
    private static final int[][] FACE_CORNERS={{5,4,0,1},{2,3,7,6},{0,4,7,3},{1,0,3,2},{5,1,2,6},{4,5,6,7}};
    private static final float ARM_RADIUS=.14375f,ARM_SOFT=.0125f;
    private record Quad(int bone,int flags,float[] pos,float[] uv,float[] normal) {}

    public static WardrobeModel parse(String id,JsonObject json,Identifier texture) {
        JsonObject tex=object(json,"texture");
        float[] size=floats(tex==null ? null : tex.get("size"),2,64);
        int texW=Math.max(1,(int)size[0]),texH=Math.max(1,(int)size[1]);
        List<JsonObject> bones=ordered(array(json,"bones"));
        int n=Math.max(1,bones.size());
        String[] names=new String[n];
        int[] parent=new int[n],anchor=new int[n],group=new int[n];
        float[] pivot=new float[n*3],rotation=new float[n*3],args=new float[n*4];
        Driver[] driver=new Driver[n];
        Map<String,Integer> index=new HashMap<>();
        if(bones.isEmpty()) { names[0]="root"; parent[0]=-1; anchor[0]=-1; group[0]=FREE; driver[0]=Driver.NONE; }
        for(int b=0;b<bones.size();b++) {
            JsonObject bone=bones.get(b);
            String name=string(bone,"name","bone"+b);
            names[b]=name;
            Integer p=index.get(string(bone,"parent",""));
            parent[b]=p==null ? -1 : p;
            float[] pv=floats(bone.get("pivot"),3,0),rot=floats(bone.get("rotation"),3,0);
            for(int k=0;k<3;k++) { pivot[b*3+k]=pv[k]; rotation[b*3+k]=(float)Math.toRadians(rot[k]); }
            anchor[b]=parent[b]<0 ? anchor(string(bone,"anchor",null)) : -1;
            group[b]=parent[b]>=0 ? group[parent[b]] : anchor[b]>=0 ? anchor[b] : FREE;
            JsonObject d=object(bone,"driver");
            driver[b]=d==null ? Driver.NONE : driver(string(d,"type","none"));
            System.arraycopy(floats(d==null ? null : d.get("args"),4,0),0,args,b*4,4);
            index.put(name,b);
        }
        List<Quad> quads=new ArrayList<>();
        for(int b=0;b<bones.size();b++) {
            float armCore=Float.NaN;
            if(parent[b]<0 && anchor[b]==Anchor.RIGHT_ARM.ordinal()) armCore=-1;
            else if(parent[b]<0 && anchor[b]==Anchor.LEFT_ARM.ordinal()) armCore=1;
            for(JsonElement cube:array(bones.get(b),"cubes")) if(cube.isJsonObject()) cube(quads,b,cube.getAsJsonObject(),texW,texH,armCore);
        }
        quads.sort((a,b)->{
            int c=Integer.compare(group[a.bone],group[b.bone]);
            return c!=0 ? c : Integer.compare(a.bone,b.bone);
        });
        int q=quads.size();
        float[] pos=new float[q*12],uv=new float[q*8],normal=new float[q*3];
        int[] quadBone=new int[q],quadFlags=new int[q];
        for(int i=0;i<q;i++) {
            Quad quad=quads.get(i);
            System.arraycopy(quad.pos,0,pos,i*12,12);
            System.arraycopy(quad.uv,0,uv,i*8,8);
            System.arraycopy(quad.normal,0,normal,i*3,3);
            quadBone[i]=quad.bone; quadFlags[i]=quad.flags;
        }
        if(id!=null && (id.startsWith("clothing/") || id.startsWith("characters/") || id.startsWith("politicians/"))) hugArms(pos,quadBone,parent,anchor);
        Map<String,Clip> clips=clips(object(json,"clips"),index,n);
        List<Effect> effects=effects(array(json,"effects"),index);
        int hides=0;
        for(JsonElement e:array(json,"hides")) hides|=switch (e.getAsString().toLowerCase(Locale.ROOT)) {
            case "hat" -> HIDE_HAT;
            case "jacket" -> HIDE_JACKET;
            case "sleeves" -> HIDE_SLEEVES;
            case "pants" -> HIDE_PANTS;
            case "cape" -> HIDE_CAPE;
            default -> 0;
        };
        return new WardrobeModel(id,texture,texW,texH,names,parent,pivot,rotation,anchor,group,driver,args,pos,uv,normal,quadBone,quadFlags,
                clips,effects,pet(object(json,"pet")),weapon(object(json,"weapon"),index),skin(object(json,"skin"),index),
                number(json,"lod",24),bounds(pos,quadBone,parent,pivot,rotation,anchor),hides);
    }

    /** Clothing on the arms is pulled in towards the arm so it fits slim arms as well as wide ones. */
    private static void hugArms(float[] pos,int[] quadBone,int[] parent,int[] anchor) {
        int right=Anchor.RIGHT_ARM.ordinal(),left=Anchor.LEFT_ARM.ordinal();
        for(int q=0;q<quadBone.length;q++) {
            int b=quadBone[q];
            if(parent[b]>=0 || anchor[b]!=right && anchor[b]!=left) continue;
            float centre=anchor[b]==right ? -.0625f : .0625f;
            for(int k=0;k<4;k++) {
                int i=q*12+k*3;
                pos[i]=centre+soften(pos[i]-centre);
                pos[i+2]=soften(pos[i+2]);
            }
        }
    }
    private static float soften(float v) {
        float a=Math.abs(v);
        if(a<=ARM_RADIUS) return v;
        float over=a-ARM_RADIUS;
        return Math.copySign(ARM_RADIUS+ARM_SOFT*over/(over+ARM_SOFT),v);
    }

    private static void cube(List<Quad> out,int bone,JsonObject cube,int texW,int texH,float armCore) {
        float[] from=floats(cube.get("from"),3,0),size=floats(cube.get("size"),3,0);
        float inflate=number(cube,"inflate",0);
        boolean mirror=bool(cube,"mirror");
        float[] rot=floats(cube.get("rotation"),3,0);
        boolean unrotated=rot[0]==0 && rot[1]==0 && rot[2]==0;
        boolean core=!Float.isNaN(armCore) && unrotated && from[0]-inflate<armCore && from[0]+size[0]+inflate>armCore
                && from[2]-inflate<0 && from[2]+size[2]+inflate>0;
        int flags=(core ? ARM_CORE : 0)|(bool(cube,"glow") ? GLOW : 0)|(bool(cube,"translucent") ? TRANSLUCENT : 0)
                |(bool(cube,"detail") ? DETAIL : 0)|(bool(cube,"glass") ? GLASS|TRANSLUCENT : 0)
                |(cube.has("sparkle") && cube.get("sparkle").isJsonPrimitive() && bool(cube,"sparkle") ? SPARKLE : 0);
        Set<String> sparkleFaces=new HashSet<>();
        if(cube.has("sparkle") && cube.get("sparkle").isJsonArray()) for(JsonElement e:cube.getAsJsonArray("sparkle")) sparkleFaces.add(e.getAsString());
        float x0=from[0]-inflate,y0=from[1]-inflate,z0=from[2]-inflate,x1=from[0]+size[0]+inflate,y1=from[1]+size[1]+inflate,z1=from[2]+size[2]+inflate;
        if(mirror) { float t=x1; x1=x0; x0=t; }
        float[][] c={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        Matrix3f turn=null;
        if(!unrotated) {
            float[] origin=floats(cube.get("origin"),3,0);
            turn=new Matrix3f().rotationZYX((float)Math.toRadians(rot[2]),(float)Math.toRadians(rot[1]),(float)Math.toRadians(rot[0]));
            Vector3f v=new Vector3f();
            for(float[] p:c) {
                v.set(p[0]-origin[0],p[1]-origin[1],p[2]-origin[2]);
                turn.transform(v);
                p[0]=v.x+origin[0]; p[1]=v.y+origin[1]; p[2]=v.z+origin[2];
            }
        }
        float[][] faces=faceUvs(cube,size);
        for(int f=0;f<6;f++) {
            float[] uv=faces[f];
            if(uv==null) continue;
            int[] corners=FACE_CORNERS[f];
            float[] pos=new float[12];
            for(int k=0;k<4;k++) {
                float[] p=c[corners[mirror ? 3-k : k]];
                pos[k*3]=p[0]/16; pos[k*3+1]=p[1]/16; pos[k*3+2]=p[2]/16;
            }
            float u0=uv[0]/texW,v0=uv[1]/texH,u1=uv[2]/texW,v1=uv[3]/texH;
            float[] uvs=mirror ? new float[]{u1,v1,u0,v1,u0,v0,u1,v0} : new float[]{u1,v0,u0,v0,u0,v1,u1,v1};
            Vector3f n=new Vector3f(FACE_NORMAL[f][0]*(mirror ? -1 : 1),FACE_NORMAL[f][1],FACE_NORMAL[f][2]);
            if(turn!=null) turn.transform(n);
            out.add(new Quad(bone,sparkleFaces.contains(FACES[f]) ? flags|SPARKLE : flags,pos,uvs,new float[]{n.x,n.y,n.z}));
        }
    }
    /** Per-face uv rectangles [u0, v0, u1, v1]; from "faces", or the classic box layout from "uv". */
    private static float[][] faceUvs(JsonObject cube,float[] size) {
        float[][] out=new float[6][];
        JsonElement faces=cube.get("faces");
        if(faces!=null && faces.isJsonObject()) {
            JsonObject o=faces.getAsJsonObject();
            for(int f=0;f<6;f++) {
                JsonElement e=o.get(FACES[f]);
                if(e!=null && e.isJsonArray() && e.getAsJsonArray().size()>=4) out[f]=floats(e,4,0);
            }
            return out;
        }
        JsonElement uv=cube.get("uv");
        if(uv==null || !uv.isJsonArray()) return out;
        float[] o=floats(uv,2,0);
        float u=o[0],v=o[1],w=size[0],h=size[1],d=size[2];
        out[0]=new float[]{u+d,v,u+d+w,v+d};
        out[1]=new float[]{u+d+w,v+d,u+d+w+w,v};
        out[2]=new float[]{u,v+d,u+d,v+d+h};
        out[3]=new float[]{u+d,v+d,u+d+w,v+d+h};
        out[4]=new float[]{u+d+w,v+d,u+d+w+d,v+d+h};
        out[5]=new float[]{u+d+w+d,v+d,u+d+w+d+w,v+d+h};
        return out;
    }
    private static float[] bounds(float[] pos,int[] quadBone,int[] parent,float[] pivot,float[] rotation,int[] anchor) {
        int n=parent.length;
        Matrix4f[] m=new Matrix4f[n];
        for(int b=0;b<n;b++) {
            Matrix4f mat=parent[b]>=0 ? new Matrix4f(m[parent[b]]) : new Matrix4f();
            if(parent[b]<0 && anchor[b]>=0) { float[] p=ANCHOR_PIVOT[anchor[b]]; mat.translate(p[0]/16,p[1]/16,p[2]/16); }
            else mat.translate(pivot[b*3]/16,pivot[b*3+1]/16,pivot[b*3+2]/16);
            mat.rotateZYX(rotation[b*3+2],rotation[b*3+1],rotation[b*3]);
            m[b]=mat;
        }
        if(quadBone.length==0) return new float[6];
        float[] out={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        Vector3f v=new Vector3f();
        for(int q=0;q<quadBone.length;q++) for(int k=0;k<4;k++) {
            int i=q*12+k*3;
            m[quadBone[q]].transformPosition(pos[i],pos[i+1],pos[i+2],v);
            out[0]=Math.min(out[0],v.x); out[1]=Math.min(out[1],v.y); out[2]=Math.min(out[2],v.z);
            out[3]=Math.max(out[3],v.x); out[4]=Math.max(out[4],v.y); out[5]=Math.max(out[5],v.z);
        }
        return out;
    }
    private static Map<String,Clip> clips(JsonObject json,Map<String,Integer> index,int bones) {
        if(json==null) return Collections.emptyMap();
        Map<String,Clip> out=new LinkedHashMap<>();
        for(var entry:json.entrySet()) {
            if(!entry.getValue().isJsonObject()) continue;
            JsonObject clip=entry.getValue().getAsJsonObject();
            Map<Integer,Track[]> tracks=new HashMap<>();
            JsonObject bonesJson=object(clip,"bones");
            float end=0;
            if(bonesJson!=null) for(var b:bonesJson.entrySet()) {
                Integer bone=index.get(b.getKey());
                if(bone==null || !b.getValue().isJsonObject()) continue;
                JsonObject o=b.getValue().getAsJsonObject();
                boolean smooth=!bool(o,"linear");
                Track[] t={track(o.get("rotation"),smooth,0),track(o.get("position"),smooth,0),track(o.get("scale"),smooth,1)};
                for(Track tr:t) if(tr!=null) end=Math.max(end,tr.times[tr.times.length-1]);
                tracks.put(bone,t);
            }
            float length=number(clip,"length",end);
            boolean loop=!clip.has("loop") || bool(clip,"loop");
            out.put(entry.getKey(),new Clip(entry.getKey(),length<=0 ? end : length,loop,tracks,bones));
        }
        return out;
    }
    private static Track track(JsonElement json,boolean smooth,float fallback) {
        if(json==null || !json.isJsonArray()) return null;
        List<float[]> keys=new ArrayList<>();
        for(JsonElement e:json.getAsJsonArray()) {
            if(!e.isJsonArray()) continue;
            JsonArray a=e.getAsJsonArray();
            float t=a.size()>0 ? a.get(0).getAsFloat() : 0,x=a.size()>1 ? a.get(1).getAsFloat() : fallback;
            float y=a.size()>2 ? a.get(2).getAsFloat() : x,z=a.size()>3 ? a.get(3).getAsFloat() : y;
            keys.add(new float[]{t,x,y,z});
        }
        if(keys.isEmpty()) return null;
        keys.sort((a,b)->Float.compare(a[0],b[0]));
        float[] times=new float[keys.size()],values=new float[keys.size()*3];
        for(int i=0;i<keys.size();i++) { float[] k=keys.get(i); times[i]=k[0]; values[i*3]=k[1]; values[i*3+1]=k[2]; values[i*3+2]=k[3]; }
        return new Track(times,values,smooth);
    }
    private static List<Effect> effects(JsonArray json,Map<String,Integer> index) {
        List<Effect> out=new ArrayList<>();
        for(JsonElement e:json) if(e.isJsonObject()) {
            Effect effect=effect(e.getAsJsonObject(),index);
            if(effect!=null) out.add(effect);
        }
        return out;
    }
    private static Effect effect(JsonObject json,Map<String,Integer> index) {
        ParticleOptions particle=particle(string(json,"particle",null));
        if(particle==null) return null;
        Integer bone=index.get(string(json,"bone",""));
        float[] offset=floats(json.get("offset"),3,0),velocity=floats(json.get("velocity"),3,0);
        When when;
        try { when=When.valueOf(string(json,"when","always").toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException e) { when=When.ALWAYS; }
        return new Effect(particle,bone==null ? -1 : bone,offset[0],offset[1],offset[2],number(json,"rate",1),velocity[0],velocity[1],velocity[2],
                number(json,"spread",.05f),when);
    }
    private static Pet pet(JsonObject json) {
        if(json==null) return null;
        PetKind kind;
        try { kind=PetKind.valueOf(string(json,"kind","walker").toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException e) { kind=PetKind.WALKER; }
        List<String> behaviors=new ArrayList<>();
        for(JsonElement e:array(json,"behaviors")) behaviors.add(e.getAsString().toLowerCase(Locale.ROOT));
        return new Pet(kind,number(json,"scale",1),number(json,"speed",1),number(json,"hover",0),string(json,"quirk",""),
                particle(string(json,"accent",null)),List.copyOf(behaviors));
    }
    private static Weapon weapon(JsonObject json,Map<String,Integer> index) {
        if(json==null) return null;
        float[] grip=floats(json.get("grip"),3,0);
        Integer arrow=index.get(string(json,"arrowBone",""));
        return new Weapon(grip[0],grip[1],grip[2],number(json,"angle",0),number(json,"roll",0),number(json,"scale",1),
                particle(string(json,"trail",null)),particle(string(json,"hit",null)),arrow==null ? -1 : arrow);
    }
    private static Skin skin(JsonObject json,Map<String,Integer> index) {
        if(json==null) return null;
        float[] pose=new float[18];
        boolean any=false;
        JsonObject idle=object(json,"idlePose");
        if(idle!=null) {
            String[] parts={"head","body","right_arm","left_arm","right_leg","left_leg"};
            for(int p=0;p<parts.length;p++) if(idle.has(parts[p])) {
                float[] v=floats(idle.get(parts[p]),3,0);
                for(int k=0;k<3;k++) pose[p*3+k]=(float)Math.toRadians(v[k]);
                any=true;
            }
        }
        return new Skin(any ? pose : null,!json.has("hideArmor") || bool(json,"hideArmor"),effects(array(json,"aura"),index));
    }
    /** "dust:#rrggbb:size" or a particle type id like "flame" / "minecraft:end_rod". */
    public static ParticleOptions particle(String name) {
        if(name==null || name.isBlank()) return null;
        String n=name.strip().toLowerCase(Locale.ROOT);
        if(n.startsWith("dust:")) {
            String[] parts=n.split(":");
            try { return new DustParticleOptions(Integer.parseInt(parts[1].replace("#",""),16),parts.length>2 ? Float.parseFloat(parts[2]) : 1); }
            catch(RuntimeException e) { return null; }
        }
        Identifier id=Identifier.tryParse(n.contains(":") ? n : "minecraft:"+n);
        if(id==null) return null;
        var type=BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        return type instanceof SimpleParticleType simple ? simple : null;
    }
    private static int anchor(String name) {
        if(name==null) return -1;
        try { return Anchor.valueOf(name.strip().toUpperCase(Locale.ROOT)).ordinal(); }
        catch(IllegalArgumentException e) { return -1; }
    }
    private static Driver driver(String name) {
        try { return Driver.valueOf(name.strip().toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException e) { return Driver.NONE; }
    }
    /** Bones in an order where every parent comes before its children. */
    private static List<JsonObject> ordered(JsonArray bones) {
        List<JsonObject> left=new ArrayList<>();
        for(JsonElement e:bones) if(e.isJsonObject()) left.add(e.getAsJsonObject());
        List<JsonObject> out=new ArrayList<>(left.size());
        Set<String> placed=new HashSet<>();
        boolean progress=true;
        while(!left.isEmpty() && progress) {
            progress=false;
            for(int i=0;i<left.size();) {
                JsonObject b=left.get(i);
                String p=string(b,"parent",null);
                if(p!=null && !placed.contains(p)) { i++; continue; }
                out.add(b); placed.add(string(b,"name","")); left.remove(i); progress=true;
            }
        }
        out.addAll(left);
        return out;
    }
    static JsonObject object(JsonObject o,String key) { return o!=null && o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : null; }
    static JsonArray array(JsonObject o,String key) { return o!=null && o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray(); }
    static String string(JsonObject o,String key,String fallback) {
        try { return o!=null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback; }
        catch(RuntimeException e) { return fallback; }
    }
    static float number(JsonObject o,String key,float fallback) {
        try { return o!=null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsFloat() : fallback; }
        catch(RuntimeException e) { return fallback; }
    }
    static boolean bool(JsonObject o,String key) {
        try { return o!=null && o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsBoolean(); }
        catch(RuntimeException e) { return false; }
    }
    static float[] floats(JsonElement e,int n,float fallback) {
        float[] out=new float[n];
        Arrays.fill(out,fallback);
        if(e==null || !e.isJsonArray()) return out;
        JsonArray a=e.getAsJsonArray();
        for(int i=0;i<n && i<a.size();i++) try { out[i]=a.get(i).getAsFloat(); } catch(RuntimeException ignored) {}
        return out;
    }
}
