package ess.ll.client.mixin;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundContainerSetSlotPacket.class)
public interface ClientboundContainerSetSlotPacketMixin {
    @Mutable
    @Accessor("itemStack")
    void setItem(ItemStack stack);
}
