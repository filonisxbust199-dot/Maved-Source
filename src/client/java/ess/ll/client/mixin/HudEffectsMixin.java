package ess.ll.client.mixin;

import ess.ll.client.module.ClientModules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Potions module replaces the vanilla effect icons in the top-right corner. */
@Mixin(Hud.class)
public abstract class HudEffectsMixin {
    @Inject(method="extractEffects",at=@At("HEAD"),cancellable=true)
    private void ess$hideEffects(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci) {
        if(ClientModules.hud("Potions")) ci.cancel();
    }
}
