package ess.ll.client.features.crystalv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GetExplosionDamageUtil {
   public static final int MAX_DAMAGE_DISTANCE = 10;
   public static final double EXPLOSION_DIAMETER = 10.0;

   public static float getExplosionDamage(BlockPos anchorPos, Player target, boolean optimized) {
      return getExplosionDamageAt(anchorPos, target, position(target), optimized);
   }

   public static float getExplosionDamage(Vec3 explosionPos, Player target, boolean optimized) {
      return getExplosionDamage(anchorPosition(explosionPos), target, optimized);
   }

   public static float getExplosionDamage(BlockPos anchorPos, Player target, int predict, boolean optimized) {
      if (target != null && anchorPos != null) {
         Vec3 samplePos = predict > 0 ? Simulated.simulateOtherPlayer(target, predict).pos : target.position();
         return getExplosionDamageAt(anchorPos, target, samplePos, optimized);
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamage(Vec3 explosionPos, Player target, int predict, boolean optimized) {
      return getExplosionDamage(anchorPosition(explosionPos), target, predict, optimized);
   }

   public static float getExplosionDamageAt(BlockPos anchorPos, Player target, Vec3 samplePos, boolean optimized) {
      return anchorPos == null
         ? 0.0F
         : GetSelfExplosionDamageUtil.getExplosionDamageAtIgnoringBlock(Vec3.atCenterOf(anchorPos), target, samplePos, anchorPos, optimized, 10.0);
   }

   public static float getExplosionDamageAt(Vec3 explosionPos, Player target, Vec3 samplePos, boolean optimized) {
      return getExplosionDamageAt(anchorPosition(explosionPos), target, samplePos, optimized);
   }

   public static float getExplosionDamageWithFutureBlock(BlockPos anchorPos, Player target, BlockPos futureBlockPos, boolean optimized) {
      return getDamageOfGhostBlock(anchorPos, target, futureBlockPos, optimized);
   }

   public static float getExplosionDamageWithFutureBlock(Vec3 explosionPos, Player target, BlockPos futureBlockPos, boolean optimized) {
      return getExplosionDamageWithFutureBlock(anchorPosition(explosionPos), target, futureBlockPos, optimized);
   }

   public static float getDamageOfGhostBlock(BlockPos anchorPos, Player target, BlockPos ghostPos, boolean optimized) {
      return getDamageOfGhostBlockAt(anchorPos, target, position(target), ghostPos, optimized);
   }

   public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, BlockPos ghostPos, boolean optimized) {
      return getDamageOfGhostBlock(anchorPosition(explosionPos), target, ghostPos, optimized);
   }

   public static float getDamageOfGhostBlock(BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos ghostPos, int predict, boolean optimized) {
      if (target != null && anchorPos != null) {
         if (predict > 0) {
            samplePos = Simulated.simulateOtherPlayer(target, predict).pos;
         }

         return getDamageOfGhostBlockAt(anchorPos, target, samplePos, ghostPos, optimized);
      } else {
         return 0.0F;
      }
   }

   public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos ghostPos, int predict, boolean optimized) {
      return getDamageOfGhostBlock(anchorPosition(explosionPos), target, samplePos, ghostPos, predict, optimized);
   }

   public static float getDamageOfGhostBlockAt(BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlockAndGhostAt(anchorPos, target, samplePos, null, ghostPos, optimized);
   }

   public static float getDamageOfGhostBlockAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos ghostPos, boolean optimized) {
      return getDamageOfGhostBlockAt(anchorPosition(explosionPos), target, samplePos, ghostPos, optimized);
   }

   public static float getDamageOfGhostBlocks(BlockPos anchorPos, Player target, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageOfGhostBlocksAt(anchorPos, target, position(target), ghostPositions, optimized);
   }

   public static float getDamageOfGhostBlocks(Vec3 explosionPos, Player target, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageOfGhostBlocks(anchorPosition(explosionPos), target, ghostPositions, optimized);
   }

   public static float getDamageOfGhostBlocksAt(BlockPos anchorPos, Player target, Vec3 samplePos, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageIgnoringBlocksAndGhostsAt(anchorPos, target, samplePos, null, ghostPositions, optimized);
   }

   public static float getDamageOfGhostBlocksAt(Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageOfGhostBlocksAt(anchorPosition(explosionPos), target, samplePos, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlock(BlockPos anchorPos, Player target, BlockPos airPos, boolean optimized) {
      return getDamageIgnoringBlockAt(anchorPos, target, position(target), airPos, optimized);
   }

   public static float getDamageIgnoringBlock(Vec3 explosionPos, Player target, BlockPos airPos, boolean optimized) {
      return getDamageIgnoringBlock(anchorPosition(explosionPos), target, airPos, optimized);
   }

   public static float getDamageIgnoringBlockAt(BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized) {
      if (anchorPos == null) {
         return 0.0F;
      } else {
         return airPos != null && !anchorPos.equals(airPos)
            ? getDamageIgnoringBlocksAt(anchorPos, target, samplePos, List.of(airPos), optimized)
            : getExplosionDamageAt(anchorPos, target, samplePos, optimized);
      }
   }

   public static float getDamageIgnoringBlockAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized) {
      return getDamageIgnoringBlockAt(anchorPosition(explosionPos), target, samplePos, airPos, optimized);
   }

   public static float getDamageIgnoringBlocks(BlockPos anchorPos, Player target, List<BlockPos> airPositions) {
      return getDamageIgnoringBlocks(anchorPos, target, airPositions, false);
   }

   public static float getDamageIgnoringBlocks(Vec3 explosionPos, Player target, List<BlockPos> airPositions) {
      return getDamageIgnoringBlocks(anchorPosition(explosionPos), target, airPositions);
   }

   public static float getDamageIgnoringBlocks(BlockPos anchorPos, Player target, List<BlockPos> airPositions, boolean optimized) {
      return getDamageIgnoringBlocksAt(anchorPos, target, position(target), airPositions, optimized);
   }

   public static float getDamageIgnoringBlocks(Vec3 explosionPos, Player target, List<BlockPos> airPositions, boolean optimized) {
      return getDamageIgnoringBlocks(anchorPosition(explosionPos), target, airPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAt(BlockPos anchorPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, boolean optimized) {
      if (anchorPos == null) {
         return 0.0F;
      } else {
         return airPositions != null && !airPositions.isEmpty()
            ? GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAt(
               Vec3.atCenterOf(anchorPos), target, samplePos, airBlocks(anchorPos, airPositions), optimized, 10.0
            )
            : getExplosionDamageAt(anchorPos, target, samplePos, optimized);
      }
   }

   public static float getDamageIgnoringBlocksAt(Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, boolean optimized) {
      return getDamageIgnoringBlocksAt(anchorPosition(explosionPos), target, samplePos, airPositions, optimized);
   }

   public static float getDamageIgnoringBlockAndGhost(BlockPos anchorPos, Player target, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlockAndGhostAt(anchorPos, target, position(target), airPos, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlockAndGhost(Vec3 explosionPos, Player target, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlockAndGhost(anchorPosition(explosionPos), target, airPos, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlockAndGhostAt(
      BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos airPos, BlockPos ghostPos, boolean optimized
   ) {
      if (anchorPos == null) {
         return 0.0F;
      } else if (ghostPos == null) {
         return getDamageIgnoringBlockAt(anchorPos, target, samplePos, airPos, optimized);
      } else {
         return airPos != null && !anchorPos.equals(airPos)
            ? getDamageIgnoringBlocksAndGhostAt(anchorPos, target, samplePos, List.of(airPos), ghostPos, optimized)
            : GetSelfExplosionDamageUtil.getExplosionDamageAtIgnoringBlockAndGhost(
               Vec3.atCenterOf(anchorPos), target, samplePos, anchorPos, ghostPos, optimized, 10.0
            );
      }
   }

   public static float getDamageIgnoringBlockAndGhostAt(Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlockAndGhostAt(anchorPosition(explosionPos), target, samplePos, airPos, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhost(BlockPos anchorPos, Player target, List<BlockPos> airPositions, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlocksAndGhostAt(anchorPos, target, position(target), airPositions, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhost(Vec3 explosionPos, Player target, List<BlockPos> airPositions, BlockPos ghostPos, boolean optimized) {
      return getDamageIgnoringBlocksAndGhost(anchorPosition(explosionPos), target, airPositions, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhostAt(
      BlockPos anchorPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, BlockPos ghostPos, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(anchorPos, target, samplePos, airPositions, singleton(ghostPos), optimized);
   }

   public static float getDamageIgnoringBlocksAndGhostAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, BlockPos ghostPos, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostAt(anchorPosition(explosionPos), target, samplePos, airPositions, ghostPos, optimized);
   }

   public static float getDamageIgnoringBlockAndGhosts(BlockPos anchorPos, Player target, BlockPos airPos, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageIgnoringBlockAndGhostsAt(anchorPos, target, position(target), airPos, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlockAndGhosts(Vec3 explosionPos, Player target, BlockPos airPos, List<BlockPos> ghostPositions, boolean optimized) {
      return getDamageIgnoringBlockAndGhosts(anchorPosition(explosionPos), target, airPos, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlockAndGhostsAt(
      BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos airPos, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(anchorPos, target, samplePos, singleton(airPos), ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlockAndGhostsAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlockAndGhostsAt(anchorPosition(explosionPos), target, samplePos, airPos, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhosts(
      BlockPos anchorPos, Player target, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(anchorPos, target, position(target), airPositions, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhosts(
      Vec3 explosionPos, Player target, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhosts(anchorPosition(explosionPos), target, airPositions, ghostPositions, optimized);
   }

   public static float getDamageIgnoringBlocksAndGhostsAt(
      BlockPos anchorPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized
   ) {
      if (anchorPos == null) {
         return 0.0F;
      } else {
         return ghostPositions != null && !ghostPositions.isEmpty()
            ? GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(
               Vec3.atCenterOf(anchorPos), target, samplePos, airBlocks(anchorPos, airPositions), ghostPositions, optimized, 10.0
            )
            : getDamageIgnoringBlocksAt(anchorPos, target, samplePos, airPositions, optimized);
      }
   }

   public static float getDamageIgnoringBlocksAndGhostsAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, List<BlockPos> airPositions, List<BlockPos> ghostPositions, boolean optimized
   ) {
      return getDamageIgnoringBlocksAndGhostsAt(anchorPosition(explosionPos), target, samplePos, airPositions, ghostPositions, optimized);
   }

   public static float getDamageWithBlockOverrides(BlockPos anchorPos, Player target, BlockPos airPos, boolean optimized, BlockPos... ghostPositions) {
      return getDamageWithBlockOverridesAt(anchorPos, target, position(target), airPos, optimized, ghostPositions);
   }

   public static float getDamageWithBlockOverrides(Vec3 explosionPos, Player target, BlockPos airPos, boolean optimized, BlockPos... ghostPositions) {
      return getDamageWithBlockOverrides(anchorPosition(explosionPos), target, airPos, optimized, ghostPositions);
   }

   public static float getDamageWithBlockOverridesAt(
      BlockPos anchorPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized, BlockPos... ghostPositions
   ) {
      if (ghostPositions != null && ghostPositions.length != 0) {
         return ghostPositions.length == 1
            ? getDamageIgnoringBlockAndGhostAt(anchorPos, target, samplePos, airPos, ghostPositions[0], optimized)
            : getDamageIgnoringBlockAndGhostsAt(anchorPos, target, samplePos, airPos, Arrays.asList(ghostPositions), optimized);
      } else {
         return getDamageIgnoringBlockAt(anchorPos, target, samplePos, airPos, optimized);
      }
   }

   public static float getDamageWithBlockOverridesAt(
      Vec3 explosionPos, Player target, Vec3 samplePos, BlockPos airPos, boolean optimized, BlockPos... ghostPositions
   ) {
      return getDamageWithBlockOverridesAt(anchorPosition(explosionPos), target, samplePos, airPos, optimized, ghostPositions);
   }

   public static Vec3 position(Player target) {
      return target == null ? null : target.position();
   }

   public static BlockPos anchorPosition(Vec3 explosionPos) {
      return explosionPos == null ? null : BlockPos.containing(explosionPos);
   }

   public static List<BlockPos> singleton(BlockPos position) {
      return position == null ? List.of() : List.of(position);
   }

   public static List<BlockPos> airBlocks(BlockPos anchorPos, List<BlockPos> airPositions) {
      List<BlockPos> result = new ArrayList<>(1 + (airPositions == null ? 0 : airPositions.size()));
      result.add(anchorPos);
      if (airPositions != null) {
         result.addAll(airPositions);
      }

      return result;
   }
}
