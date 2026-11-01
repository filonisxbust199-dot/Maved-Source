package ess.ll.client.mixin;

import ess.ll.client.features.FakePlayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Explosions the server reports hurt the fake players too. */
@Mixin(ClientPacketListener.class)
public class FakePlayerExplosionMixin {
    @Inject(method="handleExplosion",at=@At("TAIL"))
    private void ess$fakePlayers(ClientboundExplodePacket packet,CallbackInfo ci) { FakePlayers.explosion(packet.center(),packet.radius()); }
}
