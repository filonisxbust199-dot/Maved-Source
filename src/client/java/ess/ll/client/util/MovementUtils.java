package ess.ll.client.util;

import ess.ll.client.event.EntityMoveEvent;
import ess.ll.client.event.PlayerMovementEvent;
import ess.ll.client.integration.MinecraftAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class MovementUtils implements MinecraftAccess {
   private static final double GROUND_PROBE = 0.001;
   private static final float NO_DIRECTION = -1.0F;
   private static final double MIN_INPUT = 1.0E-4;
   private static final float NO_PLAYER_DIRECTION = -1.0F;
   private static final float NO_INPUT_DIRECTION = -1.0F;
   private static final float BACKWARD_TURN = 180.0F;
   private static final float LEFT_TURN = 90.0F;
   private static final float RIGHT_TURN = 90.0F;
   private static final float FORWARD_LEFT_TURN = 45.0F;
   private static final float FORWARD_RIGHT_TURN = 45.0F;
   private static final float BACK_LEFT_TURN = 45.0F;
   private static final float BACK_RIGHT_TURN = 45.0F;
   private static final double GROUND_CHECK_DEPTH = -0.05;
   private static final float FORWARD_MIN = -90.0F;
   private static final float FORWARD_MAX = 90.0F;
   private static final float BACKWARD_MIN = -90.0F;
   private static final float BACKWARD_MAX = 90.0F;
   private static final float RIGHT_MAX = 180.0F;
   private static final float LEFT_MIN = -180.0F;
   private static final double TICKS_PER_SECOND = 20.0;
   private static final double NEGATIVE_FORWARD = -1.0;
   private static final double NEGATIVE_STRAFE = -1.0;
   private static final float YAW_OFFSET_COS = 90.0F;
   private static final float YAW_OFFSET_SIN = 90.0F;
   private static final float HALF_TURN = 180.0F;
   private static final float BACKWARD_STRAFE_FACTOR = -0.5F;
   private static final float FORWARD_STRAFE_FACTOR = 0.5F;
   private static final float STRAFE_RIGHT_TURN = 90.0F;
   private static final float STRAFE_LEFT_TURN = 90.0F;
   private static final double FULL_TURN = 360.0;
   private static final double FULL_TURN_WRAP = 360.0;
   private static final double ATAN_OFFSET = 90.0;
   private static final float NO_DIFFERENCE = Float.MAX_VALUE;
   private static final float MIN_FORWARD = -1.0F;
   private static final float MIN_STRAFE = -1.0F;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static double[] moveDirectionVector() {
      float direction = moveYaw();
      if (direction == NO_DIRECTION) {
         return new double[]{0.0, 0.0};
      }

      double radians = Math.toRadians(direction);
      return new double[]{-Math.sin(radians), Math.cos(radians)};
   }

   public static void addMotionY(double dy) {
      if (mc.player != null) {
         Vec3 deltaMovement = mc.player.getDeltaMovement();
         mc.player.setDeltaMovement(deltaMovement.x, deltaMovement.y + dy, deltaMovement.z);
      }
   }

   public static Input inputForDirection(Input input, double relativeYaw, float tolerance) {
      boolean forward = input.forward();
      boolean backward = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (relativeYaw >= FORWARD_MIN + tolerance && relativeYaw <= FORWARD_MAX - tolerance) {
         forward = true;
      } else if (relativeYaw < BACKWARD_MIN - tolerance || relativeYaw > BACKWARD_MAX + tolerance) {
         backward = true;
      }

      if (relativeYaw >= 0.0F + tolerance && relativeYaw <= RIGHT_MAX - tolerance) {
         right = true;
      } else if (relativeYaw >= LEFT_MIN + tolerance && relativeYaw <= 0.0F - tolerance) {
         left = true;
      }

      return new Input(forward, backward, left, right, input.jump(), input.shift(), input.sprint());
   }

   public static double relativeMoveYaw(Vec3 motion, float yaw) {
      float moveRad = (float)Math.atan2(-motion.x, motion.z);
      double yawRad = Math.toRadians(Mth.wrapDegrees(yaw));
      return Math.toDegrees(Mth.wrapDegrees(moveRad - yawRad));
   }

   public static double horizontalSpeed() {
      return mc.player == null ? 0.0 : Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
   }

   private static double angleDistance(double a, double b) {
      double diff = Math.abs(a - b) % FULL_TURN;
      return Math.min(diff, FULL_TURN_WRAP - diff);
   }

   public static Double speedPerSecond() {
      return mc.player == null ? 0.0 : speedPerSecond(mc.player);
   }

   public static float horizontalSpeedF() {
      return mc.player == null
         ? 0.0F
         : (float)Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
   }

   public static double movedDistance() {
      if (mc.player == null) {
         return 0.0;
      }

      double dx = mc.player.getX() - mc.player.xo;
      double dz = mc.player.getZ() - mc.player.zo;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static float moveYaw() {
      if (mc.player == null) {
         return NO_PLAYER_DIRECTION;
      }

      boolean right = mc.options.keyRight.isDown();
      boolean left = mc.options.keyLeft.isDown();
      boolean forward = mc.options.keyUp.isDown();
      boolean back = mc.options.keyDown.isDown();
      float yRot = mc.player.getYRot();
      if (!right && !back && !left && !forward) {
         return NO_INPUT_DIRECTION;
      }

      if (back) {
         yRot -= BACKWARD_TURN;
      }

      boolean straight = (back || forward) && (!back || !forward);
      if (!straight) {
         if (left) {
            yRot -= LEFT_TURN;
         }

         if (right) {
            yRot += RIGHT_TURN;
         }
      } else if (yRot == mc.player.getYRot()) {
         if (left) {
            yRot -= FORWARD_LEFT_TURN;
         }

         if (right) {
            yRot += FORWARD_RIGHT_TURN;
         }
      } else {
         if (left) {
            yRot += BACK_LEFT_TURN;
         }

         if (right) {
            yRot -= BACK_RIGHT_TURN;
         }
      }

      return Mth.wrapDegrees(yRot);
   }

   public static Vec3 motionOf(Entity entity) {
      double dx = entity.getX() - entity.xo;
      double dy = entity.getY() - entity.yo;
      double dz = entity.getZ() - entity.zo;
      return new Vec3(dx, dy, dz);
   }

   public static void correctMovement(PlayerMovementEvent playerMovementEvent, float yaw, double x, double z) {
      double targetYaw = Math.toDegrees(Math.atan2(z, x)) - ATAN_OFFSET;
      targetYaw = Mth.wrapDegrees(targetYaw);
      if (mc.options.keyUp.isDown()) {
         float forward = playerMovementEvent.getForward();
         float strafe = playerMovementEvent.getStrafe();
         if (forward != 0.0F || strafe != 0.0F) {
            float bestForward = 0.0F;
            float bestStrafe = 0.0F;
            float bestDiff = NO_DIFFERENCE;

            for (float candidateForward = MIN_FORWARD; candidateForward <= 1.0F; candidateForward++) {
               for (float candidateStrafe = MIN_STRAFE; candidateStrafe <= 1.0F; candidateStrafe++) {
                  if (candidateStrafe != 0.0F || candidateForward != 0.0F) {
                     double moveYaw = Math.toDegrees(directionRadians(yaw, candidateForward, candidateStrafe));
                     moveYaw = Mth.wrapDegrees(moveYaw);
                     double diff = Math.abs(angleDistance(targetYaw, moveYaw));
                     if (diff < bestDiff) {
                        bestDiff = (float)diff;
                        bestForward = candidateForward;
                        bestStrafe = candidateStrafe;
                     }
                  }
               }
            }

            playerMovementEvent.setForward(bestForward);
            playerMovementEvent.setStrafe(bestStrafe);
         }
      }
   }

   public static void setSpeed(EntityMoveEvent entityMoveEvent, double speed, double unusedSpeed) {
      if (mc.player != null) {
         double forward = mc.player.zza;
         double strafe = mc.player.xxa;
         float yRot = mc.player.getYRot();
         applySpeed(entityMoveEvent, speed, speed, yRot, strafe, forward);
      }
   }

   private MovementUtils() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static boolean willLand(Entity entity) {
      Vec3 deltaMovement = entity.getDeltaMovement();
      if (deltaMovement.y >= 0.0) {
         return false;
      }

      AABB aabb = entity.getBoundingBox().move(deltaMovement.x, deltaMovement.y, deltaMovement.z).expandTowards(0.0, GROUND_CHECK_DEPTH, 0.0);
      return !entity.level().noCollision(entity, aabb);
   }

   public static void startFallFlying() {
      if (mc.player != null && mc.player.connection != null) {
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
      }
   }

   public static double axis(boolean positive, boolean negative, double value) {
      return axis(positive, negative, value, value);
   }

   public static double axis(boolean positive, boolean negative, double positiveValue, double negativeValue) {
      if (positive) {
         return positiveValue;
      } else {
         return negative ? -negativeValue : 0.0;
      }
   }

   public static double directionRadians(float yaw, float forward, float strafe) {
      if (forward < 0.0F) {
         yaw += HALF_TURN;
      }

      float factor = 1.0F;
      if (forward < 0.0F) {
         factor = BACKWARD_STRAFE_FACTOR;
      }

      if (forward > 0.0F) {
         factor = FORWARD_STRAFE_FACTOR;
      }

      if (strafe > 0.0F) {
         yaw -= STRAFE_RIGHT_TURN * factor;
      }

      if (strafe < 0.0F) {
         yaw += STRAFE_LEFT_TURN * factor;
      }

      return Math.toRadians(yaw);
   }

   public static boolean isOnSolidGround() {
      if (mc.player != null && mc.level != null) {
         BlockPos pos = BlockPos.containing(mc.player.getX(), Math.floor(mc.player.getY() - GROUND_PROBE), mc.player.getZ());
         BlockState blockState = mc.level.getBlockState(pos);
         return !blockState.isAir() && !(blockState.getBlock() instanceof FlowerBlock);
      } else {
         return false;
      }
   }

   public static void addMotion(double dx, double dz) {
      if (mc.player != null) {
         Vec3 deltaMovement = mc.player.getDeltaMovement();
         mc.player.setDeltaMovement(deltaMovement.x + dx, deltaMovement.y, deltaMovement.z + dz);
      }
   }

   public static void setSpeed(EntityMoveEvent entityMoveEvent, double speed) {
      if (mc.player != null) {
         double forward = mc.player.zza;
         double strafe = mc.player.xxa;
         float yRot = mc.player.getYRot();
         applySpeed(entityMoveEvent, speed, speed, yRot, strafe, forward);
      }
   }

   public static Double speedPerSecond(Entity entity) {
      double dx = entity.getX() - entity.xo;
      double dy = entity.getY() - entity.yo;
      double dz = entity.getZ() - entity.zo;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) * TICKS_PER_SECOND;
   }

   public static boolean isMovingDiagonally() {
      if (mc.player == null) {
         return false;
      }

      Vec2 moveVector = mc.player.input.getMoveVector();
      return moveVector.x != 0.0F && moveVector.y != 0.0F;
   }

   public static void applySpeed(EntityMoveEvent entityMoveEvent, double speed, double speedZ, float yaw, double strafe, double forward) {
      if (forward != 0.0) {
         if (strafe > 0.0) {
            yaw += forward > 0.0 ? -45 : 45;
         }

         if (strafe < 0.0) {
            yaw += forward > 0.0 ? 45 : -45;
         }

         strafe = 0.0;
         if (forward > 0.0) {
            forward = 1.0;
         }

         if (forward < 0.0) {
            forward = NEGATIVE_FORWARD;
         }
      }

      if (strafe > 0.0) {
         strafe = 1.0;
      }

      if (strafe < 0.0) {
         strafe = NEGATIVE_STRAFE;
      }

      double cos = Math.cos(Math.toRadians(yaw + YAW_OFFSET_COS));
      double sin = Math.sin(Math.toRadians(yaw + YAW_OFFSET_SIN));
      entityMoveEvent.setMovement(new Vec3(forward * speed * cos + strafe * speed * sin, entityMoveEvent.getMovement().y, forward * speedZ * sin - strafe * speed * cos));
   }

   public static double[] strafeVector() {
      if (mc.player == null) {
         return new double[]{0.0, 0.0};
      }

      Vec2 moveVector = mc.player.input.getMoveVector();
      double length = Math.hypot(moveVector.x, moveVector.y);
      if (length < MIN_INPUT) {
         return new double[]{0.0, 0.0};
      }

      double yawRad = Math.toRadians(mc.player.getYRot());
      double sin = Math.sin(yawRad);
      double cos = Math.cos(yawRad);
      double strafe = moveVector.x / length;
      double forward = moveVector.y / length;
      return new double[]{strafe * cos - forward * sin, forward * cos + strafe * sin};
   }

   public static boolean isMoving() {
      if (mc.player == null) {
         return false;
      }

      Vec2 moveVector = mc.player.input.getMoveVector();
      return moveVector.x != 0.0F || moveVector.y != 0.0F;
   }
}
