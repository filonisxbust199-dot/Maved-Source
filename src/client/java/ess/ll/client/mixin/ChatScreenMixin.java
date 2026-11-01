package ess.ll.client.mixin;

import ess.ll.client.render.HudRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HUD editing is driven by the chat screen's own input (the path every screen, the ClickGUI included,
 * receives): clicks, releases and the cursor position reported to extractRenderState.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Component title) { super(title); }

    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void ess$hudPress(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir) {
        if(HudRenderer.chatPress(event.x(),event.y(),event.button())) cir.setReturnValue(true);
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        HudRenderer.chatCursor((int)event.x(),(int)event.y());
        if(HudRenderer.chatRelease(event.button())) return true;
        return super.mouseReleased(event);
    }
    @Override
    public void mouseMoved(double x,double y) {
        HudRenderer.chatCursor((int)x,(int)y);
        super.mouseMoved(x,y);
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event,double deltaX,double deltaY) {
        HudRenderer.chatCursor((int)event.x(),(int)event.y());
        return super.mouseDragged(event,deltaX,deltaY);
    }
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void ess$hudCursor(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        HudRenderer.chatCursor(mouseX,mouseY);
    }
    @Inject(method="onClose",at=@At("HEAD"))
    private void ess$hudClose(CallbackInfo ci) { HudRenderer.finishEditing(); }
}
