package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ess.ll.client.event.TimerSpeedEvent;

@Mixin(Timer.class)
public class DeltaTracker$TimerMixin {
   @ModifyExpressionValue(method = "advanceGameTime", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
   private float modifyTimerSpeed(float original) {
      return TimerSpeedEvent.INSTANCE.fire((Timer)(Object)this, original);
   }
}
