package ess.ll.client.mixin;

import ess.ll.client.util.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RW sprint reset relies on the server's entity-data update. */
@Mixin(ClientPacketListener.class)
public abstract class CombatSprintPacketMixin {
    @Inject(method="handleSetEntityData",at=@At("TAIL"))
    private void ess$serverSprintState(ClientboundSetEntityDataPacket packet,CallbackInfo ci) {
        var player=Minecraft.getInstance().player;
        if(player!=null && packet.id()==player.getId())
            ModuleManager.INSTANCE.autoSprint().handleEntityData(player,packet.packedItems());
    }
}
