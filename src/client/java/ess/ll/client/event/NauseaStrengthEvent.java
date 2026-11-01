package ess.ll.client.event;

public class NauseaStrengthEvent extends Event {
   public static final NauseaStrengthEvent INSTANCE = new NauseaStrengthEvent();

   public float fire(float strength) {
      super.dispatch();
      return this.consumeCancelled() ? 0.0F : strength;
   }
}
