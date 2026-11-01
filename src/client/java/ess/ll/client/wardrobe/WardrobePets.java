package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.Wardrobe.Item;
import ess.ll.client.wardrobe.Wardrobe.Slot;
import ess.ll.client.wardrobe.WardrobeModel.Effect;
import ess.ll.client.wardrobe.WardrobeModel.PetKind;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Wardrobe pets (from Zenith's PetBehavior and WardrobePreview): your pet walks, hops, flies or floats after you
 * with its own collision physics, and keeps itself busy — sits, lies, wanders, orbits, plays, perches on your
 * shoulder or head, falls asleep when you stand still, reacts when you look at it, runs from monsters and shows
 * its quirk (sneezing sparks, smoke rings, spore puffs...). Drawn in the world for you only.
 */
public final class WardrobePets {
    private static final double STEP=.05;
    private static Pet pet;
    private static WeakReference<Level> level=new WeakReference<>(null);
    private static long lastNanos;
    private static double clock;

    private WardrobePets() {}

    /** Collision physics of a pet: a small box that walks, steps up, jumps and falls, or hovers when it flies. */
    static final class Body {
        final PetKind kind;
        final double radius,height;
        double x,y,z,prevX,prevY,prevZ,vx,vy,vz;
        float yaw,prevYaw,headYaw,prevHeadYaw;
        boolean onGround,placed,landed;
        double accumulator,speed,bob;
        Body(PetKind kind,double radius,double height) { this.kind=kind; this.radius=radius; this.height=height; }
        boolean flies() { return kind==PetKind.FLYER || kind==PetKind.FLOATER; }
        /** Fixed 20 steps a second, at most 5 per frame. */
        int steps(double dt) {
            accumulator=Math.min(accumulator+Math.max(0,dt),.25);
            int n=0;
            while(accumulator>=STEP && n<5) { accumulator-=STEP; n++; }
            return n;
        }
        void begin() { prevX=x; prevY=y; prevZ=z; prevYaw=yaw; prevHeadYaw=headYaw; landed=false; }
        void teleport(Vec3 p) { x=prevX=p.x; y=prevY=p.y; z=prevZ=p.z; vx=vy=vz=0; accumulator=0; }
        void move(double tx,double ty,double tz,double speedScale,double hover) {
            double dx=tx-x,dz=tz-z,d=Math.sqrt(dx*dx+dz*dz);
            if(d>.05) {
                double s=Math.min(1,d/2)*speedScale,air=!flies() && !onGround ? .25 : 1,accel=flies() ? .062 : .06;
                vx+=dx/d*s*accel*air; vz+=dz/d*s*accel*air;
            }
            if(flies()) {
                bob+=.08;
                double wantY=ty+hover+Math.sin(bob)*(kind==PetKind.FLOATER ? .12 : .06);
                double dy=Mth.clamp(wantY-y,-.25,.25);
                while(collides(x,y+dy,z) && dy<1) dy+=.1;
                y+=dy*.25; vy=0; onGround=false;
                moveX(false); moveZ(false);
                vx*=.85; vz*=.85;
            } else {
                vy-=.08; vy*=.98;
                if(vy<-.6) vy=-.6;
                vertical(); moveX(true); moveZ(true);
                float friction=onGround ? .78f : .91f;
                vx*=friction; vz*=friction;
            }
            double mx=x-prevX,mz=z-prevZ;
            speed=Math.sqrt(mx*mx+mz*mz)/STEP;
        }
        void jump(double v) { if(onGround && vy<=0) { vy=v; onGround=false; } }
        void face(double tx,double tz,float maxStep) {
            float want=(float)Math.toDegrees(Math.atan2(tz-z,tx-x))-90;
            yaw+=Mth.clamp(Mth.wrapDegrees(want-yaw)*.28f,-maxStep,maxStep);
        }
        void look(double tx,double tz) {
            float want=(float)Math.toDegrees(Math.atan2(tz-z,tx-x))-90;
            float d=Mth.clamp(Mth.wrapDegrees(want-yaw),-55,55);
            headYaw+=(d-headYaw)*.2f;
        }
        private void vertical() {
            boolean was=onGround;
            onGround=false;
            if(!collides(x,y+vy,z)) y+=vy;
            else {
                if(vy<0) {
                    double lo=y+vy,hi=y;
                    for(int i=0;i<8;i++) { double mid=(lo+hi)*.5; if(collides(x,mid,z)) lo=mid; else hi=mid; }
                    y=hi; onGround=true;
                }
                vy=0;
            }
            if(!onGround && collides(x,y-.02,z)) onGround=true;
            landed=onGround && !was;
        }
        private void moveX(boolean walking) {
            if(Math.abs(vx)<=1e-5) return;
            if(!collides(x+vx,y,z)) { x+=vx; return; }
            for(double up=.1;up<=(walking ? .6 : 1.2);up+=.1)
                if(!collides(x+vx,y+up,z) && !collides(x,y+up,z)) { y+=up; x+=vx; return; }
            if(walking && onGround && !collides(x+Math.signum(vx)*.35,y+1.1,z)) vy=.52;
            vx=0;
        }
        private void moveZ(boolean walking) {
            if(Math.abs(vz)<=1e-5) return;
            if(!collides(x,y,z+vz)) { z+=vz; return; }
            for(double up=.1;up<=(walking ? .6 : 1.2);up+=.1)
                if(!collides(x,y+up,z+vz) && !collides(x,y+up,z)) { y+=up; z+=vz; return; }
            if(walking && onGround && !collides(x,y+1.1,z+Math.signum(vz)*.35)) vy=.52;
            vz=0;
        }
        boolean collides(double px,double py,double pz) {
            var level=Minecraft.getInstance().level;
            return level!=null && !level.noCollision(new AABB(px-radius,py,pz-radius,px+radius,py+height,pz+radius));
        }
        Vec3 pos(float t) { return new Vec3(Mth.lerp(t,prevX,x),Mth.lerp(t,prevY,y),Mth.lerp(t,prevZ,z)); }
        float yaw(float t) { return prevYaw+Mth.wrapDegrees(yaw-prevYaw)*t; }
        float head(float t) { return prevHeadYaw+(headYaw-prevHeadYaw)*t; }
        float partial() { return (float)Mth.clamp(accumulator/STEP,0,1); }
    }

    /** One pet: its body, its animation and what it is up to. */
    static final class Pet {
        final Item item;
        final WardrobeModel model;
        final WardrobeModel.Pet data;
        final Body body;
        final WardrobeAnimator.ClipPlayer clips=new WardrobeAnimator.ClipPlayer();
        final WardrobeAnimator.Pose pose=new WardrobeAnimator.Pose();
        final RandomSource random=RandomSource.create();
        private double speedSmooth;
        private boolean walking,running,hostileNear,sleeping,perched;
        private int ticks,stillTicks,side,lookCooldown=60,happyCooldown=40,quirkCooldown,idleCooldown,noteCooldown,danger,hostileCheckAt,hopCooldown,goalTicks;
        private int activityTicks,activityCooldown=120;
        private Vec3 lastPos,goal,wanderTarget,perchFrom;
        private float lastYaw,lastPitch,walkYaw,squash,accent,perch,prevPerch,alpha=1;
        private float[] emitted=new float[0];
        private String activity="";
        private double orbitAngle;

        Pet(Item item,WardrobeModel model) {
            this.item=item; this.model=model; data=model.pet;
            double scale=Math.max(.2,data.scale());
            body=new Body(data.kind(),.18*scale,.6*scale);
            side=random.nextBoolean() ? 1 : -1;
            quirkCooldown=200+random.nextInt(200);
            idleCooldown=160+random.nextInt(160);
        }

        void update(Player player,LivingEntity target,double dt,float time) {
            Vec3 at=player.position();
            if(!body.placed) {
                body.teleport(spawn(player,at));
                body.yaw=body.prevYaw=player.yBodyRot;
                body.placed=true;
                lastPos=at;
                walkYaw=player.yBodyRot;
            }
            int steps=body.steps(dt);
            for(int i=0;i<steps;i++) step(player,at,target,time);
        }
        float perchProgress(float t) { return Mth.clamp(Mth.lerp(t,prevPerch,perch),0,1); }

        private void step(Player player,Vec3 at,LivingEntity target,float time) {
            ticks++;
            body.begin();
            cooldowns();
            prevPerch=perch;
            double dx=at.x-player.xo,dy=at.y-player.yo,dz=at.z-player.zo;
            double moved=lastPos==null ? 0 : Math.sqrt(dx*dx+dz*dz);
            if(moved>.02 && lastPos!=null) walkYaw+=Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(dz,dx))-90-walkYaw)*.35f;
            boolean still=lastPos!=null && dx*dx+dy*dy+dz*dz<4e-4 && Math.abs(Mth.wrapDegrees(player.getYRot()-lastYaw))<1 && Math.abs(player.getXRot()-lastPitch)<1;
            lastPos=at; lastYaw=player.getYRot(); lastPitch=player.getXRot();
            stillTicks=still ? stillTicks+1 : 0;
            boolean sprinting=player.isSprinting() || moved>.2,moving=moved>.03;
            if(player.hurtTime>0 || target!=null && target.isAlive()) danger=60;
            else if(danger>0) danger--;
            boolean scared=danger>0 || "hide_in_shell".equals(data.quirk()) && monstersNear(at,8);
            if(perched) { perchedStep(player,at,sprinting,scared,time); return; }
            double dist=flat(body.x,body.z,at.x,at.z);
            if(dist>12 || Math.abs(body.y-at.y)>12) {
                poof(body.x,body.y,body.z);
                body.teleport(spawn(player,at));
                poof(body.x,body.y,body.z);
                endActivity();
                return;
            }
            boolean wasSleeping=sleeping;
            sleeping=stillTicks>600 && !scared;
            if(wasSleeping && !sleeping) { clips.playOnce(model,"react",time); lookCooldown=80; }
            boolean onPlayer=activity.equals("shoulder") || activity.equals("head");
            if(sleeping || scared) endActivity();
            else if(!activity.isEmpty() && moving && !activity.equals("orbit") && !onPlayer) endActivity();
            else if(!activity.isEmpty() && --activityTicks<=0) endActivity();
            if(activity.isEmpty() && !sleeping && !scared && !moving && activityCooldown==0 && dist<4 && goal==null) pickActivity(player,at,time);
            onPlayer=activity.equals("shoulder") || activity.equals("head");
            Vec3 aim=target(player,at,target,scared,moving,sprinting,dist);
            double speed=data.speed(),toAim=flat(body.x,body.z,aim.x,aim.z);
            if(toAim>5) speed*=1.9; else if(toAim>2.5 || sprinting) speed*=1.35;
            double hover=data.hover();
            if(body.flies() && (activity.equals("sit") || activity.equals("lie"))) hover=0;
            if(!sleeping && (toAim>=.3 || goal!=null)) body.move(aim.x,aim.y,aim.z,speed,hover);
            else body.move(body.x,aim.y,body.z,0,hover);
            if(onPlayer && flat(body.x,body.z,at.x,at.z)<1.1) perch();
            if(data.kind()==PetKind.HOPPER && body.onGround && toAim>.6 && hopCooldown==0) { body.jump(.42); hopCooldown=7; squash=1; }
            if(body.landed) {
                squash=1;
                if("rain_cloud".equals(data.quirk())) burst(ParticleTypes.CLOUD,4,.2,.02);
            }
            squash=Math.max(0,squash-.12f);
            if(walking && !sleeping) body.face(aim.x,aim.z,14);
            else if(!activity.equals("lie") && !activity.equals("sit")) body.face(at.x,at.z,6);
            else body.face(at.x,at.z,3);
            body.look(at.x,at.z);
            quirks(player,at,scared,time);
            reactions(player,at,time);
            animate(time);
            sleepNotes();
        }
        private void pickActivity(Player player,Vec3 at,float time) {
            List<String> options=new ArrayList<>();
            for(String a:activities()) switch (a) {
                case "sit","lie","wander","play","orbit" -> options.add(a);
                case "shoulder","head" -> { if(player.onGround() && !player.isSprinting()) options.add(a); }
                default -> {}
            }
            if(options.isEmpty()) { activityCooldown=200; return; }
            activity=options.get(random.nextInt(options.size()));
            switch (activity) {
                case "sit" -> activityTicks=100+random.nextInt(140);
                case "lie" -> activityTicks=160+random.nextInt(180);
                case "orbit" -> { activityTicks=120+random.nextInt(100); orbitAngle=Math.atan2(body.z-at.z,body.x-at.x); }
                case "wander" -> {
                    activityTicks=140;
                    double a=random.nextDouble()*Math.PI*2,r=1.3+random.nextDouble()*1.6;
                    wanderTarget=new Vec3(at.x+Math.cos(a)*r,at.y,at.z+Math.sin(a)*r);
                }
                case "play" -> { activityTicks=50; if(!clips.playOnce(model,"play",time)) clips.playOnce(model,"happy",time); }
                default -> activityTicks=260+random.nextInt(340);
            }
        }
        private void endActivity() {
            if(activity.isEmpty()) return;
            activity=""; wanderTarget=null;
            activityCooldown=70+random.nextInt(160);
        }
        private List<String> activities() {
            if(!data.behaviors().isEmpty()) return data.behaviors();
            return switch (data.kind()) {
                case FLYER -> List.of("orbit","sit","play");
                case FLOATER -> List.of("orbit","wander","play");
                case HOPPER -> List.of("pace","sit","wander","play");
                case CRAWLER -> List.of("sit","lie","wander");
                default -> List.of("pace","sit","lie","wander","play");
            };
        }
        private void perch() {
            perched=true;
            perchFrom=new Vec3(body.x,body.y,body.z);
            perch=prevPerch=0;
            body.vx=body.vy=body.vz=0;
        }
        private void perchedStep(Player player,Vec3 at,boolean sprinting,boolean scared,float time) {
            perch=Math.min(1,perch+.16f);
            activityTicks--;
            Vec3 p=perchPos(player,at,player.yBodyRot);
            body.x=body.prevX=p.x; body.y=body.prevY=p.y; body.z=body.prevZ=p.z;
            body.yaw=body.prevYaw=player.yBodyRot;
            body.speed=0;
            boolean airborne=!player.onGround() && !player.isPassenger() && !player.isInWater();
            if(activityTicks>0 && !sprinting && !scared && !airborne && !player.isDeadOrDying()) {
                body.headYaw+=(0-body.headYaw)*.2f;
                animate(time);
            } else {
                // Hops off to the side it sits on.
                perched=false; perch=prevPerch=0;
                double a=Math.toRadians(player.yBodyRot+(side>0 ? -90 : 90));
                body.vx=-Math.sin(a)*.12; body.vz=Math.cos(a)*.12; body.vy=.18; body.onGround=false;
                endActivity();
                clips.playOnce(model,"react",time);
            }
        }
        Vec3 perchPos(Player player,Vec3 at,float yaw) {
            double a=Math.toRadians(yaw);
            boolean head=activity.equals("head");
            double up=head ? (player.isShiftKeyDown() ? 1.72 : 2) : (player.isShiftKeyDown() ? 1.24 : 1.5);
            double off=head ? 0 : .31*side;
            return new Vec3(at.x-Math.cos(a)*off+Math.sin(a)*.03,at.y+up,at.z-Math.sin(a)*off-Math.cos(a)*.03);
        }
        float perchScale() { return activity.equals("head") ? .5f : .55f; }
        private void cooldowns() {
            if(activityCooldown>0) activityCooldown--;
            if(lookCooldown>0) lookCooldown--;
            if(happyCooldown>0) happyCooldown--;
            if(quirkCooldown>0) quirkCooldown--;
            if(idleCooldown>0) idleCooldown--;
            if(noteCooldown>0) noteCooldown--;
            if(hopCooldown>0) hopCooldown--;
            if(goalTicks>0 && --goalTicks==0) goal=null;
        }
        private Vec3 target(Player player,Vec3 at,LivingEntity enemy,boolean scared,boolean moving,boolean sprinting,double dist) {
            if(goal!=null) return goal;
            if(scared) {
                Vec3 threat=enemy!=null && enemy.isAlive() ? enemy.position() : null;
                if(threat==null) return keepAround(at,dist,1.6,2.4);
                double ax=at.x-threat.x,az=at.z-threat.z,len=Math.max(.001,Math.sqrt(ax*ax+az*az));
                return new Vec3(at.x+ax/len*2.2,at.y,at.z+az/len*2.2);
            }
            switch (activity) {
                case "wander" -> {
                    if(wanderTarget!=null) {
                        if(flat(body.x,body.z,wanderTarget.x,wanderTarget.z)<.4) activityTicks=Math.min(activityTicks,20);
                        return wanderTarget;
                    }
                }
                case "orbit" -> {
                    orbitAngle+=.055;
                    double r=1.7+.2*Math.sin(ticks*.07);
                    return new Vec3(at.x+Math.cos(orbitAngle)*r,at.y,at.z+Math.sin(orbitAngle)*r);
                }
                case "shoulder","head" -> { return at; }
                case "sit","lie","play" -> {
                    if(dist<=3.5) return new Vec3(body.x,at.y,body.z);
                    endActivity();
                }
                default -> {}
            }
            if(!moving) return keepAround(at,dist,.9,3);
            double a=Math.toRadians(walkYaw);
            double fx=-Math.sin(a),fz=Math.cos(a),sx=-Math.cos(a),sz=-Math.sin(a);
            // Running: paces beside you (if it can), otherwise trails behind to one side.
            return sprinting && activities().contains("pace")
                    ? new Vec3(at.x+sx*1.15*side+fx*.35,at.y,at.z+sz*1.15*side+fz*.35)
                    : new Vec3(at.x-fx*1.3+sx*.9*side,at.y,at.z-fz*1.3+sz*.9*side);
        }
        /** Stays put inside the ring between near and far; otherwise heads back into it. */
        private Vec3 keepAround(Vec3 at,double dist,double near,double far) {
            if(dist>=near && dist<=far) return new Vec3(body.x,at.y,body.z);
            double dx=body.x-at.x,dz=body.z-at.z,len=Math.sqrt(dx*dx+dz*dz);
            if(len<.05) { double a=Math.toRadians(walkYaw+90.0*side); dx=-Math.sin(a); dz=Math.cos(a); len=1; }
            double r=(near+far)/2*.85;
            return new Vec3(at.x+dx/len*r,at.y,at.z+dz/len*r);
        }
        private Vec3 spawn(Player player,Vec3 at) {
            double a=Math.toRadians(player.yBodyRot);
            return new Vec3(at.x+Math.sin(a)*1.25-Math.cos(a)*.9*side,at.y+(body.flies() ? data.hover() : 0),at.z-Math.cos(a)*1.25-Math.sin(a)*.9*side);
        }
        /** Hearts when you crouch next to it; a start when you look straight at it. */
        private void reactions(Player player,Vec3 at,float time) {
            if(sleeping) return;
            double dx=body.x-at.x,dy=body.y-at.y,dz=body.z-at.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(happyCooldown==0 && player.isShiftKeyDown() && d<2.6 && clips.playOnce(model,"happy",time)) {
                burst(ParticleTypes.HEART,3,.35,.05);
                happyCooldown=120;
                lookCooldown=Math.max(lookCooldown,40);
            } else if(lookCooldown==0 && d<5.5) {
                Vec3 eye=player.getEyePosition(),view=player.getViewVector(1);
                Vec3 to=new Vec3(body.x-eye.x,body.y+body.height*.5-eye.y,body.z-eye.z).normalize();
                if(view.dot(to)>.975 && clips.playOnce(model,"react",time)) lookCooldown=160;
            }
        }
        private void animate(float time) {
            speedSmooth+=(body.speed-speedSmooth)*.3;
            if(walking ? speedSmooth<.3 : speedSmooth>.85) walking=!walking;
            if(running ? speedSmooth<3.4 : speedSmooth>4.4) running=!running;
            running&=walking;
            String state;
            if(perched) state="perch";
            else if(sleeping) state="sleep";
            else if(activity.equals("sit") && !walking) state="sit";
            else if(activity.equals("lie") && !walking) state="lie";
            else if(body.flies()) state=!walking && !activity.equals("orbit") ? "idle" : "fly";
            else if(!body.onGround && Math.abs(body.vy)>.12) state="jump";
            else if(running) state="run";
            else if(walking) state="walk";
            else state="idle";
            clips.speed(switch (state) {
                case "walk" -> (float)Mth.clamp(speedSmooth/2.4,.55,1.7);
                case "run" -> (float)Mth.clamp(speedSmooth/5.2,.75,1.6);
                case "fly" -> (float)Mth.clamp(.8+speedSmooth/8,.8,1.5);
                default -> 1f;
            });
            clips.want(model,state,time);
            if(state.equals("idle") && idleCooldown==0 && !clips.playingOneShot()) {
                clips.playOnce(model,"idle2",time);
                idleCooldown=200+random.nextInt(240);
            }
        }
        private void sleepNotes() {
            if(sleeping && noteCooldown==0) {
                spawn(ParticleTypes.NOTE,body.x,body.y+body.height+.1,body.z,.3,0,0);
                noteCooldown=70+random.nextInt(30);
            }
        }
        private void quirks(Player player,Vec3 at,boolean scared,float time) {
            String quirk=data.quirk()==null ? "" : data.quirk();
            switch (quirk) {
                case "hide_in_shell" -> {
                    if(scared && !clips.current().equals("hide")) clips.playOnce(model,"hide",time);
                    else if(!scared && clips.current().equals("hide")) clips.endOneShot(time);
                }
                case "ghost_phase" -> {
                    float want=goalTicks>0 ? .35f+.25f*Mth.sin(ticks*.6f) : .85f;
                    alpha+=(want-alpha)*.2f;
                }
                case "shadow_step" -> {
                    if(flat(body.x,body.z,at.x,at.z)>6 && quirkCooldown==0) {
                        burst(ParticleTypes.LARGE_SMOKE,8,.3,.02);
                        body.teleport(spawn(player,at));
                        burst(ParticleTypes.SMOKE,10,.3,.02);
                        clips.playOnce(model,"quirk",time);
                        quirkCooldown=60;
                    }
                }
                case "ink_splash" -> {
                    if(player.hurtTime==9 && quirkCooldown==0) { burst(ParticleTypes.SQUID_INK,12,.35,.08); clips.playOnce(model,"quirk",time); quirkCooldown=40; }
                    if(ticks%30==0 && !sleeping) spawn(ParticleTypes.BUBBLE_POP,body.x,body.y+body.height,body.z,0,.02,0);
                }
                default -> periodicQuirk(quirk,at,scared,time);
            }
        }
        private void periodicQuirk(String quirk,Vec3 at,boolean scared,float time) {
            if(quirkCooldown>0 || sleeping || scared || body.speed>3 || quirk.isEmpty()) return;
            quirkCooldown=260+random.nextInt(260);
            boolean played=clips.playOnce(model,"quirk",time);
            switch (quirk) {
                case "sneeze_sparks" -> { forward(ParticleTypes.FLAME,6,.12); forward(ParticleTypes.LAVA,2,.05); }
                case "smoke_rings" -> {
                    for(int i=0;i<10;i++) { double a=i/10.0*Math.PI*2; spawn(ParticleTypes.SMOKE,body.x+Math.cos(a)*.2,body.y+body.height+.2,body.z+Math.sin(a)*.2,0,.03,0); }
                }
                case "firefly_dance" -> {
                    for(int i=0;i<8;i++) { double a=i/8.0*Math.PI*2; spawn(ParticleTypes.END_ROD,body.x+Math.cos(a)*.6,body.y+.4,body.z+Math.sin(a)*.6,-Math.sin(a)*.03,.02,Math.cos(a)*.03); }
                }
                case "scan_beam" -> {
                    for(int i=0;i<10;i++) { double a=Math.toRadians(body.yaw+90)+(i-5)*.12; spawn(ParticleTypes.ELECTRIC_SPARK,body.x+Math.cos(a)*1.2,body.y+.2,body.z+Math.sin(a)*1.2,0,0,0); }
                }
                case "ghost_phase" -> goalTicks=60;
                case "spore_puff" -> { burst(ParticleTypes.SPORE_BLOSSOM_AIR,10,.4,.01); burst(ParticleTypes.MYCELIUM,8,.3,.01); }
                case "head_spin" -> {}
                case "slime_bounce" -> { body.jump(.75); squash=1; burst(ParticleTypes.ITEM_SLIME,8,.3,.05); }
                case "catch_stars" -> {
                    double a=random.nextDouble()*Math.PI*2;
                    goal=new Vec3(at.x+Math.cos(a)*2.2,at.y,at.z+Math.sin(a)*2.2);
                    goalTicks=40;
                    for(int i=0;i<6;i++) spawn(ParticleTypes.END_ROD,goal.x,goal.y+1.6+i*.2,goal.z,0,-.08,0);
                }
                case "offer_flower" -> { goal=new Vec3(at.x+(body.x-at.x)*.3,at.y,at.z+(body.z-at.z)*.3); goalTicks=50; burst(ParticleTypes.HAPPY_VILLAGER,4,.3,.02); }
                case "bear_hug" -> { goal=new Vec3(at.x+(body.x-at.x)*.2,at.y,at.z+(body.z-at.z)*.2); goalTicks=55; forward(ParticleTypes.HEART,3,.15); }
                case "spooky_grin" -> forward(ParticleTypes.SOUL_FIRE_FLAME,6,.08);
                case "rain_cloud" -> {
                    for(int i=0;i<12;i++) spawn(ParticleTypes.FALLING_WATER,body.x+(random.nextDouble()-.5)*.5,body.y+body.height+.3,body.z+(random.nextDouble()-.5)*.5,0,0,0);
                }
                default -> { if(!played) quirkCooldown=400; }
            }
        }
        /** The accent trail while it runs, and the model's own effects from their bones. */
        void particles(float dt,float[] pose) {
            var mc=Minecraft.getInstance();
            if(mc.player==null || !Wardrobe.PARTICLES.get() || mc.player.distanceToSqr(body.x,body.y,body.z)>1600) return;
            if(data.accent()!=null && body.speed>1.2) {
                accent+=dt*8;
                while(accent>=1) { accent--; spawn(data.accent(),body.x+(random.nextDouble()-.5)*.2,body.y+.1,body.z+(random.nextDouble()-.5)*.2,0,.01,0); }
            }
            List<Effect> effects=model.effects;
            if(effects.isEmpty()) return;
            if(emitted.length!=effects.size()) emitted=new float[effects.size()];
            for(int i=0;i<effects.size();i++) {
                Effect e=effects.get(i);
                boolean on=switch (e.when()) {
                    case MOVING -> body.speed>.6;
                    case IDLE -> body.speed<=.6;
                    case AIRBORNE -> !body.onGround;
                    default -> true;
                };
                if(!on || sleeping) continue;
                emitted[i]+=e.rate()*dt;
                while(emitted[i]>=1) {
                    emitted[i]--;
                    Vec3 p=bonePoint(this,pose,e.bone(),e.x(),e.y(),e.z());
                    double s=e.spread();
                    spawn(e.particle(),p.x+(random.nextDouble()-.5)*s,p.y+(random.nextDouble()-.5)*s,p.z+(random.nextDouble()-.5)*s,e.vx(),e.vy(),e.vz());
                }
            }
        }
        private boolean monstersNear(Vec3 at,double range) {
            var level=Minecraft.getInstance().level;
            if(level==null) { hostileNear=false; return false; }
            if(ticks>=hostileCheckAt) {
                hostileCheckAt=ticks+10;
                hostileNear=!level.getEntitiesOfClass(Monster.class,new AABB(at,at).inflate(range),LivingEntity::isAlive).isEmpty();
            }
            return hostileNear;
        }
        private void forward(ParticleOptions p,int n,double speed) {
            double a=Math.toRadians(body.yaw+90),fx=body.x+Math.cos(a)*.25,fz=body.z+Math.sin(a)*.25;
            for(int i=0;i<n;i++) spawn(p,fx,body.y+body.height*.6,fz,Math.cos(a)*speed+(random.nextDouble()-.5)*speed,random.nextDouble()*speed*.5,Math.sin(a)*speed+(random.nextDouble()-.5)*speed);
        }
        private void burst(ParticleOptions p,int n,double spread,double speed) {
            for(int i=0;i<n;i++) spawn(p,body.x+(random.nextDouble()-.5)*spread,body.y+body.height*.5+(random.nextDouble()-.5)*spread,body.z+(random.nextDouble()-.5)*spread,
                    (random.nextDouble()-.5)*speed,random.nextDouble()*speed,(random.nextDouble()-.5)*speed);
        }
        private void poof(double x,double y,double z) {
            for(int i=0;i<8;i++) spawn(ParticleTypes.POOF,x+(random.nextDouble()-.5)*.4,y+random.nextDouble()*.4,z+(random.nextDouble()-.5)*.4,0,.02,0);
        }
        private void spawn(ParticleOptions p,double x,double y,double z,double vx,double vy,double vz) {
            var level=Minecraft.getInstance().level;
            if(level!=null && p!=null && Wardrobe.PARTICLES.get()) level.addParticle(p,x,y,z,vx,vy,vz);
        }
        private static double flat(double x1,double z1,double x2,double z2) { double dx=x1-x2,dz=z1-z2; return Math.sqrt(dx*dx+dz*dz); }
    }

    private static float scale(Pet p) { return Wardrobe.PET_SCALE.get()*p.data.scale(); }
    /** World position of a point on a bone of the pet. */
    private static Vec3 bonePoint(Pet p,float[] pose,int bone,float x,float y,float z) {
        float t=p.body.partial();
        Vec3 at=p.body.pos(t);
        Matrix4f m=new Matrix4f().translate((float)at.x,(float)at.y,(float)at.z).rotateY((float)Math.toRadians(180-p.body.yaw(t))).scale(scale(p)).scale(-1,-1,1).translate(0,-1.501f,0);
        if(bone>=0) WardrobeRenderer.boneMatrix(p.model,pose,bone,m);
        Vector3f v=m.transformPosition(x/16,y/16,z/16,new Vector3f());
        return new Vec3(v.x,v.y,v.z);
    }

    /** Called while the level submits entities: updates your pet and draws it. */
    public static void render(PoseStack stack,SubmitNodeCollector collector) {
        var mc=Minecraft.getInstance();
        var player=mc.player;
        if(mc.level==null || player==null) { pet=null; return; }
        if(level.get()!=mc.level) { level=new WeakReference<>(mc.level); pet=null; }
        boolean on=ess.ll.client.module.ClientModules.WARDROBE!=null && ess.ll.client.module.ClientModules.WARDROBE.enabled();
        Item item=on ? Wardrobe.worn(Slot.PET) : null;
        WardrobeModel model=item==null ? null : Wardrobe.model(item);
        if(model==null || model.pet==null || player.isSpectator() || player.isInvisible()) { if(item==null) pet=null; return; }
        if(pet==null || pet.item!=item) pet=new Pet(item,model);
        long now=System.nanoTime();
        double dt=lastNanos==0 ? 0 : Math.min(.25,(now-lastNanos)/1e9);
        lastNanos=now;
        clock+=dt;
        float time=(float)(clock%100000);
        float partialTick=mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        pet.update(player,null,dt,time);
        float t=pet.body.partial();
        Vec3 at=pet.body.pos(t);
        float yaw=pet.body.yaw(t),size=scale(pet);
        float[] pose=pet.pose.compute(model,pet.clips,time,null);
        int head=model.bone("head");
        if(head>=0 && !pet.sleeping) pose[head*9+1]+=(float)Math.toRadians(-pet.body.head(t));
        pet.particles((float)dt,pose);
        if(pet.perched) {
            Vec3 me=player.getPosition(partialTick);
            yaw=Mth.rotLerp(partialTick,player.yBodyRotO,player.yBodyRot);
            Vec3 perchAt=pet.perchPos(player,me,yaw);
            float p=pet.perchProgress(t);
            if(p<1 && pet.perchFrom!=null) {
                float s=p*p*(3-2*p);
                at=pet.perchFrom.lerp(perchAt,s).add(0,Math.sin(Math.PI*p)*.55,0);
            } else at=perchAt;
            size*=Mth.lerp(p,1,pet.perchScale());
            // On your own shoulder it would sit in the camera.
            if(mc.options.getCameraType().isFirstPerson() && p>.5f) return;
        }
        Vec3 camera=mc.gameRenderer.mainCamera().position();
        int light=LightCoordsUtil.getLightCoords(mc.level,BlockPos.containing(at.x,at.y+.3,at.z));
        int alpha=Math.round(Math.max(.1f,Math.min(1,pet.alpha))*255);
        stack.pushPose();
        try {
            stack.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
            stack.rotate(new Quaternionf().rotationY((float)Math.toRadians(180-yaw)));
            float squash=1+pet.squash*.18f;
            stack.scale(size*squash,size/squash,size*squash);
            stack.scale(-1,-1,1);
            stack.translate(0,-1.501f,0);
            WardrobeRenderer.draw(model,pose,WardrobeModel.FREE,stack,collector,light,OverlayTexture.NO_OVERLAY,true,item.rarity().glows,alpha<<24|0xffffff,false);
        } finally { stack.popPose(); }
    }
}
