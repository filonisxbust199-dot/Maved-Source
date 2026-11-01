package ess.ll.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A click as the screen itself makes it: the creative inventory handles its slots its own way. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerSlotClickInvoker {
    @Invoker("slotClicked") void ess$slotClicked(Slot slot,int slotId,int button,ContainerInput input);
}
