package ess.ll.client.mixin;

import ess.ll.client.module.misc.CreamyKeys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CreamyKeys: every key press, in game and in screens, plays a switch sound. */
@Mixin(KeyboardHandler.class)
public class CreamyKeysMixin {
    @Inject(method="keyPress",at=@At("HEAD"))
    private void ess$creamyKey(long window,int action,KeyEvent event,CallbackInfo ci) {
        CreamyKeys.onKey(event.key(),action);
    }
}
