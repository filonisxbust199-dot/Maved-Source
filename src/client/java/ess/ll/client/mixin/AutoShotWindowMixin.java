package ess.ll.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Autoshot runs keep out of the way: the window opens without taking focus, past the edge of the screen, so it
 * covers nothing the user is working in. It still renders (screenshots read the game's own framebuffer), and the
 * game treats it as focused, so tests neither pause on lost focus nor lose mouse input.
 * -Dess.autoshot.foreground=true shows the window as usual, to watch a run.
 */
@Mixin(Window.class)
public abstract class AutoShotWindowMixin {
    @Shadow public abstract long handle();

    private static boolean ess$background() {
        return Boolean.getBoolean("ess.autoshot") && !Boolean.getBoolean("ess.autoshot.foreground");
    }

    @Inject(method="createWindow",at=@At("HEAD"))
    private void ess$noActivate(CallbackInfoReturnable<Long> cir) {
        if(ess$background()) SDLHints.SDL_SetHint("SDL_WINDOW_ACTIVATE_WHEN_SHOWN","0");
    }

    @Inject(method="<init>",at=@At("TAIL"))
    private void ess$offScreen(CallbackInfo ci) {
        if(ess$background()) SDLVideo.SDL_SetWindowPosition(handle(),-20000,-20000);
    }

    @Inject(method="isFocused",at=@At("HEAD"),cancellable=true)
    private void ess$focused(CallbackInfoReturnable<Boolean> cir) {
        if(ess$background()) cir.setReturnValue(true);
    }
}
