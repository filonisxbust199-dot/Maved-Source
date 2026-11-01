package ess.ll.client.features.world;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.player.AutoTool;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBreakSpeedUtils implements MinecraftAccess {
    private static final Map<BlockState, Float> BREAK_TICKS_CACHE = new HashMap<>();
    private static int cacheTick = Integer.MIN_VALUE;
    private static ResourceKey<Attribute> miningEfficiencyKey;
    private static final float NO_WORLD_BREAK_TICKS = Float.MAX_VALUE;
    private static final float UNAVAILABLE_BREAK_TICKS = Float.MAX_VALUE;
    private static final float UNBREAKABLE_TICKS = Float.MAX_VALUE;

    public static float miningEfficiency(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0F;
        }
        if (miningEfficiencyKey == null) {
            miningEfficiencyKey = Attributes.MINING_EFFICIENCY.unwrapKey().orElseThrow();
        }
        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) {
            return 0.0F;
        }
        float sx = 0.0F;
        for (var sxx : enchantments.keySet()) {
            for (EnchantmentAttributeEffect enchantmentAttributeEffect : sxx.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES)) {
                if (enchantmentAttributeEffect.attribute().is(miningEfficiencyKey)) {
                    sx += enchantmentAttributeEffect.amount().calculate(enchantments.getLevel(sxx));
                }
            }
        }
        return sx;
    }

    private static float calculateBreakTicks(BlockState blockState, BlockPos blockPos) {
        float destroyProgress = blockState.getDestroyProgress(mc.player, mc.level, blockPos);
        if (destroyProgress <= 0.0F) {
            return UNBREAKABLE_TICKS;
        }
        float var3 = normalizedDestroySpeed(mc.player.getInventory().getSelectedItem(), blockState);
        float var4 = normalizedDestroySpeed(AutoTool.bestAvailableTool(blockState), blockState);
        if (var3 > 0.0F && var4 > 0.0F) {
            destroyProgress *= var4 / var3;
        }
        return 1.0F / destroyProgress;
    }

    public static float destroySpeed(ItemStack stack, BlockState blockState) {
        float var2 = stack.isEmpty() ? 1.0F : stack.getDestroySpeed(blockState);
        if (var2 > 1.0F) {
            var2 += miningEfficiency(stack);
        }
        return var2;
    }

    public static int harvestPenalty(ItemStack stack, BlockState blockState) {
        boolean var2 = !blockState.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(blockState);
        return var2 ? 30 : 100;
    }

    public static float normalizedDestroySpeed(ItemStack stack, BlockState blockState) {
        return destroySpeed(stack, blockState) / harvestPenalty(stack, blockState);
    }

    public static float breakTicks(BlockPos blockPos) {
        return mc.level == null ? NO_WORLD_BREAK_TICKS : breakTicks(mc.level.getBlockState(blockPos), blockPos);
    }

    private BlockBreakSpeedUtils() {
    }

    public static float breakTicks(BlockState blockState, BlockPos blockPos) {
        if (mc.player != null && mc.level != null) {
            if (cacheTick != mc.player.tickCount) {
                BREAK_TICKS_CACHE.clear();
                cacheTick = mc.player.tickCount;
            }
            Float var2 = BREAK_TICKS_CACHE.get(blockState);
            if (var2 != null) {
                return var2;
            }
            float breakTicks2 = calculateBreakTicks(blockState, blockPos);
            BREAK_TICKS_CACHE.put(blockState, breakTicks2);
            return breakTicks2;
        } else {
            return UNAVAILABLE_BREAK_TICKS;
        }
    }
}
