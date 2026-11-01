package ess.ll.client.mixin;

import ess.ll.client.module.player.NoPush;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NoPush: flowing water does not carry you. */
@Mixin(Player.class)
public abstract class NoPushPlayerMixin {
    @Inject(method="isPushedByFluid",at=@At("HEAD"),cancellable=true)
    private void ess$fluid(CallbackInfoReturnable<Boolean> cir) {
        if(NoPush.blocks((Player)(Object)this,"Вода")) cir.setReturnValue(false);
    }
}
