package ess.ll.client.features.inventory;

import ess.ll.client.integration.ItemStackAccess;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;

public final class ItemFeatureProperties {
    private static final Map<Item, String> ITEM_IDS = new IdentityHashMap<>();
    private static final ItemFeatureFlags FEATURE_FLAGS = new ItemFeatureFlags(true, false, false, false);

    public static void setSwingSpeed(ItemStack stack, double var1) {
        ((ItemStackAccess) (Object) stack).hMI5_0$setSwingSpeed((int) var1);
    }

    public static boolean isItem(ItemStack stack, Item item) {
        return stack.is(item);
    }

    public static ItemFeatureFlags getFeatureFlags(ItemStack stack) {
        return FEATURE_FLAGS;
    }

    private ItemFeatureProperties() {
    }

    public static String getUseAnimationName(ItemStack stack) {
        return stack.getUseAnimation().getSerializedName();
    }

    public static boolean shouldRenderAsBlock(ItemStack stack) {
        if (Block.byItem(stack.getItem()) == Blocks.AIR) {
            return false;
        }
        int var1 = ((ItemStackAccess) (Object) stack).hMI5_0$getRenderAsBlock();
        return var1 == 1 || var1 == -1;
    }

    public static boolean isLantern(ItemStack stack) {
        return Block.byItem(stack.getItem()) instanceof LanternBlock;
    }

    public static boolean isBlock(ItemStack stack) {
        return Block.byItem(stack.getItem()) != Blocks.AIR;
    }

    private static boolean supportsTransform(ItemStack stack) {
        boolean var1 = stack.getItem() instanceof FishingRodItem || stack.is(ConventionalItemTags.RODS) || stack.is(ConventionalItemTags.TOOLS) || stack.is(ItemTags.SWORDS) || stack.is(ConventionalItemTags.MACE_TOOLS) || stack.getUseAnimation() == ItemUseAnimation.BLOCK;
        return !var1 && getAttackDamage(stack) <= 0.0F ? true : stack.getUseAnimation() == ItemUseAnimation.EAT || stack.getUseAnimation() == ItemUseAnimation.DRINK || stack.getUseAnimation() == ItemUseAnimation.SPYGLASS;
    }

    public static float getAttackDamage(ItemStack stack) {
        ItemAttributeModifiers itemAttributeModifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (itemAttributeModifiers == null) {
            return 0.0F;
        }
        float var2 = 0.0F;
        for (var var4 : itemAttributeModifiers.modifiers()) {
            if (var4.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                var2 += (float) var4.modifier().amount();
            }
        }
        return var2;
    }

    public static String getRegistryId(ItemStack stack) {
        Item item = stack.getItem();
        String var2 = ITEM_IDS.get(item);
        if (var2 == null) {
            var2 = BuiltInRegistries.ITEM.getKey(item).toString();
            ITEM_IDS.put(item, var2);
        }
        return var2;
    }

    public static void setTransformEnabled(ItemStack stack, boolean var1) {
        ((ItemStackAccess) (Object) stack).hMI5_0$setTransform(var1);
    }

    public static boolean shouldTransform(ItemStack stack) {
        int var1 = ((ItemStackAccess) (Object) stack).hMI5_0$getTransform();
        return var1 == 0 ? false : supportsTransform(stack);
    }

    public static boolean isCustomItemEnabled(ItemStack stack) {
        return ItemFeatureVariables.INSTANCE.flatBlockItems.getOrDefault(getRegistryId(stack), Boolean.FALSE);
    }

    public static boolean isThrowable(ItemStack stack) {
        return stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof ProjectileItem;
    }

    public static boolean isChargedCrossbow(ItemStack stack) {
        return CrossbowItem.isCharged(stack);
    }

    public static void setRenderAsBlock(ItemStack stack, boolean var1) {
        if (Block.byItem(stack.getItem()) != Blocks.AIR) {
            ((ItemStackAccess) (Object) stack).hMI5_0$setRenderAsBlock(var1);
        }
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack.isEmpty();
    }

    public static boolean isInTag(ItemStack stack, TagKey<Item> tagKey) {
        return stack.is(tagKey);
    }
}
