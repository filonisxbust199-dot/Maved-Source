package ess.ll.client.event;

import net.minecraft.client.DeltaTracker.Timer;
import ess.ll.client.mixin.DeltaTracker$TimerMixin_1312;

public class TimerSpeedEvent extends Event {
   public static final TimerSpeedEvent INSTANCE = new TimerSpeedEvent();
   private float speed;

   public float fire(Timer timer, float vanilla) {
      if (timer == null) {
         return vanilla;
      }

      DeltaTracker$TimerMixin_1312 accessor = (DeltaTracker$TimerMixin_1312)timer;
      float tickTime = accessor.tickTime();
      this.speed = 1.0F;
      super.dispatch();
      return this.consumeCancelled() ? tickTime / this.speed : vanilla;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public float getSpeed() {
      return this.speed;
   }
}
