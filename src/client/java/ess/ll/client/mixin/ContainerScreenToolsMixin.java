package ess.ll.client.mixin;

import ess.ll.client.module.misc.ItemScroller;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ItemScroller's wheel and Shift drag over container slots. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenToolsMixin {
    @Shadow protected Slot hoveredSlot;
    @Shadow protected abstract Slot getHoveredSlot(double x,double y);

    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void ess$scroll(double x,double y,double scrollX,double scrollY,CallbackInfoReturnable<Boolean> cir) {
        if(ItemScroller.scroll((AbstractContainerScreen<?>)(Object)this,getHoveredSlot(x,y),scrollY)) cir.setReturnValue(true);
    }

    @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true)
    private void ess$drag(MouseButtonEvent event,double dx,double dy,CallbackInfoReturnable<Boolean> cir) {
        if(ItemScroller.drag((AbstractContainerScreen<?>)(Object)this,getHoveredSlot(event.x(),event.y()),event.button(),event.hasShiftDown())) cir.setReturnValue(true);
    }

    @Inject(method="mouseReleased",at=@At("HEAD"))
    private void ess$release(MouseButtonEvent event,CallbackInfoReturnable<Boolean> cir) { ItemScroller.release(); }
}
