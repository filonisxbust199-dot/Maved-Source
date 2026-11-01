package ess.ll.client.mixin;

import ess.ll.client.util.ModuleManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class CombatBlockAckMixin {
    @Inject(method = "handleBlockChangedAck", at = @At("RETURN"))
    private void ess$confirmBreak(ClientboundBlockChangedAckPacket packet, CallbackInfo ci) {
        ModuleManager.INSTANCE.crystalAura().acknowledgeBlockChanges(packet.sequence());
    }
}
