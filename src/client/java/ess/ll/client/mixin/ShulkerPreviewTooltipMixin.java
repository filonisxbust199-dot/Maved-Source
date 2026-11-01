package ess.ll.client.mixin;

import ess.ll.client.module.misc.ShulkerPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ShulkerPreview: the slot window draws itself; every other tooltip image goes the vanilla way. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class ShulkerPreviewTooltipMixin {
    @Redirect(method={"lambda$setTooltipForNextFrame$0","lambda$setTooltipForNextFrame$1"},at=@At(value="INVOKE",
        target="Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;"))
    private static ClientTooltipComponent ess$shulkerWindow(TooltipComponent component) {
        return component instanceof ShulkerPreview.Contents contents ? ShulkerPreview.client(contents) : ClientTooltipComponent.create(component);
    }
}
