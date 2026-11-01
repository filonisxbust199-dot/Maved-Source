package ess.ll.client.features.inventory;

import ess.ll.client.render.AnimationCurveMath;
import ess.ll.client.render.ItemAnimationChecks;
import ess.ll.client.render.ItemAnimationContext;
import ess.ll.client.render.ItemAnimationCurves;
import ess.ll.client.render.ItemAnimationMath;
import ess.ll.client.render.PlayerAnimationUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class WeaponItemTypes {
    private WeaponItemTypes() {
    }

    public static void animateWeapon(ItemAnimationContext itemAnimationContext) {
        ItemFeatureVariables itemFeatureVariables = ItemFeatureVariables.INSTANCE;
        AbstractClientPlayer player = itemAnimationContext.player;
        if (player != null) {
            String useAnimationName = ItemFeatureProperties.getUseAnimationName(itemAnimationContext.stack);
            boolean usingItem = PlayerAnimationUtils.isUsingItem(player);
            InteractionHand usedHand = PlayerAnimationUtils.getUsedHand(player);
            PoseStack poseStack = itemAnimationContext.poseStack;
            double speed = PlayerAnimationUtils.getSpeed(player);
            ItemStack stack = itemAnimationContext.stack;
            boolean empty = ItemFeatureProperties.isEmpty(stack);
            boolean block = ItemFeatureProperties.isBlock(stack);
            boolean inTag = ItemFeatureProperties.isInTag(stack, ItemTagAliases.SWORDS);
            boolean inTag2 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.PICKAXES);
            boolean inTag3 = ItemFeatureProperties.isInTag(stack, ItemTagAliases.AXES);
            boolean item = ItemFeatureProperties.isItem(stack, Items.MACE);
            boolean item2 = ItemFeatureProperties.isItem(stack, Items.FILLED_MAP);
            boolean chargedCrossbow = ItemFeatureProperties.isChargedCrossbow(stack);
            if (useAnimationName.equals("spear")) {
                itemAnimationContext.equipProgress = 0.0F;
            }
            double var18 = itemAnimationContext.rightHand ? 1.0 : -1.0;
            if (inTag2) {
                itemAnimationContext.swingProgress = (float) ItemAnimationCurves.swingBezier(itemAnimationContext.swingProgress);
            } else {
                itemAnimationContext.swingProgress = (float) ItemAnimationCurves.recoveryBezier(itemAnimationContext.swingProgress);
            }
            ItemAnimationMath.translateX(poseStack, 0.2 * var18);
            double var20;
            if (itemAnimationContext.swingProgress < 0.70016) {
                var20 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.0, 0.308) * 5.1);
            } else {
                var20 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.70016, 1.0) * 5.1 - 2.0);
            }
            double var22;
            if (itemAnimationContext.swingProgress < 0.65245) {
                var22 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.0, 0.16675) * 3.14 * 3.0);
            } else {
                var22 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.65245, 1.0) * 4.4 - 1.2584);
            }
            var20 = var20 * var20 * var20;
            double var24 = ItemAnimationMath.clamp(ItemAnimationMath.sin(itemAnimationContext.swingProgress * 4.78), 0.0, 1.0);
            double var26 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.16561, 0.49422) * 4.78 * 2.0 + 4.7);
            double var28;
            if (itemAnimationContext.swingProgress < 0.65594) {
                var28 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.16561, 0.32991) * 4.78 * 2.0 + 4.7);
            } else {
                var28 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemAnimationContext.swingProgress, 0.65594, 0.82025) * 4.78 * 2.0 - 4.7);
            }
            if (usingItem && useAnimationName.equals("bow") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandBowDraw = itemFeatureVariables.offhandBowDraw + 0.075 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandBowDraw = itemFeatureVariables.offhandBowDraw - 0.07 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandBowDraw = ItemAnimationMath.clamp(itemFeatureVariables.offhandBowDraw, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("bow") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.offhandBowDraw == 1.0) {
                itemFeatureVariables.offhandBowTension = itemFeatureVariables.offhandBowTension + 0.025 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.offhandBowShake = itemFeatureVariables.offhandBowShake + 0.07 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandBowTension = itemFeatureVariables.offhandBowTension - 0.11 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandBowTension = ItemAnimationMath.clamp(itemFeatureVariables.offhandBowTension, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("bow") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.mainHandBowDraw = itemFeatureVariables.mainHandBowDraw + 0.075 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandBowDraw = itemFeatureVariables.mainHandBowDraw - 0.07 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandBowDraw = ItemAnimationMath.clamp(itemFeatureVariables.mainHandBowDraw, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("bow") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.mainHandBowDraw == 1.0) {
                itemFeatureVariables.mainHandBowTension = itemFeatureVariables.mainHandBowTension + 0.025 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.mainHandBowShake = itemFeatureVariables.mainHandBowShake + 0.07 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandBowTension = 0.0;
            }
            itemFeatureVariables.mainHandBowTension = ItemAnimationMath.clamp(itemFeatureVariables.mainHandBowTension, 0.0, 1.0);
            double var30 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSwayPitch : itemFeatureVariables.offhandSwayPitch;
            if (!itemAnimationContext.mainHand) {
                ;
            }
            if (speed > 0.05) {
                itemFeatureVariables.movePhase = itemFeatureVariables.movePhase + speed * 2.0 * itemAnimationContext.frameDelta * 30.0;
            }
            if (PlayerAnimationUtils.isInWater(player)) {
                itemFeatureVariables.waterProgress = itemFeatureVariables.waterProgress + 0.07 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.waterProgress = itemFeatureVariables.waterProgress - 0.07 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.waterProgress = ItemAnimationMath.clamp(itemFeatureVariables.waterProgress, 0.0, 1.0);
            double var34 = itemFeatureVariables.offsetX;
            ItemAnimationMath.translate(poseStack, itemFeatureVariables.offsetX * var18, itemFeatureVariables.offsetY, itemFeatureVariables.offsetZ);
            itemFeatureVariables.swingRollPhase = itemFeatureVariables.swingRollPhase + itemAnimationContext.swingProgress * itemAnimationContext.frameDelta * 3.0F;
            if (!empty) {
                ItemAnimationMath.translateZ(poseStack, -0.16);
            } else {
                ItemAnimationMath.translateZ(poseStack, -0.08);
            }
            if (item2 && itemAnimationContext.mainHand && ItemFeatureProperties.isEmpty(PlayerAnimationUtils.getOffhandItem(player))) {
                itemFeatureVariables.mapHoldProgress = itemFeatureVariables.mapHoldProgress + 0.07 * itemAnimationContext.frameDelta * 30.0;
            } else if (item2) {
                itemFeatureVariables.mapHoldProgress = itemFeatureVariables.mapHoldProgress - 0.07 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mapHoldProgress = ItemAnimationMath.clamp(itemFeatureVariables.mapHoldProgress, 0.0, 1.0);
            if (itemAnimationContext.mainHandItemChanged && itemAnimationContext.mainHand && itemFeatureVariables.mainHandDrinkProgress == 0.0) {
                itemFeatureVariables.mainHandEquipProgress = 0.0;
            }
            itemFeatureVariables.mainHandEquipProgress = itemFeatureVariables.mainHandEquipProgress + 0.015 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandEquipProgress = ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress, 0.0, 1.0);
            if (itemAnimationContext.mainHand) {
                double var36 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress, 0.0, 0.5) * 3.14) * itemFeatureVariables.mainHandEquipScale;
                double var38 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.mainHandEquipProgress, 0.0, 0.125) * 12.56) * itemFeatureVariables.mainHandEquipScale;
                var36 = AnimationCurveMath.backInOut(var36);
                if (useAnimationName.equals("trident") && itemFeatureVariables.mainHandTridentRaise > 0.9) {
                    ItemAnimationMath.translate(poseStack, 0.0, -0.15 * var38, -0.3 * var38);
                    ItemAnimationMath.rotateAroundX(poseStack, 75.0 * var38, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -75.0 * var36, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.translate(poseStack, 0.0, 0.15 * var38, 0.3 * var38);
                } else {
                    if (useAnimationName.equals("crossbow")) {
                        ItemAnimationMath.translateY(poseStack, -0.25 * var38);
                    }
                    ItemAnimationMath.translate(poseStack, 0.0 * var38, 0.0, -0.2 * var38);
                    ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var18 * var38, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -55.0 * var38, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundZ(poseStack, 40.0 * var18 * var38, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundZ(poseStack, -40.0 * var18 * var36, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 55.0 * var36, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundY(poseStack, -25.0 * var18 * var36, 0.3 * var18, -0.4, 0.0);
                    if (useAnimationName.equals("crossbow")) {
                        ItemAnimationMath.translateY(poseStack, 0.25 * var36);
                    }
                    ItemAnimationMath.translate(poseStack, 0.0 * var36, 0.0, 0.2 * var36);
                }
            }
            if (itemAnimationContext.offhandItemChanged) {
                itemFeatureVariables.offhandEquipProgress = 0.0;
            }
            itemFeatureVariables.offhandEquipProgress = itemFeatureVariables.offhandEquipProgress + 0.015 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandEquipProgress = ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress, 0.0, 1.0);
            if (!itemAnimationContext.mainHand && itemFeatureVariables.offhandEatProgress == 0.0) {
                double var103 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress, 0.0, 0.5) * 3.14) * itemFeatureVariables.offhandEquipScale;
                double var106 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.offhandEquipProgress, 0.0, 0.125) * 12.56) * itemFeatureVariables.offhandEquipScale;
                var103 = AnimationCurveMath.backInOut(var103);
                if (useAnimationName.equals("crossbow")) {
                    ItemAnimationMath.translateY(poseStack, -0.25 * var106);
                }
                ItemAnimationMath.translate(poseStack, 0.0 * var106, 0.0, -0.2 * var106);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var106 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -55.0 * var106, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 40.0 * var106 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, -40.0 * var103 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 55.0 * var103, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, -25.0 * var103 * var18, 0.3 * var18, -0.4, 0.0);
                if (useAnimationName.equals("crossbow")) {
                    ItemAnimationMath.translateY(poseStack, 0.25 * var106);
                }
                ItemAnimationMath.translate(poseStack, 0.0 * var103, 0.0, 0.2 * var103);
            }
            if (!PlayerAnimationUtils.isOnGround(player) && itemAnimationContext.mainHandSwingProgress == 0.0F && itemAnimationContext.mainHand) {
                itemFeatureVariables.canAlternateSwing = false;
            } else if (itemAnimationContext.mainHand && itemAnimationContext.mainHandSwingProgress == 0.0F) {
                itemFeatureVariables.canAlternateSwing = true;
            }
            if (itemFeatureVariables.lastMainHandSwingStarted != itemAnimationContext.mainHandSwingStarted && itemAnimationContext.mainHand) {
                itemFeatureVariables.alternateSwing = !itemFeatureVariables.alternateSwing;
            }
            if (PlayerAnimationUtils.isCrawling(player) && speed > 0.08) {
                itemFeatureVariables.crawlPhase = itemFeatureVariables.crawlPhase + speed * itemAnimationContext.frameDelta * 30.0;
            }
            if (PlayerAnimationUtils.getPitch(player) > 40.0) {
                itemFeatureVariables.lookDownProgress = itemFeatureVariables.lookDownProgress + 0.05 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.lookDownProgress = itemFeatureVariables.lookDownProgress - 0.095 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.lookDownProgress = ItemAnimationMath.clamp(itemFeatureVariables.lookDownProgress, 0.0, 1.0);
            double var105 = itemFeatureVariables.mainHandSpearFastCharge;
            double var107 = itemFeatureVariables.mainHandSpearSlowCharge;
            double var40 = itemFeatureVariables.offhandSpearFastCharge;
            double var42 = itemFeatureVariables.offhandSpearSlowCharge;
            if (usingItem && useAnimationName.equals("spear") && usedHand == itemAnimationContext.hand && itemAnimationContext.mainHand && ItemFeatureProperties.getFeatureFlags(itemAnimationContext.stack).spearAnimation) {
                ItemFeatureFlags featureFlags = ItemFeatureProperties.getFeatureFlags(itemAnimationContext.stack);
                itemFeatureVariables.mainHandSpearUse = itemFeatureVariables.mainHandSpearUse + 0.08 * itemAnimationContext.frameDelta * 30.0;
                if (!featureFlags.slowChargeDisabled) {
                    itemFeatureVariables.mainHandSpearSlowCharge = itemFeatureVariables.mainHandSpearSlowCharge + 0.035 * itemAnimationContext.frameDelta * 30.0;
                }
                if (!featureFlags.fastChargeDisabled) {
                    itemFeatureVariables.mainHandSpearFastCharge = itemFeatureVariables.mainHandSpearFastCharge + 0.055 * itemAnimationContext.frameDelta * 30.0;
                }
                if (featureFlags.thrustEnabled) {
                    itemFeatureVariables.mainHandSpearThrust = itemFeatureVariables.mainHandSpearThrust + 0.08 * itemAnimationContext.frameDelta * 30.0;
                }
                if (itemFeatureVariables.mainHandSpearThrust > 0.0) {
                    itemFeatureVariables.mainHandSpearThrust = itemFeatureVariables.mainHandSpearThrust + 0.08 * itemAnimationContext.frameDelta * 30.0;
                }
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandSpearUse = itemFeatureVariables.mainHandSpearUse - 0.08 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.mainHandSpearSlowCharge = 0.0;
                itemFeatureVariables.mainHandSpearFastCharge = 0.0;
                itemFeatureVariables.spearResetValue = 0.0;
                itemFeatureVariables.mainHandSpearThrust = 0.0;
            }
            if (itemFeatureVariables.mainHandSpearThrust >= 1.0) {
                itemFeatureVariables.mainHandSpearThrust = 0.0;
            }
            itemFeatureVariables.mainHandSpearUse = ItemAnimationMath.clamp(itemFeatureVariables.mainHandSpearUse, 0.0, 1.0) * ItemAnimationMath.clamp(1.0 - ItemAnimationMath.clamp(var24 * 8.0, 0.0, 1.0), 0.0, 1.0);
            itemFeatureVariables.mainHandSpearSlowCharge = ItemAnimationMath.clamp(itemFeatureVariables.mainHandSpearSlowCharge, 0.0, 1.0);
            itemFeatureVariables.mainHandSpearFastCharge = ItemAnimationMath.clamp(itemFeatureVariables.mainHandSpearFastCharge, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("spear") && usedHand == itemAnimationContext.hand && !itemAnimationContext.mainHand && ItemFeatureProperties.getFeatureFlags(itemAnimationContext.stack).spearAnimation) {
                ItemFeatureFlags featureFlags2 = ItemFeatureProperties.getFeatureFlags(itemAnimationContext.stack);
                itemFeatureVariables.offhandSpearUse = itemFeatureVariables.offhandSpearUse + 0.08 * itemAnimationContext.frameDelta * 30.0;
                if (!featureFlags2.slowChargeDisabled) {
                    itemFeatureVariables.offhandSpearSlowCharge = itemFeatureVariables.offhandSpearSlowCharge + 0.035 * itemAnimationContext.frameDelta * 30.0;
                }
                if (!featureFlags2.fastChargeDisabled) {
                    itemFeatureVariables.offhandSpearFastCharge = itemFeatureVariables.offhandSpearFastCharge + 0.055 * itemAnimationContext.frameDelta * 30.0;
                }
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandSpearUse = itemFeatureVariables.offhandSpearUse - 0.08 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.offhandSpearSlowCharge = 0.0;
                itemFeatureVariables.offhandSpearFastCharge = 0.0;
                itemFeatureVariables.spearResetValue = 0.0;
            }
            itemFeatureVariables.offhandSpearUse = ItemAnimationMath.clamp(itemFeatureVariables.offhandSpearUse, 0.0, 1.0) * ItemAnimationMath.clamp(1.0 - ItemAnimationMath.clamp(var24 * 8.0, 0.0, 1.0), 0.0, 1.0);
            itemFeatureVariables.offhandSpearSlowCharge = ItemAnimationMath.clamp(itemFeatureVariables.offhandSpearSlowCharge, 0.0, 1.0);
            itemFeatureVariables.offhandSpearFastCharge = ItemAnimationMath.clamp(itemFeatureVariables.offhandSpearFastCharge, 0.0, 1.0);
            if (itemFeatureVariables.mainHandSpearSlowCharge == 0.0) {
                itemFeatureVariables.mainHandSpearSlowCharge = ItemAnimationMath.lerp(0.5 * itemAnimationContext.frameDelta * 30.0, var107, itemFeatureVariables.mainHandSpearSlowCharge);
            }
            if (itemFeatureVariables.mainHandSpearFastCharge == 0.0) {
                itemFeatureVariables.mainHandSpearFastCharge = ItemAnimationMath.lerp(0.5 * itemAnimationContext.frameDelta * 30.0, var105, itemFeatureVariables.mainHandSpearFastCharge);
            }
            if (itemFeatureVariables.offhandSpearSlowCharge == 0.0) {
                itemFeatureVariables.offhandSpearSlowCharge = ItemAnimationMath.lerp(0.5 * itemAnimationContext.frameDelta * 30.0, var42, itemFeatureVariables.offhandSpearSlowCharge);
            }
            if (itemFeatureVariables.offhandSpearFastCharge == 0.0) {
                itemFeatureVariables.offhandSpearFastCharge = ItemAnimationMath.lerp(0.5 * itemAnimationContext.frameDelta * 30.0, var40, itemFeatureVariables.offhandSpearFastCharge);
            }
            if (usingItem && (useAnimationName.equals("eat") || useAnimationName.equals("drink") || useAnimationName.equals("toot_horn")) && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.mainHandEatProgress = itemFeatureVariables.mainHandEatProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandEatProgress = itemFeatureVariables.mainHandEatProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandEatProgress = ItemAnimationMath.clamp(itemFeatureVariables.mainHandEatProgress, 0.0, 1.0);
            if (usingItem && (useAnimationName.equals("eat") || useAnimationName.equals("drink") || useAnimationName.equals("toot_horn") || useAnimationName.equals("brush")) && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && (itemFeatureVariables.mainHandEatProgress == 1.0 || itemFeatureVariables.mainHandBrushProgress > 0.4)) {
                itemFeatureVariables.mainHandEatPhase = itemFeatureVariables.mainHandEatPhase + 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            if (usingItem && (useAnimationName.equals("eat") || useAnimationName.equals("drink") || useAnimationName.equals("toot_horn")) && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandEatProgress = itemFeatureVariables.offhandEatProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandEatProgress = itemFeatureVariables.offhandEatProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandEatProgress = ItemAnimationMath.clamp(itemFeatureVariables.offhandEatProgress, 0.0, 1.0);
            if (usingItem && (useAnimationName.equals("eat") || useAnimationName.equals("drink") || useAnimationName.equals("toot_horn") || useAnimationName.equals("brush")) && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && (itemFeatureVariables.offhandEatProgress == 1.0 || itemFeatureVariables.offhandBrushProgress > 0.4)) {
                itemFeatureVariables.offhandEatPhase = itemFeatureVariables.offhandEatPhase + 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            if (usingItem && useAnimationName.equals("drink") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.mainHandEatProgress == 1.0) {
                itemFeatureVariables.mainHandDrinkProgress = itemFeatureVariables.mainHandDrinkProgress + 0.04 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandDrinkProgress = itemFeatureVariables.mainHandDrinkProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandDrinkProgress = ItemAnimationMath.clamp(itemFeatureVariables.mainHandDrinkProgress, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("drink") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.offhandEatProgress == 1.0) {
                itemFeatureVariables.offhandDrinkProgress = itemFeatureVariables.offhandDrinkProgress + 0.04 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandDrinkProgress = itemFeatureVariables.offhandDrinkProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandDrinkProgress = ItemAnimationMath.clamp(itemFeatureVariables.offhandDrinkProgress, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("crossbow") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandCrossbowLoad = itemFeatureVariables.offhandCrossbowLoad + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandCrossbowLoad = itemFeatureVariables.offhandCrossbowLoad - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandCrossbowLoad = ItemAnimationMath.clamp(itemFeatureVariables.offhandCrossbowLoad, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("crossbow") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.offhandCrossbowLoad == 1.0) {
                itemFeatureVariables.offhandCrossbowTension = itemFeatureVariables.offhandCrossbowTension + 0.02 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandCrossbowTension = itemFeatureVariables.offhandCrossbowTension - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandCrossbowTension = ItemAnimationMath.clamp(itemFeatureVariables.offhandCrossbowTension, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("crossbow") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && !chargedCrossbow) {
                itemFeatureVariables.mainHandCrossbowLoad = itemFeatureVariables.mainHandCrossbowLoad + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandCrossbowLoad = itemFeatureVariables.mainHandCrossbowLoad - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandCrossbowLoad = ItemAnimationMath.clamp(itemFeatureVariables.mainHandCrossbowLoad, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("crossbow") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand && itemFeatureVariables.mainHandCrossbowLoad == 1.0 && !chargedCrossbow) {
                itemFeatureVariables.mainHandCrossbowTension = itemFeatureVariables.mainHandCrossbowTension + 0.02 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandCrossbowTension = itemFeatureVariables.mainHandCrossbowTension - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandCrossbowTension = ItemAnimationMath.clamp(itemFeatureVariables.mainHandCrossbowTension, 0.0, 1.0);
            ItemStack mainHandItem = PlayerAnimationUtils.getMainHandItem(player);
            String useAnimationName2 = ItemFeatureProperties.getUseAnimationName(mainHandItem);
            boolean var46 = !ItemFeatureProperties.isChargedCrossbow(mainHandItem);
            boolean var47 = var46 && !useAnimationName2.equals("block") && !useAnimationName2.equals("eat") && !useAnimationName2.equals("toot_horn") && !useAnimationName2.equals("drink") && !useAnimationName2.equals("brush") && !useAnimationName2.equals("spear");
            if (!itemAnimationContext.mainHand && empty && (!usingItem || !var47) && !PlayerAnimationUtils.isClimbing(player) && !PlayerAnimationUtils.isSwimming(player) && !PlayerAnimationUtils.isCrawling(player)) {
                itemFeatureVariables.emptyOffhandRaise = itemFeatureVariables.emptyOffhandRaise + 0.08 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand || usingItem && var47 || PlayerAnimationUtils.isClimbing(player) || PlayerAnimationUtils.isCrawling(player)) {
                itemFeatureVariables.emptyOffhandRaise = itemFeatureVariables.emptyOffhandRaise - 0.08 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.emptyOffhandRaise = ItemAnimationMath.clamp(itemFeatureVariables.emptyOffhandRaise, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("brush") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.mainHandBrushProgress = itemFeatureVariables.mainHandBrushProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandBrushProgress = itemFeatureVariables.mainHandBrushProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandBrushProgress = ItemAnimationMath.clamp(itemFeatureVariables.mainHandBrushProgress, 0.0, 1.0) * itemFeatureVariables.brushScale;
            if (usingItem && useAnimationName.equals("brush") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandBrushProgress = itemFeatureVariables.offhandBrushProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandBrushProgress = itemFeatureVariables.offhandBrushProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandBrushProgress = ItemAnimationMath.clamp(itemFeatureVariables.offhandBrushProgress, 0.0, 1.0) * itemFeatureVariables.brushScale;
            if (usingItem && useAnimationName.equals("trident") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandTridentRaise = itemFeatureVariables.offhandTridentRaise + 0.07 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.offhandTridentPullback = itemFeatureVariables.offhandTridentPullback + 0.045 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.offhandTridentShake = itemFeatureVariables.offhandTridentShake + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                if (PlayerAnimationUtils.isSpinAttacking(player)) {
                    itemFeatureVariables.offhandTridentRaise = itemFeatureVariables.offhandTridentRaise - 0.57 * itemAnimationContext.frameDelta * 30.0;
                    itemFeatureVariables.offhandTridentPullback = itemFeatureVariables.offhandTridentPullback - 0.53 * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.offhandTridentRaise = itemFeatureVariables.offhandTridentRaise - 0.1 * itemAnimationContext.frameDelta * 30.0;
                    itemFeatureVariables.offhandTridentPullback = itemFeatureVariables.offhandTridentPullback - 0.07 * itemAnimationContext.frameDelta * 30.0;
                }
                itemFeatureVariables.offhandTridentShake = itemFeatureVariables.offhandTridentShake - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandTridentRaise = ItemAnimationMath.clamp(itemFeatureVariables.offhandTridentRaise, 0.0, 1.0);
            itemFeatureVariables.offhandTridentPullback = ItemAnimationMath.clamp(itemFeatureVariables.offhandTridentPullback, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("trident") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.mainHandTridentRaise = itemFeatureVariables.mainHandTridentRaise + 0.07 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.mainHandTridentPullback = itemFeatureVariables.mainHandTridentPullback + 0.05 * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.mainHandTridentShake = itemFeatureVariables.mainHandTridentShake + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else if (itemAnimationContext.mainHand) {
                if (PlayerAnimationUtils.isSpinAttacking(player)) {
                    itemFeatureVariables.mainHandTridentRaise = itemFeatureVariables.mainHandTridentRaise - 0.57 * itemAnimationContext.frameDelta * 30.0;
                    itemFeatureVariables.mainHandTridentPullback = itemFeatureVariables.mainHandTridentPullback - 0.53 * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.mainHandTridentRaise = itemFeatureVariables.mainHandTridentRaise - 0.1 * itemAnimationContext.frameDelta * 30.0;
                    itemFeatureVariables.mainHandTridentPullback = itemFeatureVariables.mainHandTridentPullback - 0.07 * itemAnimationContext.frameDelta * 30.0;
                }
                itemFeatureVariables.mainHandTridentShake = itemFeatureVariables.mainHandTridentShake - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandTridentPullback = itemFeatureVariables.mainHandTridentPullback * ItemAnimationMath.pow(0.95, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.mainHandTridentRaise = ItemAnimationMath.clamp(itemFeatureVariables.mainHandTridentRaise, 0.0, 1.0);
            itemFeatureVariables.mainHandTridentPullback = ItemAnimationMath.clamp(itemFeatureVariables.mainHandTridentPullback, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("block") && itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                if (inTag) {
                    itemFeatureVariables.mainHandBlockProgress = itemFeatureVariables.mainHandBlockProgress + 0.12 * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.mainHandBlockProgress = itemFeatureVariables.mainHandBlockProgress + 0.07 * itemAnimationContext.frameDelta * 30.0;
                }
            } else if (itemAnimationContext.mainHand) {
                if (inTag) {
                    itemFeatureVariables.mainHandBlockProgress = itemFeatureVariables.mainHandBlockProgress - 0.12 * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.mainHandBlockProgress = itemFeatureVariables.mainHandBlockProgress - 0.07 * itemAnimationContext.frameDelta * 30.0;
                }
            }
            itemFeatureVariables.mainHandBlockProgress = itemFeatureVariables.mainHandBlockProgress - itemFeatureVariables.blockAttackProgress * 0.04 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.mainHandBlockProgress = ItemAnimationMath.clamp(itemFeatureVariables.mainHandBlockProgress, 0.0, 1.0);
            if (usingItem && useAnimationName.equals("block") && !itemAnimationContext.mainHand && usedHand == itemAnimationContext.hand) {
                itemFeatureVariables.offhandBlockProgress = itemFeatureVariables.offhandBlockProgress + 0.07 * itemAnimationContext.frameDelta * 30.0;
            } else if (!itemAnimationContext.mainHand) {
                itemFeatureVariables.offhandBlockProgress = itemFeatureVariables.offhandBlockProgress - 0.07 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.offhandBlockProgress = itemFeatureVariables.offhandBlockProgress - itemFeatureVariables.blockAttackProgress * 0.04 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.offhandBlockProgress = ItemAnimationMath.clamp(itemFeatureVariables.offhandBlockProgress, 0.0, 1.0);
            if (speed > 0.08 && ItemAnimationMath.abs(PlayerAnimationUtils.getVelocityY(player)) < 0.08 && PlayerAnimationUtils.isOnGround(player)) {
                itemFeatureVariables.walkPhase = itemFeatureVariables.walkPhase + speed * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.walkProgress = itemFeatureVariables.walkProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.walkProgress = itemFeatureVariables.walkProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.walkProgress = ItemAnimationMath.clamp(itemFeatureVariables.walkProgress, 0.0, 1.0);
            itemFeatureVariables.verticalBobVelocity = itemFeatureVariables.verticalBobVelocity + ((-1.0) * PlayerAnimationUtils.getVelocityY(player) + ItemAnimationMath.sin(itemFeatureVariables.sneakProgress * 3.14) * 0.14 + ItemAnimationMath.sin(itemFeatureVariables.mainHandBowDraw * 3.14) * 0.12 + ItemAnimationMath.sin(itemFeatureVariables.offhandBowDraw * 3.14) * 0.12) * 0.27 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.verticalBobVelocity = itemFeatureVariables.verticalBobVelocity - 0.1 * itemFeatureVariables.verticalBob * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.verticalBobVelocity = itemFeatureVariables.verticalBobVelocity * ItemAnimationMath.pow(0.82, itemAnimationContext.frameDelta * 30.0F);
            itemFeatureVariables.verticalBob = itemFeatureVariables.verticalBob + itemFeatureVariables.verticalBobVelocity * itemAnimationContext.frameDelta * 30.0;
            if (PlayerAnimationUtils.isSneaking(player)) {
                itemFeatureVariables.sneakProgress = itemFeatureVariables.sneakProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.sneakProgress = itemFeatureVariables.sneakProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.sneakProgress = ItemAnimationMath.clamp(itemFeatureVariables.sneakProgress, 0.0, 1.0);
            ItemAnimationMath.translateY(poseStack, -0.08 * itemFeatureVariables.sneakProgress);
            ItemAnimationMath.rotateAroundX(poseStack, 4.0 * ItemAnimationMath.sin(itemFeatureVariables.sneakProgress * 3.14), 0.0, -0.4, 0.0);
            itemFeatureVariables.idlePhase = itemFeatureVariables.idlePhase + 0.04 * itemAnimationContext.frameDelta * 30.0;
            if (PlayerAnimationUtils.isClimbing(player)) {
                itemFeatureVariables.climbProgress = itemFeatureVariables.climbProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.climbProgress = itemFeatureVariables.climbProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            if (itemFeatureVariables.climbProgress > 1.0) {
                itemFeatureVariables.climbProgress = 1.0;
            }
            if (itemFeatureVariables.climbProgress < 0.0) {
                itemFeatureVariables.climbProgress = 0.0;
            }
            if (PlayerAnimationUtils.isCrawling(player)) {
                itemFeatureVariables.crawlProgress = itemFeatureVariables.crawlProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.crawlProgress = itemFeatureVariables.crawlProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.crawlProgress = ItemAnimationMath.clamp(itemFeatureVariables.crawlProgress, 0.0, 1.0);
            if (PlayerAnimationUtils.isCrawling(player) && speed > 0.08) {
                itemFeatureVariables.crawlMoveProgress = itemFeatureVariables.crawlMoveProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.crawlMoveProgress = itemFeatureVariables.crawlMoveProgress - 0.06 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.crawlMoveProgress = ItemAnimationMath.clamp(itemFeatureVariables.crawlMoveProgress, 0.0, 1.0);
            if (PlayerAnimationUtils.isClimbing(player) && ItemAnimationMath.abs(PlayerAnimationUtils.getVelocityY(player)) > 0.08) {
                if (PlayerAnimationUtils.getVelocityY(player) > 0.0) {
                    itemFeatureVariables.climbPhase = itemFeatureVariables.climbPhase + PlayerAnimationUtils.getVelocityY(player) * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.climbPhase = itemFeatureVariables.climbPhase + PlayerAnimationUtils.getVelocityY(player) / 2.0 * itemAnimationContext.frameDelta * 30.0;
                }
            }
            if (PlayerAnimationUtils.isSwimming(player) && !usingItem) {
                itemFeatureVariables.swimPhase = itemFeatureVariables.swimPhase + speed * itemAnimationContext.frameDelta * 30.0;
                itemFeatureVariables.swimProgress = itemFeatureVariables.swimProgress + 0.1 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.swimProgress = itemFeatureVariables.swimProgress - 0.1 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.swimProgress = ItemAnimationMath.clamp(itemFeatureVariables.swimProgress, 0.0, 1.0);
            ItemAnimationMath.translateZ(poseStack, 0.3 * ItemAnimationMath.sin(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale);
            if (!inTag3 && !item) {
                ItemAnimationMath.rotateAroundX(poseStack, PlayerAnimationUtils.getPitch(player) * -0.03 + var30 * 0.1, 0.0, -0.4, 0.0);
            } else {
                ItemAnimationMath.rotateAroundX(poseStack, PlayerAnimationUtils.getPitch(player) * -0.03 + var30 * 0.1, 0.0, -0.4, 0.0);
            }
            if (empty) {
                ItemAnimationMath.translate(poseStack, 0.0, -0.15 * -ItemAnimationMath.cos(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress + 0.25 * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, -15.0 * var18 * ItemAnimationMath.cos(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -10.0 * ItemAnimationMath.cos(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -30.0 * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, -20.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
            } else {
                ItemAnimationMath.rotateAroundY(poseStack, -15.0 * var18 * ItemAnimationMath.cos(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -10.0 * ItemAnimationMath.cos(itemFeatureVariables.swimPhase * 0.55) * itemFeatureVariables.swimProgress * itemFeatureVariables.swimScale, 0.3 * var18, -0.4, 0.0);
            }
            if (!ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.BELL) && !ItemFeatureProperties.isLantern(itemAnimationContext.stack) && !ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.HANGING_SIGNS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PAINTING) && !ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.ITEM_FRAME)) {
                if (!empty && !useAnimationName.equals("crossbow")) {
                    ItemAnimationMath.translateY(poseStack, -0.12);
                    ItemAnimationMath.rotateZ(poseStack, -6.0 * var18);
                    ItemAnimationMath.rotateX(poseStack, 6.0);
                }
            } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.PINK_PETALS) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.LEAF_LITTER) || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.WILDFLOWERS)) {
                ItemAnimationMath.translate(poseStack, 0.0, 0.25, -0.05);
            } else if (ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.END_CRYSTAL)) {
                ItemAnimationMath.translateZ(poseStack, -0.12);
                ItemAnimationMath.rotateX(poseStack, -10.0);
            } else {
                ItemAnimationMath.translate(poseStack, 0.0, -0.1, 0.05);
                ItemAnimationMath.rotateX(poseStack, 25.0);
            }
            ItemAnimationMath.translateY(poseStack, 0.01 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase));
            ItemAnimationMath.rotateAroundX(poseStack, 1.1 * var18 * ItemAnimationMath.cos(itemFeatureVariables.idlePhase), 0.3 * var18, -0.4, 0.0);
            ItemAnimationMath.rotateAroundY(poseStack, 0.5 * var18 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase) * var18, 0.3 * var18, -0.4, 0.0);
            ItemAnimationMath.rotateAroundZ(poseStack, 2.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 0.3) * var18, 0.3 * var18, -0.4, 0.0);
            if (empty && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translate(poseStack, 0.0, -1.0 * AnimationCurveMath.expoInOut(itemFeatureVariables.emptyOffhandRaise), 0.5 * AnimationCurveMath.expoInOut(itemFeatureVariables.emptyOffhandRaise));
            }
            double var48;
            if (!empty && !block) {
                var48 = 1.0;
            } else {
                var48 = 0.7;
            }
            if (empty) {
                ItemAnimationMath.translateZ(poseStack, 0.06 * (itemFeatureVariables.verticalBob * var48));
            }
            ItemAnimationMath.rotateAroundX(poseStack, 2.0 * (itemFeatureVariables.verticalBob * var48), 0.0, -0.4, 0.0);
            ItemAnimationMath.translateY(poseStack, 0.06 * itemFeatureVariables.verticalBob * var48);
            double var50 = itemAnimationContext.rightHand ? itemFeatureVariables.walkPhase : itemFeatureVariables.walkPhase - 0.75;
            ItemAnimationMath.rotateAroundX(poseStack, 1.5 * ItemAnimationMath.sin(var50) * itemFeatureVariables.walkProgress, 0.0, -0.4, 0.0);
            ItemAnimationMath.rotateAroundY(poseStack, -0.5 * ItemAnimationMath.cos(itemFeatureVariables.walkPhase * 1.5) * itemFeatureVariables.walkProgress * var18, 0.0, -0.4, 0.0);
            ItemAnimationMath.rotateAroundZ(poseStack, 1.0 * ItemAnimationMath.cos(itemFeatureVariables.walkPhase * 1.5) * itemFeatureVariables.walkProgress * var18, 0.0, -0.4, 0.0);
            if (useAnimationName.equals("block") && !inTag) {
                ItemAnimationMath.translate(poseStack, 0.2 * var18, 0.0, 0.1);
                ItemAnimationMath.rotateAroundY(poseStack, 20.0 * var18, 0.3 * var18, -0.4, 0.0);
            }
            if (useAnimationName.equals("block") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translate(poseStack, -var34 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBlockProgress) + 0.1 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBlockProgress) * itemFeatureVariables.blockScale, 0.0, 0.0);
                if (inTag) {
                    ItemAnimationMath.rotateAroundY(poseStack, 50.0 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBlockProgress) * var18 * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                } else {
                    ItemAnimationMath.rotateAroundY(poseStack, 70.0 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBlockProgress) * var18 * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                }
                ItemAnimationMath.rotateAroundX(poseStack, 13.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(itemFeatureVariables.mainHandBlockProgress * 4.14), 0.0, 1.0) * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -15.0 * AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBlockProgress) * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
            }
            if (useAnimationName.equals("block") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translate(poseStack, -var34 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBlockProgress) + 0.1 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBlockProgress) * itemFeatureVariables.blockScale, 0.0, 0.0);
                if (inTag) {
                    ItemAnimationMath.rotateAroundY(poseStack, 50.0 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBlockProgress) * var18 * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                } else {
                    ItemAnimationMath.rotateAroundY(poseStack, 70.0 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBlockProgress) * var18 * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                }
                ItemAnimationMath.rotateAroundX(poseStack, 13.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(itemFeatureVariables.offhandBlockProgress * 4.14), 0.0, 1.0) * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -15.0 * AnimationCurveMath.backInOut(itemFeatureVariables.offhandBlockProgress) * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
            }
            double var52 = AnimationCurveMath.backOut(itemFeatureVariables.mainHandTridentRaise);
            if (useAnimationName.equals("trident") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translate(poseStack, 0.0, -0.15 * var52 * itemFeatureVariables.tridentScale, -0.3 * var52 * itemFeatureVariables.tridentScale);
                ItemAnimationMath.rotateAroundX(poseStack, 65.0 * var52, 0.3 * var18 * itemFeatureVariables.tridentScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 55.0 * itemFeatureVariables.mainHandTridentPullback * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * var52 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 0.3 * ItemAnimationMath.sin(itemFeatureVariables.mainHandTridentShake * var52 * 9.14) * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
            }
            if (!itemAnimationContext.mainHand) {
                if (empty) {
                    ItemAnimationMath.translateY(poseStack, 0.6 * var52 * itemFeatureVariables.tridentScale);
                } else {
                    ItemAnimationMath.translateY(poseStack, 0.2 * var52 * itemFeatureVariables.tridentScale);
                }
                ItemAnimationMath.translate(poseStack, 0.25 * var18 * var52 * itemFeatureVariables.tridentScale, 0.0, -0.15 * var52 * itemFeatureVariables.tridentScale);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var18 * var52 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -30.0 * var52 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
            }
            double var54 = AnimationCurveMath.backOut(itemFeatureVariables.offhandTridentRaise);
            if (useAnimationName.equals("trident") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translate(poseStack, 0.0, -0.15 * var54 * itemFeatureVariables.tridentScale, -0.3 * var54 * itemFeatureVariables.tridentScale);
                ItemAnimationMath.rotateAroundX(poseStack, 65.0 * var54 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * var54 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 0.3 * ItemAnimationMath.sin(itemFeatureVariables.offhandTridentShake * var54 * 9.14) * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
            }
            if (itemAnimationContext.mainHand) {
                if (empty) {
                    ItemAnimationMath.translateY(poseStack, 0.6 * var54 * itemFeatureVariables.tridentScale);
                } else {
                    ItemAnimationMath.translateY(poseStack, 0.2 * var54 * itemFeatureVariables.tridentScale);
                }
                ItemAnimationMath.translate(poseStack, 0.25 * var18 * var54 * itemFeatureVariables.tridentScale, 0.0, -0.15 * var54 * itemFeatureVariables.tridentScale);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var18 * var54 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -30.0 * var54 * itemFeatureVariables.tridentScale, 0.3 * var18, -0.4, 0.0);
            }
            double var56 = ItemAnimationMath.sin(itemAnimationContext.swingProgress * 3.14);
            double var58 = ItemAnimationMath.clamp(ItemAnimationMath.sin(itemAnimationContext.swingProgress * 6.28), 0.0, 1.0);
            double var60 = ItemAnimationMath.sin(itemAnimationContext.swingProgress * 6.28);
            if (empty) {
                ItemAnimationMath.translate(poseStack, -0.15 * var18 * var24 * itemFeatureVariables.emptyHandSwingScale, 0.1 * var60 + 0.33 * var24 + 0.05 * var20 + 0.14 * var58 * itemFeatureVariables.emptyHandSwingScale, -0.1 * var60 - 0.4 * var26 - 0.2 * var24 * itemFeatureVariables.emptyHandSwingScale);
                ItemAnimationMath.rotateX(poseStack, -10.0 * var58 * itemFeatureVariables.emptyHandSwingScale);
                ItemAnimationMath.translateZ(poseStack, 0.15 * var20 * itemFeatureVariables.emptyHandSwingScale);
                ItemAnimationMath.rotateAroundX(poseStack, -20.0 * var24 * itemFeatureVariables.emptyHandSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -7.0 * var26, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var20, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 20.0 * var18 * var24, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 5.0 * var18 * var24, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -5.0 * var60, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * var60, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 5.0 * var18 * var60, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18 * var26, 0.3 * var18 * itemFeatureVariables.emptyHandSwingScale, -0.4, 0.0);
            } else if (inTag2) {
                ItemAnimationMath.translateZ(poseStack, -0.1 * var22 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateZ(poseStack, 0.0 * var26 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateZ(poseStack, 0.0 * var28 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.0 * var26 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * var60 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var26 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateX(poseStack, 0.15 * var18 * var28 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.3 * var56 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.2 * var56 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var56 * itemFeatureVariables.pickaxeSwingScale);
                ItemAnimationMath.rotateAroundX(poseStack, 20.0 * var22 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 40.0 * var22 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var58 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var20 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -45.0 * var26 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -10.0 * var28 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -95.0 * var56 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 20.0 * var56 * var18 * itemFeatureVariables.pickaxeSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, ItemAnimationMath.clamp(20.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase) * var24, -60.0, 60.0) * itemFeatureVariables.pickaxeSwingScale, 0.5 * var18, -0.5, 0.0);
            } else if (!inTag && !item && !useAnimationName.equals("trident") && !useAnimationName.equals("spear") && !inTag3) {
                if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.SHOVELS)) {
                    ItemAnimationMath.translateY(poseStack, 0.6 * var22);
                    ItemAnimationMath.translateZ(poseStack, -0.5 * var22);
                    ItemAnimationMath.translateZ(poseStack, -0.3 * var56);
                    ItemAnimationMath.translateY(poseStack, -0.3 * var56);
                    ItemAnimationMath.translateY(poseStack, -0.4 * var20);
                    ItemAnimationMath.translateZ(poseStack, 0.1 * var20);
                    ItemAnimationMath.translateY(poseStack, -0.1 * var28);
                    ItemAnimationMath.rotateX(poseStack, -130.0 * var22);
                    ItemAnimationMath.rotateX(poseStack, 70.0 * var56);
                    ItemAnimationMath.rotateX(poseStack, 40.0 * var20);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * var28);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * var58);
                    ItemAnimationMath.rotateX(poseStack, -10.0 * var60);
                } else {
                    ItemAnimationMath.translateZ(poseStack, -0.1 * var24);
                    ItemAnimationMath.translateX(poseStack, -0.1 * var18 * var24);
                    ItemAnimationMath.translateZ(poseStack, -0.1 * var58);
                    ItemAnimationMath.translateZ(poseStack, -0.05 * var26);
                    ItemAnimationMath.translateY(poseStack, 0.25 * var26);
                    ItemAnimationMath.translateY(poseStack, 0.0 * var24);
                    ItemAnimationMath.rotateAroundX(poseStack, 5.0 * var20, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -25.0 * var26, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 5.0 * var58, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateZ(poseStack, -5.0 * var18 * var56);
                    ItemAnimationMath.rotateY(poseStack, 15.0 * var18 * var56);
                    ItemAnimationMath.rotateAroundX(poseStack, -2.0 * var60, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundZ(poseStack, 5.0 * var18 * var60, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundY(poseStack, 5.0 * var18 * var60, 0.3 * var18, -0.4, 0.0);
                }
            } else if (itemFeatureVariables.canAlternateSwing && itemFeatureVariables.alternateSwing && !itemAnimationContext.breakingBlock && inTag) {
                ItemAnimationMath.translateZ(poseStack, 0.2 * var22 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.5 * var18 * var22 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.5 * var22 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.2 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, 0.0 * var28 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.0 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * var60 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, 0.15 * var18 * var28 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.3 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.2 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, 0.15 * var18 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.rotateAroundX(poseStack, 20.0 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, 70.0 * var18 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 30.0 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var58 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var20 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -25.0 * var26 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -10.0 * var28 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -75.0 * var56 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
            } else if ((!itemFeatureVariables.canAlternateSwing || itemAnimationContext.breakingBlock || !inTag) && !useAnimationName.equals("trident") && !useAnimationName.equals("spear")) {
                if ((!itemFeatureVariables.canAlternateSwing || itemAnimationContext.breakingBlock) && inTag || item || inTag3 || itemAnimationContext.breakingBlock && useAnimationName.equals("trident")) {
                    ItemAnimationMath.translateZ(poseStack, -0.2 * var22);
                    ItemAnimationMath.translateZ(poseStack, 0.0 * var26);
                    ItemAnimationMath.translateZ(poseStack, 0.0 * var28);
                    ItemAnimationMath.translateY(poseStack, 0.0 * var26);
                    ItemAnimationMath.translateY(poseStack, -0.1 * var60);
                    ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var26);
                    ItemAnimationMath.translateX(poseStack, 0.15 * var18 * var28);
                    ItemAnimationMath.translateZ(poseStack, -0.3 * var56);
                    ItemAnimationMath.translateY(poseStack, 0.2 * var56);
                    ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var56);
                    ItemAnimationMath.rotateAroundX(poseStack, 20.0 * var22, 0.3 * var18, -0.4, 0.0);
                    if (inTag3 && itemAnimationContext.breakingBlock) {
                        ItemAnimationMath.rotateAroundZ(poseStack, -60.0 * var18 * var22, 0.3 * var18, -0.4, 0.0);
                    } else {
                        ItemAnimationMath.rotateAroundZ(poseStack, -40.0 * var18 * var22, 0.3 * var18, -0.4, 0.0);
                    }
                    ItemAnimationMath.rotateAroundX(poseStack, 30.0 * var22, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var58, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var20, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -45.0 * var26, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -10.0 * var28, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -95.0 * var56, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateZ(poseStack, ItemAnimationMath.clamp(30.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24, 0.0, 30.0));
                    ItemAnimationMath.translateY(poseStack, -0.2 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24);
                } else {
                    ItemAnimationMath.translateZ(poseStack, -0.2 * var24);
                    ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var24);
                    ItemAnimationMath.translateZ(poseStack, -0.1 * var58);
                    ItemAnimationMath.translateZ(poseStack, -0.15 * var26);
                    ItemAnimationMath.translateY(poseStack, 0.1 * var26);
                    ItemAnimationMath.translateY(poseStack, -0.3 * var24);
                    ItemAnimationMath.rotateAroundX(poseStack, 20.0 * var20, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -40.0 * var26, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, -20.0 * var26, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 5.0 * var58, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateZ(poseStack, -5.0 * var18 * var56);
                    ItemAnimationMath.rotateY(poseStack, 15.0 * var18 * var56);
                    if (inTag) {
                        ItemAnimationMath.translateZ(poseStack, -0.15 * var28);
                        ItemAnimationMath.translateZ(poseStack, -0.15 * var56);
                        ItemAnimationMath.rotateY(poseStack, -10.0 * var18 * var56);
                        ItemAnimationMath.rotateX(poseStack, -25.0 * var28);
                        ItemAnimationMath.rotateX(poseStack, -20.0 * var56);
                        ItemAnimationMath.rotateX(poseStack, 20.0 * var22);
                    }
                    if (!inTag) {
                        ItemAnimationMath.rotateAroundX(poseStack, -5.0 * var60, 0.3 * var18, -0.4, 0.0);
                    }
                    ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * var60, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundY(poseStack, 5.0 * var18 * var60, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateZ(poseStack, ItemAnimationMath.clamp(30.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24, 0.0, 30.0));
                    ItemAnimationMath.translateY(poseStack, -0.2 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24);
                }
            } else if (inTag) {
                ItemAnimationMath.translateZ(poseStack, -0.2 * var22 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.15 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.25 * var28 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.2 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * var60 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var26 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, 0.15 * var18 * var28 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateZ(poseStack, -0.45 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateY(poseStack, 0.2 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var56 * itemFeatureVariables.swordSwingScale);
                ItemAnimationMath.rotateAroundX(poseStack, 20.0 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundZ(poseStack, -70.0 * var18 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 30.0 * var22 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var58 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * var20 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -55.0 * var26 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -15.0 * var28 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -95.0 * var56 * itemFeatureVariables.swordSwingScale, 0.3 * var18, -0.4, 0.0);
            } else {
                if (useAnimationName.equals("trident")) {
                    ItemAnimationMath.translateY(poseStack, -0.5 * var22 * itemFeatureVariables.axeSwingScale);
                    ItemAnimationMath.translateZ(poseStack, 0.1 * var22 * itemFeatureVariables.axeSwingScale);
                } else {
                    ItemAnimationMath.translateY(poseStack, 0.0 * var22 * itemFeatureVariables.axeSwingScale);
                    ItemAnimationMath.translateZ(poseStack, 0.37 * var22 * itemFeatureVariables.axeSwingScale);
                }
                ItemAnimationMath.translateX(poseStack, -0.15 * var18 * var22 * itemFeatureVariables.axeSwingScale);
                if (useAnimationName.equals("trident")) {
                    ItemAnimationMath.translateZ(poseStack, -0.25 * var28 * itemFeatureVariables.axeSwingScale);
                } else {
                    ItemAnimationMath.translateZ(poseStack, -0.35 * var28 * itemFeatureVariables.axeSwingScale);
                    ItemAnimationMath.translateZ(poseStack, -0.25 * AnimationCurveMath.quartOut(var24) * itemFeatureVariables.axeSwingScale);
                }
                ItemAnimationMath.translateZ(poseStack, 0.14 * var56 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.2 * var56 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.1 * var18 * var26 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.1 * var18 * var56 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.translateX(poseStack, -0.1 * var18 * var28 * itemFeatureVariables.axeSwingScale);
                if (useAnimationName.equals("trident")) {
                    ItemAnimationMath.rotateZ(poseStack, 80.0 * var22 * var18 * itemFeatureVariables.axeSwingScale);
                } else {
                    ItemAnimationMath.rotateZ(poseStack, 20.0 * var22 * var18 * itemFeatureVariables.axeSwingScale);
                    ItemAnimationMath.rotateY(poseStack, 6.0 * var22 * var18 * itemFeatureVariables.axeSwingScale);
                }
                ItemAnimationMath.rotateZ(poseStack, 5.0 * var56 * var18 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, -40.0 * var22 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, 15.0 * var28 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, 15.0 * var26 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, 20.0 * var56 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, 10.0 * var56 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateX(poseStack, -10.0 * var58 * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.rotateZ(poseStack, ItemAnimationMath.clamp(10.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24, 0.0, 30.0) * itemFeatureVariables.axeSwingScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * ItemAnimationMath.sin(itemFeatureVariables.swingRollPhase * 2.0) * var24 * itemFeatureVariables.axeSwingScale);
            }
            if (usingItem && usedHand == itemAnimationContext.hand && useAnimationName.equals("block")) {
                if (itemAnimationContext.mainHand) {
                    ItemAnimationMath.translateX(poseStack, 0.0 - 0.25 * (ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + 4.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * 3.14)) * var18 * itemFeatureVariables.mainHandBlockProgress * itemFeatureVariables.blockScale);
                    ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * (ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + 4.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * 3.14)) * itemFeatureVariables.mainHandBlockProgress * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                } else {
                    ItemAnimationMath.translateX(poseStack, 0.0 - 0.25 * (ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + 4.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * 3.14)) * var18 * itemFeatureVariables.offhandBlockProgress * itemFeatureVariables.blockScale);
                    ItemAnimationMath.rotateAroundZ(poseStack, 10.0 * var18 * (ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) + 4.0 * ItemAnimationMath.sin(itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress * 3.14)) * itemFeatureVariables.offhandBlockProgress * itemFeatureVariables.blockScale, 0.3 * var18, -0.4, 0.0);
                }
            }
            if (useAnimationName.equals("crossbow") && itemFeatureVariables.mainHandCrossbowLoad + itemFeatureVariables.offhandCrossbowLoad == 0.0) {
                ItemAnimationMath.translateZ(poseStack, 0.0 + 0.25 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) * itemFeatureVariables.crossbowScale);
                ItemAnimationMath.rotateAroundX(poseStack, 20.0 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress) * itemFeatureVariables.crossbowScale, 0.3 * var18, -0.4, 0.0);
            } else if (itemFeatureVariables.mainHandEatProgress == 0.0 && !useAnimationName.equals("bow") && !useAnimationName.equals("block") && itemFeatureVariables.mainHandTridentRaise == 0.0 && itemFeatureVariables.offhandTridentRaise == 0.0) {
                ItemAnimationMath.translateZ(poseStack, 0.0 - 0.25 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress));
                ItemAnimationMath.rotateAroundX(poseStack, -20.0 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress), 0.3 * var18, -0.4, 0.0);
                if (item) {
                    ItemAnimationMath.translateZ(poseStack, 0.0 - 0.25 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress));
                    ItemAnimationMath.rotateZ(poseStack, 6.0 * var18 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress));
                    ItemAnimationMath.rotateAroundX(poseStack, -10.0 * ItemAnimationMath.sin(itemAnimationContext.equipProgress * itemAnimationContext.equipProgress * itemAnimationContext.equipProgress), 0.3 * var18, -0.4, 0.0);
                }
                if (!block) {
                    ItemAnimationMath.rotateX(poseStack, itemFeatureVariables.mainHandSwayPitch * 0.35 * var24);
                }
            }
            double var62 = 0.0;
            if (PlayerAnimationUtils.getPitch(player) != 0.0) {
                var62 = 90.0 / PlayerAnimationUtils.getPitch(player) / 2.5;
            } else {
                var62 = 1.0;
            }
            if (var62 > 1.0) {
                var62 = 1.0;
            }
            if (var62 < 0.0) {
                var62 = 1.0;
            }
            double var64 = ItemFeatureProperties.isLantern(itemAnimationContext.stack) ? 0.2 : 1.0;
            ItemAnimationMath.translateZ(poseStack, 0.2 * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            ItemAnimationMath.translateY(poseStack, -0.2 * ItemAnimationMath.cos(itemFeatureVariables.climbPhase) * var18 * var62 * itemFeatureVariables.climbProgress * var64 * itemFeatureVariables.climbScale);
            ItemAnimationMath.rotateX(poseStack, -30.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.climbPhase) * var62 * itemFeatureVariables.climbProgress * var64 * itemFeatureVariables.climbScale);
            ItemAnimationMath.rotateX(poseStack, PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            ItemAnimationMath.translateZ(poseStack, 0.01 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            ItemAnimationMath.translateY(poseStack, 0.003 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            ItemAnimationMath.translateX(poseStack, -0.0025 * var18 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            if (!empty) {
                ItemAnimationMath.translateX(poseStack, -0.05 * var18 * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
                ItemAnimationMath.translateZ(poseStack, -0.2 * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * itemFeatureVariables.climbProgress * itemFeatureVariables.climbScale);
            }
            ItemAnimationMath.translateZ(poseStack, 0.2 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.translateZ(poseStack, -0.2 * var18 * ItemAnimationMath.sin(itemFeatureVariables.crawlPhase) * itemFeatureVariables.crawlProgress * var62 * var64 * itemFeatureVariables.crawlMoveProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.rotateY(poseStack, 10.0 * ItemAnimationMath.sin(itemFeatureVariables.crawlPhase) * itemFeatureVariables.crawlProgress * var64 * itemFeatureVariables.crawlMoveProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.rotateX(poseStack, ItemAnimationMath.clamp(20.0 * var18 * ItemAnimationMath.cos(itemFeatureVariables.crawlPhase) * itemFeatureVariables.crawlProgress * var64 * itemFeatureVariables.crawlMoveProgress, 0.0, 20.0) * itemFeatureVariables.crawlScale);
            if (empty) {
                ItemAnimationMath.translateY(poseStack, 0.3 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
                ItemAnimationMath.translateZ(poseStack, -0.55 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
                ItemAnimationMath.rotateX(poseStack, -45.0 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
                ItemAnimationMath.rotateAroundZ(poseStack, ItemAnimationMath.clamp(16.0 * ItemAnimationMath.sin(itemFeatureVariables.crawlPhase) * itemFeatureVariables.crawlProgress * var64 * itemFeatureVariables.crawlMoveProgress, 0.0, 20.0) * itemFeatureVariables.crawlScale, 0.3, -0.4, 0.0);
            }
            ItemAnimationMath.rotateX(poseStack, PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.rotateX(poseStack, -7.0 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.translateZ(poseStack, 0.01 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            if (empty) {
                ItemAnimationMath.translateZ(poseStack, 0.005 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            }
            ItemAnimationMath.translateY(poseStack, 0.003 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            ItemAnimationMath.translateX(poseStack, -0.0025 * var18 * PlayerAnimationUtils.getPitch(player) * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            if (!empty) {
                ItemAnimationMath.translateX(poseStack, -0.1 * var18 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
                ItemAnimationMath.translateZ(poseStack, -0.2 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
                ItemAnimationMath.translateY(poseStack, -0.1 * itemFeatureVariables.crawlProgress * itemFeatureVariables.crawlScale);
            }
            double var66 = AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBowDraw) * itemFeatureVariables.bowScale;
            double var68 = AnimationCurveMath.backInOut(itemFeatureVariables.offhandBowDraw) * itemFeatureVariables.bowScale;
            if (itemFeatureVariables.mainHandBowDraw > 0.0) {
                itemFeatureVariables.emptyOffhandRaise = var66 == 1.0 ? -0.1 : 1.0 - var66;
            }
            if (useAnimationName.equals("bow") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.15 * var18);
                ItemAnimationMath.translateZ(poseStack, -0.085);
                ItemAnimationMath.translateX(poseStack, 0.1 * var18 * var66);
                ItemAnimationMath.translateZ(poseStack, 0.085 * var66);
                ItemAnimationMath.translateZ(poseStack, -0.1 * var66);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * var66);
                ItemAnimationMath.translateY(poseStack, 0.15 * var66);
                ItemAnimationMath.rotateAroundX(poseStack, 5.0 * ItemAnimationMath.sin(var66 * 3.14), 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 0.0 * var18 * var66, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.translateZ(poseStack, 0.0015 * ItemAnimationMath.sin(itemFeatureVariables.mainHandBowShake * 15.14) * itemFeatureVariables.mainHandBowTension);
            }
            if (useAnimationName.equals("bow") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.15 * var18);
                ItemAnimationMath.translateZ(poseStack, -0.085);
                ItemAnimationMath.translateX(poseStack, 0.1 * var18 * var68);
                ItemAnimationMath.translateZ(poseStack, 0.085 * var68);
                ItemAnimationMath.translateZ(poseStack, -0.1 * var68);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * var68);
                ItemAnimationMath.translateY(poseStack, 0.15 * var68);
                ItemAnimationMath.rotateAroundX(poseStack, 5.0 * ItemAnimationMath.sin(var68 * 3.14), 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 0.0 * var18 * var68, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.translateZ(poseStack, 0.0015 * ItemAnimationMath.sin(itemFeatureVariables.offhandBowShake * 15.14) * itemFeatureVariables.offhandBowTension);
            }
            if (!itemAnimationContext.mainHand && usingItem || !itemAnimationContext.mainHand && empty) {
                double var70 = AnimationCurveMath.backOut(itemFeatureVariables.mainHandBowTension) * itemFeatureVariables.bowScale;
                ItemAnimationMath.translateX(poseStack, (-var34 - var34 / 1.5) * var18 * var66);
                ItemAnimationMath.translateX(poseStack, 0.27 * var18 * var66);
                if (!empty) {
                    ItemAnimationMath.translateY(poseStack, ItemAnimationMath.sin(var66 * 1.56 + 3.14));
                }
                ItemAnimationMath.translateZ(poseStack, -0.65 * var66);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * ItemAnimationMath.sin(var66 * 3.14) * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 70.0 * var66 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var70 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateY(poseStack, 0.25 * var18 * ItemAnimationMath.sin(itemFeatureVariables.mainHandBowShake * 15.14) * var70);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var66 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.translateY(poseStack, -0.5 * ItemAnimationMath.sin(var66 * 3.14));
            }
            if (itemAnimationContext.mainHand && usingItem || itemAnimationContext.mainHand && empty) {
                double var111 = AnimationCurveMath.backOut(itemFeatureVariables.offhandBowTension);
                ItemAnimationMath.translateX(poseStack, (-var34 - var34 / 1.5) * var18 * var68);
                ItemAnimationMath.translateX(poseStack, 0.27 * var18 * var68);
                if (!empty) {
                    ItemAnimationMath.translateY(poseStack, ItemAnimationMath.sin(var68 * 1.56 + 3.14));
                }
                ItemAnimationMath.translateZ(poseStack, -0.65 * var68);
                ItemAnimationMath.rotateAroundX(poseStack, 10.0 * ItemAnimationMath.sin(var68 * 3.14) * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 70.0 * var68 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var111 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateY(poseStack, 0.25 * var18 * ItemAnimationMath.sin(itemFeatureVariables.offhandBowShake * 15.14) * var111);
                ItemAnimationMath.rotateAroundY(poseStack, 25.0 * var68 * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.translateY(poseStack, -0.5 * ItemAnimationMath.sin(var68 * 3.14));
            }
            double var112 = AnimationCurveMath.backOut(itemFeatureVariables.mainHandCrossbowLoad) * itemFeatureVariables.crossbowScale;
            double var72 = AnimationCurveMath.backOut(itemFeatureVariables.mainHandCrossbowTension) * itemFeatureVariables.crossbowScale;
            double var74 = AnimationCurveMath.backOut(itemFeatureVariables.offhandCrossbowLoad) * itemFeatureVariables.crossbowScale;
            double var76 = AnimationCurveMath.backOut(itemFeatureVariables.offhandCrossbowTension) * itemFeatureVariables.crossbowScale;
            if (useAnimationName.equals("crossbow") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateY(poseStack, -0.15 * var112);
                ItemAnimationMath.translateZ(poseStack, 0.3 * var112);
                ItemAnimationMath.rotateAroundZ(poseStack, 20.0 * var18 * var112, -0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18 * var112, -0.3 * var18, -0.4, 0.0);
            }
            if (!itemAnimationContext.mainHand && usingItem || !itemAnimationContext.mainHand && empty) {
                ItemAnimationMath.translateX(poseStack, (-var34 - var34 / 1.5) * var18 * var112);
                ItemAnimationMath.translateX(poseStack, 0.25 * var18 * var112);
                ItemAnimationMath.translateZ(poseStack, -0.1 * var112);
                ItemAnimationMath.translateY(poseStack, 0.55 * var112);
                if (!empty) {
                    ItemAnimationMath.translateY(poseStack, ItemAnimationMath.sin(var112 * 1.56 + 3.14));
                }
                ItemAnimationMath.rotateAroundZ(poseStack, 15.0 * var18 * var112, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 80.0 * var18 * var112, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18 * var72, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -7.0 * var72, 0.3 * var18, -0.4, 0.0);
            }
            if (useAnimationName.equals("crossbow") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translateY(poseStack, -0.15 * var74);
                ItemAnimationMath.translateZ(poseStack, 0.3 * var74);
                ItemAnimationMath.rotateAroundZ(poseStack, 20.0 * var18 * var74, -0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18 * var74, -0.3 * var18, -0.4, 0.0);
            }
            if (itemAnimationContext.mainHand && usingItem || itemAnimationContext.mainHand && empty) {
                ItemAnimationMath.translateX(poseStack, (-var34 - var34 / 1.5) * var18 * var74);
                ItemAnimationMath.translateX(poseStack, 0.25 * var18 * var74);
                ItemAnimationMath.translateZ(poseStack, -0.1 * var74);
                ItemAnimationMath.translateY(poseStack, 0.55 * var74);
                if (!empty) {
                    ItemAnimationMath.translateY(poseStack, ItemAnimationMath.sin(var74 * 1.56 + 3.14));
                }
                ItemAnimationMath.rotateAroundZ(poseStack, 15.0 * var18 * var74, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 80.0 * var18 * var74, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 15.0 * var18 * var76, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -7.0 * var74, 0.3 * var18, -0.4, 0.0);
            }
            if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandEatProgress = itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.eatProgressScale;
                itemFeatureVariables.offhandEatProgress = itemFeatureVariables.offhandEatProgress * itemFeatureVariables.eatProgressScale;
                itemFeatureVariables.offhandEatPhase = itemFeatureVariables.offhandEatPhase * itemFeatureVariables.eatProgressScale;
                itemFeatureVariables.mainHandEatPhase = itemFeatureVariables.mainHandEatPhase * itemFeatureVariables.eatProgressScale;
                double var78 = itemFeatureVariables.mainHandEatProgress * itemFeatureVariables.mainHandEatProgress;
                if ((useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) && itemAnimationContext.mainHand) {
                    ItemAnimationMath.translateZ(poseStack, 0.155 * var78);
                    ItemAnimationMath.translateX(poseStack, 0.135 * var18 * var78);
                    ItemAnimationMath.translateY(poseStack, -0.27 * var78);
                    ItemAnimationMath.translateY(poseStack, 0.0 * itemFeatureVariables.mainHandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 30.0 * var78);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * itemFeatureVariables.mainHandDrinkProgress);
                    if (useAnimationName.equals("eat")) {
                        ItemAnimationMath.rotateX(poseStack, 3.0 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 3.0))) * var78);
                        ItemAnimationMath.rotateY(poseStack, 4.0 * var18 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 2.0))) * var78);
                        ItemAnimationMath.rotateZ(poseStack, 6.0 * var18 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 2.0))) * var78);
                    } else {
                        ItemAnimationMath.rotateX(poseStack, 2.0 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 2.0)) * var78);
                        ItemAnimationMath.rotateY(poseStack, 3.0 * var18 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase)) * var78);
                        ItemAnimationMath.rotateZ(poseStack, 5.0 * var18 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase)) * var78);
                    }
                    ItemAnimationMath.rotateAroundY(poseStack, 60.0 * var78 * var18, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 25.0 * ItemAnimationMath.sin(var78 * 3.14), 0.3 * var18, -0.4, 0.0);
                }
                if ((useAnimationName.equals("drink") || empty || ItemFeatureProperties.isItem(itemAnimationContext.stack, Items.GLASS_BOTTLE)) && itemAnimationContext.mainHand) {
                    ItemAnimationMath.translateZ(poseStack, 0.1 * var78);
                    ItemAnimationMath.translateX(poseStack, 0.11 * var18 * var78);
                    ItemAnimationMath.translateY(poseStack, -0.5 * var78);
                    ItemAnimationMath.translateY(poseStack, 0.0 * itemFeatureVariables.mainHandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 50.0 * var78);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * itemFeatureVariables.mainHandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 2.0 * ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 6.0) * itemFeatureVariables.mainHandDrinkProgress);
                    ItemAnimationMath.rotateAroundY(poseStack, var18 * 60.0 * var78, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 25.0 * ItemAnimationMath.sin(var78 * 3.14), 0.3 * var18, -0.4, 0.0);
                }
            } else {
                double var113 = itemFeatureVariables.offhandEatProgress * itemFeatureVariables.offhandEatProgress;
                if ((useAnimationName.equals("eat") || useAnimationName.equals("toot_horn")) && !itemAnimationContext.mainHand) {
                    ItemAnimationMath.translateZ(poseStack, 0.155 * var113);
                    ItemAnimationMath.translateX(poseStack, 0.135 * var18 * var113);
                    ItemAnimationMath.translateY(poseStack, -0.27 * var113);
                    ItemAnimationMath.translateY(poseStack, 0.0 * itemFeatureVariables.offhandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 30.0 * var113);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * itemFeatureVariables.offhandDrinkProgress);
                    if (useAnimationName.equals("eat")) {
                        ItemAnimationMath.rotateX(poseStack, 3.0 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 3.0))) * var113);
                        ItemAnimationMath.rotateY(poseStack, 4.0 * var18 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 2.0))) * var113);
                        ItemAnimationMath.rotateZ(poseStack, 6.0 * var18 * AnimationCurveMath.backInOut(ItemAnimationMath.abs(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 2.0))) * var113);
                    } else {
                        ItemAnimationMath.rotateX(poseStack, 2.0 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 2.0)) * var113);
                        ItemAnimationMath.rotateY(poseStack, 3.0 * var18 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase)) * var113);
                        ItemAnimationMath.rotateZ(poseStack, 5.0 * var18 * AnimationCurveMath.sineInOut(ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase)) * var113);
                    }
                    ItemAnimationMath.rotateAroundY(poseStack, 60.0 * var18 * var113, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 25.0 * ItemAnimationMath.sin(var113 * 3.14), 0.3 * var18, -0.4, 0.0);
                }
                if (useAnimationName.equals("drink") && !itemAnimationContext.mainHand) {
                    ItemAnimationMath.translateZ(poseStack, 0.1 * var113);
                    ItemAnimationMath.translateX(poseStack, 0.11 * var18 * var113);
                    ItemAnimationMath.translateY(poseStack, -0.5 * var113);
                    ItemAnimationMath.translateY(poseStack, 0.0 * itemFeatureVariables.offhandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 50.0 * var113);
                    ItemAnimationMath.rotateX(poseStack, 20.0 * itemFeatureVariables.offhandDrinkProgress);
                    ItemAnimationMath.rotateX(poseStack, 2.0 * ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 6.0) * itemFeatureVariables.offhandDrinkProgress);
                    ItemAnimationMath.rotateAroundY(poseStack, 60.0 * var18 * var113, 0.3 * var18, -0.4, 0.0);
                    ItemAnimationMath.rotateAroundX(poseStack, 25.0 * ItemAnimationMath.sin(var113 * 3.14), 0.3 * var18, -0.4, 0.0);
                }
            }
            double var114 = AnimationCurveMath.backInOut(itemFeatureVariables.mainHandBrushProgress);
            double var80 = AnimationCurveMath.backInOut(itemFeatureVariables.offhandBrushProgress);
            if (useAnimationName.equals("brush") && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateZ(poseStack, -0.2 * var114);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * var114);
                ItemAnimationMath.translateY(poseStack, -0.3 * var114);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 4.14) * var114);
                ItemAnimationMath.translateY(poseStack, -0.3 * ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 4.14) * var114);
                ItemAnimationMath.rotateX(poseStack, 10.0 * ItemAnimationMath.sin(var114 * 3.14));
                ItemAnimationMath.rotateY(poseStack, 20.0 * var18 * var114);
                ItemAnimationMath.rotateY(poseStack, 10.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 4.14) * var114);
                ItemAnimationMath.rotateZ(poseStack, 30.0 * var18 * var114);
                ItemAnimationMath.rotateZ(poseStack, 30.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.mainHandEatPhase * 4.14) * var114);
            }
            if (useAnimationName.equals("brush") && !itemAnimationContext.mainHand) {
                ItemAnimationMath.translateZ(poseStack, -0.2 * var80);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * var80);
                ItemAnimationMath.translateY(poseStack, -0.3 * var80);
                ItemAnimationMath.translateX(poseStack, -0.2 * var18 * ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 4.14) * var80);
                ItemAnimationMath.translateY(poseStack, -0.3 * ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 4.14) * var80);
                ItemAnimationMath.rotateX(poseStack, 10.0 * ItemAnimationMath.sin(var80 * 3.14));
                ItemAnimationMath.rotateY(poseStack, 20.0 * var18 * var80);
                ItemAnimationMath.rotateY(poseStack, 10.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 4.14) * var80);
                ItemAnimationMath.rotateZ(poseStack, 30.0 * var18 * var80);
                ItemAnimationMath.rotateZ(poseStack, 30.0 * var18 * ItemAnimationMath.sin(itemFeatureVariables.offhandEatPhase * 4.14) * var80);
            }
            if (ItemFeatureProperties.isInTag(itemAnimationContext.stack, ItemTagAliases.DOORS)) {
                ItemAnimationMath.translateX(poseStack, 0.2 * var18);
                ItemAnimationMath.rotateAroundX(poseStack, 6.0, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 20.0 * var18, 0.3 * var18, -0.4, 0.0);
            }
            if (PlayerAnimationUtils.isOnCooldown(itemAnimationContext.stack, player) && useAnimationName.equals("block")) {
                itemFeatureVariables.blockAttackProgress = itemFeatureVariables.blockAttackProgress + 0.04 * itemAnimationContext.frameDelta * 30.0;
            } else if (useAnimationName.equals("block")) {
                itemFeatureVariables.blockAttackProgress = itemFeatureVariables.blockAttackProgress - 0.06 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.blockAttackProgress = ItemAnimationMath.clamp(itemFeatureVariables.blockAttackProgress, 0.0, 1.0);
            double var82 = itemFeatureVariables.blockAttackProgress * itemFeatureVariables.blockAttackProgress;
            if (useAnimationName.equals("block")) {
                ItemAnimationMath.translateZ(poseStack, -0.4 * var82);
                ItemAnimationMath.translateY(poseStack, 0.15 * var82);
                ItemAnimationMath.translateX(poseStack, -0.1 * var18 * var82);
                ItemAnimationMath.rotateX(poseStack, -30.0 * var82);
                ItemAnimationMath.rotateX(poseStack, -10.0 * ItemAnimationMath.sin(var82 * 3.14));
                ItemAnimationMath.rotateY(poseStack, -20.0 * var18 * var82);
                ItemAnimationMath.rotateZ(poseStack, -6.0 * var18 * var82);
            }
            itemFeatureVariables.lastMainHandSwingStarted = itemAnimationContext.mainHandSwingStarted;
            double var84 = ItemAnimationMath.sin(ItemAnimationMath.clamp(itemFeatureVariables.mainHandEatProgress, 0.80041, 1.0) * 3.14 * 5.0) * 0.45;
            itemFeatureVariables.eatBobVelocity = itemFeatureVariables.eatBobVelocity + var84 * itemAnimationContext.frameDelta * 30.0;
            itemFeatureVariables.eatBobVelocity = itemFeatureVariables.eatBobVelocity * ItemAnimationMath.pow(0.8, itemAnimationContext.frameDelta * 30.0F);
            if (empty && itemFeatureVariables.mainHandDrinkProgress > 0.0) {
                ItemAnimationMath.rotateZ(poseStack, -6.0 * var18);
                ItemAnimationMath.translateY(poseStack, -0.35);
            }
            double var86 = AnimationCurveMath.backInOut(itemFeatureVariables.mapHoldProgress);
            double var88 = AnimationCurveMath.backOut(itemFeatureVariables.lookDownProgress);
            if (item2) {
                ItemAnimationMath.translateX(poseStack, (0.3 - 0.1 * var88) * var18 * var86);
                ItemAnimationMath.translateY(poseStack, 0.18 * var86);
                ItemAnimationMath.translateZ(poseStack, 0.12 * var88 * var86);
                ItemAnimationMath.rotateX(poseStack, ItemAnimationMath.clamp(PlayerAnimationUtils.getPitch(player), 0.0, 50.0) * var86);
                ItemAnimationMath.rotateX(poseStack, -40.0 * var86);
                ItemAnimationMath.rotateAroundY(poseStack, (40.0 + 30.0 * var88) * var18 * var86, 0.3 * var18, -0.4, 0.0);
            }
            if (item2) {
                double var90 = 1.0 - var86;
                ItemAnimationMath.translateX(poseStack, 0.1 * var18 * var90);
                ItemAnimationMath.translateY(poseStack, -0.35 * var90);
                ItemAnimationMath.translateZ(poseStack, 0.22 * var90);
                ItemAnimationMath.rotateX(poseStack, 24.0 * var90);
                ItemAnimationMath.rotateY(poseStack, 10.0 * var18 * var90);
            }
            if (useAnimationName.equals("crossbow")) {
                ItemAnimationMath.translateX(poseStack, 0.1 * var18);
                ItemAnimationMath.translateZ(poseStack, 0.2);
                ItemAnimationMath.rotateAroundX(poseStack, -5.0, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundY(poseStack, 20.0 * var18, 0.3 * var18, -0.4, 0.0);
            }
            int var115 = itemFeatureVariables.inspectKey != 0 ? itemFeatureVariables.inspectKey : 67;
            if (ItemAnimationChecks.isAnimationKeyPressed(var115)) {
                itemFeatureVariables.inspectProgress = itemFeatureVariables.inspectProgress + 0.04 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.inspectProgress = itemFeatureVariables.inspectProgress - 0.04 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.inspectProgress = ItemAnimationMath.clamp(itemFeatureVariables.inspectProgress, 0.0, 1.0);
            if ((inTag || inTag2 || inTag3 || useAnimationName.equals("trident")) && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.35 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress));
                ItemAnimationMath.translateZ(poseStack, -0.15 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress));
                ItemAnimationMath.rotateAroundY(poseStack, 40.0 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress) * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 13.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(itemFeatureVariables.inspectProgress * 4.14), 0.0, 1.0), 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateX(poseStack, 10.0 * ItemAnimationMath.sin(AnimationCurveMath.backInOut(itemFeatureVariables.inspectFlourish) * 6.28));
            }
            if (item && itemAnimationContext.mainHand) {
                ItemAnimationMath.translateX(poseStack, 0.35 * var18 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress));
                ItemAnimationMath.translateZ(poseStack, -0.15 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress));
                ItemAnimationMath.rotateAroundY(poseStack, 40.0 * AnimationCurveMath.backInOut(itemFeatureVariables.inspectProgress) * var18, 0.3 * var18, -0.4, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 17.0 * ItemAnimationMath.clamp(ItemAnimationMath.sin(itemFeatureVariables.inspectProgress * 3.14), 0.0, 1.0), 0.3 * var18, -0.4, 0.0);
            }
            if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandCrossbowCharged = chargedCrossbow;
            } else {
                itemFeatureVariables.offhandCrossbowCharged = chargedCrossbow;
            }
            if (PlayerAnimationUtils.isSpinAttacking(player) && useAnimationName.equals("trident") && usedHand == itemAnimationContext.hand) {
                if (itemAnimationContext.mainHand) {
                    itemFeatureVariables.mainHandTridentSpin = itemFeatureVariables.mainHandTridentSpin + 0.08 * itemAnimationContext.frameDelta * 30.0;
                } else {
                    itemFeatureVariables.offhandTridentSpin = itemFeatureVariables.offhandTridentSpin + 0.08 * itemAnimationContext.frameDelta * 30.0;
                }
            } else if (itemAnimationContext.mainHand) {
                itemFeatureVariables.mainHandTridentSpin = itemFeatureVariables.mainHandTridentSpin - 0.025 * itemAnimationContext.frameDelta * 30.0;
            } else {
                itemFeatureVariables.offhandTridentSpin = itemFeatureVariables.offhandTridentSpin - 0.025 * itemAnimationContext.frameDelta * 30.0;
            }
            itemFeatureVariables.mainHandTridentSpin = ItemAnimationMath.clamp(itemFeatureVariables.mainHandTridentSpin, 0.0, 1.0);
            itemFeatureVariables.offhandTridentSpin = ItemAnimationMath.clamp(itemFeatureVariables.offhandTridentSpin, 0.0, 1.0);
            itemFeatureVariables.mainHandTridentSpin = itemFeatureVariables.mainHandTridentSpin * ItemAnimationMath.pow(0.95, itemAnimationContext.frameDelta * 30.0F) * (1.0 - ItemAnimationMath.clamp(itemAnimationContext.swingProgress * 4.0F, 0.0, 1.0)) * (1.0 - ItemAnimationMath.clamp(itemFeatureVariables.mainHandTridentRaise * 6.0, 0.0, 1.0));
            itemFeatureVariables.offhandTridentSpin = itemFeatureVariables.offhandTridentSpin * ItemAnimationMath.pow(0.95, itemAnimationContext.frameDelta * 30.0F) * (1.0 - ItemAnimationMath.clamp(itemAnimationContext.swingProgress * 4.0F, 0.0, 1.0)) * ItemAnimationMath.clamp(1.0 - itemFeatureVariables.offhandTridentRaise * 6.0, 0.0, 1.0);
            if (useAnimationName.equals("trident")) {
                double var91 = itemAnimationContext.mainHand ? AnimationCurveMath.backOut(itemFeatureVariables.mainHandTridentSpin) : AnimationCurveMath.backOut(itemFeatureVariables.offhandTridentSpin);
                double var93 = itemAnimationContext.mainHand ? ItemAnimationMath.clamp(itemFeatureVariables.mainHandTridentSpin * 3.0, 0.0, 1.0) : ItemAnimationMath.clamp(itemFeatureVariables.offhandTridentSpin * 3.0, 0.0, 1.0);
                ItemAnimationMath.translateZ(poseStack, 0.2 * var91 * var93);
                ItemAnimationMath.translateX(poseStack, -1.25 * var18 * var91 * var93);
                ItemAnimationMath.translateY(poseStack, 0.0 * var91 * var93);
                ItemAnimationMath.rotateY(poseStack, 5.0 * var18 * var91 * var93);
                ItemAnimationMath.rotateX(poseStack, -30.0 * AnimationCurveMath.backOut(ItemAnimationMath.sin(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandTridentSpin : itemFeatureVariables.offhandTridentSpin * 3.14)) * var93);
                ItemAnimationMath.rotateAroundZ(poseStack, 70.0 * AnimationCurveMath.backOut(ItemAnimationMath.sin(itemAnimationContext.mainHand ? itemFeatureVariables.mainHandTridentSpin : itemFeatureVariables.offhandTridentSpin * 3.14)) * var93 * var18, 0.7 * var18, 0.7, 0.0);
            }
            double var116 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearUse : itemFeatureVariables.offhandSpearUse;
            double var117 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearSlowCharge : itemFeatureVariables.offhandSpearSlowCharge;
            double var95 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandSpearFastCharge : itemFeatureVariables.offhandSpearFastCharge;
            double var97 = itemAnimationContext.mainHand ? itemFeatureVariables.mainHandEquipProgress : itemFeatureVariables.offhandEquipProgress;
            double var99 = itemAnimationContext.mainHand ? AnimationCurveMath.sineInOut(itemFeatureVariables.mainHandSpearThrust) : itemFeatureVariables.offhandSpearThrust;
            if (useAnimationName.equals("spear")) {
                ItemAnimationMath.translateZ(poseStack, -0.25);
                ItemAnimationMath.translateY(poseStack, 0.1);
                ItemAnimationMath.rotateX(poseStack, -20.0);
                ItemAnimationMath.translateZ(poseStack, 0.75 * ItemAnimationMath.sin(AnimationCurveMath.sineInOut(var99) * 3.14));
                ItemAnimationMath.translateZ(poseStack, -0.25 * AnimationCurveMath.backInOut(var116));
                ItemAnimationMath.translateZ(poseStack, 0.25 * AnimationCurveMath.backOut(var95) * var95);
                ItemAnimationMath.rotateY(poseStack, 8.0 * AnimationCurveMath.backInOut(var116) * var18);
                ItemAnimationMath.rotateAroundX(poseStack, 40.0 * ItemAnimationMath.sin(var116 * 3.14), 0.0, -0.2, -0.35);
                ItemAnimationMath.rotateAroundX(poseStack, -8.0 * AnimationCurveMath.backInOut(var117), 0.5 * var18, -0.5, -0.35);
                ItemAnimationMath.rotateAroundY(poseStack, -1.5 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 1.5) * AnimationCurveMath.backInOut(var117) * var18, 0.5 * var18, -0.5, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, -1.5 * ItemAnimationMath.sin(itemFeatureVariables.idlePhase * 3.0) * AnimationCurveMath.backInOut(var117), 0.5 * var18, -0.5, 0.0);
                ItemAnimationMath.rotateX(poseStack, 5.0 * ItemAnimationMath.sin(var99 * 3.14));
                ItemAnimationMath.rotateAroundZ(poseStack, -30.0 * AnimationCurveMath.backOut(var95) * var95 * var18, 0.5 * var18, -0.5, -0.35);
                ItemAnimationMath.rotateAroundZ(poseStack, -8.0 * ItemAnimationMath.sin(ItemAnimationMath.clamp(var97 * 2.0, 0.0, 1.0) * 6.28) * ItemAnimationMath.sin(ItemAnimationMath.clamp(var97 * 2.0, 0.0, 1.0) * 6.28) * var18, 0.5 * var18, -0.5, 0.0);
                ItemAnimationMath.rotateAroundX(poseStack, 20.0 * ItemAnimationMath.sin(ItemAnimationMath.clamp(var97 * 2.0, 0.0, 1.0) * 3.14) * ItemAnimationMath.sin(ItemAnimationMath.clamp(var97 * 2.0, 0.0, 1.0) * 3.14), 0.5 * var18, -0.5, 0.0);
            }
        }
    }
}
