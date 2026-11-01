package ess.ll.client.mixin;

import ess.ll.client.event.TotemPopEvent;
import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class CombatTotemPopMixin {
    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;getEventId()B", ordinal = 0),cancellable=true)
    private void ess$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level != null && packet.getEventId() == 35) {
            var entity = packet.getEntity(level);
            if (entity != null) {
                TotemPopEvent.INSTANCE.fire(entity);
                if (VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.TOTEM,ci)) {
                    Minecraft.getInstance().particleEngine.createTrackingEmitter(entity,ParticleTypes.TOTEM_OF_UNDYING,30);
                    level.playLocalSound(entity.getX(),entity.getY(),entity.getZ(),SoundEvents.TOTEM_USE,
                        entity.getSoundSource(),1,1,false);
                }
            }
        }
    }
}
