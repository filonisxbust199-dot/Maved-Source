package ess.ll.client.features.crystalv2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GetSelfExplosionDamageUtil implements V2Access {
   public static final double EXPLOSION_DIAMETER = 12.0;

   public static float getSelfExplosionDamage(Vec3 explosionPos, boolean optimized) {
      return getExplosionDamage(explosionPos, mc.player, optimized);
   }

   public static float getExplosionDamage(Vec3 explosionPos, Player target, boolean optimized) {
      return getExplosionDamageAt(explosionPos, target, target.position(), optimized);
   }

   public static float getExplosionDamage(Vec3 explosionPos, Player target, int predict, boolean optimized) {
      if (predict <= 0) {
         return getExplosionDamage(explosionPos, target, optimized);
      }

      Simulated simulated = Simulated.simulateOtherPlayer(target, predict);
      return getExplosionDamageAt(explosionPos, target, simulated.pos, optimized);
   }

   public static float getExplosionDamageAt(Vec3 explosionPos, Player target, Vec3 samplePos, boolean optimized) {
      return getExplosionDamageAt(explosionPos, target, samplePos, optimized, 12.0);
   }

   public static float getMaximumExplosionDamageAt(Vec3 explosionPos, Player target, Vec3 samplePos) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         double distanceRatio = samplePos.distanceToSqr(explosionPos) / 144.0;
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double impact = 1.0 - distanceRatio;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamageAt(Vec3 explosionPos, Player target, Vec3 samplePos, boolean optimized, double explosionDiameter) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && !(explosionDiameter <= 0.0)
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / (explosionDiameter * explosionDiameter);
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposure(explosionPos, box, optimized);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * explosionDiameter + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamageWithFutureBlock(Vec3 explosionPos, Player target, BlockPos futureBlockPos, boolean optimized) {
      return futureBlockPos == null
         ? getExplosionDamage(explosionPos, target, optimized)
         : getDamageOfGhostBlock(explosionPos, target, futureBlockPos, optimized);
   }

   public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, BlockPos ghostPos, boolean optimized) {
      return getDamageOfGhostBlockAt(explosionPos, target, target.position(), ghostPos, optimized);
   }

   public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos ghostPos, int predict, boolean optimized) {
      if (predict <= 0) {
         return getDamageOfGhostBlockAt(explosionPos, target, samplePos, ghostPos, optimized);
      }

      Simulated simulated = Simulated.simulateOtherPlayer(target, predict);
      return getDamageOfGhostBlockAt(explosionPos, target, simulated.pos, ghostPos, optimized);
   }

   public static float getDamageOfGhostBlockAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos ghostPos, boolean optimized) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && ghostPos != null
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / 144.0;
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposureGhost(explosionPos, box, optimized, ghostPos);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getDamageIgnoringBlock(Vec3 explosionPos, Player target, BlockPos airPos, boolean optimized) {
      return getExplosionDamageAtIgnoringBlock(explosionPos, target, target.position(), airPos, optimized, 12.0);
   }

   public static float getDamageIgnoringBlockAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized) {
      return getExplosionDamageAtIgnoringBlock(explosionPos, target, samplePos, airPos, optimized, 12.0);
   }

   public static float getExplosionDamageAtIgnoringBlock(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized, double explosionDiameter
   ) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && airPos != null
         && !(explosionDiameter <= 0.0)
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / (explosionDiameter * explosionDiameter);
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposureIgnoringBlock(explosionPos, box, airPos, optimized);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * explosionDiameter + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getDamageIgnoringBlocks(Vec3 explosionPos, Player target, List<BlockPos> airPositions) {
      return getDamageIgnoringBlocks(explosionPos, target, airPositions, false);
   }

   public static float getDamageIgnoringBlocks(Vec3 explosionPos, Player target, List<BlockPos> airPositions, boolean optimized) {
      return target == null ? 0.0F : getDamageIgnoringBlocksAt(explosionPos, target, target.position(), airPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAt(Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, boolean optimized) {
      return getDamageIgnoringBlocksAt(explosionPos, target, samplePos, airPositions, optimized, 12.0);
   }

   public static float getDamageIgnoringBlocksAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, boolean optimized, double explosionDiameter
   ) {
      if (airPositions != null && !airPositions.isEmpty()) {
         if (mc.level != null
            && mc.player != null
            && target != null
            && explosionPos != null
            && samplePos != null
            && !(explosionDiameter <= 0.0)
            && mc.level.getDifficulty() != Difficulty.PEACEFUL
            && !target.isInvulnerable()) {
            AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
            AABB damageBounds = new AABB(
               Mth.floor(explosionPos.x - 11.0),
               Mth.floor(explosionPos.y - 11.0),
               Mth.floor(explosionPos.z - 11.0),
               Mth.floor(explosionPos.x + 13.0),
               Mth.floor(explosionPos.y + 13.0),
               Mth.floor(explosionPos.z + 13.0)
            );
            if (!damageBounds.intersects(box)) {
               return 0.0F;
            }

            double distanceRatio = samplePos.distanceToSqr(explosionPos) / (explosionDiameter * explosionDiameter);
            if (distanceRatio > 1.0) {
               return 0.0F;
            }

            Set<BlockPos> ignoredBlocks = new HashSet<>();

            for (BlockPos position : airPositions) {
               if (position != null) {
                  ignoredBlocks.add(position.immutable());
               }
            }

            double exposure = getExposureIgnoringBlocks(explosionPos, box, ignoredBlocks, optimized);
            double impact = (1.0 - distanceRatio) * exposure;
            float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * explosionDiameter + 1.0);
            DamageSource source = mc.level.damageSources().explosion(null, null);
            damage = CombatRules.getDamageAfterAbsorb(
               target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
            );
            MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
            if (resistance != null) {
               int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
               damage = Math.max(damage * remaining / 25.0F, 0.0F);
            }

            int protection = getProtectionAmount(
               target.getItemBySlot(EquipmentSlot.HEAD),
               target.getItemBySlot(EquipmentSlot.CHEST),
               target.getItemBySlot(EquipmentSlot.LEGS),
               target.getItemBySlot(EquipmentSlot.FEET)
            );
            return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
         } else {
            return 0.0F;
         }
      } else {
         return getExplosionDamageAt(explosionPos, target, samplePos, optimized, explosionDiameter);
      }
   }

   public static float getDamageIgnoringBlockAndGhost(Vec3 explosionPos, Player target, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlockAndGhostAt(explosionPos, target, target.position(), airPos, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlockAndGhostAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      return getExplosionDamageAtIgnoringBlockAndGhost(explosionPos, target, samplePos, airPos, ghostPos, optimized, 12.0);
   }

   public static float getDamageIgnoringBlocksAndGhostAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, BlockPos ghostPos, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(explosionPos, target, samplePos, airPositions, ghostPos == null ? List.of() : List.of(ghostPos), optimized);
   }

   public static float getDamageIgnoringBlockAndGhostsAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(explosionPos, target, samplePos, airPos == null ? List.of() : List.of(airPos), ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhostsAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(explosionPos, target, samplePos, airPositions, ghostPositions, optimized, 12.0);
   }

   public static float getDamageIgnoringBlocksAndGhostsAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized, double explosionDiameter
   ) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && !(explosionDiameter <= 0.0)
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         Set<BlockPos> airBlocks = new HashSet<>();
         if (airPositions != null) {
            for (BlockPos position : airPositions) {
               if (position != null) {
                  airBlocks.add(position.immutable());
               }
            }
         }

         Set<BlockPos> ghostBlocks = new HashSet<>();
         if (ghostPositions != null) {
            for (BlockPos position : ghostPositions) {
               if (position != null) {
                  ghostBlocks.add(position.immutable());
               }
            }
         }

         ghostBlocks.removeAll(airBlocks);
         if (ghostBlocks.isEmpty()) {
            return getDamageIgnoringBlocksAt(explosionPos, target, samplePos, airPositions, optimized, explosionDiameter);
         }

         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / (explosionDiameter * explosionDiameter);
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposureIgnoringBlocksAndGhosts(explosionPos, box, airBlocks, ghostBlocks, optimized);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * explosionDiameter + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamageAtIgnoringBlockAndGhost(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, BlockPos ghostPos, boolean optimized, double explosionDiameter
   ) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && airPos != null
         && ghostPos != null
         && !(explosionDiameter <= 0.0)
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / (explosionDiameter * explosionDiameter);
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposureIgnoringBlockAndGhost(explosionPos, box, airPos, ghostPos, optimized);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * explosionDiameter + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getDamageWithBlockOverrides(Vec3 explosionPos, Player target, BlockPos airPos, boolean optimized, BlockPos... ghostPositions) {
      if (ghostPositions != null && ghostPositions.length != 0) {
         if (ghostPositions.length == 1) {
            return airPos == null
               ? getDamageOfGhostBlock(explosionPos, target, ghostPositions[0], optimized)
               : getDamageIgnoringBlockAndGhost(explosionPos, target, airPos, ghostPositions[0], optimized);
         } else {
            return getDamageWithBlockOverridesAt(explosionPos, target, target.position(), airPos, optimized, ghostPositions);
         }
      } else {
         return airPos == null ? getExplosionDamage(explosionPos, target, optimized) : getDamageIgnoringBlock(explosionPos, target, airPos, optimized);
      }
   }

   public static float getDamageWithBlockOverridesAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized, BlockPos... ghostPositions
   ) {
      if (mc.level != null
         && mc.player != null
         && target != null
         && explosionPos != null
         && samplePos != null
         && ghostPositions != null
         && mc.level.getDifficulty() != Difficulty.PEACEFUL
         && !target.isInvulnerable()) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         AABB damageBounds = new AABB(
            Mth.floor(explosionPos.x - 11.0),
            Mth.floor(explosionPos.y - 11.0),
            Mth.floor(explosionPos.z - 11.0),
            Mth.floor(explosionPos.x + 13.0),
            Mth.floor(explosionPos.y + 13.0),
            Mth.floor(explosionPos.z + 13.0)
         );
         if (!damageBounds.intersects(box)) {
            return 0.0F;
         }

         double distanceRatio = samplePos.distanceToSqr(explosionPos) / 144.0;
         if (distanceRatio > 1.0) {
            return 0.0F;
         }

         double exposure = getExposureWithBlockOverrides(explosionPos, box, airPos, optimized, ghostPositions);
         double impact = (1.0 - distanceRatio) * exposure;
         float damage = (float)Math.floor((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
         DamageSource source = mc.level.damageSources().explosion(null, null);
         damage = CombatRules.getDamageAfterAbsorb(target, damage, source, target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            int remaining = 25 - (resistance.getAmplifier() + 1) * 5;
            damage = Math.max(damage * remaining / 25.0F, 0.0F);
         }

         int protection = getProtectionAmount(
            target.getItemBySlot(EquipmentSlot.HEAD),
            target.getItemBySlot(EquipmentSlot.CHEST),
            target.getItemBySlot(EquipmentSlot.LEGS),
            target.getItemBySlot(EquipmentSlot.FEET)
         );
         return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
      } else {
         return 0.0F;
      }
   }

   public static float getExposureWithBlockOverrides(Vec3 source, AABB box, BlockPos airPos, boolean optimized, BlockPos[] ghostPositions) {
      if (optimized) {
         int visible = 0;
         int total = 0;

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
                  if (raycastWithBlockOverrides(sample, source, airPos, ghostPositions).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      } else {
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
         int visible = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  if (raycastWithBlockOverrides(sample, source, airPos, ghostPositions).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      }
   }

   public static BlockHitResult raycastWithBlockOverrides(Vec3 start, Vec3 end, BlockPos airPos, BlockPos[] ghostPositions) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, new ToStringImpl24(airPos, ghostPositions), (overrides, pos) -> {
         BlockState state;
         if (overrides.airPos() != null && pos.equals(overrides.airPos())) {
            state = Blocks.AIR.defaultBlockState();
         } else if (containsBlock(overrides.ghostPositions(), pos)) {
            state = Blocks.OBSIDIAN.defaultBlockState();
         } else {
            state = mc.level.getBlockState(pos);
         }

         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, overrides -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static boolean containsBlock(BlockPos[] positions, BlockPos wanted) {
      for (BlockPos position : positions) {
         if (position != null && position.equals(wanted)) {
            return true;
         }
      }

      return false;
   }

   public static float getExposureIgnoringBlocksAndGhosts(Vec3 source, AABB box, Set<BlockPos> airBlocks, Set<BlockPos> ghostBlocks, boolean optimized) {
      if (optimized) {
         int visible = 0;

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
                  if (raycastIgnoringBlocksAndGhosts(sample, source, airBlocks, ghostBlocks).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }
               }
            }
         }

         return visible / 8.0F;
      } else {
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
         int visible = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  if (raycastIgnoringBlocksAndGhosts(sample, source, airBlocks, ghostBlocks).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      }
   }

   public static BlockHitResult raycastIgnoringBlocksAndGhosts(Vec3 start, Vec3 end, Set<BlockPos> airBlocks, Set<BlockPos> ghostBlocks) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, new ToStringImpl31(airBlocks, ghostBlocks), (overrides, pos) -> {
         BlockState state;
         if (overrides.airBlocks().contains(pos)) {
            state = Blocks.AIR.defaultBlockState();
         } else if (overrides.ghostBlocks().contains(pos)) {
            state = Blocks.OBSIDIAN.defaultBlockState();
         } else {
            state = mc.level.getBlockState(pos);
         }

         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, overrides -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static float getExposureIgnoringBlockAndGhost(Vec3 source, AABB box, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      if (optimized) {
         int visible = 0;
         int total = 0;

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
                  if (raycastIgnoringBlockAndGhost(sample, source, airPos, ghostPos).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      } else {
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
         int visible = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  if (raycastIgnoringBlockAndGhost(sample, source, airPos, ghostPos).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      }
   }

   public static BlockHitResult raycastIgnoringBlockAndGhost(Vec3 start, Vec3 end, BlockPos airPos, BlockPos ghostPos) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, new Overrides(airPos, ghostPos), (overrides, pos) -> {
         BlockState state;
         if (pos.equals(overrides.airPos())) {
            state = Blocks.AIR.defaultBlockState();
         } else if (pos.equals(overrides.ghostPos())) {
            state = Blocks.OBSIDIAN.defaultBlockState();
         } else {
            state = mc.level.getBlockState(pos);
         }

         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, overrides -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static float getExposureIgnoringBlock(Vec3 source, AABB box, BlockPos airPos, boolean optimized) {
      if (optimized) {
         int visible = 0;
         int total = 0;

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
                  if (raycastIgnoringBlock(sample, source, airPos).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      } else {
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
         int visible = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  if (raycastIgnoringBlock(sample, source, airPos).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      }
   }

   public static BlockHitResult raycastIgnoringBlock(Vec3 start, Vec3 end, BlockPos airPos) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, airPos, (ignored, pos) -> {
         BlockState state = pos.equals(ignored) ? Blocks.AIR.defaultBlockState() : mc.level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, ignored -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static float getExposureIgnoringBlocks(Vec3 source, AABB box, Set<BlockPos> airPositions, boolean optimized) {
      if (optimized) {
         int visible = 0;
         int total = 0;

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
                  if (raycastIgnoringBlocks(sample, source, airPositions).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      } else {
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
         int visible = 0;
         int total = 0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  if (raycastIgnoringBlocks(sample, source, airPositions).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                     visible++;
                  }

                  total++;
               }
            }
         }

         return total == 0 ? 0.0F : (float)visible / total;
      }
   }

   public static BlockHitResult raycastIgnoringBlocks(Vec3 start, Vec3 end, Set<BlockPos> airPositions) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, airPositions, (ignored, pos) -> {
         BlockState state = ignored.contains(pos) ? Blocks.AIR.defaultBlockState() : mc.level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, ignored -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static Set<BlockPos> getExposureBlockingBlocksAt(Vec3 source, Player target, Vec3 samplePos) {
      Set<BlockPos> blockers = new HashSet<>();
      if (mc.level != null && mc.player != null && source != null && target != null && samplePos != null) {
         AABB box = target.getBoundingBox().move(samplePos.subtract(target.position()));
         double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
         double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
         double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
         double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
         double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;

         for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
               for (double z = 0.0; z <= 1.0; z += stepZ) {
                  Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                  BlockHitResult hit = mc.level
                     .clip(new ClipContext(sample, source, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, mc.player));
                  if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                     blockers.add(hit.getBlockPos().immutable());
                  }
               }
            }
         }

         return blockers;
      } else {
         return blockers;
      }
   }

   public static float getExposure(Vec3 source, AABB box, boolean optimized) {
      if (!optimized) {
         return getExposure(source, box);
      }

      int visible = 0;
      int total = 0;

      for (int x = 0; x <= 1; x++) {
         for (int y = 0; y <= 1; y++) {
            for (int z = 0; z <= 1; z++) {
               Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
               if (raycastMisses(sample, source)) {
                  visible++;
               }

               total++;
            }
         }
      }

      return total == 0 ? 0.0F : (float)visible / total;
   }

   public static float getExposure(Vec3 source, AABB box) {
      double stepX = 0.4545454446934474;
      double stepY = 0.21739130885479366;
      double stepZ = 0.4545454446934474;
      int visible = 0;
      int total = 0;

      for (double x = 0.0; x <= 1.0; x += stepX) {
         for (double y = 0.0; y <= 1.0; y += stepY) {
            for (double z = 0.0; z <= 1.0; z += stepZ) {
               Vec3 sample = new Vec3(
                  Mth.lerp(x, box.minX, box.maxX) + 0.045454555306552624,
                  Mth.lerp(y, box.minY, box.maxY),
                  Mth.lerp(z, box.minZ, box.maxZ) + 0.045454555306552624
               );
               if (raycastMisses(sample, source)) {
                  visible++;
               }

               total++;
            }
         }
      }

      return total == 0 ? 0.0F : (float)visible / total;
   }

   public static float getExposureGhost(Vec3 source, AABB box, boolean optimized, BlockPos ghostPos) {
      if (!optimized) {
         return getExposureGhost(source, box, ghostPos);
      }

      int visible = 0;
      int total = 0;

      for (int x = 0; x <= 1; x++) {
         for (int y = 0; y <= 1; y++) {
            for (int z = 0; z <= 1; z++) {
               Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX), Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ));
               if (raycastGhostMisses(sample, source, ghostPos)) {
                  visible++;
               }

               total++;
            }
         }
      }

      return total == 0 ? 0.0F : (float)visible / total;
   }

   public static float getExposureGhost(Vec3 source, AABB box, BlockPos ghostPos) {
      double stepX = 0.4545454446934474;
      double stepY = 0.21739130885479366;
      double stepZ = 0.4545454446934474;
      int visible = 0;
      int total = 0;

      for (double x = 0.0; x <= 1.0; x += stepX) {
         for (double y = 0.0; y <= 1.0; y += stepY) {
            for (double z = 0.0; z <= 1.0; z += stepZ) {
               Vec3 sample = new Vec3(
                  Mth.lerp(x, box.minX, box.maxX) + 0.045454555306552624,
                  Mth.lerp(y, box.minY, box.maxY),
                  Mth.lerp(z, box.minZ, box.maxZ) + 0.045454555306552624
               );
               if (raycastGhostMisses(sample, source, ghostPos)) {
                  visible++;
               }

               total++;
            }
         }
      }

      return total == 0 ? 0.0F : (float)visible / total;
   }

   public static boolean raycastMisses(Vec3 start, Vec3 end) {
      return mc.level.clip(new ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, mc.player)).getType()
         == net.minecraft.world.phys.HitResult.Type.MISS;
   }

   public static boolean raycastGhostMisses(Vec3 start, Vec3 end, BlockPos ghostPos) {
      return raycastGhost(start, end, ghostPos).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
   }

   public static BlockHitResult raycastGhost(Vec3 start, Vec3 end, BlockPos ghostPos) {
      return (BlockHitResult)BlockGetter.traverseBlocks(start, end, ghostPos, (gp, pos) -> {
         BlockState state = pos.equals(gp) ? Blocks.OBSIDIAN.defaultBlockState() : mc.level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(mc.level, pos);
         return shape.clip(start, end, pos);
      }, gp -> BlockHitResult.miss(end, Direction.getApproximateNearest(start.x - end.x, start.y - end.y, start.z - end.z), BlockPos.containing(end)));
   }

   public static int getProtectionAmount(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
      return getProtectionAmount(head) + getProtectionAmount(chest) + getProtectionAmount(legs) + getProtectionAmount(feet);
   }

   public static int getProtectionAmount(ItemStack stack) {
      int amount = 0;

      for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
         if (((Holder)entry.getKey()).is(Enchantments.BLAST_PROTECTION)) {
            amount += entry.getIntValue() * 2;
         } else if (((Holder)entry.getKey()).is(Enchantments.PROTECTION)) {
            amount += entry.getIntValue();
         }
      }

      return amount;
   }
}
