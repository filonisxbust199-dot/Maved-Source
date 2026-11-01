package ess.ll.client.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ess.ll.client.event.PlayerInputEvent;

@Mixin(KeyboardInput.class)
public abstract class CombatInputMixin {
   @Shadow
   @Final
   private Options options;

   @Inject(method = "tick", at = @At("HEAD"))
   public void tick(CallbackInfo ci) {
      PlayerInputEvent.INSTANCE.fireFromOptions(this.options);
   }

   @Redirect(method = "tick", at = @At(value = "NEW", target = "Lnet/minecraft/world/entity/player/Input;"))
   public Input redirectInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
      return PlayerInputEvent.INSTANCE.toInput();
   }

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/KeyboardInput;calculateImpulse(ZZ)F", ordinal = 0))
   public float forward(boolean positive, boolean negative) {
      return PlayerInputEvent.INSTANCE.getForward();
   }

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/KeyboardInput;calculateImpulse(ZZ)F", ordinal = 1))
   public float strafe(boolean positive, boolean negative) {
      return PlayerInputEvent.INSTANCE.getStrafe();
   }
}
