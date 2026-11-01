package ess.ll.client.event;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SprintStateEvent extends Event {
   public static final SprintStateEvent INSTANCE = new SprintStateEvent();
   private boolean allowed;

   public void setAllowed(boolean allowed) {
      this.allowed = allowed;
   }

   public boolean isAllowed() {
      return this.allowed;
   }

   public void fire(LivingEntity livingEntity, boolean sprinting, CallbackInfo ci) {
      if (sprinting && livingEntity instanceof LocalPlayer) {
         this.allowed = true;
         super.dispatch();
         if (!this.allowed) {
            ci.cancel();
         }
      }
   }
}
