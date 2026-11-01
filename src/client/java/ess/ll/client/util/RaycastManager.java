package ess.ll.client.util;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.RaycastEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class RaycastManager implements MinecraftAccess {
   private static final double EPSILON_UNUSED = 1.0E-4;
   private static final double ZERO_UNUSED = 0;
   private static final double MIN_DIRECTION_SQ = 1.0E-8;
   private static final double INSIDE_UP_OFFSET = 0.51;
   private static final double INSIDE_PUSH = 0.51;
   private static final double SKIP_STEP = 1.0E-4;
   private static final double EXIT_EPSILON = 1.0E-4;
   private static final double POSITIVE_EPSILON = 1.0E-8;
   private static final double NEGATIVE_EPSILON = -1.0E-8;

   @Subscribe
   private void onRaycast(RaycastEvent raycastEvent) {
      RaycastEvent.Type type = raycastEvent.getType();
      if (type == RaycastEvent.Type.ENTITY) {
         this.onEntityRaycast(raycastEvent);
      } else if (type == RaycastEvent.Type.BLOCK) {
         Entity entity = raycastEvent.getEntity();
         if (entity == mc.player) {
            if (raycastEvent.getResult() instanceof BlockHitResult hit) {
               Level level = entity.level();
               if (level != null) {
                  if (!this.isAllowed(level, entity, hit)) {
                     float tickDelta = raycastEvent.getTickDelta();
                     double range = raycastEvent.getRange();
                     Vec3 eye = entity.getEyePosition(tickDelta);
                     Vec3 look = entity.getViewVector(tickDelta);
                     Vec3 end = eye.add(look.scale(range));
                     BlockHitResult result = this.raycastThrough(level, entity, eye, end, look, range, raycastEvent.isIncludeFluids(), hit);
                     raycastEvent.setResult(result);
                     raycastEvent.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   private BlockHitResult raycastThrough(Level level, Entity entity, Vec3 eye, Vec3 end, Vec3 direction, double range, boolean includeFluids, BlockHitResult first) {
      BlockHitResult hit = first;
      Fluid fluid = includeFluids ? Fluid.ANY : Fluid.NONE;
      double rangeSq = range * range;

      for (int i = 0; i < 64; i++) {
         Vec3 start = this.nextStart(direction, hit);
         if (start.distanceToSqr(eye) >= rangeSq) {
            return BlockHitResult.miss(end, hit.getDirection(), hit.getBlockPos());
         }

         hit = level.clip(new ClipContext(start, end, Block.OUTLINE, fluid, entity));
         if (hit.getType() == Type.MISS) {
            return hit;
         }

         if (this.isAllowed(level, entity, hit)) {
            return hit;
         }
      }

      return BlockHitResult.miss(end, hit.getDirection(), net.minecraft.core.BlockPos.containing(end));
   }

   private Vec3 exitPoint(Vec3 origin, Vec3 direction, BlockPos pos) {
      double x = pos.getX();
      double y = pos.getY();
      double z = pos.getZ();
      double maxX = x + 1.0;
      double maxY = y + 1.0;
      double maxZ = z + 1.0;
      double t = Double.POSITIVE_INFINITY;
      t = this.slabExit(t, direction.x, x - origin.x, maxX - origin.x);
      t = this.slabExit(t, direction.y, y - origin.y, maxY - origin.y);
      t = this.slabExit(t, direction.z, z - origin.z, maxZ - origin.z);
      return !Double.isFinite(t) ? null : origin.add(direction.scale(t + EXIT_EPSILON));
   }

   private Vec3 nextStart(Vec3 direction, BlockHitResult blockHitResult) {
      if (blockHitResult.isInside()) {
         Vec3 exit = this.exitPoint(blockHitResult.getLocation(), direction, blockHitResult.getBlockPos());
         if (exit != null) {
            return exit;
         }

         Vec3 normal = direction.normalize();
         return normal.lengthSqr() < MIN_DIRECTION_SQ ? blockHitResult.getLocation().add(0.0, INSIDE_UP_OFFSET, 0.0) : Vec3.atCenterOf(blockHitResult.getBlockPos()).add(normal.scale(INSIDE_PUSH));
      } else {
         return blockHitResult.getLocation().add(direction.scale(SKIP_STEP));
      }
   }

   private boolean isAllowed(Level level, Entity entity, BlockHitResult blockHitResult) {
      BlockPos blockPos = blockHitResult.getBlockPos();
      return HitResultEvent.INSTANCE.fireBlock(entity, blockPos, level.getBlockState(blockPos));
   }

   private double slabExit(double current, double direction, double toMin, double toMax) {
      if (direction > POSITIVE_EPSILON) {
         double tMax = toMax / direction;
         if (tMax >= 0.0 && tMax < current) {
            return tMax;
         }
      } else if (direction < NEGATIVE_EPSILON) {
         double tMin = toMin / direction;
         if (tMin >= 0.0 && tMin < current) {
            return tMin;
         }
      }

      return current;
   }

   public RaycastManager() {
      EventBus.register(this);
   }

   private void onEntityRaycast(RaycastEvent raycastEvent) {
      Entity entity = raycastEvent.getResult() instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
      if (HitResultEvent.INSTANCE.fireEntity(entity)) {
         raycastEvent.setResult(null);
         raycastEvent.setCancelled(true);
      }
   }
}
