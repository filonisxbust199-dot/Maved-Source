package ess.ll.client.mixin;

import ess.ll.client.core.FreeLookController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla's tick-based hand lag in camera space, not silent combat rotation space. */
@Mixin(LocalPlayer.class)
public abstract class HandRotationInterpolationMixin {
    @Inject(method="applyInput",at=@At("RETURN"))
    private void ess$cameraHandLag(CallbackInfo ci) {
        LocalPlayer player=(LocalPlayer)(Object)this;
        if(player!=Minecraft.getInstance().player)return;
        float yaw=FreeLookController.isActive()?FreeLookController.getYaw():player.getYRot();
        float pitch=FreeLookController.isActive()?FreeLookController.getPitch():player.getXRot();
        // applyInput has already captured the previous tick in *BobO. Do not advance it per frame.
        player.xBob=player.xBobO+(pitch-player.xBobO)*.5f;
        player.yBob=player.yBobO+Mth.wrapDegrees(yaw-player.yBobO)*.5f;
    }
}
