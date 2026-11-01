package ess.ll.client.mixin;

import ess.ll.client.render.GlassRenderer;
import ess.ll.client.render.MenuScene;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void ess$glassBackdrop(CallbackInfo ci) {
        // The menu scene first, so glass in the menu blurs the scene rather than the empty main target.
        GlassRenderer.beforeGuiRender(MenuScene.beforeGuiRender());
    }
}
