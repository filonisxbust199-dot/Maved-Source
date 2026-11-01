package ess.ll.client.integration;

import ess.ll.client.config.ParticleAnimationConfig;
import ess.ll.client.event.ViewModelOffsetEvent;
import ess.ll.client.features.inventory.WeaponItemTypes;
import ess.ll.client.render.ItemAnimationContext;
import ess.ll.client.render.ItemAnimationMath;
import ess.ll.client.render.ItemAnimationState;
import ess.ll.client.render.ParticleConfigCodec;
import ess.ll.client.util.TridentItemUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public final class ItemInHandRendererHooks {
    public static boolean enabled = false;
    private static final ItemStackRenderState ITEM_STATE = new ItemStackRenderState();
    public static volatile ItemInHandRendererHooks.ItemRenderOverride itemRenderOverride;
    public static volatile ItemInHandRendererHooks.ParticleLayer particleLayer;
    private static final float ZERO_UNUSED = 0;
    private static final String LAYER_SCREEN = "SCREEN";
    private static final double HAND_OFFSET_Y = -0.35;
    private static final double HAND_OFFSET_Z = 0.2;
    private static final double ARM_X = 1.5;
    private static final double ARM_Y = -0.3;
    private static final double ARM_Z = -0.6;
    private static final double ARM_ROT_ANGLE = 15.0;
    private static final float ARM_ROT_X = 0.5F;
    private static final double ARM_ROT_Y = 0.5;
    private static final double ARM_ROT_Z = 0.5;
    private static final float ARM_YAW_ANGLE = 35.0F;
    private static final float ARM_YAW_X = 0.5F;
    private static final double ARM_YAW_Y = 0.5;
    private static final double ARM_YAW_Z = 0.5;
    private static final float ARM_ROLL_ANGLE = -65.0F;
    private static final float ARM_ROLL_X = 0.5F;
    private static final double ARM_ROLL_Y = 0.5;
    private static final double ARM_ROLL_Z = 0.5;
    private static final float ARM_SCALE_X = 0.9F;
    private static final float ARM_SCALE_Y = 0.9F;
    private static final float ARM_SCALE_Z = 0.9F;
    private static final double ITEM_X = 0.5;
    private static final double ITEM_Y = -0.15;
    private static final double ITEM_Z = -0.85;
    private static final double ITEM_ROT_ANGLE = 15.0;
    private static final double ITEM_ROT_X = 0.5;
    private static final double ITEM_ROT_Y = 0.5;
    private static final double ITEM_ROT_Z = 0.5;
    private static final float ITEM_SCALE_X = 0.9F;
    private static final float ITEM_SCALE_Y = 0.9F;
    private static final float ITEM_SCALE_Z = 0.9F;
    private static final float MAP_X = -0.05F;
    private static final float MAP_Y = 0.2F;
    private static final float MAP_Z = 0.1F;
    private static final float MAP_YAW = -12.0F;
    private static final String LAYER_ITEM = "ITEM";
    private static final float LEFT_SIDE = -1.0F;
    private static final float SWING_X_SCALE = -0.3F;
    private static final float SWING_X_FREQ = (float) Math.PI;
    private static final float SWING_Y_SCALE = 0.4F;
    private static final float SWING_Y_FREQ = (float) (Math.PI * 2);
    private static final float SWING_Z_SCALE = -0.4F;
    private static final float SWING_Z_FREQ = (float) Math.PI;
    private static final float ARM_BASE_X = 0.64000005F;
    private static final float ARM_BASE_Y = -0.6F;
    private static final float EQUIP_DROP = -0.6F;
    private static final float ARM_BASE_Z = -0.71999997F;
    private static final float ARM_BASE_YAW = 45.0F;
    private static final float SWING_ROLL_FREQ = (float) Math.PI;
    private static final float SWING_YAW_FREQ = (float) Math.PI;
    private static final float SWING_YAW = 70.0F;
    private static final float SWING_ROLL = -20.0F;
    private static final float PIVOT_X = -1.0F;
    private static final float PIVOT_Y = 3.6F;
    private static final float PIVOT_Z = 3.5F;
    private static final float PIVOT_ROLL = 120.0F;
    private static final float PIVOT_PITCH = 200.0F;
    private static final float PIVOT_YAW = -135.0F;
    private static final float PIVOT_OFFSET_X = 5.6F;

    private static void applyViewModelOffset(AbstractClientPlayer player, InteractionHand interactionHand, PoseStack poseStack) {
        ViewModelOffsetEvent.INSTANCE.fire(player, interactionHand, poseStack);
    }

    public static void renderArmWithItem(FirstPersonHandsAndItemsRenderer firstPersonHandsAndItemsRenderer, AbstractClientPlayer player, float tickDelta, float xRot, InteractionHand interactionHand, float swing, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
        if (!player.isScoping()) {
            ItemAnimationState state = ItemAnimationState.INSTANCE;
            boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean rightArm = arm == HumanoidArm.RIGHT;
            int side = rightArm ? 1 : -1;
            ItemAnimationContext itemAnimationContext = state.contextForHand(mainHand);
            state.updateContext(itemAnimationContext, player, stack, interactionHand, mainHand, rightArm, side, swing, equip, tickDelta, xRot, poseStack, submitNodeCollector, light);
            poseStack.pushPose();
            applyViewModelOffset(player, interactionHand, poseStack);
            poseStack.pushPose();
            applyHandOffset(itemAnimationContext);
            poseStack.pushPose();
            applyArmTransform(itemAnimationContext);
            if (player.isInvisible() && !HeldItemRenderHooks.drawInvisibleArm()) {
                renderArm(poseStack, light, 0.0F, 0.0F, arm);
            } else {
                ((PlayerArmRenderAccess) firstPersonHandsAndItemsRenderer).hmi$renderPlayerArm(poseStack, submitNodeCollector, light, 0.0F, 0.0F, arm);
            }
            poseStack.popPose();
            poseStack.pushPose();
            itemAnimationContext.swingProgress = swing;
            renderHeldItem(firstPersonHandsAndItemsRenderer, itemAnimationContext, side, rightArm);
            poseStack.popPose();
            poseStack.popPose();
            state.rememberItem(itemAnimationContext);
            ItemInHandRendererHooks.ParticleLayer layer = particleLayer;
            if (layer != null) {
                poseStack.pushPose();
                layer.render(itemAnimationContext.particles, poseStack, submitNodeCollector, LAYER_SCREEN, interactionHand, light, player, tickDelta);
                poseStack.popPose();
            }
            poseStack.popPose();
            state.tickDelta = tickDelta;
        }
    }

    private static void renderItem(FirstPersonHandsAndItemsRenderer firstPersonHandsAndItemsRenderer, ItemAnimationContext itemAnimationContext, boolean rightHand) {
        ItemInHandRendererHooks.ItemRenderOverride override = itemRenderOverride;
        if (override == null || !override.render(itemAnimationContext, firstPersonHandsAndItemsRenderer)) {
            ItemDisplayContext itemDisplayContext = rightHand ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            // 26.3 draws items from render states: this one is resolved for the third-person hand pose HMI uses.
            Minecraft.getInstance().getItemModelResolver().updateForLiving(ITEM_STATE, itemAnimationContext.stack, itemDisplayContext, itemAnimationContext.player);
            // Weapon and item skins from the wardrobe replace the item here too, as vanilla's hand does in WardrobeHeldMixin.
            if (!ess.ll.client.wardrobe.WardrobeHeld.handOverhaul(itemAnimationContext.stack, ITEM_STATE, itemAnimationContext.poseStack, itemAnimationContext.collector, itemAnimationContext.packedLight))
                ITEM_STATE.submit(itemAnimationContext.poseStack, itemAnimationContext.collector, itemAnimationContext.packedLight, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    private static void applyItemTransform(ItemAnimationContext itemAnimationContext) {
        int side = itemAnimationContext.rightHand ? 1 : -1;
        PoseStack poseStack = itemAnimationContext.poseStack;
        poseStack.translate(ITEM_X * side, ITEM_Y, ITEM_Z);
        ItemAnimationMath.rotateAroundX(poseStack, ITEM_ROT_ANGLE, ITEM_ROT_X, ITEM_ROT_Y, ITEM_ROT_Z);
        poseStack.scale(ITEM_SCALE_X, ITEM_SCALE_Y, ITEM_SCALE_Z);
        ParticleConfigCodec.animateItemEffects(itemAnimationContext);
    }

    private static void renderHeldItem(FirstPersonHandsAndItemsRenderer firstPersonHandsAndItemsRenderer, ItemAnimationContext itemAnimationContext, int side, boolean rightHand) {
        ItemStack stack = itemAnimationContext.stack;
        if (!stack.isEmpty()) {
            ItemUseAnimation useAnimation = stack.getUseAnimation();
            boolean usesItem = useAnimation == ItemUseAnimation.EAT || useAnimation == ItemUseAnimation.DRINK || useAnimation == ItemUseAnimation.SPEAR;
            applyItemTransform(itemAnimationContext);
            if (usesItem) {
                renderItem(firstPersonHandsAndItemsRenderer, itemAnimationContext, rightHand);
            } else {
                PoseStack poseStack = itemAnimationContext.poseStack;
                if (stack.has(DataComponents.MAP_ID)) {
                    poseStack.pushPose();
                    poseStack.translate(MAP_X * side, MAP_Y, MAP_Z);
                    poseStack.rotate(Axis.YP.rotationDegrees(MAP_YAW * side));
                    ((PlayerArmRenderAccess) firstPersonHandsAndItemsRenderer).hmi$renderMap(poseStack, itemAnimationContext.collector, itemAnimationContext.packedLight, stack);
                    poseStack.popPose();
                } else {
                    renderItem(firstPersonHandsAndItemsRenderer, itemAnimationContext, rightHand);
                }
                if (rightHand) {
                    ItemAnimationState.INSTANCE.mainHandPose.set(poseStack.last().pose());
                } else {
                    ItemAnimationState.INSTANCE.offhandPose.set(poseStack.last().pose());
                }
                ItemInHandRendererHooks.ParticleLayer layer = particleLayer;
                if (layer != null) {
                    poseStack.pushPose();
                    layer.render(itemAnimationContext.particles, poseStack, itemAnimationContext.collector, LAYER_ITEM, itemAnimationContext.hand, itemAnimationContext.packedLight, itemAnimationContext.player, itemAnimationContext.tickDelta);
                    poseStack.popPose();
                }
            }
        }
    }

    private static void applyArmTransform(ItemAnimationContext itemAnimationContext) {
        TridentItemUtils.applyTransform(itemAnimationContext);
        if (!itemAnimationContext.stack.isEmpty()) {
            int side = itemAnimationContext.rightHand ? 1 : -1;
            PoseStack poseStack = itemAnimationContext.poseStack;
            poseStack.translate(ARM_X * side, ARM_Y, ARM_Z);
            ItemAnimationMath.rotateAroundX(poseStack, ARM_ROT_ANGLE, ARM_ROT_X * side, ARM_ROT_Y, ARM_ROT_Z);
            ItemAnimationMath.rotateAroundY(poseStack, ARM_YAW_ANGLE * side, ARM_YAW_X * side, ARM_YAW_Y, ARM_YAW_Z);
            ItemAnimationMath.rotateAroundZ(poseStack, ARM_ROLL_ANGLE * side, ARM_ROLL_X * side, ARM_ROLL_Y, ARM_ROLL_Z);
            poseStack.scale(ARM_SCALE_X, ARM_SCALE_Y, ARM_SCALE_Z);
        }
    }

    private ItemInHandRendererHooks() {
    }

    private static void applyHandOffset(ItemAnimationContext itemAnimationContext) {
        WeaponItemTypes.animateWeapon(itemAnimationContext);
        if (!itemAnimationContext.stack.isEmpty()) {
            itemAnimationContext.poseStack.translate(0.0, HAND_OFFSET_Y, HAND_OFFSET_Z);
        }
    }

    private static void renderArm(PoseStack poseStack, int light, float equip, float swing, HumanoidArm humanoidArm) {
        boolean right = humanoidArm != HumanoidArm.LEFT;
        float side = right ? 1.0F : LEFT_SIDE;
        float swingRoot = Mth.sqrt(swing);
        float swingX = SWING_X_SCALE * Mth.sin(swingRoot * SWING_X_FREQ);
        float swingY = SWING_Y_SCALE * Mth.sin(swingRoot * SWING_Y_FREQ);
        float swingZ = SWING_Z_SCALE * Mth.sin(swing * SWING_Z_FREQ);
        poseStack.translate(side * (swingX + ARM_BASE_X), swingY + ARM_BASE_Y + equip * EQUIP_DROP, swingZ + ARM_BASE_Z);
        poseStack.rotate(Axis.YP.rotationDegrees(side * ARM_BASE_YAW));
        float swingRoll = Mth.sin(swing * swing * SWING_ROLL_FREQ);
        float swingYaw = Mth.sin(swingRoot * SWING_YAW_FREQ);
        poseStack.rotate(Axis.YP.rotationDegrees(side * swingYaw * SWING_YAW));
        poseStack.rotate(Axis.ZP.rotationDegrees(side * swingRoll * SWING_ROLL));
        poseStack.translate(side * PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.rotate(Axis.ZP.rotationDegrees(side * PIVOT_ROLL));
        poseStack.rotate(Axis.XP.rotationDegrees(PIVOT_PITCH));
        poseStack.rotate(Axis.YP.rotationDegrees(side * PIVOT_YAW));
        poseStack.translate(side * PIVOT_OFFSET_X, 0.0F, 0.0F);
    }

    public interface ItemRenderOverride {
        boolean render(ItemAnimationContext itemAnimationContext, FirstPersonHandsAndItemsRenderer firstPersonHandsAndItemsRenderer);
    }

    public interface ParticleLayer {
        void render(List<ParticleAnimationConfig> particleAnimationConfigs, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, String layer, InteractionHand interactionHand, int light, AbstractClientPlayer player, float tickDelta);
    }
}
