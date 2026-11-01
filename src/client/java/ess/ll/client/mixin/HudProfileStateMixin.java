package ess.ll.client.mixin;

import ess.ll.client.dev.HudProfile;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev: GUI elements by pipeline for HudProfile. */
@Mixin(GuiRenderState.class)
public abstract class HudProfileStateMixin {
    @Inject(method="addGuiElement",at=@At("HEAD"))
    private void ess$element(GuiElementRenderState state,CallbackInfo ci) { HudProfile.element(state); }
}
