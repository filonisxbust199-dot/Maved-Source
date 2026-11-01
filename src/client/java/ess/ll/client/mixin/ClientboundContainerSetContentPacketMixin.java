package ess.ll.client.mixin;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundContainerSetContentPacket.class)
public interface ClientboundContainerSetContentPacketMixin {
    @Mutable
    @Accessor("items")
    void setItems(List<ItemStack> stacks);
}
