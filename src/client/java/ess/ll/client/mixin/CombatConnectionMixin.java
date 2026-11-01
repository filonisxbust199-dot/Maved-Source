package ess.ll.client.mixin;
import ess.ll.client.network.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Connection.class)
public class CombatConnectionMixin {
 @Inject(method="channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"),cancellable=true)
 private void ess$receive(ChannelHandlerContext context,Packet<?> packet,CallbackInfo ci){
  var mc=net.minecraft.client.Minecraft.getInstance();
  if(mc.getConnection()==null || mc.getConnection().getConnection()!=(Object)this)return;
  if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(p->PacketEvent.INSTANCE.fireCancellable(p,PacketEvent.Direction.RECEIVE,ci,(Connection)(Object)this));
  else PacketEvent.INSTANCE.fireCancellable(packet,PacketEvent.Direction.RECEIVE,ci,(Connection)(Object)this);
 }
 @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("HEAD"),cancellable=true)
 private void ess$send(Packet<?> packet,ChannelFutureListener listener,boolean flush,CallbackInfo ci){
  if(!ess.ll.client.module.combat.CombatRuntime.bypassPackets())PacketEvent.INSTANCE.fireCancellable(packet,PacketEvent.Direction.SEND,ci,(Connection)(Object)this);
 }
}
