package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ess.ll.client.event.NauseaStrengthEvent;
import ess.ll.client.event.VisualRemovalEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class RemovalsHudMixin {
    @Inject(method="displayScoreboardSidebar",at=@At("HEAD"),cancellable=true)
    private void ess$scoreboard(GuiGraphicsExtractor graphics,Objective objective,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.SCOREBOARD,ci);
    }
    @Inject(method="extractTitle",at=@At("HEAD"),cancellable=true)
    private void ess$overlayMessage(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.OVERLAY_MESSAGE,ci);
    }
    @Inject(method="extractEffects",at=@At("HEAD"),cancellable=true)
    private void ess$effects(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.MINECRAFT_EFFECTS,ci);
    }
    @Inject(method="extractSelectedItemName",at=@At("HEAD"),cancellable=true)
    private void ess$itemTitle(GuiGraphicsExtractor graphics,CallbackInfo ci) {
        VisualRemovalEvent.INSTANCE.cancelIfRemoved(VisualRemovalEvent.Type.ITEM_TITLES,ci);
    }
    @ModifyExpressionValue(method="extractCameraOverlays",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"),require=0)
    private float ess$nauseaOverlay(float strength) {
        return NauseaStrengthEvent.INSTANCE.fire(strength);
    }
}
