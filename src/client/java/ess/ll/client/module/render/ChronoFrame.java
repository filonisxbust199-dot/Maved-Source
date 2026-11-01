package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.render.GhostModelBatch;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A complete interpolable avatar pose sampled from Minecraft's own render state. */
final class ChronoFrame {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final PoseStack POSE=new PoseStack();
    private static PlayerModel wide,slim;
    final long time;
    final double x,y,z;
    final float yaw,pitch,scale,deathTime,age,flightScale,flyingYRot,swimAmount;
    final boolean flying,autoSpin,applyFlyingYaw,inWater,visuallySwimming,slimArms;
    final Identifier skin;
    final PartPose[] parts;
    final boolean[] visible;
    final List<ChronoArmor.Piece> armor;

    private ChronoFrame(long time,double x,double y,double z,float yaw,float pitch,float scale,float deathTime,
                        float age,float flightScale,float flyingYRot,float swimAmount,boolean flying,boolean autoSpin,
                        boolean applyFlyingYaw,boolean inWater,boolean visuallySwimming,boolean slimArms,
                        Identifier skin,PartPose[] parts,boolean[] visible,List<ChronoArmor.Piece> armor) {
        this.time=time; this.x=x; this.y=y; this.z=z; this.yaw=yaw; this.pitch=pitch; this.scale=scale;
        this.deathTime=deathTime; this.age=age; this.flightScale=flightScale; this.flyingYRot=flyingYRot;
        this.swimAmount=swimAmount; this.flying=flying; this.autoSpin=autoSpin;
        this.applyFlyingYaw=applyFlyingYaw; this.inWater=inWater; this.visuallySwimming=visuallySwimming;
        this.slimArms=slimArms; this.skin=skin; this.parts=parts; this.visible=visible; this.armor=armor;
    }
    static ChronoFrame capture(AbstractClientPlayer player,float partial,long time,boolean captureArmor) {
        if(!(MC.getEntityRenderDispatcher().extractEntity(player,partial) instanceof AvatarRenderState state) || state.skin==null) return null;
        boolean slimArms=state.skin.model()==PlayerModelType.SLIM;
        PlayerModel model=model(slimArms);
        model.setupAnim(state);
        ModelPart[] modelParts=parts(model);
        PartPose[] poses=new PartPose[modelParts.length]; boolean[] visible=new boolean[modelParts.length];
        for(int i=0;i<modelParts.length;i++) { poses[i]=modelParts[i].storePose(); visible[i]=modelParts[i].visible; }
        return new ChronoFrame(time,lerp(player.xOld,player.getX(),partial),lerp(player.yOld,player.getY(),partial),
                lerp(player.zOld,player.getZ(),partial),state.bodyRot,state.xRot,state.scale,state.deathTime,state.ageInTicks,
                state.fallFlyingScale(),state.flyingYRot,state.swimAmount,state.isFallFlying,state.isAutoSpinAttack,
                state.shouldApplyFlyingYRot,state.isInWater,state.isVisuallySwimming,slimArms,state.skin.body().texturePath(),poses,visible,
                captureArmor?ChronoArmor.capture(state):List.of());
    }
    static ChronoFrame at(ArrayDeque<ChronoFrame> history,long target) {
        ChronoFrame previous=null;
        for(ChronoFrame next:history) {
            if(next.time>target) {
                if(previous==null) return null;
                if(previous.distanceSq(next)>64) return previous;
                return interpolate(previous,next,(target-previous.time)/(float)Math.max(1,next.time-previous.time));
            }
            previous=next;
        }
        return previous;
    }
    private static ChronoFrame interpolate(ChronoFrame a,ChronoFrame b,float t) {
        t=Math.clamp(t,0,1);
        if(a.slimArms!=b.slimArms || !a.skin.equals(b.skin)) return t<.5f?a:b;
        ChronoFrame discrete=t<.5f?a:b;
        PartPose[] poses=new PartPose[a.parts.length];
        for(int i=0;i<poses.length;i++) {
            PartPose p=a.parts[i],q=b.parts[i];
            poses[i]=new PartPose(mix(p.x(),q.x(),t),mix(p.y(),q.y(),t),mix(p.z(),q.z(),t),
                    rotLerpRad(p.xRot(),q.xRot(),t),rotLerpRad(p.yRot(),q.yRot(),t),rotLerpRad(p.zRot(),q.zRot(),t),
                    mix(p.xScale(),q.xScale(),t),mix(p.yScale(),q.yScale(),t),mix(p.zScale(),q.zScale(),t));
        }
        return new ChronoFrame(a.time+(long)((b.time-a.time)*t),lerp(a.x,b.x,t),lerp(a.y,b.y,t),lerp(a.z,b.z,t),
                rotLerpDeg(a.yaw,b.yaw,t),rotLerpDeg(a.pitch,b.pitch,t),mix(a.scale,b.scale,t),mix(a.deathTime,b.deathTime,t),
                mix(a.age,b.age,t),mix(a.flightScale,b.flightScale,t),rotLerpRad(a.flyingYRot,b.flyingYRot,t),
                mix(a.swimAmount,b.swimAmount,t),discrete.flying,discrete.autoSpin,discrete.applyFlyingYaw,
                discrete.inWater,discrete.visuallySwimming,discrete.slimArms,discrete.skin,poses,discrete.visible,discrete.armor);
    }
    double distanceSq(ChronoFrame other) {
        double dx=x-other.x,dy=y-other.y,dz=z-other.z; return dx*dx+dy*dy+dz*dz;
    }
    void emit(Vec3 camera,int tint,boolean soul,GhostModelBatch batch) {
        PlayerModel model=model(slimArms);
        ModelPart[] modelParts=parts(model);
        for(int i=0;i<modelParts.length;i++) { modelParts[i].loadPose(parts[i]); modelParts[i].visible=visible[i] && (!soul || i<6); }
        POSE.pushPose(); applyTransform(POSE,x-camera.x,y-camera.y,z-camera.z,1);
        for(int i=0;i<6;i++) if(modelParts[i].visible) modelParts[i].render(POSE,batch,0xF000F0,0,tint);
        POSE.popPose();
    }
    void applyTransform(PoseStack pose,double dx,double dy,double dz,float expansion) {
        pose.translate(dx,dy,dz); pose.scale(scale,scale,scale);
        rotate(pose,new Quaternionf().rotationY((float)Math.toRadians(180-yaw)));
        if(deathTime>0) {
            float fall=Math.clamp((float)Math.sqrt((deathTime-1)/20*1.6f),0,1);
            rotate(pose,new Quaternionf().rotationZ((float)Math.toRadians(fall*90)));
        } else if(autoSpin) {
            rotate(pose,new Quaternionf().rotationX((float)Math.toRadians(-90-pitch)));
            rotate(pose,new Quaternionf().rotationY((float)Math.toRadians(age*-75)));
        } else if(flying) {
            rotate(pose,new Quaternionf().rotationX((float)Math.toRadians(flightScale*(-90-pitch))));
            if(applyFlyingYaw) rotate(pose,new Quaternionf().rotationY(flyingYRot));
        } else if(swimAmount>0) {
            rotate(pose,new Quaternionf().rotationX((float)Math.toRadians(swimAmount*(inWater?-90-pitch:-90))));
            if(visuallySwimming) pose.translate(0,-1,.3);
        }
        pose.scale(-expansion,-expansion,expansion); pose.scale(.9375f,.9375f,.9375f); pose.translate(0,-1.501f,0);
    }
    Vec3 anchor(ModelPart part,float localX,float localY,float localZ) {
        PlayerModel model=model(slimArms); ModelPart[] modelParts=parts(model);
        for(int i=0;i<6;i++) modelParts[i].loadPose(parts[i]);
        PoseStack pose=new PoseStack(); pose.pushPose(); applyTransform(pose,x,y,z,1);
        pose.pushPose(); part.translateAndRotate(pose);
        Vector3f point=pose.last().pose().transformPosition(localX,localY,localZ,new Vector3f());
        pose.popPose(); pose.popPose(); return new Vec3(point.x,point.y,point.z);
    }
    static PlayerModel model(boolean slimArms) {
        if(slimArms) {
            if(slim==null) slim=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,true),64,64).bakeRoot(),true);
            return slim;
        }
        if(wide==null) wide=new PlayerModel(MC.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
        return wide;
    }
    static ModelPart[] parts(PlayerModel model) {
        return new ModelPart[]{model.head,model.body,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg,
                model.hat,model.jacket,model.rightSleeve,model.leftSleeve,model.rightPants,model.leftPants};
    }
    private static void rotate(PoseStack pose,Quaternionf q) { pose.mulPose(new Matrix4f().rotation(q)); }
    private static float mix(float a,float b,float t) { return a+(b-a)*t; }
    private static double lerp(double a,double b,float t) { return a+(b-a)*t; }
    private static float rotLerpRad(float a,float b,float t) { return a+(float)Math.atan2(Math.sin(b-a),Math.cos(b-a))*t; }
    private static float rotLerpDeg(float a,float b,float t) { return a+(float)Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(b-a)),Math.cos(Math.toRadians(b-a))))*t; }
}
