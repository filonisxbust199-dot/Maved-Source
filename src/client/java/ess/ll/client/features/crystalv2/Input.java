package ess.ll.client.features.crystalv2;

import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Input extends ClientInput {
   public boolean forceSafeWalk = false;
   public float movementForward;
   public float movementSideways;
   public net.minecraft.world.entity.player.Input playerInput;
   public static final double MAX_WALKING_SPEED = 0.121;
   public final boolean useLocalInputState;

   public Input(net.minecraft.world.entity.player.Input input) {
      this(input, false);
   }

   public Input(net.minecraft.world.entity.player.Input input, boolean useLocalInputState) {
      this.playerInput = input;
      this.useLocalInputState = useLocalInputState;
   }

   public void update() {
      if (this.useLocalInputState && V2Access.pc.forwardPressed != V2Access.pc.backPressed) {
         this.movementForward = V2Access.pc.forwardPressed ? 1.0F : -1.0F;
      } else if (this.playerInput.forward() != this.playerInput.backward()) {
         this.movementForward = this.playerInput.forward() ? 1.0F : -1.0F;
      } else {
         this.movementForward = 0.0F;
      }

      if (this.useLocalInputState && V2Access.pc.leftPressed != V2Access.pc.rightPressed) {
         this.movementSideways = V2Access.pc.leftPressed ? 1.0F : -1.0F;
      } else if (this.playerInput.left() == this.playerInput.right()) {
         this.movementSideways = 0.0F;
      } else {
         this.movementSideways = this.playerInput.left() ? 1.0F : -1.0F;
      }

      if (this.playerInput.shift() || this.useLocalInputState && V2Access.pc.sneakPressed) {
         this.movementSideways *= 0.3F;
         this.movementForward *= 0.3F;
      }
   }

   public String toString() {
      return "SimulatedPlayerInput(forwards={"
         + this.playerInput.forward()
         + "}, backwards={"
         + this.playerInput.backward()
         + "}, left={"
         + this.playerInput.left()
         + "}, right={"
         + this.playerInput.right()
         + "}, jumping={"
         + this.playerInput.jump()
         + "}, sprinting="
         + this.playerInput.sprint()
         + ", slowDown="
         + this.playerInput.shift()
         + ")";
   }

   public static Input fromClientPlayer(net.minecraft.world.entity.player.Input input) {
      return new Input(input, true);
   }

   public static Input guessInput(Player entity) {
      Vec3 velocity = entity.getPosition(1.0F).subtract(new Vec3(entity.xOld, entity.yOld, entity.zOld));
      double horizontalVelocity = velocity.horizontalDistanceSqr();
      net.minecraft.world.entity.player.Input input = new net.minecraft.world.entity.player.Input(
         false, false, false, false, !entity.onGround(), entity.isShiftKeyDown(), horizontalVelocity >= 0.014641
      );
      if (horizontalVelocity > 0.0025000000000000005) {
         double velocityAngle = JumpYMotionUtil.getDegreesRelativeToView(velocity, entity.getYRot());
         double wrappedAngle = Mth.wrapDegrees(velocityAngle);
         input = JumpYMotionUtil.getDirectionalInputForDegrees(input, wrappedAngle);
      }

      return new Input(input);
   }
}
