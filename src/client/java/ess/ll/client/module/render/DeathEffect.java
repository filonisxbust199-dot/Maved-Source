package ess.ll.client.module.render;

import com.mojang.blaze3d.platform.NativeImage;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.io.InputStream;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * DeathEffect: when a living entity dies its body is hidden and replaced by either
 * - "Кубики": the model shatters into small cubes that fly apart, bounce on the ground and shrink away.
 *   Players are voxelised from their actual skin (every cube face shows the skin pixels it came from);
 *   other mobs fill their hitbox with cubes coloured from their texture.
 * - "Светлячки": the body dissolves into glowing fireflies that drift upwards.
 */
public final class DeathEffect {
    public static final ModeSettings MODE=new ModeSettings("Эффект","Кубики","Светлячки");
    public static final SliderSetting FORCE=new SliderSetting("Разлёт",.2f,3,.1f,1).visibleWhen(()->MODE.is("Кубики"));
    public static final SliderSetting CUBE=new SliderSetting("Размер кубиков",1,4,.5f,1.5f).visibleWhen(()->MODE.is("Кубики"));
    public static final SliderSetting CUBES=new SliderSetting("Кол-во кубиков",50,2000,50,700).visibleWhen(()->MODE.is("Кубики"));
    public static final BooleanSetting GRAVITY=new BooleanSetting("Гравитация",true).visibleWhen(()->MODE.is("Кубики"));
    public static final SliderSetting COUNT=new SliderSetting("Количество",10,200,5,70).visibleWhen(()->MODE.is("Светлячки"));
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Тема","Белый","Радуга").visibleWhen(()->MODE.is("Светлячки"));
    public static final SliderSetting LIFE=new SliderSetting("Время",1,6,.5f,3);
    public static final MultiBooleanSettings TARGETS=new MultiBooleanSettings("Для","Игроки","Мобы","Себя");
    public static final Setting[] SETTINGS={MODE,FORCE,CUBE,CUBES,GRAVITY,COUNT,COLOR,LIFE,TARGETS};
    private static final Identifier WHITE=Identifier.fromNamespaceAndPath("ess-best","textures/cosmetic/white.png");
    private static final Identifier GLOW=Identifier.fromNamespaceAndPath("ess-best","textures/effect/glow.png");
    private static final int MAX_CUBES=4000, MAX_FIREFLIES=1500;
    /** Player model scale: vanilla renders players at 15/16 and model units are pixels. */
    private static final float PLAYER_PX=.9375f/16;
    /** Faces of a unit cube: normal axis/sign and four corners wound counter-clockwise from outside. */
    private static final float[][] FACES=new float[6][];
    private static final int[] FACE_AXIS=new int[6], FACE_SIGN=new int[6];
    private static final Set<Integer> dying=new HashSet<>();
    private static final List<Cube> cubes=new ArrayList<>();
    private static final List<Firefly> fireflies=new ArrayList<>();
    private static final Map<Identifier,int[]> palettes=new HashMap<>();
    private static final Random random=new Random();
    private static long lastFrame;

    static {
        int f=0;
        for(int axis=0;axis<3;axis++) for(int sign=-1;sign<=1;sign+=2) {
            int b=(axis+1)%3,c=(axis+2)%3;
            float[] pts=new float[12];
            float[][] uv={{-1,-1},{1,-1},{1,1},{-1,1}};
            for(int k=0;k<4;k++) { pts[k*3+axis]=sign; pts[k*3+b]=uv[k][0]; pts[k*3+c]=uv[k][1]; }
            // Flip the winding when the corners turn clockwise around the outward normal.
            Vector3f e1=new Vector3f(pts[3]-pts[0],pts[4]-pts[1],pts[5]-pts[2]),e2=new Vector3f(pts[6]-pts[0],pts[7]-pts[1],pts[8]-pts[2]);
            Vector3f n=new Vector3f(); n.setComponent(axis,sign);
            if(e1.cross(e2).dot(n)<0) for(int i=0;i<3;i++) { float t=pts[3+i]; pts[3+i]=pts[9+i]; pts[9+i]=t; }
            FACES[f]=pts; FACE_AXIS[f]=axis; FACE_SIGN[f]=sign; f++;
        }
    }
    private DeathEffect() {}

    public static void initialize() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->render(ctx.submitNodeCollector(),ctx.poseStack(),ctx.gameRenderer().mainCamera()));
    }

    /** True while a dead entity is replaced by the effect, so its own model is skipped. */
    public static boolean hidden(Entity entity) {
        return entity instanceof LivingEntity && !dying.isEmpty() && dying.contains(entity.getId()) && ClientModules.DEATH_EFFECT.enabled();
    }

    public static void tick(Minecraft mc) {
        if(mc.level==null || !ClientModules.DEATH_EFFECT.enabled()) { clear(); return; }
        Set<Integer> present=new HashSet<>();
        for(Entity e:mc.level.entitiesForRendering()) {
            if(!(e instanceof LivingEntity living) || !living.isDeadOrDying()) continue;
            present.add(e.getId());
            if(dying.contains(e.getId()) || !wanted(mc,living)) continue;
            dying.add(e.getId());
            try { spawn(mc,living); }
            catch(RuntimeException ex) { System.err.println("[ESS] DeathEffect: "+ex); }
        }
        dying.retainAll(present);
    }

    private static boolean wanted(Minecraft mc,LivingEntity e) {
        if(e==mc.player) return TARGETS.selected("Себя");
        return e instanceof Player ? TARGETS.selected("Игроки") : TARGETS.selected("Мобы");
    }

    private static void clear() {
        dying.clear(); cubes.clear(); fireflies.clear();
    }

    /* ---------------- spawning ---------------- */

    private static void spawn(Minecraft mc,LivingEntity e) {
        float life=LIFE.get();
        int light=mc.getEntityRenderDispatcher().getPackedLightCoords(e,1);
        double cx=e.getX(),cy=e.getY(),cz=e.getZ();
        float yaw=(float)Math.toRadians(e.yBodyRot);
        if(MODE.is("Светлячки")) { fireflies(e,cx,cy,cz,life); return; }
        int start=cubes.size();
        if(e instanceof AbstractClientPlayer player) shatterPlayer(player,cx,cy,cz,yaw,light,life);
        else shatterMob(mc,e,cx,cy,cz,yaw,light,life);
        limit(start);
    }

    /** Keeps at most Кол-во кубиков of the cubes one body produced, picked at random so the whole body is still covered. */
    private static void limit(int start) {
        List<Cube> body=cubes.subList(start,cubes.size());
        int keep=Math.round(CUBES.get());
        if(body.size()<=keep) return;
        Collections.shuffle(body,random);
        body.subList(keep,body.size()).clear();
    }

    /** Dev preview: shatters a copy of the entity at a given spot and facing, without hiding the entity itself. */
    public static void preview(LivingEntity e,double x,double y,double z,float bodyYawDegrees) {
        var mc=Minecraft.getInstance();
        int light=mc.getEntityRenderDispatcher().getPackedLightCoords(e,1);
        float yaw=(float)Math.toRadians(bodyYawDegrees);
        if(MODE.is("Светлячки")) { fireflies(e,x,y,z,LIFE.get()); return; }
        int start=cubes.size();
        if(e instanceof AbstractClientPlayer player) shatterPlayer(player,x,y,z,yaw,light,LIFE.get());
        else shatterMob(mc,e,x,y,z,yaw,light,LIFE.get());
        limit(start);
    }

    /** A box of the player model in pixels: X to the player's left, Y up, Z forward; uv is the skin box origin. */
    private record Part(float x0,float y0,float z0,float x1,float y1,float z1,int u,int v) {
        float w() { return x1-x0; } float h() { return y1-y0; } float d() { return z1-z0; }
        /** Skin pixel for a point on the given face, using Minecraft's box unwrap. */
        void uv(int axis,int sign,float x,float y,float z,float[] out,int at) {
            float w=w(),h=h(),d=d(),u0=u,v0=v,uu,vv;
            if(axis==2) { uu=sign>0 ? u0+d+(x-x0) : u0+2*d+w+(x1-x); vv=v0+d+(y1-y); }
            else if(axis==0) { uu=sign<0 ? u0+(z-z0) : u0+d+w+(z1-z); vv=v0+d+(y1-y); }
            else { uu=(sign>0 ? u0+d : u0+d+w)+(x-x0); vv=v0+(z-z0); }
            out[at]=uu/64f; out[at+1]=vv/64f;
        }
    }

    private static void shatterPlayer(AbstractClientPlayer p,double cx,double cy,double cz,float yaw,int light,float life) {
        var skin=p.getSkin();
        Identifier texture=skin.body().texturePath();
        boolean slim=skin.model()==PlayerModelType.SLIM;
        float arm=slim ? 3 : 4;
        Part[] parts={
            new Part(-4,24,-4,4,32,4,0,0),          // head
            new Part(-4,12,-2,4,24,2,16,16),        // body
            new Part(-4-arm,12,-2,-4,24,2,40,16),   // right arm
            new Part(4,12,-2,4+arm,24,2,32,48),     // left arm
            new Part(-4,0,-2,0,12,2,0,16),          // right leg
            new Part(0,0,-2,4,12,2,16,48),          // left leg
        };
        float scale=p.getScale()*PLAYER_PX,step=CUBE.get();
        Quaternionf base=new Quaternionf().rotateY(-yaw);
        Vector3f centre=new Vector3f(0,16,0);
        for(Part part:parts) {
            int nx=Math.max(1,Math.round(part.w()/step)),ny=Math.max(1,Math.round(part.h()/step)),nz=Math.max(1,Math.round(part.d()/step));
            float sx=part.w()/nx,sy=part.h()/ny,sz=part.d()/nz;
            for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nz;k++) {
                if(cubes.size()>=MAX_CUBES) return;
                float mx=part.x0+(i+.5f)*sx,my=part.y0+(j+.5f)*sy,mz=part.z0+(k+.5f)*sz;
                Cube c=new Cube();
                c.texture=texture; c.color=-1; c.light=light;
                c.hx=sx/2*scale; c.hy=sy/2*scale; c.hz=sz/2*scale;
                c.uv=new float[48];
                for(int f=0;f<6;f++) {
                    float[] pts=FACES[f];
                    for(int q=0;q<4;q++) part.uv(FACE_AXIS[f],FACE_SIGN[f],
                            Math.clamp(mx+pts[q*3]*sx/2,part.x0,part.x1),Math.clamp(my+pts[q*3+1]*sy/2,part.y0,part.y1),
                            Math.clamp(mz+pts[q*3+2]*sz/2,part.z0,part.z1),c.uv,f*8+q*2);
                }
                Vector3f local=base.transform(new Vector3f(mx,my,mz).mul(scale));
                launch(c,cx+local.x,cy+local.y,cz+local.z,base,new Vector3f(mx,my,mz).sub(centre),life);
            }
        }
    }

    private static void shatterMob(Minecraft mc,LivingEntity e,double cx,double cy,double cz,float yaw,int light,float life) {
        int[] palette=palette(mc,e);
        float w=e.getBbWidth(),h=e.getBbHeight();
        float size=Math.max(.06f,CUBE.get()*1.6f/16*Math.max(.6f,Math.min(2,(w+h)/2.4f)));
        int nx=Math.max(1,Math.round(w/size)),ny=Math.max(1,Math.round(h/size));
        float sx=w/nx,sy=h/ny;
        Quaternionf base=new Quaternionf().rotateY(-yaw);
        for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nx;k++) {
            if(cubes.size()>=MAX_CUBES) return;
            // A hollow shell is enough for big mobs; inner cubes are never seen before they scatter.
            boolean shell=i==0 || j==0 || k==0 || i==nx-1 || j==ny-1 || k==nx-1;
            if(!shell && random.nextFloat()<.7f) continue;
            float mx=-w/2+(i+.5f)*sx,my=(j+.5f)*sy,mz=-w/2+(k+.5f)*sx;
            Cube c=new Cube();
            c.texture=WHITE; c.light=light;
            c.color=palette.length==0 ? 0xff9a9a9a : palette[random.nextInt(palette.length)];
            c.hx=c.hz=sx/2; c.hy=sy/2;
            Vector3f local=base.transform(new Vector3f(mx,my,mz));
            launch(c,cx+local.x,cy+local.y,cz+local.z,base,new Vector3f(mx,my-h/2,mz).mul(16),life);
        }
    }

    /** Outward burst from the body centre plus a lift; strength scales everything. */
    private static void launch(Cube c,double x,double y,double z,Quaternionf base,Vector3f fromCentre,float life) {
        float force=FORCE.get();
        Vector3f dir=fromCentre.lengthSquared()<1e-4f ? new Vector3f(0,1,0) : new Vector3f(fromCentre).normalize();
        base.transform(dir);
        float speed=(1.2f+random.nextFloat()*2.2f)*force;
        c.x=x; c.y=y; c.z=z;
        c.vx=dir.x*speed+(random.nextFloat()-.5f)*force;
        c.vy=dir.y*speed*.6f+(1.6f+random.nextFloat()*2.4f)*force;
        c.vz=dir.z*speed+(random.nextFloat()-.5f)*force;
        c.rot=new Quaternionf(base);
        c.axis=new Vector3f(random.nextFloat()-.5f,random.nextFloat()-.5f,random.nextFloat()-.5f).normalize();
        c.spin=(2+random.nextFloat()*8)*(.4f+force*.6f);
        c.life=life*(.75f+random.nextFloat()*.5f);
        cubes.add(c);
    }

    /** Opaque pixels of a mob's texture, read once from the resource pack. */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static int[] palette(Minecraft mc,LivingEntity e) {
        Identifier texture;
        try {
            var renderer=mc.getEntityRenderDispatcher().getRenderer(e);
            if(!(renderer instanceof LivingEntityRenderer living)) return new int[0];
            texture=living.getTextureLocation((net.minecraft.client.renderer.entity.state.LivingEntityRenderState)living.createRenderState(e,1));
        } catch(RuntimeException ex) { return new int[0]; }
        return palettes.computeIfAbsent(texture,id->{
            var resource=mc.getResourceManager().getResource(id);
            if(resource.isEmpty()) return new int[0];
            try(InputStream in=resource.get().open();NativeImage image=NativeImage.read(in)) {
                List<Integer> colors=new ArrayList<>();
                for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
                    int argb=image.getPixel(x,y);
                    if((argb>>>24)>200) colors.add(argb|0xff000000);
                }
                return colors.stream().mapToInt(Integer::intValue).toArray();
            } catch(Exception ex) { return new int[0]; }
        });
    }

    private static void fireflies(LivingEntity e,double cx,double cy,double cz,float life) {
        int count=Math.round(COUNT.get());
        float w=e.getBbWidth(),h=e.getBbHeight();
        int accent=ClientModules.THEME_ACCENT.get();
        for(int i=0;i<count && fireflies.size()<MAX_FIREFLIES;i++) {
            Firefly f=new Firefly();
            f.x=cx+(random.nextFloat()-.5f)*w; f.y=cy+random.nextFloat()*h; f.z=cz+(random.nextFloat()-.5f)*w;
            // Lower parts let go a little later, so the body dissolves from the top down.
            f.delay=(1-(float)(f.y-cy)/h)*.45f+random.nextFloat()*.25f;
            f.rise=.35f+random.nextFloat()*.9f;
            f.swayX=(random.nextFloat()-.5f)*.9f; f.swayZ=(random.nextFloat()-.5f)*.9f;
            f.phase=random.nextFloat()*6.283f; f.freq=1.2f+random.nextFloat()*1.8f;
            f.size=.05f+random.nextFloat()*.06f;
            f.life=life*(.6f+random.nextFloat()*.4f);
            f.color=switch (COLOR.get()) {
                case "Белый" -> 0xfffff4dc;
                case "Радуга" -> java.awt.Color.HSBtoRGB(random.nextFloat(),.55f,1);
                default -> accent;
            };
            fireflies.add(f);
        }
    }

    /* ---------------- simulation + drawing ---------------- */

    private static final class Cube {
        double x,y,z; float vx,vy,vz,hx,hy,hz,spin,age,life; boolean grounded;
        Quaternionf rot; Vector3f axis; Identifier texture; int color,light; float[] uv;
    }
    private static final class Firefly {
        double x,y,z; float delay,rise,swayX,swayZ,phase,freq,size,age,life; int color;
    }

    private static void step(Minecraft mc,float dt) {
        float gravity=GRAVITY.get() ? 13f : 0;
        for(Iterator<Cube> it=cubes.iterator();it.hasNext();) {
            Cube c=it.next();
            c.age+=dt;
            if(c.age>=c.life) { it.remove(); continue; }
            if(gravity==0) { c.vx*=1-dt*1.6f; c.vy*=1-dt*1.6f; c.vz*=1-dt*1.6f; }
            c.vy-=gravity*dt;
            double nx=c.x+c.vx*dt,ny=c.y+c.vy*dt,nz=c.z+c.vz*dt;
            // Land on the top of a solid block, bounce a little and let friction stop the slide and the spin.
            if(gravity>0 && c.vy<0) {
                BlockPos pos=BlockPos.containing(nx,ny-c.hy,nz);
                var state=mc.level.getBlockState(pos);
                if(!state.getCollisionShape(mc.level,pos).isEmpty()) {
                    double top=pos.getY()+state.getCollisionShape(mc.level,pos).max(net.minecraft.core.Direction.Axis.Y);
                    if(c.y-c.hy>=top-.05) {
                        ny=top+c.hy;
                        c.vy=Math.abs(c.vy)>1.2f ? -c.vy*.32f : 0;
                        c.vx*=.62f; c.vz*=.62f; c.spin*=.55f;
                        c.grounded=c.vy==0;
                    }
                }
            }
            if(c.grounded) { c.vx*=1-Math.min(1,dt*6); c.vz*=1-Math.min(1,dt*6); c.spin*=1-Math.min(1,dt*5); }
            c.x=nx; c.y=ny; c.z=nz;
            if(c.spin>.01f) c.rot.rotateAxis(c.spin*dt,c.axis.x,c.axis.y,c.axis.z);
        }
        for(Iterator<Firefly> it=fireflies.iterator();it.hasNext();) {
            Firefly f=it.next();
            f.age+=dt;
            if(f.age>=f.delay+f.life) { it.remove(); continue; }
            if(f.age<f.delay) continue;
            float t=f.age-f.delay;
            f.y+=f.rise*dt*(.6f+Math.min(1,t)*.4f);
            f.x+=Math.sin(t*f.freq+f.phase)*f.swayX*dt;
            f.z+=Math.cos(t*f.freq*.8f+f.phase)*f.swayZ*dt;
        }
    }

    private static void render(net.minecraft.client.renderer.SubmitNodeCollector collector,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.Camera camera) {
        var mc=Minecraft.getInstance();
        long now=System.nanoTime();
        float dt=lastFrame==0 ? 0 : Math.min(.05f,(now-lastFrame)/1e9f);
        lastFrame=now;
        if(mc.level==null || (cubes.isEmpty() && fireflies.isEmpty())) return;
        if(!mc.isPaused()) step(mc,dt);
        var cam=camera.position();
        if(!cubes.isEmpty()) drawCubes(collector,pose,cam.x,cam.y,cam.z);
        if(!fireflies.isEmpty()) drawFireflies(collector,pose,camera,cam.x,cam.y,cam.z);
    }

    private static void drawCubes(net.minecraft.client.renderer.SubmitNodeCollector collector,com.mojang.blaze3d.vertex.PoseStack pose,double camX,double camY,double camZ) {
        Map<Identifier,List<Cube>> byTexture=new HashMap<>();
        for(Cube c:cubes) byTexture.computeIfAbsent(c.texture,k->new ArrayList<>()).add(c);
        for(var entry:byTexture.entrySet()) {
            List<Cube> list=entry.getValue();
            collector.submitCustomGeometry(pose,RenderTypes.entityTranslucent(entry.getKey()),(p,vc)->{
                Vector3f v=new Vector3f(),n=new Vector3f();
                for(Cube c:list) {
                    float left=c.life-c.age,shrink=Math.clamp(left/(c.life*.35f),0,1);
                    shrink=shrink*shrink*(3-2*shrink);
                    if(shrink<=.01f) continue;
                    int alpha=Math.round(255*Math.min(1,shrink*1.4f));
                    int color=(c.color&0xffffff)|(alpha<<24);
                    float ox=(float)(c.x-camX),oy=(float)(c.y-camY),oz=(float)(c.z-camZ);
                    for(int f=0;f<6;f++) {
                        float[] pts=FACES[f];
                        n.set(0); n.setComponent(FACE_AXIS[f],FACE_SIGN[f]); c.rot.transform(n);
                        for(int q=0;q<4;q++) {
                            v.set(pts[q*3]*c.hx*shrink,pts[q*3+1]*c.hy*shrink,pts[q*3+2]*c.hz*shrink);
                            c.rot.transform(v);
                            float u=c.uv==null ? .5f : c.uv[f*8+q*2],w=c.uv==null ? .5f : c.uv[f*8+q*2+1];
                            vc.addVertex(p,ox+v.x,oy+v.y,oz+v.z).setColor(color).setUv(u,w)
                                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(c.light).setNormal(p,n.x,n.y,n.z);
                        }
                    }
                }
            });
        }
    }

    private static void drawFireflies(net.minecraft.client.renderer.SubmitNodeCollector collector,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.Camera camera,double camX,double camY,double camZ) {
        Vector3f right=new Vector3f(camera.leftVector()).negate(),up=new Vector3f(camera.upVector());
        // Additive and without depth writes (the Particles pipeline), so overlapping glows add up instead of
        // the square of one halo cutting off the fireflies behind it.
        collector.submitCustomGeometry(pose,ess.ll.client.render.EffectRenderPipelines.particles(GLOW),(p,vc)->{
            for(Firefly f:fireflies) {
                if(f.age<f.delay) continue;
                float t=f.age-f.delay,in=Math.min(1,t/.2f),out=Math.clamp((f.life-t)/(f.life*.45f),0,1);
                float twinkle=.75f+.25f*(float)Math.sin(t*9+f.phase*3);
                float a=in*out*out*twinkle;
                if(a<=.01f) continue;
                float ox=(float)(f.x-camX),oy=(float)(f.y-camY),oz=(float)(f.z-camZ);
                // Wide soft halo, then a small bright core on top of it.
                quad(vc,p,ox,oy,oz,right,up,f.size*4.2f,(f.color&0xffffff)|(Math.round(a*18)<<24));
                quad(vc,p,ox,oy,oz,right,up,f.size*1.5f,(f.color&0xffffff)|(Math.round(a*55)<<24));
                quad(vc,p,ox,oy,oz,right,up,f.size*.6f,0xffffff|(Math.round(a*120)<<24));
            }
        });
    }
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc,com.mojang.blaze3d.vertex.PoseStack.Pose p,float x,float y,float z,
                             Vector3f r,Vector3f u,float s,int color) {
        float[][] k={{-1,-1,0,1},{1,-1,1,1},{1,1,1,0},{-1,1,0,0}};
        for(float[] c:k) vc.addVertex(p,x+(r.x*c[0]+u.x*c[1])*s,y+(r.y*c[0]+u.y*c[1])*s,z+(r.z*c[0]+u.z*c[1])*s)
                .setUv(c[2],c[3]).setColor(color);
    }
}
