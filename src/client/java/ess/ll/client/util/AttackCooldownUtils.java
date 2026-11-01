package ess.ll.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public final class AttackCooldownUtils {
   private static int lastTicker = -1;
   private static long tickerStartMs = AttackCooldownUtils.UNSET;
   private static final float MS_PER_TICK = 50.0F;
   private static final float MS_PER_TICK_DIVISOR = 50.0F;
   private static final long UNSET = -1L;

   private AttackCooldownUtils() {
   }

   public static int ticksUntilCharged(Player player) {
      if (player == null) {
         return 0;
      }

      float delay = player.getCurrentItemAttackStrengthDelay();
      return Float.isFinite(delay) && !(delay <= 0.0F) ? Math.max(0, Mth.ceil(delay - smoothTicker(player))) : 0;
   }

   public static int ticksUntilCharged(Player player, float offset, float fraction) {
      if (player == null) {
         return 0;
      }

      float delay = player.getCurrentItemAttackStrengthDelay();
      return Float.isFinite(delay) && !(delay <= 0.0F) ? Math.max(0, Mth.ceil(fraction * delay - offset) - ((LivingEntityMixin_1248)(Object)player).getAttackStrengthTicker()) : 0;
   }

   public static float attackStrength(Player player, float partialTick) {
      if (player == null) {
         return 0.0F;
      } else {
         float delay = player.getCurrentItemAttackStrengthDelay();
         if (Float.isFinite(delay) && !(delay <= 0.0F)) {
            float scale = (smoothTicker(player) + partialTick) / delay;
            return Mth.clamp(scale, 0.0F, 1.0F);
         } else {
            return player.getAttackStrengthScale(partialTick);
         }
      }
   }

   private static float smoothTicker(Player player) {
      int ticker = ((LivingEntityMixin_1248)(Object)player).getAttackStrengthTicker();
      long now = System.currentTimeMillis();
      if (tickerStartMs < 0L || ticker < lastTicker) {
         tickerStartMs = now - (long)(ticker * MS_PER_TICK);
      }

      lastTicker = ticker;
      return Mth.clamp((float)(now - tickerStartMs) / MS_PER_TICK_DIVISOR, ticker, ticker + 1.0F);
   }

   public static int ticksUntilFraction(Player player, float offset, float fraction) {
      if (player == null) {
         return 0;
      }

      float delay = player.getCurrentItemAttackStrengthDelay();
      return Float.isFinite(delay) && !(delay <= 0.0F) ? Math.max(1, Mth.floor(fraction * delay - offset) + 1) : 0;
   }

   public static boolean isCharged(Player player, float offset) {
      if (player == null) {
         return false;
      }

      float delay = player.getCurrentItemAttackStrengthDelay();
      return Float.isFinite(delay) && !(delay <= 0.0F) ? ((LivingEntityMixin_1248)(Object)player).getAttackStrengthTicker() >= Mth.ceil(delay - offset) : true;
   }
}
