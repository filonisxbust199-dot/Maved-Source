package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.wardrobe.Wardrobe.Category;
import ess.ll.client.wardrobe.Wardrobe.Item;
import ess.ll.client.wardrobe.Wardrobe.Slot;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Draws the wardrobe on your player (from Zenith's WardrobeFeatureRenderer): every worn clothing piece, cape,
 * accessory and character skin on its body parts, animated by its clips and drivers. A character skin replaces the
 * player (only accessories stay over it). Clothing moves outward over worn armour.
 */
public final class WardrobeLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    /** Render state id of the wardrobe preview in the menu: drawn as you, with the hovered item tried on. */
    public static final int PREVIEW_ID=Integer.MIN_VALUE+11;

    public WardrobeLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent) { super(parent); }

    /** Whether this render state is you (in the world or in a preview). */
    public static boolean mine(AvatarRenderState state) {
        if(state.id==PREVIEW_ID) return true;
        var player=Minecraft.getInstance().player;
        return player!=null && state.id==player.getId() && ess.ll.client.module.ClientModules.WARDROBE!=null && ess.ll.client.module.ClientModules.WARDROBE.enabled();
    }
    public static List<Item> outfit(AvatarRenderState state) {
        return mine(state) ? Wardrobe.outfit(state.id==PREVIEW_ID) : List.of();
    }
    /** The character skin worn by this player, if its model is loaded. */
    public static WardrobeModel character(AvatarRenderState state) {
        Item item=Wardrobe.find(outfit(state),Slot.CHARACTER);
        return item==null ? null : Wardrobe.model(item);
    }
    /** Armour is hidden under character skins (unless allowed in the settings or by the skin itself). */
    public static boolean hidesArmor(AvatarRenderState state) {
        if(Wardrobe.ARMOR_ON_SKINS.get()) return false;
        WardrobeModel skin=character(state);
        return skin!=null && (skin.skin==null || skin.skin.hideArmor());
    }
    /** The elytra would cover a cape or a back item (or a skin), so it hides unless you are gliding. */
    public static boolean hidesElytra(AvatarRenderState state) {
        if(Wardrobe.ARMOR_ON_SKINS.get() || state.isFallFlying) return false;
        List<Item> outfit=outfit(state);
        return Wardrobe.find(outfit,Slot.BACK)!=null || Wardrobe.find(outfit,Slot.CAPE)!=null || hidesArmor(state);
    }
    /** Hides the vanilla skin layers the worn items replace. */
    public static void hideParts(AvatarRenderState state) {
        List<Item> outfit=outfit(state);
        if(outfit.isEmpty()) return;
        if(Wardrobe.find(outfit,Slot.CHARACTER)!=null) { state.showCape=false; return; }
        for(Item item:outfit) {
            Category c=item.category();
            if(c!=Category.CLOTHING && c!=Category.CAPE && c!=Category.ACCESSORY) continue;
            WardrobeModel model=Wardrobe.model(item);
            if(model==null) continue;
            int hides=model.hides;
            if((hides&WardrobeModel.HIDE_HAT)!=0) state.showHat=false;
            if((hides&WardrobeModel.HIDE_JACKET)!=0) state.showJacket=false;
            if((hides&WardrobeModel.HIDE_SLEEVES)!=0) { state.showLeftSleeve=false; state.showRightSleeve=false; }
            if((hides&WardrobeModel.HIDE_PANTS)!=0) { state.showLeftPants=false; state.showRightPants=false; }
            if((hides&WardrobeModel.HIDE_CAPE)!=0 || item.slot()==Slot.BACK || item.slot()==Slot.CAPE) state.showCape=false;
        }
    }
    /**
     * A character skin: the vanilla body parts are hidden (the skin draws the body) and, standing still, the
     * parts ease into the skin's idle pose. Returns true while a skin is worn.
     */
    public static boolean poseCharacter(PlayerModel model,AvatarRenderState state) {
        WardrobeModel skin=character(state);
        if(skin==null) return false;
        if(skin.skin!=null && skin.skin.idlePose()!=null) {
            float still=1-Math.min(1,state.walkAnimationSpeed*2.5f);
            if(state.swingAnimation>0 || state.isUsingItem || state.isFallFlying || state.isVisuallySwimming || state.isPassenger || state.isCrouching) still=0;
            if(still>0) {
                float[] pose=skin.skin.idlePose();
                ModelPart[] parts={model.head,model.body,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};
                for(int p=0;p<parts.length;p++) {
                    float x=pose[p*3],y=pose[p*3+1],z=pose[p*3+2];
                    ModelPart part=parts[p];
                    if(p==0) { part.xRot+=x*still; part.yRot+=y*still; part.zRot+=z*still; }
                    else if(x!=0 || y!=0 || z!=0) {
                        part.xRot+=(x-part.xRot)*still; part.yRot+=(y-part.yRot)*still; part.zRot+=(z-part.zRot)*still;
                    }
                }
            }
        }
        return true;
    }

    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,AvatarRenderState state,float yRot,float xRot) {
        if(state.isInvisible) return;
        List<Item> outfit=outfit(state);
        if(outfit.isEmpty()) return;
        PlayerModel model=getParentModel();
        int overlay=LivingEntityRenderer.getOverlayCoords(state,0);
        Item character=Wardrobe.find(outfit,Slot.CHARACTER);
        long now=System.nanoTime();
        Wardrobe.OutfitSet set=character==null ? Wardrobe.fullSet(outfit) : null;
        for(Item item:outfit) {
            Category c=item.category();
            if(c==Category.PET || item.slot().held()) continue;
            // Over a character skin only accessories are drawn.
            if(character!=null && item!=character && c!=Category.ACCESSORY) continue;
            WardrobeModel m=Wardrobe.model(item);
            if(m==null) continue;
            WardrobeAnimator.Cache cache=WardrobeAnimator.Cache.of(state.id,item.id(),state.id==PREVIEW_ID);
            float dt=cache.frame(now);
            float[] bones=animate(m,cache,state,model,dt);
            if(item==character && !cache.clips.playingOneShot()) cache.pose.restRoots(m);
            boolean detail=state.distanceToCameraSq<m.lod*m.lod;
            for(int g=0;g<6;g++) {
                if(!m.hasGroup(g)) continue;
                pose.pushPose();
                model.root().translateAndRotate(pose);
                part(model,g).translateAndRotate(pose);
                if(item!=character) overArmor(pose,state,g,c);
                WardrobeRenderer.draw(m,bones,g,pose,collector,light,overlay,detail,item.rarity().glows,-1,false);
                pose.popPose();
            }
            WardrobeEffects.emit(m,cache,state,model,bones,dt,cache.motion.moving,cache.motion.airborne,set!=null);
        }
        if(set!=null) WardrobeEffects.setBonus(set,state,now);
    }

    /**
     * First-person arm. With skin=true it draws a character skin's arm instead of yours (true when it did); with
     * skin=false it draws the clothing and accessories that sit on that arm, unless a skin is worn.
     */
    public static boolean firstPersonArm(PlayerModel model,ModelPart arm,PoseStack pose,SubmitNodeCollector collector,int light,boolean skin) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || ess.ll.client.module.ClientModules.WARDROBE==null || !ess.ll.client.module.ClientModules.WARDROBE.enabled()) return false;
        List<Item> outfit=Wardrobe.outfit(false);
        Item character=Wardrobe.find(outfit,Slot.CHARACTER);
        int group=arm==model.rightArm ? WardrobeModel.Anchor.RIGHT_ARM.ordinal() : WardrobeModel.Anchor.LEFT_ARM.ordinal();
        float time=(mc.player.tickCount+mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))/20;
        if(skin) {
            WardrobeModel m=character==null ? null : Wardrobe.model(character);
            if(m==null || !m.hasGroup(group)) return false;
            arm.resetPose();
            arm.visible=true;
            model.leftArm.zRot=-.1f;
            model.rightArm.zRot=.1f;
            drawArm(character,m,arm,group,pose,collector,light,time,true);
            for(Item item:outfit) if(item.category()==Category.ACCESSORY) {
                WardrobeModel a=Wardrobe.model(item);
                if(a!=null && a.hasGroup(group)) drawArm(item,a,arm,group,pose,collector,light,time,false);
            }
            return true;
        }
        if(character!=null) return false;
        for(Item item:outfit) {
            Category c=item.category();
            if(c!=Category.CLOTHING && c!=Category.ACCESSORY && c!=Category.CAPE) continue;
            WardrobeModel m=Wardrobe.model(item);
            if(m!=null && m.hasGroup(group)) drawArm(item,m,arm,group,pose,collector,light,time,false);
        }
        return false;
    }
    private static void drawArm(Item item,WardrobeModel m,ModelPart arm,int group,PoseStack pose,SubmitNodeCollector collector,int light,float time,boolean skin) {
        var cache=WardrobeAnimator.Cache.of(Minecraft.getInstance().player.getId(),item.id()+":hand",false);
        cache.clips.want(m,"idle",time);
        float[] bones=cache.pose.compute(m,cache.clips,time,null);
        if(skin) cache.pose.restRoots(m);
        pose.pushPose();
        try {
            arm.translateAndRotate(pose);
            WardrobeRenderer.draw(m,bones,group,pose,collector,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,true,item.rarity().glows,-1,false);
        } finally { pose.popPose(); }
    }

    static ModelPart part(PlayerModel model,int group) {
        return switch (group) {
            case 0 -> model.head;
            case 1 -> model.body;
            case 2 -> model.rightArm;
            case 3 -> model.leftArm;
            case 4 -> model.rightLeg;
            default -> model.leftLeg;
        };
    }
    /** Motion of the wearer into the drivers, the state clip (idle / walk / run / sneak / fly / glide), the pose. */
    private static float[] animate(WardrobeModel m,WardrobeAnimator.Cache cache,AvatarRenderState state,PlayerModel model,float dt) {
        WardrobeAnimator.Motion motion=cache.motion;
        motion.capeFlap=state.capeFlap; motion.capeLean=state.capeLean; motion.capeLean2=state.capeLean2;
        motion.leftLegPitch=model.leftLeg.xRot; motion.rightLegPitch=model.rightLeg.xRot;
        motion.walkSpeed=state.walkAnimationSpeed; motion.dt=dt;
        motion.sneaking=state.isCrouching;
        motion.moving=state.walkAnimationSpeed>.15f;
        motion.airborne=airborne(state);
        float body=state.bodyRot;
        if(!Float.isNaN(cache.lastBodyYaw) && dt>0) {
            float d=body-cache.lastBodyYaw;
            while(d>180) d-=360;
            while(d<-180) d+=360;
            motion.yawSpeed=d/dt;
        } else motion.yawSpeed=0;
        cache.lastBodyYaw=body;
        float time=state.ageInTicks/20;
        String clip=state.isFallFlying ? "glide" : motion.airborne ? "fly" : state.isCrouching ? "sneak"
                : state.walkAnimationSpeed>.6f ? "run" : motion.moving ? "walk" : "idle";
        cache.clips.want(m,clip,time);
        cache.clips.speed(!clip.equals("walk") && !clip.equals("run") ? 1 : .6f+Math.min(1.2f,state.walkAnimationSpeed));
        return cache.pose.compute(m,cache.clips,time,motion);
    }
    private static boolean airborne(AvatarRenderState state) {
        if(state.isFallFlying) return true;
        var level=Minecraft.getInstance().level;
        if(level==null || state.id==PREVIEW_ID) return false;
        var entity=level.getEntity(state.id);
        return entity!=null && !entity.onGround() && !entity.isPassenger() && !entity.isInWater();
    }
    /**
     * Worn armour adds a shell around a body part, so clothing on that part grows outward by the same amount
     * (helmet: 1 px, chestplate: 1 px on the torso and arms, leggings / boots: about 1 px on the legs).
     */
    private static void overArmor(PoseStack pose,AvatarRenderState state,int group,Category category) {
        if(!Wardrobe.CLOTHES_OVER_ARMOR.get() || category==Category.CHARACTER) return;
        boolean head=!state.headEquipment.isEmpty(),chest=!state.chestEquipment.isEmpty() && !state.chestEquipment.has(DataComponents.GLIDER);
        boolean legs=!state.legsEquipment.isEmpty(),feet=!state.feetEquipment.isEmpty();
        boolean slim=state.skin!=null && state.skin.model()==PlayerModelType.SLIM;
        switch (group) {
            case 0 -> { if(head) scaleAbout(pose,0,-4,0,1.25f,1.25f,1.25f); }
            case 1 -> { if(chest) scaleAbout(pose,0,6,0,1.25f,1.08f,1.5f); else if(legs) scaleAbout(pose,0,6,0,1.12f,1.04f,1.25f); }
            case 2,3 -> { if(chest) scaleAbout(pose,group==2 ? -1+(slim ? .5f : 0) : 1-(slim ? .5f : 0),4,0,1.5f,1.1f,1.5f); }
            default -> { if(feet) scaleAbout(pose,0,6,0,1.45f,1.08f,1.45f); else if(legs) scaleAbout(pose,0,6,0,1.2f,1.04f,1.2f); }
        }
    }
    private static void scaleAbout(PoseStack pose,float x,float y,float z,float sx,float sy,float sz) {
        pose.translate(x/16,y/16,z/16);
        pose.scale(sx,sy,sz);
        pose.translate(-x/16,-y/16,-z/16);
    }
}
