package ess.ll.client.util;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
public final class RotationChangeTracker implements MinecraftAccess {
   private static float lastDifference;
   private static int lastTick = -100;
   private static int nudge;
   private static final float REPEAT_EPSILON = 0.01F;

   @Subscribe(4)
   private void onPlayerUpdate(PlayerUpdateEvent playerUpdateEvent) {
      if (mc.player == null) {
         lastDifference = 0.0F;
         lastTick = -100;
      } else {
         int ticksSince = mc.player.tickCount - lastTick;
         if (ticksSince < 0 || ticksSince > 1) {
            float step = RotationMath.gcd();
            if (!(step <= 0.0F)) {
               for (int attempt = 0; attempt < 4 && isRepeated(yawDifference()); attempt++) {
                  nudge = nudge % 3 + 1;
                  mc.player.setYRot(mc.player.getYRot() + step * nudge);
               }
            }
         }
      }
   }

   public RotationChangeTracker() {
      EventBus.register(this);
   }

   public static void record() {
      if (mc.player != null) {
         lastDifference = yawDifference();
         lastTick = mc.player.tickCount;
      }
   }

   private static boolean isRepeated(float difference) {
      return difference > 2.0F && Math.abs(difference - lastDifference) < REPEAT_EPSILON;
   }

   private static float yawDifference() {
      float serverYaw = RotationManager.getServerYaw();
      return mc.player != null && !Float.isNaN(serverYaw) ? Math.abs(mc.player.getYRot() - serverYaw) : 0.0F;
   }
}
