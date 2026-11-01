package ess.ll.client.mixin;

import ess.ll.client.screen.LoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** "Preparing world", "Saving world" and similar waits: the ESS loading screen with the message as its caption. */
@Mixin(GenericMessageScreen.class)
public abstract class MessageScreenMixin extends Screen {
    protected MessageScreenMixin(Component title) { super(title); }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        LoadingScreen.draw(g,LoadingScreen.timeline(),-1,getTitle().getString().toUpperCase(Locale.ROOT));
    }
    @Inject(method="extractBackground",at=@At("HEAD"),cancellable=true)
    private void ess$background(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci) { ci.cancel(); }
}
