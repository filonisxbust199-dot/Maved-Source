package ess.ll.client.mixin;

import ess.ll.client.module.misc.PvpSafe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PvpSafe: the pause-menu disconnect button is greyed out during PvP. */
@Mixin(PauseScreen.class)
public class PvpSafePauseScreenMixin {
    @Shadow private Button disconnectButton;

    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void ess$disableExit(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        if(disconnectButton!=null) disconnectButton.active=!PvpSafe.isPvpProtectionActive();
    }
}
