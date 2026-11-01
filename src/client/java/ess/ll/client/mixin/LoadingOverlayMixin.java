package ess.ll.client.mixin;

import ess.ll.client.screen.LoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the Mojang loading screen's drawing with the ESS one. Vanilla's tick still finishes the reload;
 * this keeps its fades (the screen behind shows while fading) and holds the fade-out until the logo intro has played.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Unique private long ess$start=-1;

    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void ess$draw(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        ci.cancel();
        long now=Util.getMillis();
        if(ess$start<0) ess$start=now;
        if(fadeIn && fadeInStart==-1) fadeInStart=now;
        float out=fadeOutStart>-1 ? (now-fadeOutStart)/600f : -1;
        float in=fadeInStart>-1 ? (now-fadeInStart)/400f : -1;
        var screen=minecraft.gui.screen();
        float alpha;
        if(out>=0) {
            if(screen!=null) { screen.extractRenderStateWithTooltipAndSubtitles(g,mouseX,mouseY,delta); g.nextStratum(); }
            alpha=1-Math.clamp(out,0,1);
        } else if(fadeIn) {
            if(screen!=null && in<1) { screen.extractRenderStateWithTooltipAndSubtitles(g,mouseX,mouseY,delta); g.nextStratum(); }
            alpha=Math.clamp(in,0,1);
        } else alpha=1;
        currentProgress=Math.clamp(currentProgress*.95f+reload.getActualProgress()*.05f,0,1);
        LoadingScreen.draw(g,(now-ess$start)/1000.0,currentProgress,alpha);
        if(out>=1) minecraft.gui.setOverlay(null);
    }

    /** The logo needs about two seconds to assemble and light up before the screen may fade away. */
    @Inject(method="isReadyToFadeOut",at=@At("RETURN"),cancellable=true)
    private void ess$waitForIntro(CallbackInfoReturnable<Boolean> cir) {
        if(ess$start<0 || Util.getMillis()-ess$start<2300) cir.setReturnValue(false);
    }
}
