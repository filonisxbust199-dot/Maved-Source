package ess.ll.client.mixin;

import ess.ll.client.module.player.NoPush;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoPush: standing inside a block does not shove you out of it. */
@Mixin(LocalPlayer.class)
public abstract class NoPushLocalPlayerMixin {
    @Inject(method="moveTowardsClosestSpace",at=@At("HEAD"),cancellable=true)
    private void ess$blocks(double x,double z,CallbackInfo ci) {
        if(NoPush.blocks((LocalPlayer)(Object)this,"Блоки")) ci.cancel();
    }
}
