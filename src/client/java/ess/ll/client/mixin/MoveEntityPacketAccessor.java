package ess.ll.client.mixin;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Bots: the entity a relative move belongs to, without a level to look it up in. */
@Mixin(ClientboundMoveEntityPacket.class)
public interface MoveEntityPacketAccessor {
    @Accessor("entityId") int ess$entityId();
}
