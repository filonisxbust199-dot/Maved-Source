package ess.ll.client.mixin;

import ess.ll.client.module.misc.ShulkerPreview;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ShulkerPreview: a shulker box's tooltip image is its slot window. */
@Mixin(ItemStack.class)
public abstract class ShulkerPreviewMixin {
    @Inject(method="getTooltipImage",at=@At("HEAD"),cancellable=true)
    private void ess$shulkerImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        var image=ShulkerPreview.image((ItemStack)(Object)this);
        if(image.isPresent()) cir.setReturnValue(image);
    }
}
