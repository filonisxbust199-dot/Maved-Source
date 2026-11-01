package ess.ll.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.module.misc.BetterChest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BetterChest's bar over storage screens: the search box joins the screen's widgets (so it gets focus and typed
 * letters), the bar is drawn after the container, and while the box is focused keys stay in it.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class BetterChestMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Unique private EditBox ess$search;
    @Unique private boolean ess$bar;

    protected BetterChestMixin(Component title) { super(title); }

    @Unique private AbstractContainerScreen<?> ess$self() { return (AbstractContainerScreen<?>)(Object)this; }

    @Inject(method="init",at=@At("TAIL"))
    private void ess$init(CallbackInfo ci) {
        ess$bar=BetterChest.applies(ess$self());
        if(!ess$bar) { ess$search=null; return; }
        String kept=ess$search==null ? "" : ess$search.getValue();
        ess$search=BetterChest.SEARCH.get() ? addWidget(BetterChest.searchBox(leftPos,topPos,imageWidth)) : null;
        if(ess$search!=null) ess$search.setValue(kept);
    }

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void ess$render(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial,CallbackInfo ci) {
        // AucHelper frames the cheapest lots of any container, BetterChest bar or not.
        ess.ll.client.module.misc.AucHelper.render(g,ess$self(),leftPos,topPos);
        if(ess$bar && BetterChest.enabled()) BetterChest.render(g,ess$self(),ess$search,leftPos,topPos,imageWidth,mouseX,mouseY,partial);
    }

    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void ess$click(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir) {
        if(!ess$bar || !BetterChest.enabled()) return;
        if(ess$search!=null) {
            boolean inside=ess$search.isMouseOver(event.x(),event.y());
            ess$search.setFocused(inside);
            if(inside) { setFocused(ess$search); ess$search.mouseClicked(event,doubleClick); cir.setReturnValue(true); return; }
            if(getFocused()==ess$search) setFocused(null);
        }
        if(BetterChest.click(ess$self(),leftPos,topPos,imageWidth,event.x(),event.y())) cir.setReturnValue(true);
    }

    /** E, Q and hotbar keys type into a focused search instead of closing the chest; Esc leaves the field. */
    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void ess$key(KeyEvent event,CallbackInfoReturnable<Boolean> cir) {
        if(ess$search==null || !ess$search.isFocused()) return;
        if(event.key()==InputConstants.KEY_ESCAPE) { ess$search.setFocused(false); setFocused(null); cir.setReturnValue(true); return; }
        ess$search.keyPressed(event);
        cir.setReturnValue(true);
    }
}
