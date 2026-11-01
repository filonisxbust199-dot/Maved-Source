package ess.ll.client.mixin;

import ess.ll.client.sound.UiSynth;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Synth sounds have no backing file; skip the existence check so their events still register. */
@Mixin(SoundManager.class)
public abstract class SynthRegistrationMixin {
    @Inject(method="validateSoundResource",at=@At("HEAD"),cancellable=true)
    private static void ess$synthExists(Sound sound,Identifier event,ResourceProvider resources,CallbackInfoReturnable<Boolean> cir) {
        Identifier path=sound.getPath();
        if(path.getNamespace().equals("ess-best") && UiSynth.isSynth(path.getPath())) cir.setReturnValue(true);
    }
}
