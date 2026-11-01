package ess.ll.client.core;

import ess.ll.client.integration.MinecraftAccess;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CollisionBlockView implements BlockGetter, MinecraftAccess {
   public static final CollisionBlockView INSTANCE = new CollisionBlockView();
   private static final int ZERO_INT = 0;
   private static final double ZERO_A = 0;
   private static final double ZERO_B = 0;
   private final long[] stateKeys = new long[8192];
   private final BlockState[] states = new BlockState[8192];
   private int cachedTick;
   private Object cachedLevel;
   private final long[] shapeKeys;
   private final VoxelShape[] shapes;
   private final int[] shapeGenerations;
   private CollisionContext shapeContext;
   private int generation;
   private Vec3 rayStart;
   private Vec3 rayEnd;
   private CollisionContext rayContext;
   private boolean ignoreInside;
   private static final int NO_TICK = Integer.MIN_VALUE;
   private static final double MIN_RAY_LENGTH_SQ = 1.0E-7;
   private static final double PROBE_X = 0.001;
   private static final double PROBE_Y = 0.001;
   private static final double PROBE_Z = 0.001;
   private static final double POSITIVE_X_EPSILON = 1.0E-7;
   private static final double NEGATIVE_X_EPSILON = -1.0E-7;
   private static final double POSITIVE_Y_EPSILON = 1.0E-7;
   private static final double NEGATIVE_Y_EPSILON = -1.0E-7;
   private static final double POSITIVE_Z_EPSILON = 1.0E-7;
   private static final double NEGATIVE_Z_EPSILON = -1.0E-7;
   private static final double UNIT_MIN = -1.0;
   private static final double FACE_MIN_U_EPSILON = 1.0E-7;
   private static final double FACE_MAX_U_EPSILON = 1.0E-7;
   private static final double FACE_MIN_V_EPSILON = 1.0E-7;
   private static final double FACE_MAX_V_EPSILON = 1.0E-7;
   private static final long SHAPE_HASH_SALT = -7046029254386353131L;
   private static final int NO_PLAYER_TICK = Integer.MIN_VALUE;

   public void invalidate() {
      Arrays.fill(this.states, null);
      this.generation++;
   }

   @Override
   public int getHeight() {
      return mc.level == null ? 0 : mc.level.getHeight();
   }

   public BlockPos rayBlock(Vec3 vec, Vec3 vec2, CollisionContext collisionContext, boolean ignoreInside) {
      if (mc.level == null) {
         return null;
      }

      this.refresh();
      if (this.shapeContext != collisionContext) {
         this.shapeContext = collisionContext;
         this.generation++;
      }

      this.rayStart = vec;
      this.rayEnd = vec2;
      this.rayContext = collisionContext;
      this.ignoreInside = ignoreInside;
      return BlockGetter.traverseBlocks(vec, vec2, this, CollisionBlockView::firstSolid, context -> null);
   }

   @Override
   public int getMinY() {
      return mc.level == null ? 0 : mc.level.getMinY();
   }

   private static boolean insideUnit(double value) {
      return Mth.floor(Mth.clamp(value, UNIT_MIN, 1.0)) == 0;
   }

   public BlockPos rayBlock(Vec3 vec, Vec3 vec2, CollisionContext collisionContext) {
      return this.rayBlock(vec, vec2, collisionContext, false);
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      if (mc.level == null) {
         return Blocks.AIR.defaultBlockState();
      }

      this.refresh();
      long key = pos.asLong();
      int slot = (int)HashCommon.mix(key) & 8191;
      BlockState blockState = this.states[slot];
      if (blockState != null && this.stateKeys[slot] == key) {
         return blockState;
      }

      BlockState fetched = mc.level.getBlockState(pos);
      this.stateKeys[slot] = key;
      this.states[slot] = fetched;
      return fetched;
   }

   private void refresh() {
      int tick = mc.player == null ? NO_PLAYER_TICK : mc.player.tickCount;
      if (tick != this.cachedTick || this.cachedLevel != mc.level) {
         this.cachedTick = tick;
         this.cachedLevel = mc.level;
         this.invalidate();
      }
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   private CollisionBlockView() {
      this.cachedTick = NO_TICK;
      this.shapeKeys = new long[8192];
      this.shapes = new VoxelShape[8192];
      this.shapeGenerations = new int[8192];
   }

   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      return mc.level == null ? null : mc.level.getBlockEntity(pos);
   }

   private static BlockPos firstSolid(CollisionBlockView collisionBlockView, BlockPos pos) {
      VoxelShape voxelShape = collisionBlockView.collisionShape(pos);
      if (voxelShape.isEmpty()) {
         return null;
      } else if (voxelShape == Shapes.block()) {
         return collisionBlockView.hitsCube(pos) ? pos.immutable() : null;
      } else {
         BlockHitResult blockHitResult = voxelShape.clip(collisionBlockView.rayStart, collisionBlockView.rayEnd, pos);
         if (blockHitResult == null) {
            return null;
         } else {
            return collisionBlockView.ignoreInside && blockHitResult.isInside() ? null : pos.immutable();
         }
      }
   }

   private boolean hitsCube(BlockPos pos) {
      Vec3 vec = this.rayStart;
      double dx = this.rayEnd.x - vec.x;
      double dy = this.rayEnd.y - vec.y;
      double dz = this.rayEnd.z - vec.z;
      if (dx * dx + dy * dy + dz * dz < MIN_RAY_LENGTH_SQ) {
         return false;
      }

      double x = pos.getX();
      double y = pos.getY();
      double z = pos.getZ();
      if (insideUnit(vec.x + dx * PROBE_X - x) && insideUnit(vec.y + dy * PROBE_Y - y) && insideUnit(vec.z + dz * PROBE_Z - z)) {
         return !this.ignoreInside;
      }

      double maxX = x + 1.0;
      double maxY = y + 1.0;
      double maxZ = z + 1.0;
      if (dx > POSITIVE_X_EPSILON
         ? !crossesFace(x, vec.x, dx, vec.y, dy, y, maxY, vec.z, dz, z, maxZ)
         : !(dx < NEGATIVE_X_EPSILON) || !crossesFace(maxX, vec.x, dx, vec.y, dy, y, maxY, vec.z, dz, z, maxZ)) {
         if (dy > POSITIVE_Y_EPSILON
            ? !crossesFace(y, vec.y, dy, vec.z, dz, z, maxZ, vec.x, dx, x, maxX)
            : !(dy < NEGATIVE_Y_EPSILON) || !crossesFace(maxY, vec.y, dy, vec.z, dz, z, maxZ, vec.x, dx, x, maxX)) {
            return dz > POSITIVE_Z_EPSILON
               ? crossesFace(z, vec.z, dz, vec.x, dx, x, maxX, vec.y, dy, y, maxY)
               : dz < NEGATIVE_Z_EPSILON && crossesFace(maxZ, vec.z, dz, vec.x, dx, x, maxX, vec.y, dy, y, maxY);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private VoxelShape collisionShape(BlockPos pos) {
      long key = pos.asLong();
      int slot = (int)HashCommon.mix(key ^ SHAPE_HASH_SALT) & 8191;
      if (this.shapeGenerations[slot] == this.generation && this.shapeKeys[slot] == key && this.shapes[slot] != null) {
         return this.shapes[slot];
      }

      VoxelShape shape = this.getBlockState(pos).getCollisionShape(this, pos, this.rayContext);
      this.shapeKeys[slot] = key;
      this.shapes[slot] = shape;
      this.shapeGenerations[slot] = this.generation;
      return shape;
   }

   private static boolean crossesFace(
      double plane, double start, double delta, double uStart, double uDelta, double uMin, double uMax, double vStart, double vDelta, double vMin, double vMax
   ) {
      double t = (plane - start) / delta;
      double u = uStart + t * uDelta;
      double v = vStart + t * vDelta;
      return 0.0 < t && t < 1.0 && uMin - FACE_MIN_U_EPSILON < u && u < uMax + FACE_MAX_U_EPSILON && vMin - FACE_MIN_V_EPSILON < v && v < vMax + FACE_MAX_V_EPSILON;
   }
}
