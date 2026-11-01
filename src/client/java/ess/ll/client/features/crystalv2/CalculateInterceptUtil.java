package ess.ll.client.features.crystalv2;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CalculateInterceptUtil {
   @Contract("_, _, _ -> new")
   @NotNull
   public static Pair<@Nullable Vec3, @Nullable Direction> calculateIntercept(@NotNull AABB self, @NotNull Vec3 origin, @NotNull Vec3 end) {
      Vec3 minX = getIntermediateWithXValue(origin, end, self.minX);
      Vec3 maxX = getIntermediateWithXValue(origin, end, self.maxX);
      Vec3 minY = getIntermediateWithYValue(origin, end, self.minY);
      Vec3 maxY = getIntermediateWithYValue(origin, end, self.maxY);
      Vec3 minZ = getIntermediateWithZValue(origin, end, self.minZ);
      Vec3 maxZ = getIntermediateWithZValue(origin, end, self.maxZ);
      if (!isVecInYZ(self, minX)) {
         minX = null;
      }

      if (!isVecInYZ(self, maxX)) {
         maxX = null;
      }

      if (!isVecInXZ(self, minY)) {
         minY = null;
      }

      if (!isVecInXZ(self, maxY)) {
         maxY = null;
      }

      if (!isVecInXY(self, minZ)) {
         minZ = null;
      }

      if (!isVecInXY(self, maxZ)) {
         maxZ = null;
      }

      Vec3 best = null;
      Direction bestFace = null;
      if (minX != null) {
         best = minX;
         bestFace = Direction.WEST;
      }

      if (maxX != null && (best == null || origin.distanceToSqr(maxX) < origin.distanceToSqr(best))) {
         best = maxX;
         bestFace = Direction.EAST;
      }

      if (minY != null && (best == null || origin.distanceToSqr(minY) < origin.distanceToSqr(best))) {
         best = minY;
         bestFace = Direction.DOWN;
      }

      if (maxY != null && (best == null || origin.distanceToSqr(maxY) < origin.distanceToSqr(best))) {
         best = maxY;
         bestFace = Direction.UP;
      }

      if (minZ != null && (best == null || origin.distanceToSqr(minZ) < origin.distanceToSqr(best))) {
         best = minZ;
         bestFace = Direction.NORTH;
      }

      if (maxZ != null && (best == null || origin.distanceToSqr(maxZ) < origin.distanceToSqr(best))) {
         best = maxZ;
         bestFace = Direction.SOUTH;
      }

      return new Pair(best, bestFace);
   }

   @Contract("_, null -> false")
   public static boolean isVecInYZ(@NotNull AABB self, @Nullable Vec3 vec) {
      return vec != null && vec.y >= self.minY && vec.y <= self.maxY && vec.z >= self.minZ && vec.z <= self.maxZ;
   }

   @Contract("_, null -> false")
   public static boolean isVecInXZ(@NotNull AABB self, @Nullable Vec3 vec) {
      return vec != null && vec.x >= self.minX && vec.x <= self.maxX && vec.z >= self.minZ && vec.z <= self.maxZ;
   }

   @Contract("_, null -> false")
   public static boolean isVecInXY(@NotNull AABB self, @Nullable Vec3 vec) {
      return vec != null && vec.x >= self.minX && vec.x <= self.maxX && vec.y >= self.minY && vec.y <= self.maxY;
   }

   @Nullable
   public static Vec3 getIntermediateWithXValue(@NotNull Vec3 self, @NotNull Vec3 other, double x) {
      double deltaX = other.x - self.x;
      double deltaY = other.y - self.y;
      double deltaZ = other.z - self.z;
      if (deltaX * deltaX < 1.0E-7F) {
         return null;
      }

      double d3 = (x - self.x) / deltaX;
      return d3 >= 0.0 && d3 <= 1.0 ? self.add(deltaX * d3, deltaY * d3, deltaZ * d3) : null;
   }

   @Nullable
   public static Vec3 getIntermediateWithYValue(@NotNull Vec3 self, @NotNull Vec3 other, double y) {
      double deltaX = other.x - self.x;
      double deltaY = other.y - self.y;
      double deltaZ = other.z - self.z;
      if (deltaY * deltaY < 1.0E-7F) {
         return null;
      }

      double d3 = (y - self.y) / deltaY;
      return d3 >= 0.0 && d3 <= 1.0 ? self.add(deltaX * d3, deltaY * d3, deltaZ * d3) : null;
   }

   @Nullable
   public static Vec3 getIntermediateWithZValue(@NotNull Vec3 self, @NotNull Vec3 other, double z) {
      double deltaX = other.x - self.x;
      double deltaY = other.y - self.y;
      double deltaZ = other.z - self.z;
      if (deltaZ * deltaZ < 1.0E-7F) {
         return null;
      }

      double d3 = (z - self.z) / deltaZ;
      return d3 >= 0.0 && d3 <= 1.0 ? self.add(deltaX * d3, deltaY * d3, deltaZ * d3) : null;
   }

   public CalculateInterceptUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
