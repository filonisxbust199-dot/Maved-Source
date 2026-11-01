package ess.ll.client.features.crystalv2;

import java.util.Objects;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class JumpYMotionUtil implements V2Access {
   public static void jumpYMotion(double yMotion) {
      Vec3 movement = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(movement.x, yMotion, movement.z);
      if (mc.player.isSprinting()) {
         float angle = mc.player.getYRot() * (float) (Math.PI / 180.0);
         mc.player.addDeltaMovement(new Vec3(-Mth.sin(angle) * 0.2, 0.0, Mth.cos(angle) * 0.2));
      }
   }

   public static double getMovementThreshold() {
      return pc.viaVersionProtocol() < 758 ? 0.03 : 2.0E-4;
   }

   public static double[] calculateDirection(double distance) {
      float forward = mc.player.input.moveVector.y;
      float sideways = mc.player.input.moveVector.x;
      float yaw = mc.player.getYRot();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians(yaw + 90.0F));
      double cosYaw = Math.cos(Math.toRadians(yaw + 90.0F));
      double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
      double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public static void setMotion(double velocity) {
      setMotionDirect(velocity, 0.0, true);
   }

   public static void setMotion(double velocity, double y) {
      setMotionDirect(velocity, y, false);
   }

   public static void setMotionDirect(double distance, double y, boolean keepCurrentY) {
      float forward = Objects.requireNonNull(mc.player).input.moveVector.y;
      float sideways = mc.player.input.moveVector.x;
      float yaw = mc.player.getYRot();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians(yaw + 90.0F));
      double cosYaw = Math.cos(Math.toRadians(yaw + 90.0F));
      double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
      double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;
      if (keepCurrentY) {
         y = mc.player.getDeltaMovement().y;
      }

      mc.player.setDeltaMovement(xMovement, y, zMovement);
   }

   public static double getDegreesRelativeToView(Vec3 positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians(Mth.wrapDegrees(yaw));
      return Math.toDegrees(Mth.wrapDegrees(optimalYaw - currentYaw));
   }

   public static net.minecraft.world.entity.player.Input getDirectionalInputForDegrees(
      net.minecraft.world.entity.player.Input input, double dgs, float deadAngle
   ) {
      boolean forwards = input.forward();
      boolean backwards = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (dgs >= -90.0F + deadAngle && dgs <= 90.0F - deadAngle) {
         forwards = true;
      } else if (dgs < -90.0F - deadAngle || dgs > 90.0F + deadAngle) {
         backwards = true;
      }

      if (dgs >= 0.0F + deadAngle && dgs <= 180.0F - deadAngle) {
         right = true;
      } else if (dgs >= -180.0F + deadAngle && dgs <= 0.0F - deadAngle) {
         left = true;
      }

      return new net.minecraft.world.entity.player.Input(forwards, backwards, left, right, input.jump(), input.shift(), input.sprint());
   }

   public static net.minecraft.world.entity.player.Input getDirectionalInputForDegrees(net.minecraft.world.entity.player.Input input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   public static float getdir() {
      boolean isr = pc.rightPressed;
      boolean isl = pc.leftPressed;
      boolean isf = pc.forwardPressed;
      boolean isb = pc.backPressed;
      float startfloat = mc.player.getYRot();
      if (!isr && !isb && !isl && !isf) {
         return -1.0F;
      }

      if (isb) {
         startfloat -= 180.0F;
      }

      boolean willmove = (isb || isf) && (!isb || !isf);
      if (!willmove) {
         if (isl) {
            startfloat -= 90.0F;
         }

         if (isr) {
            startfloat += 90.0F;
         }
      } else if (startfloat == mc.player.getYRot()) {
         if (isl) {
            startfloat -= 45.0F;
         }

         if (isr) {
            startfloat += 45.0F;
         }
      } else {
         if (isl) {
            startfloat += 45.0F;
         }

         if (isr) {
            startfloat -= 45.0F;
         }
      }

      if (!willmove) {
         willmove = (isl || isr) && (!isl || !isr);
      }

      return !willmove ? -1.0F : startfloat;
   }

   public static double getspeed3() {
      double motx = mc.player.getDeltaMovement().x;
      if (mc.player.onGround()) {
      }

      double motz = mc.player.getDeltaMovement().z;
      return Math.sqrt(motx * motx + motz * motz);
   }

   public JumpYMotionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
