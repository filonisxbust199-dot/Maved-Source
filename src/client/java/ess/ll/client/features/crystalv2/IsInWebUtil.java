package ess.ll.client.features.crystalv2;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;

public final class IsInWebUtil implements V2Access {
   public static final double FACE_INSET = 0.001;
   public static final double[][] possibleEyeHeights = new double[3][];

   public static boolean isInWeb() {
      AABB box = mc.player.getBoundingBox().inflate(1.0E-6);
      int minX = Mth.floor(box.minX);
      int minY = Mth.floor(box.minY);
      int minZ = Mth.floor(box.minZ);
      int maxX = Mth.floor(box.maxX);
      int maxY = Mth.floor(box.maxY);
      int maxZ = Mth.floor(box.maxZ);

      for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
         if (isWeb(pos)) {
            return true;
         }
      }

      return false;
   }

   public static boolean isBlockedByEntitiesForCrystal(BlockPos placePos) {
      for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
         if (!(entity instanceof ItemEntity) && intersectsEntityBoxPlace(placePos, entity)) {
            return true;
         }
      }

      return false;
   }

   public static Pair<Double, Direction> getBestVisibleFaceAntiCrystal(BlockPos pos, Player entity, double minDamage, boolean nearestPoint) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestSelfDamage = 0.0;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (face == Direction.UP) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            if (!(min > maxReach * maxReach)) {
               BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
               if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
                  BlockPos placePos = pos.relative(face);
                  if (mc.level.getBlockState(placePos).isAir() && !isBlockedByEntitiesForCrystal(placePos)) {
                     double selfDamage = GetSelfExplosionDamageUtil.getExplosionDamage(Vec3.atBottomCenterOf(pos.above()), mc.player, true);
                     if (!(selfDamage < bestSelfDamage) && !(selfDamage < minDamage)) {
                        double selfDamage2 = GetSelfExplosionDamageUtil.getDamageOfGhostBlock(Vec3.atBottomCenterOf(placePos), mc.player, placePos, true);
                        if (!(selfDamage2 > minDamage)) {
                           bestSelfDamage = selfDamage;
                           bestFace = face;
                        }
                     }
                  }
               }
            }
         }
      }

      return new Pair(bestSelfDamage, bestFace);
   }

   public static Pair<Rotation, Direction> getBestVisibleFace(BlockPos pos, Player entity, boolean nearestPoint) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestDist = Double.MAX_VALUE;
      Rotation bestRotation = null;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               double dist = eye.distanceToSqr(point);
               if (dist < bestDist) {
                  bestDist = dist;
                  bestFace = face;
                  bestRotation = rot;
               }
            }
         }
      }

      return new Pair(bestRotation, bestFace);
   }

   public static Pair<FaceDefensive, FaceDefensive> getBestVisibleFaceDefensive(
      BlockPos pos, Player entity, Player target, Simulated simulatedPlayer, double minDamage, double maxSelfDamage, boolean nearestPoint
   ) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestSelfDamage = Double.MAX_VALUE;
      Rotation bestRotation = null;
      Direction includeFace = null;
      double includeSelfDamage = Double.MAX_VALUE;
      Rotation includeRotation = null;
      Simulated sim = pc.simulated;
      Simulated sim2 = pc.simulated2;
      Simulated sim3 = pc.simulated3;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos placePos = pos.relative(face);
               if (mc.level.getBlockState(placePos).isAir()
                  && !blocked(placePos).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim.boundingBox).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim2.boundingBox).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim3.boundingBox).equals(ValuesEnum9.PLAYER)) {
                  double damage = GetExplosionDamageUtil.getDamageIgnoringBlockAndGhostAt(
                     Vec3.atCenterOf(pos), target, simulatedPlayer.pos, pos, placePos, true
                  );
                  if (!(damage < minDamage)) {
                     double selfDamage = GetExplosionDamageUtil.getDamageIgnoringBlockAndGhost(Vec3.atCenterOf(pos), mc.player, pos, placePos, true);
                     if (selfDamage < includeSelfDamage) {
                        includeFace = face;
                        includeRotation = rot;
                        includeSelfDamage = selfDamage;
                     }

                     if (!(selfDamage > maxSelfDamage) && selfDamage < bestSelfDamage) {
                        bestSelfDamage = selfDamage;
                        bestFace = face;
                        bestRotation = rot;
                     }
                  }
               }
            }
         }
      }

      return new Pair(new FaceDefensive(bestFace, bestSelfDamage, bestRotation), new FaceDefensive(includeFace, includeSelfDamage, includeRotation));
   }

   public static Pair<Rotation, Direction> getBestVisibleFaceDefensive2(
      BlockPos pos, Player entity, Player target, BlockPos ghost, double minDamage, double maxSelfDamage, boolean nearestPoint
   ) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestSelfDamage = Double.MAX_VALUE;
      Rotation bestRotation = null;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos placePos = pos.relative(face);
               if (mc.level.getBlockState(placePos).isAir() && !blocked(placePos).equals(ValuesEnum9.PLAYER) && !blocked(ghost).equals(ValuesEnum9.PLAYER)) {
                  double selfDamage = GetExplosionDamageUtil.getDamageIgnoringBlockAndGhosts(
                     Vec3.atCenterOf(pos), mc.player, pos, List.of(ghost, placePos), true
                  );
                  if (!(selfDamage > maxSelfDamage)) {
                     double damage = GetExplosionDamageUtil.getDamageIgnoringBlockAndGhosts(Vec3.atCenterOf(pos), target, pos, List.of(ghost, placePos), true);
                     if (!(damage < minDamage) && selfDamage < bestSelfDamage) {
                        bestSelfDamage = selfDamage;
                        bestFace = face;
                        bestRotation = rot;
                     }
                  }
               }
            }
         }
      }

      return new Pair(bestRotation, bestFace);
   }

   public static Pair<Rotation, Direction> getBestVisibleFaceAutoTrap(BlockPos pos, Player entity, Simulated simulatedPlayer, boolean nearestPoint) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      Rotation bestRotation = null;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos sidePos = pos.relative(face);
               if (!blockedBlock(pos).equals(ValuesEnum9.PLAYER)
                  && !blockedBlock(sidePos).equals(ValuesEnum9.PLAYER)
                  && mc.level.getBlockState(sidePos).isAir()
                  && simulatedPlayer.posToBlockPos(simulatedPlayer.pos).equals(sidePos)) {
                  bestFace = face;
                  bestRotation = rot;
               }
            }
         }
      }

      return new Pair(bestRotation, bestFace);
   }

   public static Triple<Rotation, Direction, Double> getBestVisibleFaceSelfDamage(
      BlockPos pos, BlockPos explosionPos, List<BlockPos> ghosts, Player entity, boolean nearestPoint
   ) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestSelfDamage = Double.MAX_VALUE;
      Rotation bestRotation = null;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos placePos = pos.relative(face);
               if (mc.level.getBlockState(placePos).isAir()) {
                  List<BlockPos> ghosts2 = new ArrayList<>();
                  ghosts2.add(placePos);
                  ghosts2.addAll(ghosts);
                  double selfDamage = GetSelfExplosionDamageUtil.getDamageIgnoringBlockAndGhostsAt(
                     Vec3.atBottomCenterOf(explosionPos), mc.player, mc.player.position(), explosionPos, ghosts2, true
                  );
                  if (selfDamage < bestSelfDamage) {
                     bestSelfDamage = selfDamage;
                     bestFace = face;
                     bestRotation = rot;
                  }
               }
            }
         }
      }

      return Triple.of(bestRotation, bestFace, bestSelfDamage);
   }

   public static Pair<Rotation, Direction> getBestVisibleFaceCrystal(BlockPos pos, Player entity, boolean nearestPoint) {
      Vec3 eye = entity.getEyePosition();
      Direction bestFace = null;
      double bestDist = Double.MAX_VALUE;
      Rotation bestRotation = null;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               if (face == Direction.UP) {
                  return new Pair(rot, face);
               }

               double dist = eye.distanceToSqr(point);
               if (dist < bestDist) {
                  bestDist = dist;
                  bestFace = face;
                  bestRotation = rot;
               }
            }
         }
      }

      return new Pair(bestRotation, bestFace);
   }

   public static double[] getPossibleEyeHeights() {
      if (pc.viaVersionProtocol() != 0 && pc.viaVersionProtocol() <= 477) {
         if (pc.viaVersionProtocol() > 107) {
            if (possibleEyeHeights[0] == null || possibleEyeHeights[1] == null || possibleEyeHeights[2] == null) {
               possibleEyeHeights[2] = new double[]{0.4, 1.62, 1.54};
               possibleEyeHeights[1] = new double[]{1.54, 1.62, 0.4};
               possibleEyeHeights[0] = new double[]{1.62, 1.54, 0.4};
            }
         } else if (possibleEyeHeights[0] == null || possibleEyeHeights[1] == null) {
            possibleEyeHeights[1] = new double[]{1.54F, 1.62F};
            possibleEyeHeights[0] = new double[]{1.62F, 1.54F};
         }
      } else if (possibleEyeHeights[0] == null || possibleEyeHeights[1] == null || possibleEyeHeights[2] == null) {
         float scale = (float)mc.player.getAttributeValue(Attributes.SCALE);
         possibleEyeHeights[2] = new double[]{0.4 * scale, 1.62 * scale, 1.27 * scale};
         possibleEyeHeights[1] = new double[]{1.27 * scale, 1.62 * scale, 0.4 * scale};
         possibleEyeHeights[0] = new double[]{1.62 * scale, 1.27 * scale, 0.4 * scale};
      }

      if (pc.viaVersionProtocol() < 107) {
         return possibleEyeHeights[mc.player.isShiftKeyDown() ? 1 : 0];
      }

      return switch (mc.player.getPose()) {
         case FALL_FLYING, SPIN_ATTACK, SWIMMING -> possibleEyeHeights[2];
         case CROUCHING -> possibleEyeHeights[1];
         default -> possibleEyeHeights[0];
      };
   }

   public static Direction getBestVisibleFaceAutoWeb(BlockPos pos, Player entity, Simulated simulated, ValuesEnum5 type, boolean nearestPoint) {
      Vec3 eye = entity.getEyePosition();
      BlockPos check = simulated.posToBlockPos(simulated.pos);
      if (type.equals(ValuesEnum5.UP)) {
         check = check.above();
      }

      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
         Rotation rot = CalculateFovUtil.calculate(point);
         if (!(min > maxReach * maxReach)) {
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos placePos = pos.relative(face);
               if (mc.level.getBlockState(placePos).isAir() && placePos.equals(check)) {
                  if (type.equals(ValuesEnum5.DOWN) && mc.level.getBlockState(check.above()).getBlock() == Blocks.COBWEB) {
                     return null;
                  }

                  return face;
               }
            }
         }
      }

      return null;
   }

   public static Vec3 getFacePoint(BlockPos pos, Direction face) {
      return new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.5001, pos.getY() + 0.5 + face.getStepY() * 0.5001, pos.getZ() + 0.5 + face.getStepZ() * 0.5001);
   }

   public static Vec3 getNearestFacePoint(BlockPos pos, Direction face, Vec3 eye) {
      double x = Mth.clamp(eye.x, pos.getX() + 0.001, pos.getX() + 1.0 - 0.001);
      double y = Mth.clamp(eye.y, pos.getY() + 0.001, pos.getY() + 1.0 - 0.001);
      double z = Mth.clamp(eye.z, pos.getZ() + 0.001, pos.getZ() + 1.0 - 0.001);
      switch (face) {
         case WEST:
            x = pos.getX();
            break;
         case EAST:
            x = pos.getX() + 1.0;
            break;
         case DOWN:
            y = pos.getY();
            break;
         case UP:
            y = pos.getY() + 1.0;
            break;
         case NORTH:
            z = pos.getZ();
            break;
         case SOUTH:
            z = pos.getZ() + 1.0;
      }

      return new Vec3(x, y, z);
   }

   public static double getDistanceToNearestFaceSqr(BlockPos pos, Direction face, Vec3 eye) {
      double x = Mth.clamp(eye.x, pos.getX() + 0.001, pos.getX() + 1.0 - 0.001);
      double y = Mth.clamp(eye.y, pos.getY() + 0.001, pos.getY() + 1.0 - 0.001);
      double z = Mth.clamp(eye.z, pos.getZ() + 0.001, pos.getZ() + 1.0 - 0.001);
      switch (face) {
         case WEST:
            x = pos.getX();
            break;
         case EAST:
            x = pos.getX() + 1.0;
            break;
         case DOWN:
            y = pos.getY();
            break;
         case UP:
            y = pos.getY() + 1.0;
            break;
         case NORTH:
            z = pos.getZ();
            break;
         case SOUTH:
            z = pos.getZ() + 1.0;
      }

      double dx = eye.x - x;
      double dy = eye.y - y;
      double dz = eye.z - z;
      return dx * dx + dy * dy + dz * dz;
   }

   public static ToStringImpl7 bestFaceCAura(BlockPos pos, Player entity, double minDamage, double maxSelfDamage, boolean nearestPoint) {
      Direction bestFace = null;
      Rotation bestRotation = null;
      double bestDamage = 0.0;
      double bestAwaitingDamage = 0.0;
      BlockPos defensivePos = null;
      Direction defensiveFace = null;
      Rotation defensiveRotation = null;
      double defensiveDamage = 0.0;
      Vec3 eye = mc.player.getEyePosition();
      Simulated sim = pc.simulated;
      Simulated sim2 = pc.simulated2;
      Simulated sim3 = pc.simulated3;
      AABB simBox = sim.boundingBox;
      AABB simBox2 = sim2.boundingBox;
      AABB simBox3 = sim3.boundingBox;
      boolean isBreak = false;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            BlockPos sidePos = pos.relative(face);
            BlockPos placePos = sidePos.above();
            if ((mc.level.getBlockState(sidePos).isAir() || mc.level.getBlockState(sidePos).liquid())
               && mc.level.getBlockState(placePos).isAir()
               && DidRayTraceHitUtil.canReachBox(
                  new AABB(placePos.getX() - 0.5, placePos.getY(), placePos.getZ() - 0.5, placePos.getX() + 1.5, placePos.getY() + 1.5, placePos.getZ() + 1.5),
                  3.0
               )) {
               Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
               Rotation rot = CalculateFovUtil.calculate(point);
               BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
               if (raycast != null
                  && raycast.getDirection() == face
                  && DidRayTraceHitUtil.didRayTraceHit(pos, face)
                  && !blockedPlace(sidePos).equals(ValuesEnum9.PLAYER)
                  && !blockedPlace(sidePos, simBox).equals(ValuesEnum9.PLAYER)
                  && !blockedPlace(sidePos, simBox2).equals(ValuesEnum9.PLAYER)
                  && !blockedPlace(sidePos, simBox3).equals(ValuesEnum9.PLAYER)) {
                  BlockPos pidorPos = entity.blockPosition();
                  double damage = GetSelfExplosionDamageUtil.getDamageIgnoringBlockAndGhostAt(
                     Vec3.atBottomCenterOf(placePos), entity, sim.pos, pidorPos, sidePos, true
                  );
                  if (!(damage < minDamage) && !(damage < bestDamage)) {
                     double selfDamage = GetSelfExplosionDamageUtil.getDamageIgnoringBlockAndGhost(
                        Vec3.atBottomCenterOf(placePos), mc.player, pidorPos, sidePos, true
                     );
                     if (selfDamage > maxSelfDamage) {
                        for (int x = -2; x < 2; x++) {
                           for (int y = -2; y < 2; y++) {
                              for (int z = -2; z < 2; z++) {
                                 BlockPos checkPos = new BlockPos(
                                    mc.player.blockPosition().getX() + x, mc.player.blockPosition().getY() + y, mc.player.blockPosition().getZ() + z
                                 );
                                 if (mc.level.getBlockState(checkPos).isSolid()) {
                                    Pair<Rotation, Direction> face0 = getBestVisibleFaceCrystal(checkPos, mc.player, true);
                                    if (face0 != null && face0.getFirst() != null && face0.getSecond() != null) {
                                       BlockPos place = checkPos.relative((Direction)face0.getSecond());
                                       if (mc.level.getBlockState(place).isAir()
                                          && !blocked(place).equals(ValuesEnum9.PLAYER)
                                          && !blocked(place, sim.boundingBox).equals(ValuesEnum9.PLAYER)
                                          && !blocked(place, sim2.boundingBox).equals(ValuesEnum9.PLAYER)
                                          && !blocked(place, sim3.boundingBox).equals(ValuesEnum9.PLAYER)) {
                                          float selfDamageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(
                                             Vec3.atBottomCenterOf(placePos), mc.player, mc.player.position(), List.of(), List.of(sidePos, place), true
                                          );
                                          if (!(selfDamageNew > maxSelfDamage)) {
                                             float damageNew = GetSelfExplosionDamageUtil.getDamageIgnoringBlocksAndGhostsAt(
                                                Vec3.atBottomCenterOf(placePos), entity, sim.pos, List.of(), List.of(sidePos, place), true
                                             );
                                             if (!(damageNew < bestDamage) && !(damageNew < defensiveDamage) && !(damageNew < minDamage)) {
                                                defensivePos = checkPos;
                                                defensiveFace = (Direction)face0.getSecond();
                                                defensiveRotation = (Rotation)face0.getFirst();
                                                defensiveDamage = damageNew;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     } else if (blockedObsidian(placePos).equals(ValuesEnum9.ITEM)) {
                        bestAwaitingDamage = damage;
                     } else {
                        isBreak = !mc.level.getBlockState(pidorPos).isSolid()
                           && GetSelfExplosionDamageUtil.getDamageOfGhostBlockAt(Vec3.atBottomCenterOf(placePos), entity, sim.pos, sidePos, true) <= 1.0F;
                        if (isBreak) {
                           Pair<Rotation, Direction> faceInside = getBestVisibleFace(pidorPos, mc.player, true);
                           if (faceInside.getFirst() == null || faceInside.getSecond() == null) {
                              continue;
                           }

                           bestFace = (Direction)faceInside.getSecond();
                           bestRotation = (Rotation)faceInside.getFirst();
                        } else {
                           bestFace = face;
                           bestRotation = rot;
                        }

                        bestDamage = damage;
                     }
                  }
               }
            }
         }
      }

      return defensiveDamage > bestDamage
         ? new ToStringImpl7(defensivePos, defensiveRotation, defensiveFace, defensiveDamage, 0.0, false)
         : new ToStringImpl7(null, bestRotation, bestFace, bestDamage, bestAwaitingDamage, isBreak);
   }

   public static ToStringImpl7 bestFaceYAura(
      BlockPos pos, Player entity, Simulated simulatedPlayer, double minDamage, double maxSelfDamage, boolean nearestPoint
   ) {
      Direction bestFace = null;
      Rotation bestRotation = null;
      double bestDamage = 0.0;
      Vec3 eye = mc.player.getEyePosition();
      Simulated sim = pc.simulated;
      Simulated sim2 = pc.simulated2;
      Simulated sim3 = pc.simulated3;
      double min = Double.MAX_VALUE;
      double[] possibleEyeHeights = getPossibleEyeHeights();

      for (double d : possibleEyeHeights) {
         AABB box = new AABB(pos);
         Vec3 best = GetNearestPointUtil.cutBoxToVector(mc.player.getX(), mc.player.getY() + d, mc.player.getZ(), box);
         min = Math.min(min, best.distanceToSqr(mc.player.getX(), mc.player.getY() + d, mc.player.getZ()));
      }

      double maxReach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double threshold = JumpYMotionUtil.getMovementThreshold();
      maxReach += Math.hypot(threshold, threshold);

      for (Direction face : Direction.values()) {
         if (!(min > maxReach * maxReach)) {
            Vec3 point = nearestPoint ? getNearestFacePoint(pos, face, eye) : getFacePoint(pos, face);
            Rotation rot = CalculateFovUtil.calculate(point);
            BlockHitResult raycast = DidRayTraceHitUtil.raycastBlock(rot, pos, 8.0);
            if (raycast != null && raycast.getDirection() == face && DidRayTraceHitUtil.didRayTraceHit(pos, face)) {
               BlockPos placePos = pos.relative(face);
               if (mc.level.getBlockState(placePos).isAir()
                  && !blocked(placePos).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim.boundingBox).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim2.boundingBox).equals(ValuesEnum9.PLAYER)
                  && !blocked(placePos, sim3.boundingBox).equals(ValuesEnum9.PLAYER)) {
                  double damage = GetExplosionDamageUtil.getExplosionDamageAt(Vec3.atCenterOf(placePos), entity, sim.pos, true);
                  if (!(damage < minDamage) && !(damage < bestDamage)) {
                     double selfDamage = GetExplosionDamageUtil.getExplosionDamage(Vec3.atCenterOf(placePos), mc.player, true);
                     if (selfDamage > maxSelfDamage) {
                        Pair<FaceDefensive, FaceDefensive> best = getBestVisibleFaceDefensive(
                           placePos, mc.player, entity, simulatedPlayer, Math.max(minDamage, bestDamage), maxSelfDamage, true
                        );
                        if (((FaceDefensive)best.getFirst()).face() == null || ((FaceDefensive)best.getFirst()).rotation() == null) {
                           continue;
                        }
                     }

                     bestRotation = rot;
                     bestFace = face;
                     bestDamage = damage;
                  }
               }
            }
         }
      }

      return new ToStringImpl7(null, bestRotation, bestFace, bestDamage, 0.0, false);
   }

   public static ValuesEnum9 blocked(BlockPos pos) {
      for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity item && item.tickCount > 10 && intersectsEntityBox(pos, entity)) {
            return ValuesEnum9.ITEM;
         }

         if (!(entity instanceof ItemEntity) && intersectsEntityBox(pos, entity)) {
            return ValuesEnum9.PLAYER;
         }
      }

      return ValuesEnum9.NONE;
   }

   public static ValuesEnum9 blockedPlace(BlockPos pos) {
      for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity item && item.tickCount > 10 && intersectsEntityBoxPlace(pos, entity)) {
            return ValuesEnum9.ITEM;
         }

         if (!(entity instanceof ItemEntity) && intersectsEntityBoxPlace(pos, entity)) {
            return ValuesEnum9.PLAYER;
         }
      }

      return ValuesEnum9.NONE;
   }

   public static ValuesEnum9 blockedBlock(BlockPos pos) {
      for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity item && item.tickCount > 10 && intersectsEntityBox(pos, entity)) {
            return ValuesEnum9.ITEM;
         }

         if (!(entity instanceof ItemEntity) && intersectsEntityBox(pos, entity)) {
            return ValuesEnum9.PLAYER;
         }
      }

      return ValuesEnum9.NONE;
   }

   public static ValuesEnum9 blockedObsidian(BlockPos pos) {
      for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity item && item.tickCount > 10 && !item.onGround()) {
            AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
            PositionAndRotation inter = entity.getInterpolation() == null ? null : entity.getInterpolation().target();
            Vec3 position = inter == null ? item.position() : inter.position();
            if (item.dimensions.makeBoundingBox(position).intersects(blockBox)) {
               return ValuesEnum9.ITEM;
            }
         }
      }

      return ValuesEnum9.NONE;
   }

   public static ValuesEnum9 blocked(BlockPos pos, AABB box) {
      return intersectsEntityBox(pos, box) ? ValuesEnum9.PLAYER : ValuesEnum9.NONE;
   }

   public static ValuesEnum9 blockedPlace(BlockPos pos, AABB box) {
      return intersectsEntityBoxPlace(pos, box) ? ValuesEnum9.PLAYER : ValuesEnum9.NONE;
   }

   public static boolean intersectsEntityBox(BlockPos pos, net.minecraft.world.entity.Entity entity) {
      AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
      PositionAndRotation inter = entity.getInterpolation() == null ? null : entity.getInterpolation().target();
      Vec3 position = inter == null ? entity.position() : inter.position();
      return entity.dimensions.makeBoundingBox(position).intersects(blockBox);
   }

   public static boolean intersectsEntityBoxPlace(BlockPos pos, net.minecraft.world.entity.Entity entity) {
      AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
      PositionAndRotation inter = entity.getInterpolation() == null ? null : entity.getInterpolation().target();
      Vec3 position = inter == null ? entity.position() : inter.position();
      return entity.dimensions.makeBoundingBox(position).intersects(blockBox);
   }

   public static boolean intersectsEntityBoxPlace(BlockPos pos, AABB box) {
      AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
      return box.intersects(blockBox);
   }

   public static boolean intersectsEntityBox(BlockPos pos, AABB box) {
      AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
      return box.intersects(blockBox);
   }

   public static boolean isWeb(BlockPos pos) {
      return mc.level.getBlockState(pos).is(Blocks.COBWEB);
   }

   public IsInWebUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
