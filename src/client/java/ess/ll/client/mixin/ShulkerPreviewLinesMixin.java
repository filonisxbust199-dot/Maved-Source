package ess.ll.client.mixin;

import ess.ll.client.module.misc.ShulkerPreview;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ShulkerPreview: no "contains ..." text under the slot window. */
@Mixin(ItemContainerContents.class)
public abstract class ShulkerPreviewLinesMixin {
    @Inject(method="addToTooltip",at=@At("HEAD"),cancellable=true)
    private void ess$hideLines(Item.TooltipContext context,Consumer<Component> lines,TooltipFlag flag,DataComponentGetter components,CallbackInfo ci) {
        if(ShulkerPreview.hidesContentLines()) ci.cancel();
    }
}
