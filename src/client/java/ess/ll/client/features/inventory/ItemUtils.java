package ess.ll.client.features.inventory;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.EnchantmentUtils;
import ess.ll.client.util.MovementUtils;
import ess.ll.client.util.PlayerUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public final class ItemUtils implements MinecraftAccess {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public static boolean wearsElytra(LivingEntity entity) {
        return entity instanceof Player var1 && var1.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    public static double protectionScore(Player player) {
        double var1 = 0.0;
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack itemBySlot = player.getItemBySlot(equipmentSlot);
            if (!itemBySlot.isEmpty() && itemBySlot.isEnchanted()) {
                var1 += EnchantmentUtils.getLevel(itemBySlot, Enchantments.PROTECTION) * 0.25;
            }
        }
        return var1;
    }

    public static double weightedHealth(LivingEntity entity) {
        float var1 = PlayerUtils.health(entity) + entity.getAbsorptionAmount();
        return entity instanceof Player var2 ? var1 * (protectionScore(var2) / 20.0) : var1;
    }

    public static double weightedHealthRatio(LivingEntity entity) {
        return weightedHealth(entity) / entity.getMaxHealth();
    }

    public static double relativeYaw(LivingEntity entity) {
        return Math.abs(MovementUtils.relativeMoveYaw(entity.position().subtract(mc.player.position()), mc.player.getYRot()));
    }

    public static double armorScore(LivingEntity entity) {
        return (entity instanceof Player var1 ? protectionScore(var1) : entity.getArmorValue()) / 20.0;
    }

    private ItemUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
