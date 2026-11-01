package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;

public class MouseTurnEvent extends Event {
   public static final MouseTurnEvent INSTANCE = new MouseTurnEvent();
   private double deltaYaw;
   private double deltaPitch;

   public void setDeltaPitch(double deltaPitch) {
      this.deltaPitch = deltaPitch;
   }

   public double getDeltaYaw() {
      return this.deltaYaw;
   }

   public double getDeltaPitch() {
      return this.deltaPitch;
   }

   public void fire(LocalPlayer player, double deltaYaw, double deltaPitch) {
      if (!this.hasListeners()) {
         player.turn(deltaYaw, deltaPitch);
      } else {
         this.deltaYaw = deltaYaw;
         this.deltaPitch = deltaPitch;
         super.dispatch();
         if (!this.consumeCancelled()) {
            player.turn(this.deltaYaw, this.deltaPitch);
         }
      }
   }

   public void setDeltaYaw(double deltaYaw) {
      this.deltaYaw = deltaYaw;
   }
}
