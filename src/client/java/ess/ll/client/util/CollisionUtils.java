package ess.ll.client.util;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CollisionUtils {
   private static final MutableBlockPos MUTABLE_POS = new MutableBlockPos();
   private static final double EPSILON_UNUSED = 1.0E-7;
   private static final double EPSILON_MIN_X = 1.0E-7;
   private static final double EPSILON_MAX_X = 1.0E-7;
   private static final double EPSILON_MIN_Y = 1.0E-7;
   private static final double EPSILON_MAX_Y = 1.0E-7;
   private static final double EPSILON_MIN_Z = 1.0E-7;
   private static final double EPSILON_MAX_Z = 1.0E-7;

   public static boolean noCollision(Level level, AABB aabb, Vec3 motion) {
      if (level == null) {
         return false;
      }

      int minX = Mth.floor(aabb.minX + Math.min(motion.x, 0.0) - EPSILON_MIN_X) - 1;
      int maxX = Mth.floor(aabb.maxX + Math.max(motion.x, 0.0) + EPSILON_MAX_X) + 1;
      int minY = Mth.floor(aabb.minY + Math.min(motion.y, 0.0) - EPSILON_MIN_Y) - 1;
      int maxY = Mth.floor(aabb.maxY + Math.max(motion.y, 0.0) + EPSILON_MAX_Y) + 1;
      int minZ = Mth.floor(aabb.minZ + Math.min(motion.z, 0.0) - EPSILON_MIN_Z) - 1;
      int maxZ = Mth.floor(aabb.maxZ + Math.max(motion.z, 0.0) + EPSILON_MAX_Z) + 1;

      for (int x = minX; x <= maxX; x++) {
         int edgeX = x != minX && x != maxX ? 0 : 1;

         for (int y = minY; y <= maxY; y++) {
            int edgeXY = edgeX + (y != minY && y != maxY ? 0 : 1);

            for (int z = minZ; z <= maxZ; z++) {
               int edges = edgeXY + (z != minZ && z != maxZ ? 0 : 1);
               if (edges != 3) {
                  BlockState blockState = level.getBlockState(MUTABLE_POS.set(x, y, z));

                  boolean blocked = switch (edges) {
                     case 0 -> !blockState.isAir();
                     case 1 -> blockState.hasLargeCollisionShape();
                     default -> blockState.is(Blocks.MOVING_PISTON);
                  };
                  if (blocked) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   private CollisionUtils() {
   }
}
