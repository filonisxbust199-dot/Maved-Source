package ess.ll.client.event;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;

public abstract class PlayerMovementEvent extends Event {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private static final float BACKWARD = -1.0F;

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public void reset() {
      this.forward = 0.0F;
      this.strafe = 0.0F;
      this.jump = false;
      this.sneak = false;
      this.sprint = false;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public float getForward() {
      return this.forward;
   }

   public abstract boolean isPostInput();

   public boolean isJump() {
      return this.jump;
   }

   protected void setAll(float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
      this.forward = forward;
      this.strafe = strafe;
      this.jump = jump;
      this.sneak = sneak;
      this.sprint = sprint;
   }

   public boolean isMoving() {
      return this.forward != 0.0F || this.strafe != 0.0F;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public abstract boolean isPreInput();

   public void setForward(float forward) {
      this.forward = forward;
   }

   public void stopMoving() {
      this.forward = 0.0F;
      this.strafe = 0.0F;
   }

   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   protected void readFrom(Options options) {
      this.forward = axis(options.keyUp.isDown(), options.keyDown.isDown());
      this.strafe = axis(options.keyLeft.isDown(), options.keyRight.isDown());
      this.jump = options.keyJump.isDown();
      this.sneak = options.keyShift.isDown();
      this.sprint = options.keySprint.isDown();
   }

   public Input toInput() {
      return new Input(
         this.forward > 0.0F, this.forward < 0.0F, this.strafe > 0.0F, this.strafe < 0.0F, this.jump, this.sneak, this.sprint
      );
   }

   private static float axis(boolean positive, boolean negative) {
      return positive == negative ? 0.0F : (positive ? 1.0F : BACKWARD);
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }
}
