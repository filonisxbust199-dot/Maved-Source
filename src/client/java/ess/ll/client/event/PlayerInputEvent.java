package ess.ll.client.event;

import net.minecraft.client.Options;

public class PlayerInputEvent extends PlayerMovementEvent {
   public static final PlayerInputEvent INSTANCE = new PlayerInputEvent();

   public void fireFromValues(float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
      this.setAll(forward, strafe, jump, sneak, sprint);
      super.dispatch();
   }

   @Override
   public boolean isPreInput() {
      return true;
   }

   @Override
   public boolean isPostInput() {
      return false;
   }

   public void fireFromOptions(Options options) {
      this.readFrom(options);
      super.dispatch();
   }
}
