package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectVisibilityEvent extends Event {
   public static final MobEffectVisibilityEvent INSTANCE = new MobEffectVisibilityEvent();
   private MobEffect effect;

   public void setEffect(MobEffect mobEffect) {
      this.effect = mobEffect;
   }

   public boolean fire(LivingEntity livingEntity, MobEffect mobEffect, boolean visible) {
      if (livingEntity instanceof LocalPlayer && this.hasListeners()) {
         this.effect = mobEffect;
         super.dispatch();
         return this.consumeCancelled() ? false : visible;
      } else {
         return visible;
      }
   }

   public MobEffect getEffect() {
      return this.effect;
   }
}
