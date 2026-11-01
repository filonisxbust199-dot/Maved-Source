package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GhostModelBatch;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** HitGhost's five hit poses, animated skin afterimage, two spiral strands and bloom envelope. */
public final class HitGhost {
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Theme","White","Custom");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xff8f7cff).visibleWhen(()->COLOR.is("Custom"));
    public static final BooleanSetting SPIRAL=new BooleanSetting("Спираль",true);
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true);
    public static final SliderSetting GLOW_POWER=new SliderSetting("Сила свечения",.2f,1.6f,.1f,.7f).visibleWhen(GLOW::get);
    public static final Setting[] SETTINGS={COLOR,OWN,SPIRAL,GLOW,GLOW_POWER};
    private static final Minecraft MC=Minecraft.getInstance();
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static final PoseStack POSE=new PoseStack();
    private static final List<Ghost> GHOSTS=new ArrayList<>();
    private static final HitGhost INSTANCE=new HitGhost();
    private static PlayerModel model;
    private static Object modelSource;
    private static boolean registered;
    static { GlowFx.add(HitGhost::paint); }
    private HitGhost() {}

    private record Ghost(double x,double y,double z,float yaw,float pitch,float widthScale,float heightScale,
                         float motion,float phase,double driftX,double driftZ,boolean critical,Identifier skin,
                         int poseKind,float mirror,int bodyPose,long born) {}
    private record GhostPose(float x,float y,float z,double worldX,double worldY,double worldZ,
                             float scale,float yawSway,float lean,float roll) {}

    public static void initialize() {
        if(!registered) { registered=true; EventBus.register(INSTANCE); }
    }
    public static void tick(Minecraft mc) {
        if(!enabled() || mc.level==null) { GHOSTS.clear(); return; }
        long now=System.currentTimeMillis();
        GHOSTS.removeIf(g->now-g.born>=895);
    }
    public static boolean enabled() { return ClientModules.HIT_GHOST!=null && ClientModules.HIT_GHOST.enabled(); }
    @Subscribe private void attack(AttackEntityEvent event) {
        if(!enabled() || event.getPhase()!=AttackEntityEvent.Phase.POST || MC.player==null || MC.level==null ||
                !(event.getEntity() instanceof AbstractClientPlayer target)) return;
        ThreadLocalRandom random=ThreadLocalRandom.current();
        Vec3 pos=target.position(),velocity=target.getDeltaMovement();
        float speed=(float)Math.sqrt(velocity.x*velocity.x+velocity.z*velocity.z);
        float yaw=target.getYRot(),pitch=target.getXRot();
        float motion=Math.clamp(speed*2.8f+.16f,.18f,.9f);
        float phase=random.nextFloat()*(float)(Math.PI*2);
        double driftAngle=Math.toRadians(yaw+90+random.nextFloat(-12,12));
        double drift=random.nextDouble(.12,.2);
        boolean critical=MC.player.fallDistance>0 && !MC.player.onGround() && !MC.player.isInWater()
                && !MC.player.isPassenger() && !MC.player.isSprinting();
        int bodyPose=target.isFallFlying()?3:target.isVisuallySwimming()?2:target.isCrouching()?1:0;
        GHOSTS.add(new Ghost(pos.x,pos.y,pos.z,yaw,pitch,1,1,motion,phase,
                Math.cos(driftAngle)*drift,Math.sin(driftAngle)*drift,critical,target.getSkin().body().texturePath(),
                random.nextInt(5),random.nextBoolean()?1:-1,bodyPose,System.currentTimeMillis()));
        while(GHOSTS.size()>12) GHOSTS.removeFirst();
    }
    private static void paint(GlowFx fx) {
        if(!enabled() || GHOSTS.isEmpty() || MC.level==null) return;
        if(model==null || modelSource!=MC.getEntityModels()) {
            modelSource=MC.getEntityModels();
            model=new PlayerModel(MC.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
        }
        long now=System.currentTimeMillis();
        int color=COLOR.is("White")?0xfff2f5ff:COLOR.is("Custom")?OWN.get():Theme.accent();
        for(Ghost ghost:GHOSTS) {
            float progress=Math.clamp((now-ghost.born)/895f,0,1);
            float fade=disappearFade(progress),alpha=.82f*fade;
            if(alpha<=.01f || fx.eye.distanceToSqr(ghost.x,ghost.y,ghost.z)>4096) continue;
            GhostPose pose=pose(ghost,now,fx.eye,progress);
            float glowStrength=GLOW.get()?alpha*GLOW_POWER.get():0;
            if(GLOW.get()) envelope(fx,ghost,pose,now,color,glowStrength);
            if(SPIRAL.get()) spiral(fx,ghost,pose,now,color,alpha);
            BATCH.begin(); renderModel(ghost,pose,now,alpha,color);
            BATCH.flush(ghost.skin,glowStrength,false,false);
        }
    }
    private static void renderModel(Ghost ghost,GhostPose pose,long now,float alpha,int color) {
        POSE.pushPose();
        POSE.translate(pose.x,pose.y,pose.z);
        POSE.scale(pose.scale*ghost.widthScale,pose.scale*ghost.heightScale,pose.scale*ghost.widthScale);
        rotateY((float)Math.toRadians(180-ghost.yaw+pose.yawSway));
        if(ghost.bodyPose==2 || ghost.bodyPose==3) {
            rotateX((float)Math.toRadians(-90-ghost.pitch));
            POSE.translate(0,-1,.3f);
        }
        rotateX((float)Math.toRadians(pose.lean)); rotateZ((float)Math.toRadians(pose.roll));
        if(ghost.bodyPose==1) POSE.scale(1,.8333334f,1);
        POSE.scale(-1,-1,1); POSE.scale(.9375f,.9375f,.9375f); POSE.translate(0,-1.501f,0);
        animateModel(ghost,now);
        int r=Math.min(255,(int)((color>>16&255)*.45f+140));
        int g=Math.min(255,(int)((color>>8&255)*.45f+140));
        int b=Math.min(255,(int)((color&255)*.45f+140));
        int tint=Math.clamp((int)(alpha*255),0,255)<<24|r<<16|g<<8|b;
        model.head.render(POSE,BATCH,0xF000F0,0,tint);
        model.body.render(POSE,BATCH,0xF000F0,0,tint);
        model.rightArm.render(POSE,BATCH,0xF000F0,0,tint);
        model.leftArm.render(POSE,BATCH,0xF000F0,0,tint);
        model.rightLeg.render(POSE,BATCH,0xF000F0,0,tint);
        model.leftLeg.render(POSE,BATCH,0xF000F0,0,tint);
        POSE.popPose();
    }
    private static void animateModel(Ghost ghost,long now) {
        float time=(now-ghost.born)/1000f,progress=Math.clamp((now-ghost.born)/895f,0,1);
        float strike=ghost.poseKind==0?0:smooth(progress/.22f),m=ghost.mirror,walkFade=1-strike*.85f;
        float walk=ghost.phase+time*(1.35f+ghost.motion*1.05f);
        float armSwing=(float)Math.sin(walk)*(.46f+ghost.motion*.14f)*walkFade;
        float legSwing=(float)Math.sin(walk)*(.52f+ghost.motion*.16f)*walkFade;
        float bodyTwist=(float)Math.sin(ghost.phase*.8f+time*.86f)*.08f;
        float bodyRoll=(float)Math.cos(ghost.phase*1.1f+time*.72f)*.05f;
        float headYaw=(float)Math.sin(ghost.phase*1.25f+time*.95f)*.07f;
        float idle=(float)Math.sin(ghost.phase*.65f+time*.8f)*.035f;
        float breathe=(float)Math.sin(ghost.phase+time*2.2f)*.05f;
        model.hat.visible=false; model.leftSleeve.visible=false; model.rightSleeve.visible=false;
        model.leftPants.visible=false; model.rightPants.visible=false; model.jacket.visible=false;
        float bodyX=0,bodyY=bodyTwist,bodyZ=bodyRoll;
        float headX=Math.clamp(ghost.pitch,-45,45)*.0105f+idle,headY=headYaw;
        float headZ=(float)Math.cos(ghost.phase*1.15f+time*.88f)*.03f;
        float raX=-armSwing-idle*1.2f,laX=armSwing+idle*1.2f;
        float raY=bodyTwist*.8f,laY=bodyTwist*.8f;
        float raZ=.12f+bodyRoll*.8f,laZ=-.12f-bodyRoll*.8f;
        float rlX=legSwing,llX=-legSwing,rlY=-bodyTwist*.45f,llY=bodyTwist*.45f;
        float rlZ=-.025f,llZ=.025f;
        switch (ghost.poseKind) {
            case 1 -> {
                raX=mix(raX,-2.85f+breathe,strike); laX=mix(laX,-2.95f-breathe,strike);
                raZ=mix(raZ,.26f,strike); laZ=mix(laZ,-.26f,strike);
                headX=mix(headX,-.5f+idle,strike);
                rlX=mix(rlX,m>0?.24f:-.12f,strike); llX=mix(llX,m>0?-.12f:.24f,strike);
            }
            case 2 -> {
                raX=mix(raX,-1.78f+breathe*.6f,strike); laX=mix(laX,-1.64f-breathe*.6f,strike);
                raZ=mix(raZ,-.5f,strike); laZ=mix(laZ,.5f,strike);
                headX=mix(headX,.52f,strike); bodyX=mix(bodyX,.1f,strike);
                rlX=mix(rlX,.2f,strike); llX=mix(llX,-.06f,strike);
                rlY=mix(rlY,.16f,strike); llY=mix(llY,-.16f,strike);
            }
            case 3 -> {
                raX=mix(raX,.55f,strike); laX=mix(laX,.62f,strike);
                raZ=mix(raZ,1.12f+breathe,strike); laZ=mix(laZ,-1.2f-breathe,strike);
                headX=mix(headX,-.42f,strike);
                rlX=mix(rlX,m>0?-.55f:-.22f,strike); llX=mix(llX,m>0?-.22f:-.55f,strike);
            }
            case 4 -> {
                bodyY=mix(bodyY,.55f*m,strike); headY=mix(headY,.48f*m,strike); headZ=mix(headZ,.1f*m,strike);
                if(m>0) { raX=mix(raX,-2.35f+breathe,strike); raZ=mix(raZ,.34f,strike);
                    laX=mix(laX,.5f,strike); laZ=mix(laZ,-.42f,strike); }
                else { laX=mix(laX,-2.35f+breathe,strike); laZ=mix(laZ,-.34f,strike);
                    raX=mix(raX,.5f,strike); raZ=mix(raZ,.42f,strike); }
                rlX=mix(rlX,.3f,strike); llX=mix(llX,-.32f,strike);
                rlY=mix(rlY,.26f*m,strike); llY=mix(llY,.26f*m,strike);
            }
        }
        if(ghost.bodyPose==2 || ghost.bodyPose==3) {
            float flutter=(float)Math.sin(walk*1.2f)*(.08f+ghost.motion*.06f);
            float spread=ghost.bodyPose==3?.1f:.22f;
            bodyX=bodyY=0; bodyZ=bodyRoll*.4f; headX=idle*.6f; headY=headZ=0;
            raX=.16f+flutter; laX=.16f-flutter; raY=laY=0; raZ=spread; laZ=-spread;
            rlX=-flutter; llX=flutter; rlY=llY=0; rlZ=-.04f; llZ=.04f;
        } else if(ghost.bodyPose==1) { bodyX=.5f; raX-=.628f; laX-=.628f; }
        model.body.xRot=bodyX; model.body.yRot=bodyY; model.body.zRot=bodyZ;
        model.head.xRot=headX; model.head.yRot=headY; model.head.zRot=headZ;
        model.rightArm.xRot=raX; model.leftArm.xRot=laX;
        model.rightArm.yRot=raY; model.leftArm.yRot=laY;
        model.rightArm.zRot=raZ; model.leftArm.zRot=laZ;
        model.rightLeg.xRot=rlX; model.leftLeg.xRot=llX;
        model.rightLeg.yRot=rlY; model.leftLeg.yRot=llY;
        model.rightLeg.zRot=rlZ; model.leftLeg.zRot=llZ;
    }
    private static GhostPose pose(Ghost ghost,long now,Vec3 camera,float progress) {
        float lift=easeOutCubic(progress)*2,side=progress*progress,dissolve=1-disappearFade(progress);
        float scale=.985f*(.985f+progress*.03f+dissolve*.1f),time=(now-ghost.born)/1000f;
        float bob=(float)Math.sin(ghost.phase+time*1.5f)*.012f*(.65f+ghost.motion*.35f);
        float yawSway=(float)Math.sin(ghost.phase*.7f+time*.92f)*(1.3f+ghost.motion*1.5f);
        float lean=(float)Math.sin(ghost.phase+time*1.32f)*(1.2f+ghost.motion*1.7f);
        float roll=(float)Math.cos(ghost.phase*1.1f+time*1.06f)*(.85f+ghost.motion*1.15f);
        float strike=ghost.poseKind==0?0:smooth(progress/.22f);
        lean+=strike*switch (ghost.poseKind) { case 1 -> -5f; case 2 -> 7.5f; case 3 -> -14f; case 4 -> -2f; default -> 0f; };
        if(ghost.poseKind==4) roll+=7*ghost.mirror*strike;
        double worldX=ghost.x+ghost.driftX*side,worldY=ghost.y+lift+bob+dissolve*.08f,worldZ=ghost.z+ghost.driftZ*side;
        return new GhostPose((float)(worldX-camera.x),(float)(worldY-camera.y),(float)(worldZ-camera.z),
                worldX,worldY,worldZ,scale,yawSway,lean,roll);
    }
    private static void envelope(GlowFx fx,Ghost ghost,GhostPose pose,long now,int color,float intensity) {
        float time=(now-ghost.born)/1000f;
        float pulse=.85f+.15f*(float)Math.sin(ghost.phase+time*4.5f);
        float boost=intensity*pulse*(ghost.critical?1.15f:1);
        int bright=lift(color,.45f);
        float bodyHeight=1.9f*ghost.heightScale*pose.scale;
        float bodyWidth=ghost.widthScale*pose.scale;
        double bx=pose.worldX,bz=pose.worldZ;
        if(ghost.bodyPose==2 || ghost.bodyPose==3) {
            double yawRad=Math.toRadians(ghost.yaw),half=bodyHeight*.35;
            double ux=-Math.sin(yawRad),uz=Math.cos(yawRad),ly=pose.worldY+.3;
            fx.line(new Vec3(bx-ux*half,ly,bz-uz*half),new Vec3(bx+ux*half,ly,bz+uz*half),.17f*bodyWidth,color,boost*.07f,boost*.07f,.8f);
            fx.line(new Vec3(bx-ux*half,ly,bz-uz*half),new Vec3(bx+ux*half,ly,bz+uz*half),.09f*bodyWidth,bright,boost*.11f,boost*.11f,.9f);
            fx.sprite(new Vec3(bx,ly+.06,bz),.12f*bodyWidth,GlowFx.DOT,bright,boost*.13f,.8f,0);
            return;
        }
        double y0=pose.worldY+bodyHeight*.28,y1=pose.worldY+bodyHeight*.8;
        fx.line(new Vec3(bx,y0,bz),new Vec3(bx,y1,bz),.17f*bodyWidth,color,boost*.07f,boost*.07f,.8f);
        fx.line(new Vec3(bx,y0,bz),new Vec3(bx,y1,bz),.09f*bodyWidth,bright,boost*.11f,boost*.11f,.9f);
        fx.sprite(new Vec3(bx,pose.worldY+bodyHeight*.55,bz),.12f*bodyWidth,GlowFx.DOT,bright,boost*.13f,.8f,0);
    }
    private static void spiral(GlowFx fx,Ghost ghost,GhostPose pose,long now,int color,float alpha) {
        int segments=60; float turns=2.6f;
        float radiusBase=.66f*ghost.widthScale*pose.scale,height=2.35f*ghost.heightScale*pose.scale;
        float time=(now-ghost.born)/1000f,spin=time*(1.2f+ghost.motion*.85f)+ghost.phase;
        int bright=lift(color,.5f);
        for(int strand=0;strand<2;strand++) {
            Vec3 previous=null;
            for(int i=0;i<=segments;i++) {
                float pc=i/(float)segments,angle=spin+strand*(float)Math.PI+pc*(float)(Math.PI*2)*turns;
                float wave=.78f+.22f*(float)Math.sin(ghost.phase+pc*6+time*1.7f);
                float radius=radiusBase*wave;
                Vec3 point=new Vec3(pose.worldX+Math.cos(angle)*radius,pose.worldY+pc*height-.1,
                        pose.worldZ+Math.sin(angle)*radius);
                if(previous!=null) fx.line(previous,point,.02f,bright,alpha*(.3f-pc*.08f),alpha*(.3f-pc*.08f),.8f);
                previous=point;
            }
        }
    }
    private static void rotateX(float radians) { POSE.mulPose(new Matrix4f().rotation(new Quaternionf().rotationX(radians))); }
    private static void rotateY(float radians) { POSE.mulPose(new Matrix4f().rotation(new Quaternionf().rotationY(radians))); }
    private static void rotateZ(float radians) { POSE.mulPose(new Matrix4f().rotation(new Quaternionf().rotationZ(radians))); }
    private static float mix(float a,float b,float t) { return a+(b-a)*t; }
    private static float smooth(float v) { float p=Math.clamp(v,0,1); return p*p*(3-2*p); }
    private static float disappearFade(float progress) { return smooth(progress/.08f)*(1-Math.clamp((progress-.12f)/.88f,0,1)); }
    private static float easeOutCubic(float v) { float p=1-Math.clamp(v,0,1); return 1-p*p*p; }
    private static int lift(int c,float t) {
        int r=(int)((c>>16&255)+(255-(c>>16&255))*t),g=(int)((c>>8&255)+(255-(c>>8&255))*t);
        int b=(int)((c&255)+(255-(c&255))*t); return r<<16|g<<8|b;
    }
}
