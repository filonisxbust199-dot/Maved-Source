package ess.ll.client.mixin;

import ess.ll.client.module.misc.ResourcePackSpoof;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ResourcePackSpoof: answers a pack push with "accepted" and "loaded" instead of downloading it. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ResourcePackSpoofMixin {
    @Shadow @Final protected Connection connection;

    @Inject(method="handleResourcePackPush",at=@At("HEAD"),cancellable=true)
    private void ess$spoof(ClientboundResourcePackPushPacket packet,CallbackInfo ci) {
        if(!ResourcePackSpoof.active()) return;
        connection.send(new ServerboundResourcePackPacket(packet.id(),ServerboundResourcePackPacket.Action.ACCEPTED));
        connection.send(new ServerboundResourcePackPacket(packet.id(),ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        ci.cancel();
    }
}
