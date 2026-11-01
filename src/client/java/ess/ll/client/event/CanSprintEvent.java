package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;

public class CanSprintEvent extends Event {
   public static final CanSprintEvent INSTANCE = new CanSprintEvent();
   private LocalPlayer player;
   private Boolean override;

   public boolean fire(boolean vanilla, LocalPlayer player) {
      this.player = player;
      this.override = null;
      super.dispatch();
      if (this.override != null) {
         return this.override;
      } else {
         return this.consumeCancelled() ? player.input.keyPresses.forward() : vanilla;
      }
   }

   public void setOverride(Boolean override) {
      this.override = override;
   }

   public LocalPlayer getPlayer() {
      return this.player;
   }

   public void setPlayer(LocalPlayer player) {
      this.player = player;
   }

   public Boolean getOverride() {
      return this.override;
   }
}
