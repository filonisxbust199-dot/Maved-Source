package ess.ll.client.util;

import ess.ll.client.integration.MinecraftAccess;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RaycastUtils implements MinecraftAccess {
   private static final float DEG_TO_RAD_YAW = (float) (Math.PI / 180.0);
   private static final float PI = (float) Math.PI;
   private static final float DEG_TO_RAD_PITCH = (float) (Math.PI / 180.0);
   private static final double REACH_X = 6.0;
   private static final double REACH_Y = 6.0;
   private static final double REACH_Z = 6.0;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static boolean canSeeBlock(BlockPos pos) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 center = Vec3.atCenterOf(pos);
         Vec3 look = mc.player.getViewVector(1.0F);
         double distance = eyePosition.distanceTo(center) + 1.0;
         Vec3 end = eyePosition.add(look.scale(distance));
         VoxelShape shape = mc.level.getBlockState(pos).getCollisionShape(mc.level, pos);
         if (shape.isEmpty()) {
            return false;
         }

         var clipped = shape.bounds().move(pos).clip(eyePosition, end);
         if (clipped.isEmpty()) {
            return false;
         }

         BlockHitResult hit = clipBlocks(eyePosition, clipped.get());
         return hit.getType() == Type.BLOCK && hit.getBlockPos().equals(pos);
      } else {
         return false;
      }
   }

   public static HitResult clip(Vec3 from, Vec3 to, Block block, Fluid fluid) {
      return mc.level == null ? null : mc.level.clip(new ClipContext(from, to, block, fluid, mc.player));
   }

   public static boolean isLookingAt(Vec3 eye, float yaw, float pitch, double range, boolean checkWalls, AABB aabb) {
      if (mc.level == null) {
         return false;
      } else {
         Vec3 end = eye.add(directionVector(pitch, yaw).scale(range));
         if (aabb.contains(eye)) {
            return true;
         } else {
            var clipped = aabb.clip(eye, end);
            if (clipped.isEmpty()) {
               return false;
            } else if (!checkWalls) {
               return true;
            } else {
               BlockHitResult blockHitResult = clipBlocks(eye, (Vec3)clipped.get());
               if (blockHitResult.getType() == Type.BLOCK) {
                  double blockDistance = eye.distanceTo(blockHitResult.getLocation());
                  double boxDistance = eye.distanceTo((Vec3)clipped.get());
                  return !(blockDistance < boxDistance);
               } else {
                  return true;
               }
            }
         }
      }
   }

   public static boolean isLookingAt(float yaw, float pitch, double range, Entity entity, boolean checkWalls, AABB aabb) {
      return mc.player == null ? false : isLookingAt(mc.player.getEyePosition(), yaw, pitch, range, checkWalls, aabb);
   }

   public static boolean isLookingAtBlock(BlockPos pos, double range) {
      float yaw = RotationManager.getServerYaw();
      return Float.isNaN(yaw) || isLookingAtBlock(pos, yaw, RotationManager.getServerPitch(), range);
   }

   public static boolean isLookingAt(float yaw, float pitch, double range, Entity entity) {
      return isLookingAt(yaw, pitch, range, entity, false);
   }

   private RaycastUtils() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static boolean isLookingAtBlock(BlockPos pos, float yaw, float pitch, double range) {
      if (mc.player == null) {
         return false;
      }

      Vec3 eyePosition = mc.player.getEyePosition();
      Vec3 end = eyePosition.add(directionVector(pitch, yaw).scale(range));
      AABB aabb = new AABB(pos);
      return aabb.contains(eyePosition) || aabb.clip(eyePosition, end).isPresent();
   }

   public static Entity entityInView(float yaw, float pitch, float range, boolean stopAtBlocks) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 end = eyePosition.add(directionVector(pitch, yaw).scale(range));
         if (stopAtBlocks) {
            BlockHitResult blockHitResult = clipBlocks(eyePosition, end);
            if (blockHitResult.getType() != Type.MISS) {
               end = blockHitResult.getLocation();
            }
         }

         Entity closest = null;
         double closestDistance = range;

         for (Entity candidate : mc.level.entitiesForRendering()) {
            if (candidate != mc.player && candidate.isAlive()) {
               var clipped = candidate.getBoundingBox().clip(eyePosition, end);
               if (clipped.isPresent()) {
                  double distance = eyePosition.distanceTo((Vec3)clipped.get());
                  if (distance < closestDistance) {
                     closestDistance = distance;
                     closest = candidate;
                  }
               }
            }
         }

         return closest;
      } else {
         return null;
      }
   }

   public static boolean isLookingAt(float yaw, float pitch, double range, Entity entity, boolean checkWalls) {
      return isLookingAt(yaw, pitch, range, entity, checkWalls, entity.getBoundingBox());
   }

   public static boolean isLookingAtBlock(net.minecraft.world.level.block.Block block) {
      if (mc.player != null && mc.level != null) {
         BlockHitResult blockHitResult = clipBlocks(mc.player.getEyePosition(), reachEnd());
         return blockHitResult.getType() == Type.BLOCK ? mc.level.getBlockState(blockHitResult.getBlockPos()).is(block) : false;
      } else {
         return false;
      }
   }

   private static BlockHitResult clipBlocks(Vec3 from, Vec3 to) {
      return mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player));
   }

   public static Vec3 cameraHit(float yaw, float pitch, double range) {
      if (mc.level == null) {
         return null;
      }

      Vec3 camera = mc.gameRenderer.mainCamera().position();
      Vec3 end = camera.add(directionVector(pitch, yaw).scale(range));
      BlockHitResult blockHitResult = clipBlocks(camera, end);
      return blockHitResult.getType() == Type.BLOCK ? blockHitResult.getLocation() : null;
   }

   public static Vec3 directionVector(float pitch, float yaw) {
      float yawRad = -yaw * DEG_TO_RAD_YAW - PI;
      float pitchRad = -pitch * DEG_TO_RAD_PITCH;
      float cosYaw = Mth.cos(yawRad);
      float sinYaw = Mth.sin(yawRad);
      float negCosPitch = -Mth.cos(pitchRad);
      float sinPitch = Mth.sin(pitchRad);
      return new Vec3(sinYaw * negCosPitch, sinPitch, cosYaw * negCosPitch);
   }

   private static Vec3 reachEnd() {
      Vec3 viewVector = mc.player.getViewVector(1.0F);
      return mc.player.getEyePosition().add(viewVector.x * REACH_X, viewVector.y * REACH_Y, viewVector.z * REACH_Z);
   }

   public static BlockHitResult lookedAtBlock(net.minecraft.world.level.block.Block... blocks) {
      if (mc.player != null && mc.level != null) {
         BlockHitResult blockHitResult = clipBlocks(mc.player.getEyePosition(), reachEnd());
         if (blockHitResult.getType() == Type.BLOCK) {
            BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());

            for (net.minecraft.world.level.block.Block block : blocks) {
               if (blockState.is(block)) {
                  return blockHitResult;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
