package ess.ll.client.features.crystalv2;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class DidRayTraceHitUtil implements V2Access {
   public static boolean didRayTraceHit(BlockPos pos, Direction face) {
      AABB box = new AABB(pos);
      Rotation rot = CalculateFovUtil.calculate(IsInWebUtil.getNearestFacePoint(pos, face, mc.player.getEyePosition(1.0F)));
      List<Vector3f> possibleLookDirs = new ArrayList<>(
         Arrays.asList(new Vector3f(rot.getYaw(), rot.getPitch(), 0.0F), new Vector3f(pc.LastYaw, pc.LastPitch, 0.0F))
      );
      double[] possibleEyeHeights = IsInWebUtil.getPossibleEyeHeights();
      double minEyeHeight = Double.MAX_VALUE;
      double maxEyeHeight = Double.MIN_VALUE;

      for (double height : possibleEyeHeights) {
         minEyeHeight = Math.min(minEyeHeight, height);
         maxEyeHeight = Math.max(maxEyeHeight, height);
      }

      AABB eyePositions = new AABB(
         mc.player.getX(), mc.player.getY() + minEyeHeight, mc.player.getZ(), mc.player.getX(), mc.player.getY() + maxEyeHeight, mc.player.getZ()
      );
      eyePositions.expandTowards(JumpYMotionUtil.getMovementThreshold(), JumpYMotionUtil.getMovementThreshold(), JumpYMotionUtil.getMovementThreshold());
      if (eyePositions.intersects(box)) {
         return true;
      }

      if (pc.viaVersionProtocol() >= 107) {
         possibleLookDirs.add(new Vector3f(pc.LastYaw, pc.LastPitch, 0.0F));
      }

      if (pc.viaVersionProtocol() < 47) {
         possibleLookDirs = Collections.singletonList(new Vector3f(mc.player.getYRot(), mc.player.getXRot(), 0.0F));
      }

      double distance = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

      for (double d : possibleEyeHeights) {
         for (Vector3f lookDir : possibleLookDirs) {
            Vec3 starting = new Vec3(mc.player.getX(), mc.player.getY() + d, mc.player.getZ());
            Trace trace = new Trace(mc.player, starting.x, starting.y, starting.z, lookDir.x, lookDir.y);
            Pair<Vec3, Direction> intercept = CalculateInterceptUtil.calculateIntercept(box, trace.origin(), trace.getPointAtDistance(distance));
            if (intercept.getFirst() != null) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean didRayTraceHit(BlockPos pos, Direction face, Rotation rotation) {
      AABB box = new AABB(pos);
      Rotation rot = CalculateFovUtil.calculate(IsInWebUtil.getNearestFacePoint(pos, face, mc.player.getEyePosition(1.0F)));
      List<Vector3f> possibleLookDirs = new ArrayList<>(
         Arrays.asList(new Vector3f(rot.getYaw(), rot.getPitch(), 0.0F), new Vector3f(pc.LastYaw, pc.LastPitch, 0.0F))
      );
      double[] possibleEyeHeights = IsInWebUtil.getPossibleEyeHeights();
      double minEyeHeight = Double.MAX_VALUE;
      double maxEyeHeight = Double.MIN_VALUE;

      for (double height : possibleEyeHeights) {
         minEyeHeight = Math.min(minEyeHeight, height);
         maxEyeHeight = Math.max(maxEyeHeight, height);
      }

      AABB eyePositions = new AABB(
         mc.player.getX(), mc.player.getY() + minEyeHeight, mc.player.getZ(), mc.player.getX(), mc.player.getY() + maxEyeHeight, mc.player.getZ()
      );
      eyePositions.expandTowards(JumpYMotionUtil.getMovementThreshold(), JumpYMotionUtil.getMovementThreshold(), JumpYMotionUtil.getMovementThreshold());
      if (eyePositions.intersects(box)) {
         return true;
      }

      if (pc.viaVersionProtocol() >= 107) {
         possibleLookDirs.add(new Vector3f(pc.LastYaw, pc.LastPitch, 0.0F));
      }

      if (pc.viaVersionProtocol() < 47) {
         possibleLookDirs = Collections.singletonList(new Vector3f(mc.player.getYRot(), mc.player.getXRot(), 0.0F));
      }

      double distance = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

      for (double d : possibleEyeHeights) {
         for (Vector3f lookDir : possibleLookDirs) {
            Vec3 starting = new Vec3(mc.player.getX(), mc.player.getY() + d, mc.player.getZ());
            Trace trace = new Trace(mc.player, starting.x, starting.y, starting.z, lookDir.x, lookDir.y);
            Pair<Vec3, Direction> intercept = CalculateInterceptUtil.calculateIntercept(box, trace.origin(), trace.getPointAtDistance(distance));
            if (intercept.getFirst() != null) {
               return true;
            }
         }
      }

      return false;
   }

   public static BlockHitResult raycast(double range, Rotation angle, boolean includeFluids) {
      return raycast(Objects.requireNonNull(mc.player).getEyePosition(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3 vec, double range, Rotation angle, boolean includeFluids) {
      net.minecraft.world.entity.Entity entity = mc.player;
      if (entity == null) {
         return null;
      }

      Vec3 rotationVec = angle.toVector();
      Vec3 end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
      ClientLevel world = mc.level;
      if (world == null) {
         return null;
      }

      net.minecraft.world.level.ClipContext.Fluid fluidMode = includeFluids ? net.minecraft.world.level.ClipContext.Fluid.ANY : net.minecraft.world.level.ClipContext.Fluid.NONE;
      ClipContext context = new ClipContext(vec, end, net.minecraft.world.level.ClipContext.Block.OUTLINE, fluidMode, entity);
      return world.clip(context);
   }

   public static HitResult raycastLikeMinecraft(Rotation rotation, double distance) {
      return raycastLikeMinecraft(rotation, distance, false);
   }

   public static HitResult raycastLikeMinecraft(Rotation rotation, double distance, boolean includeFluids) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 viewVec = rotation.toVector();
         double range = distance;
         double maxDistSq = range * range;
         Vec3 blockEnd = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
         Fluid fluid = includeFluids ? Fluid.ANY : Fluid.NONE;
         BlockHitResult blockHit = mc.level.clip(new ClipContext(eyePos, blockEnd, net.minecraft.world.level.ClipContext.Block.OUTLINE, fluid, mc.player));
         double blockDistSq = blockHit.getLocation().distanceToSqr(eyePos);
         if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            maxDistSq = blockDistSq;
            range = Math.sqrt(maxDistSq);
         }

         Vec3 entityEnd = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
         AABB searchBox = mc.player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0, 1.0, 1.0);
         EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(mc.player, eyePos, entityEnd, searchBox, EntitySelector.CAN_BE_PICKED, maxDistSq);
         return entityHit != null && entityHit.getLocation().distanceToSqr(eyePos) < blockDistSq
            ? filterHitResult(entityHit, eyePos, distance)
            : filterHitResult(blockHit, eyePos, distance);
      } else {
         return null;
      }
   }

   public static HitResult raycastLikeMinecraft(float yaw, float pitch, double distance) {
      return raycastLikeMinecraft(yaw, pitch, distance, false);
   }

   public static HitResult raycastLikeMinecraft(float yaw, float pitch, double distance, boolean includeFluids) {
      return raycastLikeMinecraft(new Rotation(yaw, pitch), distance, includeFluids);
   }

   public static HitResult filterHitResult(HitResult hit, Vec3 eyePos, double distance) {
      Vec3 loc = hit.getLocation();
      if (!loc.closerThan(eyePos, distance)) {
         Direction dir = Direction.getApproximateNearest(loc.x - eyePos.x, loc.y - eyePos.y, loc.z - eyePos.z);
         return BlockHitResult.miss(loc, dir, BlockPos.containing(loc));
      } else {
         return hit;
      }
   }

   public static BlockHitResult raycast(Vec3 start, Vec3 end, net.minecraft.world.level.ClipContext.Block Block) {
      return raycast(start, end, Block, mc.player);
   }

   public static BlockHitResult raycast(Vec3 start, Vec3 end, net.minecraft.world.level.ClipContext.Block Block, net.minecraft.world.entity.Entity entity) {
      return mc.level.clip(new ClipContext(start, end, Block, Fluid.NONE, entity));
   }

   public static boolean rayTrace(Vec3 clientVec, double range, AABB box) {
      Vec3 cameraVec = Objects.requireNonNull(mc.player).getEyePosition(1.0F);
      return box.contains(cameraVec) || box.clip(cameraVec, cameraVec.add(clientVec.multiply(range, range, range))).isPresent();
   }

   public static boolean rayTrace(Rotation clientVec, double range, AABB box) {
      Vec3 cameraVec = Objects.requireNonNull(mc.player).getEyePosition(1.0F);
      return box.contains(cameraVec) || box.clip(cameraVec, cameraVec.add(clientVec.toVector().multiply(range, range, range))).isPresent();
   }

   public static boolean rayTrace(Vec3 pos, Rotation clientVec, double range, AABB box) {
      return box.contains(pos) || box.clip(pos, pos.add(clientVec.toVector().multiply(range, range, range))).isPresent();
   }

   public static boolean rayTrace(Vec3 clientVec, double range, net.minecraft.world.entity.Entity entity, int predictTicks) {
      if (entity == null) {
         return false;
      }

      Vec3 cameraVec = Objects.requireNonNull(mc.player).getEyePosition(1.0F);
      Vec3 endVec = cameraVec.add(clientVec.multiply(range, range, range));
      return rayTraceEntityBox(cameraVec, endVec, entity, predictTicks);
   }

   public static boolean rayTrace(Rotation clientVec, double range, net.minecraft.world.entity.Entity entity, int predictTicks) {
      if (entity == null) {
         return false;
      }

      Vec3 cameraVec = Objects.requireNonNull(mc.player).getEyePosition(1.0F);
      Vec3 endVec = cameraVec.add(clientVec.toVector().multiply(range, range, range));
      return rayTraceEntityBox(cameraVec, endVec, entity, predictTicks);
   }

   public static ToStringImpl34 rayTraceBlock(Player player, double distance, net.minecraft.world.level.block.Block block) {
      Vec3 start = player.getEyePosition(1.0F);
      Vec3 direction = mc.player.getLookAngle();
      Vec3 end = start.add(direction.multiply(distance, distance, distance));
      BlockHitResult hit = player.level().clip(new ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.OUTLINE, Fluid.NONE, player));
      return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && mc.level.getBlockState(hit.getBlockPos()).is(block)
         ? new ToStringImpl34(hit.getBlockPos())
         : null;
   }

   public static BlockHitResult raycastBlock(BlockPos targetPos, double maxDistance) {
      return mc.player != null && mc.level != null ? raycastBlock(mc.player, targetPos, maxDistance) : null;
   }

   public static BlockHitResult raycastBlock(Rotation rotation, BlockPos targetPos, double maxDistance) {
      return mc.player != null && mc.level != null ? raycastBlock(rotation, mc.player, targetPos, maxDistance) : null;
   }

   public static Vec3 lookToBlock(Rotation rotation, BlockPos targetPos, double distance) {
      Vec3 eyePos = Objects.requireNonNull(mc.player).getEyePosition(1.0F);
      Vec3 endPos = eyePos.add(rotation.toVector().scale(distance));
      return new AABB(targetPos).clip(eyePos, endPos).orElse(endPos);
   }

   public static BlockHitResult raycastBlock(Player player, BlockPos targetPos, double maxDistance) {
      Vec3 eyePos = player.getEyePosition(1.0F);
      Vec3 lookVec = player.getLookAngle();
      Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
      AABB blockBox = new AABB(targetPos);
      Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
      if (hitOpt.isEmpty()) {
         return null;
      }

      Vec3 hitVec = hitOpt.get();
      Direction side = getHitSide(hitVec, targetPos);
      return new BlockHitResult(hitVec, side, targetPos, false);
   }

   public static BlockHitResult raycastBlock(Rotation rotation, Player player, BlockPos targetPos, double maxDistance) {
      Vec3 eyePos = player.getEyePosition(1.0F);
      Vec3 lookVec = player.calculateViewVector(rotation.getPitch(), rotation.getYaw());
      Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
      AABB blockBox = new AABB(targetPos);
      Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
      if (hitOpt.isEmpty()) {
         return null;
      }

      Vec3 hitVec = hitOpt.get();
      Direction side = getHitSide(hitVec, targetPos);
      return new BlockHitResult(hitVec, side, targetPos, false);
   }

   public static BlockHitResult raycastBlock(float yaw, float pitch, BlockPos targetPos, double maxDistance) {
      if (mc.player == null) {
         return null;
      }

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      Vec3 lookVec = getRotationVec(yaw, pitch);
      Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
      AABB blockBox = new AABB(targetPos);
      Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
      if (hitOpt.isEmpty()) {
         return null;
      }

      Vec3 hitVec = hitOpt.get();
      Direction side = getHitSide(hitVec, targetPos);
      return new BlockHitResult(hitVec, side, targetPos, false);
   }

   public static boolean isLookingAt(BlockPos targetPos, double maxDistance) {
      return raycastBlock(targetPos, maxDistance) != null;
   }

   public static boolean canReachBlock(BlockPos blockPos, double maxDistance) {
      if (mc.player == null) {
         return false;
      }

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      AABB blockBox = new AABB(blockPos);
      double distanceSq = distanceToBoxSqr(eyePos, blockBox);
      return distanceSq <= maxDistance * maxDistance;
   }

   public static boolean canReachEntity(net.minecraft.world.entity.Entity entity, double maxDistance) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         AABB entityBox = entity.getBoundingBox();
         double distanceSq = distanceToBoxSqr(eyePos, entityBox);
         return distanceSq <= maxDistance * maxDistance;
      } else {
         return false;
      }
   }

   public static boolean canReachBox(AABB box, double maxDistance) {
      if (mc.player == null) {
         return false;
      }

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      double distanceSq = distanceToBoxSqr(eyePos, box);
      return distanceSq <= maxDistance * maxDistance;
   }

   public static double getDistanceToBlock(BlockPos blockPos) {
      if (mc.player == null) {
         return Double.MAX_VALUE;
      }

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      AABB blockBox = new AABB(blockPos);
      return Math.sqrt(distanceToBoxSqr(eyePos, blockBox));
   }

   public static double getDistanceToEntity(net.minecraft.world.entity.Entity entity) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         AABB entityBox = entity.getBoundingBox();
         return Math.sqrt(distanceToBoxSqr(eyePos, entityBox));
      } else {
         return Double.MAX_VALUE;
      }
   }

   public static boolean isLookingAtEntity(net.minecraft.world.entity.Entity entity, double maxDistance) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 lookVec = mc.player.getLookAngle();
         Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
         return rayTraceEntityBox(eyePos, endPos, entity, 1);
      } else {
         return false;
      }
   }

   public static boolean isLookingAtEntity(Rotation rotation, net.minecraft.world.entity.Entity entity, double maxDistance) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 lookVec = mc.player.calculateViewVector(rotation.getPitch(), rotation.getYaw());
         Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
         return rayTraceEntityBox(eyePos, endPos, entity, 1);
      } else {
         return false;
      }
   }

   public static Vec3 raycastEntity(net.minecraft.world.entity.Entity entity, double maxDistance) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 lookVec = mc.player.getLookAngle();
         Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
         AABB entityBox = entity.getBoundingBox();
         return (Vec3)entityBox.clip(eyePos, endPos).orElse(null);
      } else {
         return null;
      }
   }

   public static Vec3 raycastEntity(Rotation rotation, net.minecraft.world.entity.Entity entity, double maxDistance) {
      if (mc.player != null && entity != null) {
         Vec3 eyePos = mc.player.getEyePosition(1.0F);
         Vec3 lookVec = mc.player.calculateViewVector(rotation.getPitch(), rotation.getYaw());
         Vec3 endPos = eyePos.add(lookVec.multiply(maxDistance, maxDistance, maxDistance));
         AABB entityBox = entity.getBoundingBox();
         return (Vec3)entityBox.clip(eyePos, endPos).orElse(null);
      } else {
         return null;
      }
   }

   public static boolean rayTraceEntityBox(Vec3 eyePos, Vec3 endPos, net.minecraft.world.entity.Entity entity, int predictTicks) {
      AABB box = entity.getBoundingBox();
      return box.contains(eyePos) || box.clip(eyePos, endPos).isPresent();
   }

   public static double distanceToBoxSqr(Vec3 point, AABB box) {
      double x = clamp(point.x, box.minX, box.maxX);
      double y = clamp(point.y, box.minY, box.maxY);
      double z = clamp(point.z, box.minZ, box.maxZ);
      double dx = point.x - x;
      double dy = point.y - y;
      double dz = point.z - z;
      return dx * dx + dy * dy + dz * dz;
   }

   public static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public static Direction getHitSide(Vec3 hitVec, BlockPos blockPos) {
      double dx = hitVec.x - (blockPos.getX() + 0.5);
      double dy = hitVec.y - (blockPos.getY() + 0.5);
      double dz = hitVec.z - (blockPos.getZ() + 0.5);
      double absDx = Math.abs(dx);
      double absDy = Math.abs(dy);
      double absDz = Math.abs(dz);
      if (absDx > absDy && absDx > absDz) {
         return dx > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (absDy > absDz) {
         return dy > 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   public static Vec3 getRotationVec(float yaw, float pitch) {
      double radPitch = Math.toRadians(pitch);
      double radYaw = Math.toRadians(yaw);
      double cosP = Math.cos(-radPitch);
      double x = Math.sin(-radYaw - Math.PI) * cosP;
      double y = Math.sin(-radPitch);
      double z = Math.cos(-radYaw - Math.PI) * cosP;
      return new Vec3(x, y, z);
   }

   public DidRayTraceHitUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
