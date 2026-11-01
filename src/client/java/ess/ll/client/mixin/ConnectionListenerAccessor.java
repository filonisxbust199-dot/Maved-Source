package ess.ll.client.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Bots: switching accounts swaps the listener of a connection that is already in play. The protocol stays the
 * same, so only the listener changes; going through setupInboundProtocol would rebuild a configured pipeline.
 */
@Mixin(Connection.class)
public interface ConnectionListenerAccessor {
    @Accessor("packetListener") void ess$setPacketListener(PacketListener listener);
    @Accessor("disconnectListener") void ess$setDisconnectListener(PacketListener listener);
}
