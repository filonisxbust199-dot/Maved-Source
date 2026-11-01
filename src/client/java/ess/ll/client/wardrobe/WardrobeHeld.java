package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.mixin.ItemStackRenderStateAccessor;
import ess.ll.client.mixin.LayerRenderStateAccessor;
import ess.ll.client.wardrobe.Wardrobe.Category;
import ess.ll.client.wardrobe.Wardrobe.Item;
import ess.ll.client.wardrobe.Wardrobe.Slot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Weapon and item skins (from Zenith's HeldItemPoseController): a sword, axe, bow, mace, spear, crossbow, golden
 * apple, totem, anchor or crystal in your hand is drawn as the worn skin, in first and third person. Bows and
 * crossbows play their pull clip with the draw; swings leave the skin's trail, hits burst its particle, and your
 * arrows trail the bow skin's particle.
 */
public final class WardrobeHeld {
    private WardrobeHeld() {}

    /** The worn skin for a stack in your hand, or null. */
    static Item skin(ItemStack stack) { return skin(stack,false); }
    /** With preview, what the Cosmetics preview shows (the item being tried on included). */
    private static Item skin(ItemStack stack,boolean preview) {
        if(!preview && (ess.ll.client.module.ClientModules.WARDROBE==null || !ess.ll.client.module.ClientModules.WARDROBE.enabled())) return null;
        Slot slot=Slot.of(stack);
        return slot==null ? null : Wardrobe.find(Wardrobe.outfit(preview),slot);
    }

    /** First person: true when the skin was drawn instead of the vanilla item. */
    public static boolean firstPerson(ItemStack stack,ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light) {
        return ownHand(stack,state,pose,collector,light,false);
    }
    /**
     * First person under the HMI hand overhaul, which draws the item itself, posed by its third-person hand
     * transform: the skin is placed as on a third-person player.
     */
    public static boolean handOverhaul(ItemStack stack,ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light) {
        return ownHand(stack,state,pose,collector,light,true);
    }
    private static boolean ownHand(ItemStack stack,ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light,boolean thirdPersonPose) {
        var mc=Minecraft.getInstance();
        var player=mc.player;
        if(player==null) return false;
        Item item=skin(stack);
        if(item==null) return false;
        float pull=pull(player,stack);
        float time=(player.tickCount+mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))/20;
        return draw(item,player.getId(),state,pose,collector,light,pull,time,thirdPersonPose);
    }
    /** Third person (also previews of you): the same, posed by the render state. */
    public static boolean thirdPerson(int entityId,ItemStack stack,boolean usingItem,float ticksUsing,float crossbowDuration,float ageInTicks,
                                      ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light) {
        var player=Minecraft.getInstance().player;
        if(player==null || entityId!=player.getId() && entityId!=WardrobeLayer.PREVIEW_ID) return false;
        Item item=skin(stack,entityId==WardrobeLayer.PREVIEW_ID);
        if(item==null) return false;
        float pull=0;
        if(Slot.CROSSBOW.matches(stack)) pull=CrossbowItem.isCharged(stack) ? 1 : usingItem && crossbowDuration>0 ? Math.min(.97f,ticksUsing/crossbowDuration) : 0;
        else if(usingItem && Slot.BOW.matches(stack)) pull=BowItem.getPowerForTime((int)ticksUsing);
        return draw(item,entityId,state,pose,collector,light,pull,ageInTicks/20,true);
    }
    private static float pull(LivingEntity entity,ItemStack stack) {
        boolean using=entity.isUsingItem() && entity.getUseItem()==stack;
        if(Slot.CROSSBOW.matches(stack)) return CrossbowItem.isCharged(stack) ? 1 : using ? Math.min(.97f,(float)entity.getTicksUsingItem()/CrossbowItem.getChargeDuration(stack,entity)) : 0;
        if(using && Slot.BOW.matches(stack)) return BowItem.getPowerForTime(entity.getTicksUsingItem());
        return 0;
    }
    private static boolean draw(Item item,int entityId,ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light,float pull,float time,boolean thirdPerson) {
        WardrobeModel model=Wardrobe.model(item);
        if(model==null || model.weapon==null) return false;
        var access=(ItemStackRenderStateAccessor)state;
        if(access.ess$activeLayerCount()<=0 || access.ess$displayContext()==null) return false;
        var transform=((LayerRenderStateAccessor)access.ess$layers()[0]).ess$itemTransform();
        WardrobeModel.Weapon w=model.weapon;
        var cache=WardrobeAnimator.Cache.of(entityId,item.id(),false);
        cache.clips.want(model,"idle",time);
        float[] bones=cache.pose.compute(model,cache.clips,time,null);
        cache.pose.overlay(model,cache.clips,"pull",pull,pull>0 ? 1 : 0);
        // The nocked arrow shows only while drawn.
        if(w.arrowBone()>=0) {
            float shown=pull>(item.slot()==Slot.CROSSBOW ? .99f : .05f) ? 1 : 0;
            bones[w.arrowBone()*9+6]=shown; bones[w.arrowBone()*9+7]=shown; bones[w.arrowBone()*9+8]=shown;
        }
        pose.pushPose();
        try {
            if(transform!=null) transform.apply(access.ess$displayContext().leftHand(),pose.last());
            pose.translate(w.gripX()/16,w.gripY()/16,w.gripZ()/16);
            // Items (not anchors) lie flat in the hand of a third-person player.
            boolean flat=thirdPerson && item.category()==Category.ITEM && item.slot()!=Slot.ANCHOR;
            if(flat) pose.translate(0,0,.22f);
            if(w.angle()!=0) pose.rotate(Axis.ZP.rotationDegrees(w.angle()));
            pose.rotate(Axis.XP.rotationDegrees(180));
            if(w.roll()!=0) pose.rotate(Axis.XP.rotationDegrees(w.roll()));
            if(flat) { pose.rotate(Axis.XP.rotationDegrees(90)); pose.rotate(Axis.YP.rotationDegrees(180)); }
            if(w.scale()!=1) pose.scale(w.scale(),w.scale(),w.scale());
            WardrobeRenderer.draw(model,bones,WardrobeModel.FREE,pose,collector,light,OverlayTexture.NO_OVERLAY,true,item.rarity().glows,-1,false);
        } finally { pose.popPose(); }
        return true;
    }

    /** Client tick: the swing trail of the held skin and the bow skin's trail behind your flying arrows. */
    public static void tick(Minecraft mc) {
        CsKnife.tick(mc);
        var player=mc.player;
        if(player==null || mc.level==null || !Wardrobe.PARTICLES.get()) return;
        WardrobeModel held=model(skin(player.getMainHandItem()));
        if(held!=null && held.weapon!=null && held.weapon.trail()!=null && player.isSwinging()) {
            float swing=player.getSwingAnimation(1);
            float yaw=(float)Math.toRadians(player.getYRot());
            Vec3 eye=player.getEyePosition(),forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),side=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw));
            for(int i=0;i<3;i++) {
                float t=Math.min(1,swing+i*.08f);
                Vec3 at=eye.add(forward.scale(.95)).add(side.scale(.7-1.4*t)).add(0,.35-.7*t,0);
                spawn(held.weapon.trail(),at.x,at.y,at.z,0,0,0);
            }
        }
        Slot bow=Slot.CROSSBOW.matches(player.getMainHandItem()) || Slot.CROSSBOW.matches(player.getOffhandItem()) ? Slot.CROSSBOW : Slot.BOW;
        WardrobeModel launcher=model(Wardrobe.worn(bow));
        if(launcher!=null && launcher.weapon!=null && launcher.weapon.trail()!=null) {
            for(Entity e:mc.level.entitiesForRendering())
                if(e instanceof AbstractArrow arrow && arrow.getOwner()==player && arrow.getDeltaMovement().lengthSqr()>.01 && arrow.tickCount<200)
                    spawn(launcher.weapon.trail(),arrow.getX(),arrow.getY(),arrow.getZ(),0,0,0);
        }
    }
    /** A hit with a skinned weapon bursts its hit particle over the target. */
    public static void attack(Entity target) {
        var player=Minecraft.getInstance().player;
        if(player==null || target==null || !Wardrobe.PARTICLES.get()) return;
        WardrobeModel held=model(skin(player.getItemInHand(InteractionHand.MAIN_HAND)));
        if(held==null || held.weapon==null || held.weapon.hit()==null) return;
        Vec3 c=target.getBoundingBox().getCenter();
        for(int i=0;i<10;i++) spawn(held.weapon.hit(),c.x+(Math.random()-.5)*target.getBbWidth(),c.y+(Math.random()-.5)*target.getBbHeight()*.8,
                c.z+(Math.random()-.5)*target.getBbWidth(),(Math.random()-.5)*.2,Math.random()*.15,(Math.random()-.5)*.2);
    }
    private static WardrobeModel model(Item item) { return item==null ? null : Wardrobe.model(item); }
    private static void spawn(net.minecraft.core.particles.ParticleOptions p,double x,double y,double z,double vx,double vy,double vz) {
        var level=Minecraft.getInstance().level;
        if(level!=null) level.addParticle(p,x,y,z,vx,vy,vz);
    }
}
