package ess.ll.client.render;

import ess.ll.client.features.inventory.ItemFeatureProperties;
import ess.ll.client.features.inventory.ItemFeatureVariables;
import ess.ll.client.features.inventory.ItemTagAliases;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ParticleConfigCodec {
    private static final Identifier ORANGE_GLOW = particleTexture("orange_glow");
    private static final Identifier BLUE_GLOW = particleTexture("blue_glow");
    private static final Identifier RED_GLOW = particleTexture("red_glow");
    private static final Identifier FIREFLY = particleTexture("firefly");
    private static final Identifier PINK_PETALS_SMALL = particleTexture("pink_petals_1");
    private static final Identifier PINK_PETALS_MEDIUM = particleTexture("pink_petals_2");
    private static final Identifier PINK_PETALS_LARGE = particleTexture("pink_petals_4");
    private static final Identifier WILDFLOWERS_SMALL = particleTexture("wild_flowers_1");
    private static final Identifier WILDFLOWERS_MEDIUM = particleTexture("wild_flowers_2");
    private static final Identifier WILDFLOWERS_LARGE = particleTexture("wild_flowers_4");
    private static final Identifier LEAF_LITTER_SMALL = particleTexture("leaf_litter_1");
    private static final Identifier LEAF_LITTER_MEDIUM = particleTexture("leaf_litter_2");
    private static final Identifier LEAF_LITTER_LARGE = particleTexture("leaf_litter_4");
    private static final String[] CROP_ITEMS = new String[] { "minecraft:string", "minecraft:resin_clump", "minecraft:vine", "minecraft:kelp", "minecraft:seagrass", "minecraft:iron_bars", "minecraft:glass_pane", "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane", "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane", "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane", "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane", "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane", "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane", "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane", "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane", "minecraft:ladder", "minecraft:oak_sign", "minecraft:spruce_sign", "minecraft:birch_sign", "minecraft:jungle_sign", "minecraft:acacia_sign", "minecraft:dark_oak_sign", "minecraft:mangrove_sign", "minecraft:cherry_sign", "minecraft:bamboo_sign", "minecraft:crimson_sign", "minecraft:warped_sign", "minecraft:pale_oak_sign", "minecraft:tripwire_hook", "minecraft:hopper", "minecraft:cauldron", "minecraft:rail", "minecraft:powered_rail", "minecraft:detector_rail", "minecraft:activator_rail", "minecraft:repeater", "minecraft:comparator", "minecraft:twisting_vines", "minecraft:weeping_vines", "minecraft:sniffer_egg", "minecraft:candle", "minecraft:white_candle", "minecraft:orange_candle", "minecraft:magenta_candle", "minecraft:light_blue_candle", "minecraft:yellow_candle", "minecraft:lime_candle", "minecraft:pink_candle", "minecraft:gray_candle", "minecraft:light_gray_candle", "minecraft:cyan_candle", "minecraft:purple_candle", "minecraft:blue_candle", "minecraft:brown_candle", "minecraft:green_candle", "minecraft:red_candle", "minecraft:black_candle", "minecraft:frogspawn", "minecraft:light", "minecraft:structure_void", "minecraft:barrier", "minecraft:carrot", "minecraft:powder_snow_bucket", "minecraft:glow_berries", "minecraft:potato", "minecraft:sweet_berries", "minecraft:redstone" };

    private static void spawnPetals(ItemAnimationContext itemAnimationContext, double var1, Identifier identifier, Identifier identifier2, Identifier identifier3) {
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.75 * var1, -0.2, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.4, FIREFLY, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.75 * var1, -0.2, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.4, FIREFLY, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, identifier, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, identifier, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, identifier2, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, identifier2, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.2, identifier3, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
        ParticleKeyframe.add(itemAnimationContext.particles, true, 0.65 * var1, -0.25, -0.9, (Math.random() * 0.12 - 0.06) * var1, Math.random() * 0.12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.2, identifier3, "SCREEN", itemAnimationContext.hand, "OPACITY", "TRANSLUCENT_L", 1.0, 255.0);
    }

    static {
        ItemFeatureVariables var0 = ItemFeatureVariables.INSTANCE;
        var0.blockModelItems.put("minecraft:string", false);
        var0.blockModelItems.put("minecraft:resin_clump", false);
        var0.blockModelItems.put("minecraft:vine", false);
        var0.blockModelItems.put("minecraft:bamboo", false);
        for (String var4 : CROP_ITEMS) {
            var0.blockModelItems.put(var4, false);
            if (!var4.equals("bamboo")) {
                var0.flatBlockItems.put(var4, true);
            }
        }
        var0.weaponChargeTicks.put("minecraft:trident", 12);
        var0.weaponChargeTicks.put("minecraft:iron_spear", 15);
        var0.weaponChargeTicks.put("minecraft:copper_spear", 15);
        var0.weaponChargeTicks.put("minecraft:diamond_spear", 15);
        var0.weaponChargeTicks.put("minecraft:wooden_spear", 15);
        var0.weaponChargeTicks.put("minecraft:stone_spear", 15);
        var0.weaponChargeTicks.put("minecraft:golden_spear", 15);
        var0.weaponChargeTicks.put("minecraft:netherite_spear", 15);
        var0.weaponChargeTicks.put("minecraft:mace", 12);
    }

    private static void spawnItemGlow(ItemAnimationContext itemAnimationContext, double var1, double var3, double var5, double var7, Identifier identifier, double var10) {
        ParticleKeyframe.add(itemAnimationContext.particles, false, var1, var3, var5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, var7, identifier, "ITEM", itemAnimationContext.hand, "SPAWN", "ADDITIVE", 0.0, var10);
    }

    private ParticleConfigCodec() {
    }

    private static Identifier particleTexture(String var0) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/particle/" + var0 + ".png");
    }

    public static void animateItemEffects(ItemAnimationContext itemAnimationContext) {
        ItemFeatureVariables itemFeatureVariables = ItemFeatureVariables.INSTANCE;
        AbstractClientPlayer player = itemAnimationContext.player;
        if (player != null) {
            PoseStack poseStack = itemAnimationContext.poseStack;
            String useAnimationName = ItemFeatureProperties.getUseAnimationName(itemAnimationContext.stack);
            double var5 = itemAnimationContext.rightHand ? 1.0 : -1.0;
            ItemStack stack = itemAnimationContext.stack;
            String registryId = ItemFeatureProperties.getRegistryId(stack);
            boolean block = ItemFeatureProperties.isBlock(stack);
            boolean lantern = ItemFeatureProperties.isLantern(stack);
            boolean inTag = ItemFeatureProperties.isInTag(stack, ItemTagAliases.SWORDS);
            boolean inTag2 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.PICKAXES);
            boolean inTag3 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.AXES);
            boolean inTag4 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.SHOVELS);
            boolean inTag5 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.SKULLS);
            boolean inTag6 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.HANGING_SIGNS);
            itemFeatureVariables.mainHandSwayRollVelocity = itemFeatureVariables.mainHandSwayRollVelocity + ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 4.14) * itemFeatureVariables.mainHandBrushProgress * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayRollVelocity = itemFeatureVariables.mainHandSwayRollVelocity - 0.04 * itemFeatureVariables.mainHandSwayRoll * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayRollVelocity = itemFeatureVariables.mainHandSwayRollVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.mainHandSwayRoll = itemFeatureVariables.mainHandSwayRoll + itemFeatureVariables.mainHandSwayRollVelocity * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayRollVelocity = itemFeatureVariables.offhandSwayRollVelocity + ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 4.14) * itemFeatureVariables.offhandBrushProgress * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayRollVelocity = itemFeatureVariables.offhandSwayRollVelocity - 0.04 * itemFeatureVariables.offhandSwayRoll * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayRollVelocity = itemFeatureVariables.offhandSwayRollVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.offhandSwayRoll = itemFeatureVariables.offhandSwayRoll + itemFeatureVariables.offhandSwayRollVelocity * itemAnimationContext.frameDelta * 30.0;
            double var17 = ItemFeatureProperties.getUseAnimationName(PlayerAnimationUtils.getMainHandItem(player)).equals("spear") ? 1.0 : 0.0;
            double var19 = ItemFeatureProperties.getUseAnimationName(PlayerAnimationUtils.getOffhandItem(player)).equals("spear") ? 1.0 : 0.0;
            double var21 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearUse : itemFeatureVariables.offhandSpearUse;
            double var23 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearSlowCharge : itemFeatureVariables.offhandSpearSlowCharge;
            double var25 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearFastCharge : itemFeatureVariables.offhandSpearFastCharge;
            double var27 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandEquipProgress : itemFeatureVariables.offhandEquipProgress;
            double var29 = itemAnimationContext.mainHand ? AnimationCurveMath.sineInOut(itemFeatureVariables.mainHandSpearThrust) : itemFeatureVariables.offhandSpearThrust;
            itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity + (PlayerAnimationUtils.getSpeed(player) * 22.0 * itemFeatureVariables.walkProgress * -1.0 - ItemAnimationMath.sin(itemAnimationContext.mainHandSwingProgress * 3.14) * 8.0 + itemFeatureVariables.verticalBob * 3.0 + ItemAnimationMath.sin(itemFeatureVariables.sneakProgress * 3.14) * 0.3 + (PlayerAnimationUtils.getPitch(player) - itemFeatureVariables.previousPitch)) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            if (useAnimationName.equals("block") && itemAnimationContext.mainHand && !inTag) {
                itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity + 10.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * 3.14) * 0.15 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity + 12.0 * ItemAnimationMath.sin(itemFeatureVariables.mainHandBlockProgress * 3.14) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity + ((-20.0) * ItemAnimationMath.sin(itemFeatureVariables.mainHandSpearSlowCharge * 3.14) * itemFeatureVariables.mainHandSpearUse + 20.0 * ItemAnimationMath.sin(itemFeatureVariables.mainHandSpearFastCharge * 3.14) * itemFeatureVariables.mainHandSpearUse + 12.0 * ItemAnimationMath.sin(itemFeatureVariables.inspectProgress * 3.14) + 15.0 * ItemAnimationMath.sin(itemFeatureVariables.mainHandSpearUse * 3.14) + -10.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(AnimationCurveMath.backIn(itemFeatureVariables.mainHandSpearThrust) * 6.28), 0.0, 1.0) + 40.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress * 1.5 * var17, 0.0, 1.0) * 6.28), 0.0, 1.0)) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity - 0.04 * itemFeatureVariables.mainHandSwayPitch * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayPitchVelocity = itemFeatureVariables.mainHandSwayPitchVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.mainHandSwayPitch = itemFeatureVariables.mainHandSwayPitch + itemFeatureVariables.mainHandSwayPitchVelocity * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayYawVelocity = itemFeatureVariables.mainHandSwayYawVelocity + (ItemAnimationMath.sin(itemFeatureVariables.walkPhase) * 3.0 * itemFeatureVariables.walkProgress + ItemAnimationMath.sin(itemAnimationContext.mainHandSwingProgress * 3.14) * 8.0 + ItemAnimationMath.sin(itemFeatureVariables.swimPhase * itemFeatureVariables.swimProgress) * 3.0 + ItemAnimationMath.sin(itemFeatureVariables.mainHandEquipProgress * 6.28) * 3.0 + PlayerAnimationUtils.getHeadYaw(player) - itemFeatureVariables.previousHeadYaw) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayYawVelocity = itemFeatureVariables.mainHandSwayYawVelocity - 0.04 * itemFeatureVariables.mainHandSwayYaw * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandSwayYawVelocity = itemFeatureVariables.mainHandSwayYawVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.mainHandSwayYaw = itemFeatureVariables.mainHandSwayYaw + itemFeatureVariables.mainHandSwayYawVelocity * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity + (PlayerAnimationUtils.getSpeed(player) * 22.0 * itemFeatureVariables.walkProgress * -1.0 - ItemAnimationMath.sin(itemAnimationContext.offhandSwingProgress * 3.14) * 8.0 + itemFeatureVariables.verticalBob * 3.0 + ItemAnimationMath.sin(itemFeatureVariables.sneakProgress * 3.14) * 0.3 + (PlayerAnimationUtils.getPitch(player) - itemFeatureVariables.previousPitch)) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            if (useAnimationName.equals("block") && !itemAnimationContext.mainHand && !inTag) {
                itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity + 10.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * 3.14) * 0.15 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity + 12.0 * ItemAnimationMath.sin(itemFeatureVariables.offhandBlockProgress * 3.14) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity + ((-20.0) * ItemAnimationMath.sin(itemFeatureVariables.offhandSpearSlowCharge * 3.14) * itemFeatureVariables.offhandSpearUse + 20.0 * ItemAnimationMath.sin(itemFeatureVariables.offhandSpearFastCharge * 3.14) * itemFeatureVariables.offhandSpearUse + 15.0 * ItemAnimationMath.sin(itemFeatureVariables.offhandSpearUse * 3.14) + 40.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress * 1.5 * var19, 0.0, 1.0) * 6.28), 0.0, 1.0)) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity - 0.04 * itemFeatureVariables.offhandSwayPitch * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayPitchVelocity = itemFeatureVariables.offhandSwayPitchVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.offhandSwayPitch = itemFeatureVariables.offhandSwayPitch + itemFeatureVariables.offhandSwayPitchVelocity * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayYawVelocity = itemFeatureVariables.offhandSwayYawVelocity + (ItemAnimationMath.sin(itemFeatureVariables.walkPhase) * 3.0 * itemFeatureVariables.walkProgress + ItemAnimationMath.sin(itemAnimationContext.offhandSwingProgress * 3.14) * 8.0 + ItemAnimationMath.sin(itemFeatureVariables.swimPhase * itemFeatureVariables.swimProgress) * 3.0 + ItemAnimationMath.sin(itemFeatureVariables.offhandEquipProgress * 6.28) * 3.0 + PlayerAnimationUtils.getHeadYaw(player) - itemFeatureVariables.previousHeadYaw) * 0.15 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayYawVelocity = itemFeatureVariables.offhandSwayYawVelocity - 0.04 * itemFeatureVariables.offhandSwayYaw * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandSwayYawVelocity = itemFeatureVariables.offhandSwayYawVelocity * ItemAnimationMath.pow(0.85, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.offhandSwayYaw = itemFeatureVariables.offhandSwayYaw + itemFeatureVariables.offhandSwayYawVelocity * itemAnimationContext.frameDelta * 30.0;
            double var31 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSwayYaw : itemFeatureVariables.offhandSwayYaw;
            double var33 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSwayPitch : itemFeatureVariables.offhandSwayPitch;
            if (inTag2) {
                itemAnimationContext.swingProgress = (float) ItemAnimationCurves.swingBezier(itemAnimationContext.swingProgress);
            } else {
                itemAnimationContext.swingProgress = (float) ItemAnimationCurves.recoveryBezier(itemAnimationContext.swingProgress);
            }
            double var35;
            if (itemAnimationContext.swingProgress < 0.70016) {
                var35 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.0, 0.308) * 5.1);
            } else {
                var35 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.70016, 1.0) * 5.1 - 2.0);
            }
            double var37;
            if (itemAnimationContext.swingProgress < 0.65245) {
                var37 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.0, 0.16675) * 3.14 * 3.0);
            } else {
                var37 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.65245, 1.0) * 4.4 - 1.3);
            }
            var35 = var35 * var35 * var35;
            double var39 = ItemAnimationMath.clamp(ItemAnimationMath.sin(itemAnimationContext.swingProgress * 4.78), 0.0, 1.0);
            double var41 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.16561, 0.49422) * 4.78 * 2.0 + 4.7);
            double var43 = ItemAnimationMath.sin(itemAnimationContext.swingProgress * 3.14);
            double var45 = ItemAnimationMath.clamp(ItemAnimationMath.sin(itemAnimationContext.swingProgress * 6.28), 0.0, 1.0);
            double var47 = ItemAnimationMath.sin(itemAnimationContext.swingProgress * 6.28);
            double var49;
            if (itemAnimationContext.swingProgress < 0.65594) {
                var49 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.16561, 0.32991) * 4.78 * 2.0 + 4.7);
            } else {
                var49 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.65594, 0.82025) * 4.78 * 2.0 - 4.7);
            }
            if (useAnimationName.equals("spear")) {
                ItemAnimationMath.rotateZ(poseStack, 180.0 * var5);
                ItemAnimationMath.rotateZ(poseStack, -180.0 * AnimationCurveMath.backInOut(ItemAnimationMath.clamp(var27 * 2.0, 0.0, 1.0)) * var5);
                ItemAnimationMath.translateZ(poseStack, -0.2 * AnimationCurveMath.sineInOut(AnimationCurveMath.backInOut(var21 * 0.8)));
                ItemAnimationMath.translateY(poseStack, -0.05 * AnimationCurveMath.backInOut(var23));
                ItemAnimationMath.rotateX(poseStack, -70.0 * AnimationCurveMath.backInOut(var21 * 0.8));
                ItemAnimationMath.rotateX(poseStack, -8.0 * AnimationCurveMath.backInOut(var23));
                ItemAnimationMath.rotateY(poseStack, 60.0 * AnimationCurveMath.backInOut(var21 * 0.8) * var5);
                ItemAnimationMath.rotateY(poseStack, -30.0 * AnimationCurveMath.backInOut(var23) * var5);
                ItemAnimationMath.rotateY(poseStack, -60.0 * AnimationCurveMath.backOut(var25) * var25 * var5);
                ItemAnimationMath.translateY(poseStack, -0.25 * ItemAnimationMath.clamp(ItemAnimationMath.sin(AnimationCurveMath.sineInOut(var29) * 6.28), 0.0, 1.0));
            }
            if (!useAnimationName.equals("block") && !useAnimationName.equals("crossbow") || inTag) {
                ItemAnimationMath.translateZ(poseStack, -0.05 * var35);
                ItemAnimationMath.translateY(poseStack, -0.05 * var35);
                ItemAnimationMath.rotateX(poseStack, 10.0 * var35);
                ItemAnimationMath.rotateX(poseStack, -30.0 * var35);
                ItemAnimationMath.rotateX(poseStack, -10.0 * var41);
                if (!inTag) {
                    if (!useAnimationName.equals("trident") && !useAnimationName.equals("spear")) {
                        ItemAnimationMath.translateZ(poseStack, -0.05 * var35);
                        ItemAnimationMath.translateY(poseStack, -0.05 * var35);
                        ItemAnimationMath.rotateX(poseStack, -10.0 * var35);
                        ItemAnimationMath.rotateX(poseStack, -25.0 * var41);
                    } else {
                        ItemAnimationMath.translateZ(poseStack, -0.1 * var35);
                        ItemAnimationMath.translateY(poseStack, -0.05 * var35);
                        if (useAnimationName.equals("spear")) {
                            ItemAnimationMath.translateY(poseStack, -0.15 * var41);
                            ItemAnimationMath.rotateX(poseStack, -5.0 * var41);
                        }
                        ItemAnimationMath.rotateX(poseStack, -10.0 * var35);
                        ItemAnimationMath.rotateX(poseStack, -15.0 * var41);
                        if (useAnimationName.equals("trident")) {
                            ItemAnimationMath.rotateX(poseStack, -45.0 * var43);
                        } else {
                            ItemAnimationMath.rotateX(poseStack, -45.0 * var37);
                        }
                        ItemAnimationMath.translateY(poseStack, 0.05 * var41);
                        ItemAnimationMath.translateY(poseStack, 0.3 * var43);
                    }
                }
                if (inTag4) {
                    ItemAnimationMath.translateY(poseStack, 0.12 * var37);
                    ItemAnimationMath.translateZ(poseStack, 0.05 * var37);
                    ItemAnimationMath.rotateX(poseStack, 10.0 * var37);
                    ItemAnimationMath.rotateX(poseStack, -30.0 * var43);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * var35);
                    ItemAnimationMath.rotateX(poseStack, 10.0 * var49);
                }
                if (inTag) {
                    var39 = ItemAnimationMath.sin(itemAnimationContext.swingProgress * 3.14);
                    ItemAnimationMath.translateY(poseStack, -0.1 * AnimationCurveMath.backInOut(var39));
                    if (inTag) {
                        ItemAnimationMath.rotateX(poseStack, -60.0 * AnimationCurveMath.backInOut(var39));
                    } else {
                        ItemAnimationMath.rotateX(poseStack, -30.0 * AnimationCurveMath.backInOut(var39));
                    }
                }
                if (useAnimationName.equals("bow")) {
                    ItemAnimationMath.translateX(poseStack, -0.065 * var5);
                }
            }
            if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.BEDS)) {
                ItemAnimationMath.translateZ(poseStack, 0.2);
                ItemAnimationMath.rotateAroundY(poseStack, 180.0 * var5, -0.15 * var5, -0.4, 0.0);
            }
            if (!ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL) && !lantern && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL) && !inTag6 && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS)) {
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PAINTING) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ITEM_FRAME)) {
                    itemAnimationContext.swingProgress = 0.0F;
                    ItemAnimationMath.rotateX(poseStack, -25.0);
                    ItemAnimationMath.translateY(poseStack, -0.65);
                    ItemAnimationMath.rotateAroundX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player) / 2.5, -25.0, 90.0) + var33, 0.0, 0.45, 0.0);
                    ItemAnimationMath.rotateAroundZ(poseStack, var31 * -1.0, 0.0, 0.55, 0.0);
                } else if (block) {
                    ItemAnimationMath.translateY(poseStack, -0.025);
                    ItemAnimationMath.translateZ(poseStack, -0.025);
                    ItemAnimationMath.rotateX(poseStack, -5.0);
                } else {
                    if (!block && !ItemFeatureProperties.isEmpty(itemAnimationContext.stack) && useAnimationName.equals("none") && !useAnimationName.equals("crossbow")) {
                        if (!inTag3 && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.MACE)) {
                            ItemAnimationMath.rotateAroundX(poseStack, -20.0 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + var33 * 0.05, 0.3 * var5, -0.4, 0.0);
                        } else {
                            double var51 = ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.MACE) ? 0.2 : 0.15;
                            ItemAnimationMath.rotateAroundX(poseStack, -20.0 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + var33 * var51, 0.3 * var5, -0.3, 0.0);
                        }
                    }
                    if ((inTag3 || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.MACE)) && !useAnimationName.equals("crossbow")) {
                        ItemAnimationMath.rotateAroundX(poseStack, PlayerAnimationUtils.getPitch(player) * -0.05 + var33 * 0.2, 0.0, -0.2, 0.0);
                    } else if (!useAnimationName.equals("crossbow")) {
                        ItemAnimationMath.rotateAroundX(poseStack, PlayerAnimationUtils.getPitch(player) * -0.025 + var33 * 0.1, 0.0, -0.2, 0.0);
                    }
                }
            } else {
                if (!ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL)) {
                    ItemAnimationMath.translateY(poseStack, -0.62);
                }
                if (inTag6) {
                    ItemAnimationMath.translateY(poseStack, -0.07);
                }
                if (!ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS)) {
                    ItemAnimationMath.translateZ(poseStack, 0.2);
                    ItemAnimationMath.rotateX(poseStack, -25.0);
                } else {
                    ItemAnimationMath.translateY(poseStack, 0.4);
                    ItemAnimationMath.rotateX(poseStack, -70.0);
                }
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER)) {
                    ItemAnimationMath.rotateAroundX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player) / 2.5, -20.0, 90.0) + var33 + var31 * 0.5, 0.0, -0.13, 0.0);
                }
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL) || lantern || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL)) {
                    if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL)) {
                        ItemAnimationMath.scale(poseStack, 1.0 + 0.01 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 15.0), 1.0 + 0.01 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 15.0), 1.0 + 0.01 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 8.0));
                        ItemAnimationMath.translateY(poseStack, 0.03 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 2.0));
                        ItemAnimationMath.translateY(poseStack, 0.25);
                        ItemAnimationMath.translateY(poseStack, var33 / 150.0);
                        ItemAnimationMath.translateX(poseStack, var31 / 150.0 * var5 * -1.0);
                        ItemAnimationMath.rotateZ(poseStack, 5.0 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase));
                        ItemAnimationMath.scale(poseStack, 0.7, 0.7, 0.7);
                    } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL)) {
                        ItemAnimationMath.translateX(poseStack, 0.15 * var5);
                        ItemAnimationMath.translateY(poseStack, -0.05);
                        ItemAnimationMath.translateZ(poseStack, -0.1);
                        ItemAnimationMath.scale(poseStack, 1.2, 1.2, 1.2);
                        ItemAnimationMath.rotateAroundX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player) / 2.5, -20.0, 90.0) + var33, -0.1 * var5, 0.4, 0.1);
                        ItemAnimationMath.rotateAroundZ(poseStack, var31 * -1.0, -0.1 * var5, 0.4, 0.1);
                    } else {
                        ItemAnimationMath.rotateAroundX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player) / 2.5, -20.0, 90.0) + var33, 0.0, 0.4, 0.0);
                        ItemAnimationMath.rotateAroundZ(poseStack, var31 * -1.0, 0.0, 0.4, 0.0);
                    }
                }
                if (inTag6) {
                    ItemAnimationMath.rotateAroundX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player) / 2.5, -35.0, 90.0) + var33, 0.0, 0.55, 0.0);
                    ItemAnimationMath.rotateAroundZ(poseStack, var31 * -1.0, 0.0, 0.55, 0.0);
                }
            }
            if ((useAnimationName.equals("drink") || useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.02 * var5 * itemFeatureVariables.mainHandEatProgress);
                ItemAnimationMath.translateZ(poseStack, -0.05 * itemFeatureVariables.mainHandEatProgress);
                if (useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) {
                    ItemAnimationMath.rotateX(poseStack, -23.0 * itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.mainHandEatProgress);
                    ItemAnimationMath.rotateZ(poseStack, -12.0 * var5 * itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.mainHandEatProgress);
                }
                ItemAnimationMath.rotateY(poseStack, -50.0 * var5 * itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.mainHandEatProgress);
                if (useAnimationName.equals("drink")) {
                    ItemAnimationMath.rotateX(poseStack, 15.0 * itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.mainHandEatProgress);
                }
            }
            if ((useAnimationName.equals("drink") || useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.02 * var5 * itemFeatureVariables.offhandEatProgress);
                ItemAnimationMath.translateZ(poseStack, -0.05 * itemFeatureVariables.offhandEatProgress);
                if (useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) {
                    ItemAnimationMath.rotateX(poseStack, -23.0 * itemFeatureVariables.offhandEatProgress * itemFeatureVariables.offhandEatProgress);
                    ItemAnimationMath.rotateZ(poseStack, -12.0 * var5 * itemFeatureVariables.offhandEatProgress * itemFeatureVariables.offhandEatProgress);
                }
                ItemAnimationMath.rotateY(poseStack, -50.0 * var5 * itemFeatureVariables.offhandEatProgress * itemFeatureVariables.offhandEatProgress);
                if (useAnimationName.equals("drink")) {
                    ItemAnimationMath.rotateX(poseStack, 15.0 * itemFeatureVariables.offhandEatProgress * itemFeatureVariables.offhandEatProgress);
                }
            }
            if (useAnimationName.equals("brush") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateZ(poseStack, -0.03 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBrushProgress));
                ItemAnimationMath.rotateX(poseStack, -30.0 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBrushProgress));
                ItemAnimationMath.rotateZ(poseStack, 15.0 * var5 * ItemAnimationMath.sin((itemFeatureVariables.mainHandEatPhase - 0.5) * 4.14) * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBrushProgress));
                ItemAnimationMath.rotateZ(poseStack, var5 * itemFeatureVariables.mainHandSwayRoll);
            }
            if (useAnimationName.equals("brush") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translateZ(poseStack, -0.03 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBrushProgress));
                ItemAnimationMath.rotateX(poseStack, -30.0 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBrushProgress));
                ItemAnimationMath.rotateZ(poseStack, 15.0 * var5 * ItemAnimationMath.sin((itemFeatureVariables.offhandEatPhase - 0.5) * 4.14) * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBrushProgress));
                ItemAnimationMath.rotateZ(poseStack, var5 * itemFeatureVariables.offhandSwayRoll);
            }
            if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.DOORS)) {
                ItemAnimationMath.translateX(poseStack, 0.1 * var5);
                ItemAnimationMath.translateZ(poseStack, 0.25);
                ItemAnimationMath.translateY(poseStack, -0.35);
                ItemAnimationMath.rotateZ(poseStack, -10.0 * var5);
                ItemAnimationMath.rotateY(poseStack, -90.0 * var5);
            } else if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.BEDS)) {
                ItemAnimationMath.translateZ(poseStack, 0.17);
                ItemAnimationMath.rotateAroundY(poseStack, -35.0 * var5, 0.3 * var5, -0.4, 0.0);
                ItemAnimationMath.scale(poseStack, 0.9, 0.9, 0.9);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SLIME_BALL) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SLIME_BLOCK) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.HONEY_BLOCK)) {
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SLIME_BALL)) {
                    ItemAnimationMath.translateY(poseStack, -0.1);
                    double var68 = itemFeatureVariables.verticalBob < 0.0 ? itemFeatureVariables.verticalBob * 0.06 : itemFeatureVariables.verticalBob * 0.12;
                    ItemAnimationMath.scale(poseStack, 1.0, 1.0 + var68, 1.0);
                    ItemAnimationMath.translateY(poseStack, 0.1);
                } else {
                    double var69 = itemFeatureVariables.verticalBob < 0.0 ? itemFeatureVariables.verticalBob * 0.05 : itemFeatureVariables.verticalBob * 0.1;
                    double var53 = itemFeatureVariables.verticalBob < 0.0 ? itemFeatureVariables.verticalBob * 0.1 : itemFeatureVariables.verticalBob * 0.3;
                    ItemAnimationMath.translateY(poseStack, 0.0);
                    ItemAnimationMath.scale(poseStack, 1.0 - var69, 1.0 + var53, 1.0 - var69);
                    ItemAnimationMath.translateY(poseStack, 0.0);
                    if (itemAnimationContext.rightHand) {
                        ItemAnimationMath.shear(poseStack, 0.0, 0.0 - var31 * 0.006, 0.0);
                    } else {
                        ItemAnimationMath.shear(poseStack, 0.0, 0.0 + var31 * 0.006, 0.0);
                    }
                }
            }
            if (inTag4) {
                ItemAnimationMath.translateX(poseStack, -0.09 * var5);
                ItemAnimationMath.rotateY(poseStack, 80.0 * var5);
            }
            itemFeatureVariables.previousPitch = PlayerAnimationUtils.getPitch(player);
            itemFeatureVariables.previousHeadYaw = PlayerAnimationUtils.getHeadYaw(player);
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.MAGMA_CREAM)) {
                ItemAnimationMath.scale(poseStack, 1.0 - itemFeatureVariables.verticalBob / 5.0, 1.0 + itemFeatureVariables.verticalBob / 5.0, 1.0);
            }
            double var70 = AnimationCurveMath.backIn(ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandEquipProgress : itemFeatureVariables.offhandEquipProgress, 0.09723, 0.60632) * 3.24 * 1.65 - 0.1));
            if ((ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.BUNDLES) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ENDER_PEARL) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ENDER_EYE) || ItemFeatureProperties.isThrowable(itemAnimationContext.stack) || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.MUSIC_DISCS) || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.NUGGETS) || inTag5) && !useAnimationName.equals("trident")) {
                ItemAnimationMath.rotateX(poseStack, -10.0 * var70);
                ItemAnimationMath.translateY(poseStack, 0.62 * var70);
                ItemAnimationMath.translateY(poseStack, ItemAnimationMath.clamp(0.1 * itemFeatureVariables.verticalBob, 0.0, 255.0));
                boolean var71 = itemAnimationContext.mainHand && itemAnimationContext.mainHandItemChanged || itemAnimationContext.offhandItemChanged;
                if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.NUGGETS)) {
                    if (var71) {
                        ItemAnimationSoundPlayer.play("entity.experience_orb.pickup", 0.3F);
                    }
                    ItemAnimationMath.translateY(poseStack, -0.07);
                    ItemAnimationMath.rotateAroundX(poseStack, 360.0 * AnimationCurveMath.backInOut(itemAnimationContext.mainHand ? ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress * 1.65, 0.0, 1.0) : ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress * 1.65, 0.0, 1.0)), 0.0, 0.1, 0.0);
                } else if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.MUSIC_DISCS)) {
                    if (var71) {
                        ItemAnimationSoundPlayer.play("entity.context.player.attack.weak", 0.3F);
                    }
                    ItemAnimationMath.rotateAroundZ(poseStack, 360.0 * AnimationCurveMath.backInOut(itemAnimationContext.mainHand ? ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress * 1.65, 0.0, 1.0) : ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress * 1.65, 0.0, 1.0)), -0.1 * var5, 0.25, 0.0);
                } else {
                    if (var71) {
                        ItemAnimationSoundPlayer.play("entity.context.player.attack.weak", 0.3F);
                    }
                    double var54 = itemAnimationContext.mainHand ? ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress * 1.2, 0.0, 1.0) : ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress * 1.2, 0.0, 1.0);
                    ItemAnimationMath.rotateZ(poseStack, -7.0 * var5 * ItemAnimationMath.sin(ItemAnimationMath.clamp(var54, 0.0943, 0.66791) * 7.07 * 1.5 - 0.8));
                }
            }
            double var72 = AnimationCurveMath.backInOut(itemFeatureVariables.mapHoldProgress);
            double var55 = AnimationCurveMath.backInOut(itemFeatureVariables.lookDownProgress);
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.FILLED_MAP)) {
                ItemAnimationMath.rotateZ(poseStack, 5.0 * var5 * var72);
                ItemAnimationMath.rotateY(poseStack, ((-40.0) - 20.0 * var55) * var5 * var72);
                ItemAnimationMath.rotateZ(poseStack, 15.0 * var5 * var72);
                ItemAnimationMath.rotateX(poseStack, -10.0 * var55 * var72);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.FILLED_MAP)) {
                double var57 = 1.0 - var72;
                ItemAnimationMath.translateZ(poseStack, -0.05 * var57);
                ItemAnimationMath.translateY(poseStack, -0.05 * var57);
                ItemAnimationMath.rotateX(poseStack, -40.0 * var57);
                ItemAnimationMath.rotateY(poseStack, -10.0 * var5 * var57);
                ItemAnimationMath.rotateZ(poseStack, 5.0 * var5 * var57);
            } else if (ItemFeatureProperties.shouldTransform(itemAnimationContext.stack) && !block && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BONE) && !useAnimationName.equals("bow") && !useAnimationName.equals("spear")) {
                ItemAnimationMath.translateX(poseStack, -0.05 * var5);
                ItemAnimationMath.rotateX(poseStack, -8.0);
                ItemAnimationMath.rotateY(poseStack, -10.0 * var5);
                ItemAnimationMath.rotateZ(poseStack, 6.0 * var5);
            }
            if (ItemFeatureProperties.isCustomItemEnabled(itemAnimationContext.stack)) {
                ItemAnimationMath.translateX(poseStack, -0.05 * var5);
                ItemAnimationMath.rotateX(poseStack, -8.0);
                ItemAnimationMath.rotateY(poseStack, -10.0 * var5);
                ItemAnimationMath.rotateZ(poseStack, 6.0 * var5);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SHEARS)) {
                if (!itemAnimationContext.rightHand) {
                    ItemAnimationMath.translateZ(poseStack, 0.1);
                    ItemAnimationMath.rotateY(poseStack, 180.0);
                }
                ItemAnimationMath.rotateZ(poseStack, 45.0);
            }
            if (inTag5 && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.DRAGON_HEAD)) {
                ItemAnimationMath.translateX(poseStack, -0.1 * var5);
                ItemAnimationMath.translateY(poseStack, 0.11);
                ItemAnimationMath.rotateZ(poseStack, 15.0 * var5);
                ItemAnimationMath.rotateY(poseStack, -85.0 * var5);
                ItemAnimationMath.rotateX(poseStack, -55.0);
            } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.DRAGON_HEAD)) {
                ItemAnimationMath.translateY(poseStack, 0.25);
                ItemAnimationMath.rotateZ(poseStack, 6.0 * var5);
                ItemAnimationMath.rotateY(poseStack, 160.0 * var5);
            }
            if (itemAnimationContext.mainHand && itemAnimationContext.mainHandItemChanged || itemAnimationContext.offhandItemChanged) {
                ItemAnimationSoundPlayer.play("context.item.armor.equip_leather", 0.2F);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BREWING_STAND) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.REDSTONE_TORCH) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.TORCH) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LANTERN) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SOUL_TORCH) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SOUL_LANTERN)) {
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BREWING_STAND) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.TORCH)) {
                    spawnItemGlow(itemAnimationContext, 0.5 * var5, 0.6, 0.5, 1.3, ORANGE_GLOW, 200.0 + 20.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LANTERN)) {
                    spawnItemGlow(itemAnimationContext, 0.45 * var5, 0.15, 0.5, 1.3, ORANGE_GLOW, 200.0 + 20.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                } else if (lantern && registryId.contains("copper")) {
                    spawnItemGlow(itemAnimationContext, 0.45 * var5, 0.15, 0.5, 1.3, ORANGE_GLOW, 200.0 + 20.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SOUL_TORCH)) {
                    spawnItemGlow(itemAnimationContext, 0.5 * var5, 0.6, 0.5, 1.3, BLUE_GLOW, 110.0 + 10.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SOUL_LANTERN)) {
                    spawnItemGlow(itemAnimationContext, 0.45 * var5, 0.15, 0.5, 1.3, BLUE_GLOW, 110.0 + 10.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.REDSTONE_TORCH)) {
                    spawnItemGlow(itemAnimationContext, 0.5 * var5, 0.6, 0.5, 1.3, RED_GLOW, 110.0 + 10.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
                }
            }
            int var73 = itemFeatureVariables.inspectKey != 0 ? itemFeatureVariables.inspectKey : 67;
            if (ItemAnimationChecks.isAnimationKeyPressed(var73)) {
                itemFeatureVariables.inspectFlourish = itemFeatureVariables.inspectFlourish + 0.025 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.inspectFlourish = 0.0;
            }
            itemFeatureVariables.inspectFlourish = ItemAnimationMath.clamp(itemFeatureVariables.inspectFlourish, 0.0, 1.0);
            if ((inTag || inTag2 || inTag3 || useAnimationName.equals("trident")) && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, -0.2 * var5 * itemFeatureVariables.inspectProgress);
                ItemAnimationMath.rotateAroundX(poseStack, -360.0 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectFlourish), 0.0, 0.0, 0.15);
            }
            itemFeatureVariables.lastTickCount = PlayerAnimationUtils.getTickCount(player);
            if (itemFeatureVariables.lastSwingCount != PlayerAnimationUtils.getSwingCount(player) && itemAnimationContext.mainHand && ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL)) {
                ItemAnimationSoundPlayer.play("block.bell.use", 0.3F);
            }
            itemFeatureVariables.lastSwingCount = PlayerAnimationUtils.getSwingCount(player);
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER)) {
                Identifier identifier;
                Identifier identifier2;
                Identifier identifier3;
                if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS)) {
                    identifier = PINK_PETALS_SMALL;
                    identifier2 = PINK_PETALS_MEDIUM;
                    identifier3 = PINK_PETALS_LARGE;
                } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS)) {
                    identifier = WILDFLOWERS_SMALL;
                    identifier2 = WILDFLOWERS_MEDIUM;
                    identifier3 = WILDFLOWERS_LARGE;
                } else {
                    identifier = LEAF_LITTER_SMALL;
                    identifier2 = LEAF_LITTER_MEDIUM;
                    identifier3 = LEAF_LITTER_LARGE;
                }
                if (itemFeatureVariables.mainHandSwingStartedSnapshot != itemAnimationContext.mainHandSwingStarted && itemAnimationContext.mainHand) {
                    ItemAnimationSoundPlayer.play("block.leaf_litter.place", 0.7F);
                    spawnPetals(itemAnimationContext, var5, identifier, identifier2, identifier3);
                } else if (itemFeatureVariables.lastOffhandSwingStarted != itemAnimationContext.offhandSwingStarted && !itemAnimationContext.mainHand) {
                    ItemAnimationSoundPlayer.play("block.leaf_litter.place", 0.7F);
                    spawnPetals(itemAnimationContext, var5, identifier, identifier2, identifier3);
                }
            }
            if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandSwingStartedSnapshot = itemAnimationContext.mainHandSwingStarted;
            } else {
                itemFeatureVariables.lastOffhandSwingStarted = itemAnimationContext.offhandSwingStarted;
            }
            if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.COPPER_GOLEM_STATUES)) {
                itemFeatureVariables.blockModelItems.put(registryId, false);
            }
            if (inTag4) {
                itemFeatureVariables.weaponChargeTicks.put(registryId, 14);
            }
            double var74 = AnimationCurveMath.quartIn(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandEatProgress : itemFeatureVariables.offhandEatProgress);
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.AXOLOTL_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.POWDER_SNOW_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PUFFERFISH_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.TADPOLE_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.SALMON_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.COD_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.TROPICAL_FISH_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WATER_BUCKET) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LAVA_BUCKET)) {
                ItemAnimationMath.translateY(poseStack, 0.025);
                ItemAnimationMath.translateX(poseStack, 0.0 * var5);
                ItemAnimationMath.translateZ(poseStack, -0.1);
                ItemAnimationMath.rotateY(poseStack, 180.0);
                ItemAnimationMath.rotateX(poseStack, -82.5);
                ItemAnimationMath.rotateZ(poseStack, -20.0 * var5);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.MILK_BUCKET)) {
                ItemAnimationMath.translateY(poseStack, 0.025);
                ItemAnimationMath.translateX(poseStack, 0.0 * var5);
                ItemAnimationMath.translateZ(poseStack, -0.1);
                ItemAnimationMath.rotateY(poseStack, 180.0);
                ItemAnimationMath.rotateX(poseStack, -82.5);
                ItemAnimationMath.rotateZ(poseStack, -20.0 * var5);
                ItemAnimationMath.rotateX(poseStack, 0.0 * var74);
                ItemAnimationMath.rotateZ(poseStack, 30.0 * var5 * var74);
                ItemAnimationMath.rotateY(poseStack, 0.0 * var5 * var74);
                ItemAnimationMath.translateX(poseStack, 0.0 * var5 * var74);
                ItemAnimationMath.translateY(poseStack, 0.1 * var74);
                ItemAnimationMath.translateZ(poseStack, 0.02 * var74);
            }
            if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LAVA_BUCKET)) {
                spawnItemGlow(itemAnimationContext, -0.05 * var5, 0.0, 0.0, 2.0, ORANGE_GLOW, 150.0 + 20.0 * ItemAnimationMath.sin(PlayerAnimationUtils.getTickCount(player) * 0.2));
            }
            if (registryId.equals("bucket_of_frog:frog_bucket_cold") || registryId.equals("bucket_of_frog:frog_bucket_warm") || registryId.equals("bucket_of_frog:frog_bucket_temperate")) {
                ItemAnimationMath.translateY(poseStack, 0.025);
                ItemAnimationMath.translateX(poseStack, 0.0 * var5);
                ItemAnimationMath.translateZ(poseStack, -0.1);
                ItemAnimationMath.rotateY(poseStack, 180.0);
                ItemAnimationMath.rotateX(poseStack, -82.5);
                ItemAnimationMath.rotateZ(poseStack, -20.0 * var5);
            }
            if (useAnimationName.equals("trident")) {
                double var75 = AnimationCurveMath.backOut(ItemAnimationMath.clamp(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandTridentRaise : itemFeatureVariables.offhandTridentRaise * 1.5, 0.0, 1.0));
                ItemAnimationMath.rotateX(poseStack, 180.0 * var75);
                ItemAnimationMath.rotateZ(poseStack, 170.0 * var5 * var75);
                ItemAnimationMath.translateZ(poseStack, -0.1);
                ItemAnimationMath.rotateY(poseStack, 40.0 * var5);
            }
            if (useAnimationName.equals("spear")) {
                ItemAnimationMath.translateZ(poseStack, -0.1);
                ItemAnimationMath.rotateY(poseStack, 10.0 * var5);
            }
            if (useAnimationName.equals("trident")) {
                ItemAnimationMath.rotateX(poseStack, -90.0 * AnimationCurveMath.backOut(ItemAnimationMath.sin(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandTridentSpin : itemFeatureVariables.offhandTridentSpin * 3.14)));
                ItemAnimationMath.rotateZ(poseStack, -45.0 * var5 * AnimationCurveMath.backOut(ItemAnimationMath.sin(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandTridentSpin : itemFeatureVariables.offhandTridentSpin * 3.14)));
            }
            if (inTag6 || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.DOORS) || inTag5 || ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.SIGNS)) {
                itemFeatureVariables.itemBlockModelOverrides.put(registryId, false);
            }
            if (block && itemFeatureVariables.itemBlockModelOverrides.getOrDefault(registryId, true) && itemFeatureVariables.blockModelItems.getOrDefault(registryId, true) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.REDSTONE) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL)) {
                ItemAnimationMath.translateZ(poseStack, -0.05);
                if (!lantern) {
                    ItemAnimationMath.translateY(poseStack, -0.15);
                    ItemAnimationMath.rotateZ(poseStack, 6.0 * var5);
                    ItemAnimationMath.rotateX(poseStack, -8.0);
                }
                ItemAnimationMath.rotateY(poseStack, 25.0 * var5);
                ItemAnimationMath.scale(poseStack, 1.1, 1.1, 1.1);
            }
            double var76 = AnimationCurveMath.backOut(itemFeatureVariables.mainHandBowTension);
            double var62 = AnimationCurveMath.backOut(itemFeatureVariables.offhandBowTension);
            double var64 = itemAnimationContext.mainHand ? var76 : var62;
            if (var64 < 0.1) {
                itemFeatureVariables.itemPullingStates.put("minecraft:bow", false);
            } else {
                itemFeatureVariables.itemPullingStates.put("minecraft:bow", true);
            }
            itemFeatureVariables.itemPullAngles.put("minecraft:bow", AnimationCurveMath.smoothstep(var64) * 20.0);
        }
    }
}
