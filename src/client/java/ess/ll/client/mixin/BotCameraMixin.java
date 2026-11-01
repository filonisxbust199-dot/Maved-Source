package ess.ll.client.mixin;

import ess.ll.client.bots.BotCamera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bots: the bot camera pass runs before the frame is extracted; the player's hands stay out of it. */
@Mixin(GameRenderer.class)
public abstract class BotCameraMixin {
    @Inject(method="extract",at=@At("HEAD"))
    private void ess$botCamera(DeltaTracker delta,boolean renderLevel,CallbackInfo ci) {
        if(renderLevel) BotCamera.beforeExtract((GameRenderer)(Object)this,delta);
    }
    @Inject(method="render3dHud",at=@At("HEAD"),cancellable=true)
    private void ess$noHands(CallbackInfo ci) {
        if(BotCamera.capturing()) ci.cancel();
    }
}
