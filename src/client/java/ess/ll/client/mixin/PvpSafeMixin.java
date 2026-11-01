package ess.ll.client.mixin;

import ess.ll.client.module.misc.PvpSafe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PvpSafe: leaving the server is ignored while a PvP boss bar is on screen. */
@Mixin(Minecraft.class)
public class PvpSafeMixin {
    @Inject(method={"disconnectWithProgressScreen()V","disconnectWithSavingScreen()V"},at=@At("HEAD"),cancellable=true)
    private void ess$blockPvpDisconnect(CallbackInfo ci) {
        if(PvpSafe.isPvpProtectionActive()) ci.cancel();
    }

    @Inject(method="disconnectWithProgressScreen(Z)V",at=@At("HEAD"),cancellable=true)
    private void ess$blockPvpTransferDisconnect(boolean transfer,CallbackInfo ci) {
        if(PvpSafe.isPvpProtectionActive()) ci.cancel();
    }
}
