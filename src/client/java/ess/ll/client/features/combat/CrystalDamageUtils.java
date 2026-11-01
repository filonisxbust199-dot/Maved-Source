package ess.ll.client.features.combat;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.EnchantmentUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

public final class CrystalDamageUtils implements MinecraftAccess {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final double RADIANS_TO_DEGREES = 180.0F / (float) Math.PI;
    private static final float X_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float Z_DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    public static float yawToTarget(Vec3 vector, LivingEntity entity) {
        return (float) (Mth.atan2(entity.getZ() - vector.z, entity.getX() - vector.x) * RADIANS_TO_DEGREES) - 90.0F;
    }

    public static Vec3 predictKnockback(LivingEntity entity, Vec3 vector, boolean var2, Vec3 vector2, float var4, float var5) {
        double var6 = knockbackResistance(entity);
        Vec3 vector3 = applyKnockback(vector, 0.4F, vector2.x - entity.getX(), vector2.z - entity.getZ(), var2, var6);
        if (var5 > 0.0F) {
            double var9 = Mth.sin(var4 * X_DEGREES_TO_RADIANS);
            double var11 = -Mth.cos(var4 * Z_DEGREES_TO_RADIANS);
            vector3 = applyKnockback(vector3, var5, var9, var11, var2, var6);
        }
        return vector3;
    }

    private static double knockbackResistance(LivingEntity entity) {
        double[] var1 = new double[] { 0.0 };
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            entity.getItemBySlot(equipmentSlot).forEachModifier(equipmentSlot, (var1x, var2) -> {
                if (var1x.equals(Attributes.KNOCKBACK_RESISTANCE) && var2.operation() == Operation.ADD_VALUE) {
                    var1[0] += var2.amount();
                }
            });
        }
        return Mth.clamp(Math.max(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), var1[0]), 0.0, 1.0);
    }

    private CrystalDamageUtils() {
    }

    public static Vec3 applyKnockback(Vec3 vector, double var1, double var3, double var5, boolean var7, double var8) {
        double var10 = var1 * (1.0 - var8);
        if (!(var10 <= 0.0) && !(var3 * var3 + var5 * var5 < 1.0E-5F)) {
            Vec3 vector2 = new Vec3(var3, 0.0, var5).normalize().scale(var10);
            return new Vec3(vector.x / 2.0 - vector2.x, var7 ? Math.min(0.4, vector.y / 2.0 + var10) : vector.y, vector.z / 2.0 - vector2.z);
        } else {
            return vector;
        }
    }

    public static float attackKnockback() {
        return attackKnockback(mc.player.isSprinting() && mc.player.getAttackStrengthScale(0.5F) > 0.9F);
    }

    public static float attackKnockback(boolean var0) {
        float attributeValue = (float) mc.player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        attributeValue += EnchantmentUtils.getLevel(mc.player.getMainHandItem(), Enchantments.KNOCKBACK);
        return attributeValue / 2.0F + (var0 ? 0.5F : 0.0F);
    }
}
