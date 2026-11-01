package ess.ll.client.integration;

import ess.ll.client.util.Rotation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RotationRenderHooks implements MinecraftAccess {
   private static final float ZERO_A = 0;
   private static final float ZERO_B = 0;
   private static final float ZERO_C = 0;
   private static final float ZERO_D = 0;
   private static final float ZERO_E = 0;
   private static boolean active;
   private static float headYaw;
   private static float prevHeadYaw;
   private static float bodyYaw;
   private static float prevBodyYaw;
   private static float pitch;
   private static float prevPitch;
   private static final double MOVE_THRESHOLD_SQ = 0.0025000002F;
   private static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
   private static final float YAW_OFFSET = 90.0F;
   private static final float BACKWARDS_MIN = 95.0F;
   private static final float BACKWARDS_MAX = 265.0F;
   private static final float HALF_TURN = 180.0F;
   private static final float BODY_TURN_FACTOR = 0.3F;
   private static final float MAX_HEAD_OFFSET_BLOCKING = 15.0F;
   private static final float MAX_HEAD_OFFSET = 50.0F;

   private RotationRenderHooks() {
   }

   private static HitResult clampToRange(HitResult hitResult, Vec3 vec, double range) {
      Vec3 location = hitResult.getLocation();
      if (location.closerThan(vec, range)) {
         return hitResult;
      }

      Vec3 vec2 = location.subtract(vec);
      return BlockHitResult.miss(location, Direction.getApproximateNearest(vec2.x, vec2.y, vec2.z), BlockPos.containing(location));
   }

   public static float pitchOr(float vanilla) {
      return isOverriding() ? lerpPitch(partialTick()) : vanilla;
   }

   private static boolean isOverriding() {
      LocalPlayer player = mc.player;
      return player != null && mc.getCameraEntity() == player && isRotating(player);
   }

   private static float updateBodyYaw(LivingEntity livingEntity, float bodyYaw, float headYaw) {
      double dx = livingEntity.getX() - livingEntity.xo;
      double dz = livingEntity.getZ() - livingEntity.zo;
      float targetYaw = bodyYaw;
      if (dx * dx + dz * dz > MOVE_THRESHOLD_SQ) {
         float moveYaw = (float)Mth.atan2(dz, dx) * RAD_TO_DEG - YAW_OFFSET;
         float diff = Mth.abs(Mth.wrapDegrees(headYaw) - moveYaw);
         targetYaw = BACKWARDS_MIN < diff && diff < BACKWARDS_MAX ? moveYaw - HALF_TURN : moveYaw;
      }

      if (livingEntity.getSwingAnimation(1f) > 0.0F) {
         targetYaw = headYaw;
      }

      bodyYaw += Mth.wrapDegrees(targetYaw - bodyYaw) * BODY_TURN_FACTOR;
      float headOffset = Mth.wrapDegrees(headYaw - bodyYaw);
      float maxOffset = livingEntity.isBlocking() ? MAX_HEAD_OFFSET_BLOCKING : MAX_HEAD_OFFSET;
      if (Math.abs(headOffset) > maxOffset) {
         bodyYaw += headOffset - Mth.sign(headOffset) * maxOffset;
      }

      return bodyYaw;
   }

   public static float headYawOr(float vanilla) {
      return isOverriding() ? lerpHeadYaw(partialTick()) : vanilla;
   }

   public static boolean isRotating(LivingEntity livingEntity) {
      return active && livingEntity == mc.player && !livingEntity.isFallFlying() && !livingEntity.isPassenger();
   }

   private static float partialTick() {
      return mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
   }

   public static float lerpPitch(float partialTick) {
      return Mth.lerp(partialTick, prevPitch, pitch);
   }

   private static boolean hasCaughtUp(LivingEntity livingEntity) {
      return Math.abs(Mth.wrapDegrees(bodyYaw - livingEntity.yBodyRot)) < 1.0F && Math.abs(Mth.wrapDegrees(headYaw - livingEntity.yHeadRot)) < 1.0F;
   }

   public static float lerpHeadYaw(float partialTick) {
      return Mth.rotLerp(partialTick, prevHeadYaw, headYaw);
   }

   public static HitResult pick(HitResult hitResult) {
      if (mc.level != null && isOverriding()) {
         LocalPlayer player = mc.player;
         float partialTick = partialTick();
         double blockRange = player.blockInteractionRange();
         double range = Math.max(blockRange, player.entityInteractionRange());
         Vec3 eyePosition = player.getEyePosition(partialTick);
         Vec3 viewVector = player.calculateViewVector(lerpPitch(partialTick), lerpHeadYaw(partialTick));
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePosition, eyePosition.add(viewVector.scale(range)), Block.OUTLINE, Fluid.NONE, player));
         double blockDistanceSq = blockHitResult.getLocation().distanceToSqr(eyePosition);
         if (blockHitResult.getType() != Type.MISS) {
            range = Math.sqrt(blockDistanceSq);
         }

         Vec3 vec = viewVector.scale(range);
         EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
            player, eyePosition, eyePosition.add(vec), player.getBoundingBox().expandTowards(vec).inflate(1.0), EntitySelector.CAN_BE_PICKED, range * range
         );
         return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(eyePosition) < blockDistanceSq
            ? clampToRange(entityHitResult, eyePosition, player.entityInteractionRange())
            : clampToRange(blockHitResult, eyePosition, blockRange);
      } else {
         return hitResult;
      }
   }

   public static float lerpBodyYaw(float partialTick) {
      return Mth.rotLerp(partialTick, prevBodyYaw, bodyYaw);
   }

   public static Direction facingOr(Direction direction) {
      return isOverriding() ? Direction.fromYRot(lerpHeadYaw(partialTick())) : direction;
   }

   public static void update(Rotation rotation) {
      LocalPlayer player = mc.player;
      if (player != null && (rotation != null || active)) {
         if (!active) {
            headYaw = player.yHeadRot;
            bodyYaw = player.yBodyRot;
            pitch = player.getXRot();
            active = true;
         }

         prevHeadYaw = headYaw;
         prevBodyYaw = bodyYaw;
         prevPitch = pitch;
         headYaw = rotation != null ? rotation.getYaw() : player.yHeadRot;
         pitch = rotation != null ? rotation.getPitch() : player.getXRot();
         bodyYaw = updateBodyYaw(player, bodyYaw, headYaw);
         if (rotation == null && hasCaughtUp(player)) {
            active = false;
         }
      } else {
         active = false;
      }
   }
}
