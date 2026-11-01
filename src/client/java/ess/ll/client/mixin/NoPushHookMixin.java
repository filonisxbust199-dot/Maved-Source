package ess.ll.client.mixin;

import ess.ll.client.module.player.NoPush;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoPush: a fishing hook caught in you does not pull you (entity event 31 is the hook's pull). */
@Mixin(ClientPacketListener.class)
public abstract class NoPushHookMixin {
    @Inject(method="handleEntityEvent",at=@At("HEAD"),cancellable=true)
    private void ess$hook(ClientboundEntityEventPacket packet,CallbackInfo ci) {
        var mc=Minecraft.getInstance();
        if(packet.getEventId()!=31 || mc.level==null || !mc.isSameThread()) return;
        if(packet.getEntity(mc.level) instanceof FishingHook hook && NoPush.blocks(hook.getHookedIn(),"Удочка")) ci.cancel();
    }
}
