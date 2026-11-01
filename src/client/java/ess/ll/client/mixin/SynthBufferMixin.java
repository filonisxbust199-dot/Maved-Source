package ess.ll.client.mixin;

import com.mojang.blaze3d.audio.SoundBuffer;
import ess.ll.client.sound.UiSynth;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;

/** Serves ess-best:synth/* sounds from UiSynth instead of decoding .ogg files. */
@Mixin(SoundBufferLibrary.class)
public abstract class SynthBufferMixin {
    @Inject(method="getCompleteBuffer",at=@At("HEAD"),cancellable=true)
    private void ess$synth(Identifier path,CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        if(!path.getNamespace().equals("ess-best") || !UiSynth.isSynth(path.getPath())) return;
        SoundBuffer buffer=UiSynth.buffer(path.getPath());
        if(buffer!=null) cir.setReturnValue(CompletableFuture.completedFuture(buffer));
    }
}
