package ess.ll.client.mixin;

import ess.ll.client.event.ContainerCloseEvent;
import ess.ll.client.event.ElytraStartEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ElytraInventoryPlayerMixin {
    @Shadow protected Minecraft minecraft;
    @Inject(method="closeContainer",at=@At("HEAD"),cancellable=true)
    private void ess$inventoryMoveClose(CallbackInfo ci) {
        LocalPlayer player=(LocalPlayer)(Object)this;
        if(player==minecraft.player) ContainerCloseEvent.INSTANCE.fire(minecraft,player,ci);
    }
    @Inject(method="aiStep",at=@At(value="FIELD",target="Lnet/minecraft/client/player/LocalPlayer;wasFallFlying:Z",opcode=181,shift=Shift.AFTER))
    private void ess$autoElytraStart(CallbackInfo ci) {
        LocalPlayer player=(LocalPlayer)(Object)this;
        if(player==minecraft.player && ElytraStartEvent.INSTANCE.fire(player) && player.tryToStartFallFlying())
            player.connection.send(new ServerboundPlayerCommandPacket(player,ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }
}
