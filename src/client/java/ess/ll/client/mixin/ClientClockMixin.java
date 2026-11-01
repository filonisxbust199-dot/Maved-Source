package ess.ll.client.mixin;

import ess.ll.client.module.render.ClientClockAccess;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockMixin {
    @Inject(method="getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;",at=@At("RETURN"))
    private void ess$clock(Holder<WorldClock> clock,CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir) {
        ((ClientClockAccess)(Object)cir.getReturnValue()).ess$setOverworld(clock.is(WorldClocks.OVERWORLD));
    }
}
