package ess.ll.client.core;

import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.RaycastUtils;
import ess.ll.client.util.Rotation;
import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityAimPointSampler implements MinecraftAccess {
   private static final int ZERO_INT = 0;
   private static final double ZERO_A = 0;
   private static final double ZERO_B = 0;
   private final ArrayList<Vec3> points = new ArrayList<>();
   private Vec3 wanderOffset = Vec3.ZERO;
   private Vec3 wanderStep = Vec3.ZERO;
   private Vec3 closestVisiblePoint = null;
   private Vec3 currentPoint = null;
   private boolean currentInRange = false;
   private int currentEntityId = -1;
   private int nextResampleTick;
   private static final int NO_TICK = Integer.MIN_VALUE;
   private static final double MAX_INSET_X = 0.1;
   private static final double INSET_FACTOR_X = 0.25;
   private static final double MAX_INSET_Y = 0.1;
   private static final double INSET_FACTOR_Y = 0.25;
   private static final double MAX_INSET_Z = 0.1;
   private static final double INSET_FACTOR_Z = 0.25;
   private static final double STEP_Z = 0.19;
   private static final double STEP_Y = 0.19;
   private static final double STEP_X = 0.19;
   private static final float MIN_STEP_X = 0.01F;
   private static final float MAX_STEP_X = 0.03F;
   private static final float MIN_STEP_Y = 0.01F;
   private static final float MAX_STEP_Y = 0.03F;
   private static final float INIT_STEP_MIN_X = -0.05F;
   private static final float INIT_STEP_MAX_X = 0.05F;
   private static final float INIT_STEP_MIN_Y = -0.05F;
   private static final float INIT_STEP_MAX_Y = 0.05F;
   private static final float INIT_STEP_MIN_Z = -0.05F;
   private static final float INIT_STEP_MAX_Z = 0.05F;
   private static final double EDGE_MARGIN_X = 0.05;
   private static final double HALF_DIVISOR_X = 2.0;
   private static final double HALF_DIVISOR_Y = 2.0;
   private static final double EDGE_MARGIN_Z = 0.05;
   private static final double HALF_DIVISOR_Z = 2.0;
   private static final double EDGE_MARGIN_NEG_X = 0.05;
   private static final double HALF_DIVISOR_NEG_X = 2.0;
   private static final float MAX_STEP_NEG_X = 0.03F;
   private static final double MIN_OFFSET_Y = 0.1;
   private static final double EDGE_MARGIN_NEG_Z = 0.05;
   private static final double HALF_DIVISOR_NEG_Z = 2.0;
   private static final float JITTER_MIN_X = -0.03F;
   private static final float JITTER_MAX_X = 0.03F;
   private static final float JITTER_MIN_Z = -0.03F;
   private static final float JITTER_MAX_Z = 0.03F;
   private static final double SEARCH_HALF_DIVISOR = 2.0;
   private static final float SEARCH_START_Y = 0.05F;
   private static final float SEARCH_STEP_Y = 0.15F;
   private static final float SEARCH_STEP_Z = 0.05F;
   private static final float SEARCH_STEP_X = 0.05F;

   private Vec3 closestVisible(LivingEntity livingEntity) {
      if (this.points.isEmpty()) {
         return null;
      }

      if (mc.player == null) {
         return null;
      }

      Vec3 eyePosition = mc.player.getEyePosition();
      Vec3 closest = null;
      double closestDistanceSq = Double.POSITIVE_INFINITY;

      for (Vec3 offset : this.points) {
         Vec3 point = offset.add(livingEntity.position());
         if (GuiHitTestUtils.canSee(point)) {
            double distanceSq = point.distanceToSqr(eyePosition);
            if (distanceSq < closestDistanceSq) {
               closestDistanceSq = distanceSq;
               closest = point;
            }
         }
      }

      return closest;
   }

   private void samplePoints(LivingEntity livingEntity, AABB aabb) {
      this.points.clear();
      double insetX = Math.min(MAX_INSET_X, aabb.getXsize() * INSET_FACTOR_X);
      double insetY = Math.min(MAX_INSET_Y, aabb.getYsize() * INSET_FACTOR_Y);
      double insetZ = Math.min(MAX_INSET_Z, aabb.getZsize() * INSET_FACTOR_Z);
      double minX = aabb.minX + insetX;
      double minY = aabb.minY + insetY;
      double minZ = aabb.minZ + insetZ;
      double maxX = aabb.maxX - insetX;
      double maxY = aabb.maxY - insetY;
      double maxZ = aabb.maxZ - insetZ;
      Vec3 origin = livingEntity.position();

      for (double x = minX; x <= maxX; x += STEP_X) {
         for (double y = minY; y <= maxY; y += STEP_Y) {
            for (double z = minZ; z <= maxZ; z += STEP_Z) {
               this.points.add(new Vec3(x, y, z).subtract(origin));
            }
         }
      }
   }

   public EntityAimPointSampler() {
      this.nextResampleTick = NO_TICK;
   }

   private Vec3 pickPoint(Vec3 base, Vec3 eye, double range) {
      double rangeSq = range * range;
      Vec3 picked = null;
      Vec3 closest = null;
      double closestDistanceSq = Double.POSITIVE_INFINITY;
      int inRangeCount = 0;

      for (Vec3 offset : this.points) {
         double dx = base.x + offset.x - eye.x;
         double dy = base.y + offset.y - eye.y;
         double dz = base.z + offset.z - eye.z;
         double distanceSq = dx * dx + dy * dy + dz * dz;
         if (distanceSq < closestDistanceSq) {
            closestDistanceSq = distanceSq;
            closest = offset;
         }

         if (distanceSq <= rangeSq) {
            if (GuiHitTestUtils.RANDOM.nextInt(++inRangeCount) == 0) {
               picked = offset;
            }
         }
      }

      this.currentInRange = picked != null;
      return picked != null ? picked : closest;
   }

   public Vec3 wanderPoint(Entity entity, double range) {
      if (mc.player == null) {
         return entity.position();
      }

      float minStepX = MIN_STEP_X;
      float maxStepX = MAX_STEP_X;
      float minStepY = MIN_STEP_Y;
      float maxStepY = MAX_STEP_Y;
      double xsize = entity.getBoundingBox().getXsize();
      double ysize = entity.getBoundingBox().getYsize();
      double zsize = entity.getBoundingBox().getZsize();
      if (this.wanderStep.equals(Vec3.ZERO)) {
         this.wanderStep = new Vec3(
            GuiHitTestUtils.randomBetween(INIT_STEP_MIN_X, INIT_STEP_MAX_X),
            GuiHitTestUtils.randomBetween(INIT_STEP_MIN_Y, INIT_STEP_MAX_Y),
            GuiHitTestUtils.randomBetween(INIT_STEP_MIN_Z, INIT_STEP_MAX_Z)
         );
      }

      this.wanderOffset = this.wanderOffset.add(this.wanderStep);
      if (this.wanderOffset.x >= (xsize - EDGE_MARGIN_X) / HALF_DIVISOR_X) {
         this.wanderStep = new Vec3(-GuiHitTestUtils.randomBetween(minStepX, maxStepX), this.wanderStep.y(), this.wanderStep.z());
      }

      if (this.wanderOffset.y >= ysize / HALF_DIVISOR_Y) {
         this.wanderStep = new Vec3(this.wanderStep.x(), -GuiHitTestUtils.randomBetween(minStepY, maxStepY), this.wanderStep.z());
      }

      if (this.wanderOffset.z >= (zsize - EDGE_MARGIN_Z) / HALF_DIVISOR_Z) {
         this.wanderStep = new Vec3(this.wanderStep.x(), this.wanderStep.y(), -GuiHitTestUtils.randomBetween(minStepX, maxStepX));
      }

      if (this.wanderOffset.x <= -(xsize - EDGE_MARGIN_NEG_X) / HALF_DIVISOR_NEG_X) {
         this.wanderStep = new Vec3(GuiHitTestUtils.randomBetween(minStepX, MAX_STEP_NEG_X), this.wanderStep.y(), this.wanderStep.z());
      }

      if (this.wanderOffset.y <= MIN_OFFSET_Y) {
         this.wanderStep = new Vec3(this.wanderStep.x(), GuiHitTestUtils.randomBetween(minStepY, maxStepY), this.wanderStep.z());
      }

      if (this.wanderOffset.z <= -(zsize - EDGE_MARGIN_NEG_Z) / HALF_DIVISOR_NEG_Z) {
         this.wanderStep = new Vec3(this.wanderStep.x(), this.wanderStep.y(), GuiHitTestUtils.randomBetween(minStepX, maxStepX));
      }

      this.wanderOffset.add(GuiHitTestUtils.randomBetween(JITTER_MIN_X, JITTER_MAX_X), 0.0, GuiHitTestUtils.randomBetween(JITTER_MIN_Z, JITTER_MAX_Z));
      if (!this.isLookingAt(mc.player.getLookAngle(), range, entity.getBoundingBox())) {
         float halfWidth = (float)(xsize / SEARCH_HALF_DIVISOR);

         for (float dx = -halfWidth; dx <= halfWidth; dx += SEARCH_STEP_X) {
            for (float dz = -halfWidth; dz <= halfWidth; dz += SEARCH_STEP_Z) {
               for (float dy = SEARCH_START_Y; dy <= entity.getBoundingBox().getYsize(); dy += SEARCH_STEP_Y) {
                  Vec3 point = new Vec3(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);
                  Rotation rotation = Rotation.fromDirection(point);
                  Vec3 look = RaycastUtils.directionVector(rotation.getPitch(), rotation.getYaw());
                  if (this.isLookingAt(look, range, entity.getBoundingBox())) {
                     this.wanderOffset = new Vec3(dx, dy, dz);
                     break;
                  }
               }
            }
         }
      }

      return entity.position().add(this.wanderOffset);
   }

   public Vec3 getClosestVisiblePoint() {
      return this.closestVisiblePoint;
   }

   private boolean isStillValid(Vec3 point, AABB aabb, Vec3 eye, double range) {
      return !aabb.contains(point) ? false : !this.currentInRange || point.distanceTo(eye) <= range;
   }

   public void update(LivingEntity livingEntity) {
      if (livingEntity == null) {
         this.points.clear();
         this.closestVisiblePoint = null;
         this.currentPoint = null;
      } else {
         this.samplePoints(livingEntity, livingEntity.getBoundingBox());
         this.closestVisiblePoint = this.closestVisible(livingEntity);
      }
   }

   public ArrayList<Vec3> getPoints() {
      return this.points;
   }

   public Vec3 aimPoint(LivingEntity livingEntity, Vec3 eye, double range) {
      if (!this.points.isEmpty() && mc.player != null) {
         Vec3 origin = livingEntity.position();
         AABB boundingBox = livingEntity.getBoundingBox();
         int tick = mc.player.tickCount;
         if (this.currentPoint == null
            || livingEntity.getId() != this.currentEntityId
            || tick >= this.nextResampleTick
            || !this.isStillValid(this.currentPoint.add(origin), boundingBox, eye, range)) {
            this.currentPoint = this.pickPoint(origin, eye, range);
            this.currentEntityId = livingEntity.getId();
            this.nextResampleTick = tick + 10 + GuiHitTestUtils.RANDOM.nextInt(11);
         }

         return this.currentPoint == null ? null : this.currentPoint.add(origin);
      } else {
         return null;
      }
   }

   private boolean isLookingAt(Vec3 look, double range, AABB aabb) {
      if (mc.player == null) {
         return false;
      }

      Vec3 eyePosition = mc.player.getEyePosition();
      return aabb.contains(eyePosition) || aabb.clip(eyePosition, eyePosition.add(look.scale(range))).isPresent();
   }
}
