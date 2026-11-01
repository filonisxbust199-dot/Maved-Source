package ess.ll.client.mixin;

import ess.ll.client.screen.LoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Entering a world shows the ESS loading screen instead of the chunk map. Portal travel keeps the vanilla
 * portal backdrop, since it appears mid-game for a moment.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class WorldLoadingScreensMixin {
    @Shadow private LevelLoadTracker loadTracker;
    @Shadow private LevelLoadingScreen.Reason reason;
    @Unique private float ess$progress;

    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void ess$draw(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        if(reason!=LevelLoadingScreen.Reason.OTHER) return;
        ci.cancel();
        boolean known=loadTracker!=null && loadTracker.hasProgress();
        if(known) ess$progress+=(loadTracker.serverProgress()-ess$progress)*.15f;
        String caption=loadTracker!=null && loadTracker.isLevelReady() ? "ГОТОВО"
                : known ? "ЗАГРУЗКА МИРА · "+Math.round(ess$progress*100)+"%" : "ЗАГРУЗКА МИРА";
        LoadingScreen.draw(g,LoadingScreen.timeline(),known ? ess$progress : -1,caption.toUpperCase(Locale.ROOT));
    }
    @Inject(method="extractBackground",at=@At("HEAD"),cancellable=true)
    private void ess$background(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        if(reason==LevelLoadingScreen.Reason.OTHER) ci.cancel();
    }
}
