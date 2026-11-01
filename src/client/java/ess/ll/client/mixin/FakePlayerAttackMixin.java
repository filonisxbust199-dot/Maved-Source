package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ess.ll.client.features.FakePlayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A hit on a fake player is not sent to the server: it is dealt here, and the rest of the attack runs as usual. */
@Mixin(MultiPlayerGameMode.class)
public class FakePlayerAttackMixin {
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void ess$fakePlayer(ClientPacketListener connection,Packet<?> packet,Operation<Void> send,Player player,Entity target) {
        if(!FakePlayers.attack(player,target)) send.call(connection,packet);
    }
}
