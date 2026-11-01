package ess.ll.client.mixin;

import ess.ll.client.dev.HudProfile;
import java.util.List;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev: GUI renderer timing for HudProfile (does nothing unless -Dess.hudprofile=true). */
@Mixin(GuiRenderer.class)
public abstract class HudProfileMixin {
    @Shadow @Final private List<?> draws;
    @Inject(method="render",at=@At("HEAD"))
    private void ess$start(CallbackInfo ci) { HudProfile.guiStart(); }
    @Inject(method="draw",at=@At("HEAD"))
    private void ess$draws(CallbackInfo ci) { HudProfile.drawCount(draws.size()); }
    @Inject(method="render",at=@At("TAIL"))
    private void ess$end(CallbackInfo ci) { HudProfile.guiEnd(0); }
}
