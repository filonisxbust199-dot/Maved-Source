package ess.ll.client.mixin;

import ess.ll.client.bots.Bots;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bots: every game packet a tracked connection receives is offered to its recorder before it is handled. */
@Mixin(Connection.class)
public abstract class BotConnectionMixin {
    @Inject(method="channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"))
    private void ess$record(ChannelHandlerContext context,Packet<?> packet,CallbackInfo ci) {
        Bots.received((Connection)(Object)this,packet);
    }
}
