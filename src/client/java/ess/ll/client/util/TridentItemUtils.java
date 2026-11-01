package ess.ll.client.util;

import ess.ll.client.features.inventory.ItemFeatureProperties;
import ess.ll.client.features.inventory.ItemFeatureVariables;
import ess.ll.client.features.inventory.ItemTagAliases;
import ess.ll.client.render.AnimationCurveMath;
import ess.ll.client.render.ItemAnimationContext;
import ess.ll.client.render.ItemAnimationMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Items;

public final class TridentItemUtils {
    private static final double LEFT_SIDE = -1.0;
    private static final double TRANSLATE_X = 1.5;
    private static final double TRANSLATE_Y = -0.3;
    private static final double TRANSLATE_Z = -0.47;
    private static final double ROT_ANGLE = 15.0;
    private static final double ROT_X = 0.5;
    private static final double ROT_Y = 0.5;
    private static final double ROT_Z = 0.5;
    private static final double YAW_ANGLE = 35.0;
    private static final double YAW_X = 0.5;
    private static final double YAW_Y = 0.5;
    private static final double YAW_Z = 0.5;
    private static final double ROLL_ANGLE = -65.0;
    private static final double ROLL_X = 0.5;
    private static final double ROLL_Y = 0.5;
    private static final double ROLL_Z = 0.5;
    private static final double SCALE_X = 0.9;
    private static final double SCALE_Y = 0.9;
    private static final double SCALE_Z = 0.9;
    private static final double CURVE_START = 0.09723;
    private static final double CURVE_END = 0.60632;
    private static final double CURVE_SCALE = 3.24;
    private static final double CURVE_SCALE_2 = 1.65;
    private static final double CURVE_OFFSET = 0.1;
    private static final String TRIDENT_NAME = "trident";
    private static final double THROW_PITCH = 10.0;
    private static final double THROW_ROLL = 6.0;

    private TridentItemUtils() {
    }

    public static void applyTransform(ItemAnimationContext itemAnimationContext) {
        ItemFeatureVariables features = ItemFeatureVariables.INSTANCE;
        PoseStack poseStack = itemAnimationContext.poseStack;
        double side = itemAnimationContext.rightHand ? 1.0 : LEFT_SIDE;
        String itemName = ItemFeatureProperties.getUseAnimationName(itemAnimationContext.stack);
        if (ItemFeatureProperties.isEmpty(itemAnimationContext.stack) && features.mainHandDrinkProgress > 0.0) {
            ItemAnimationMath.translateX(poseStack, TRANSLATE_X * side);
            ItemAnimationMath.translateY(poseStack, TRANSLATE_Y);
            ItemAnimationMath.translateZ(poseStack, TRANSLATE_Z);
            ItemAnimationMath.rotateAroundX(poseStack, ROT_ANGLE, ROT_X * side, ROT_Y, ROT_Z);
            ItemAnimationMath.rotateAroundY(poseStack, YAW_ANGLE * side, YAW_X * side, YAW_Y, YAW_Z);
            ItemAnimationMath.rotateAroundZ(poseStack, ROLL_ANGLE * side, ROLL_X * side, ROLL_Y, ROLL_Z);
            ItemAnimationMath.scale(poseStack, SCALE_X, SCALE_Y, SCALE_Z);
        }
        double progress = itemAnimationContext.mainHand ? features.mainHandEquipProgress : features.offhandEquipProgress;
        double curve = AnimationCurveMath.backIn(ItemAnimationMath.sin(ItemAnimationMath.clamp(progress, CURVE_START, CURVE_END) * CURVE_SCALE * CURVE_SCALE_2 - CURVE_OFFSET));
        if ((ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.BUNDLES) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ENDER_PEARL) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ENDER_EYE) || ItemFeatureProperties.isThrowable(itemAnimationContext.stack) || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.MUSIC_DISCS) || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.NUGGETS) || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.SKULLS)) && !itemName.equals(TRIDENT_NAME)) {
            ItemAnimationMath.rotateX(poseStack, THROW_PITCH * curve);
            ItemAnimationMath.rotateZ(poseStack, THROW_ROLL * curve);
        }
    }
}
