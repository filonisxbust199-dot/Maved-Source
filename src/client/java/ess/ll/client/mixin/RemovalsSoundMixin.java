package ess.ll.client.mixin;

import ess.ll.client.event.WorldSoundEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public abstract class RemovalsSoundMixin {
    @Redirect(method="playSound",at=@At(value="NEW",
        target="(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/util/RandomSource;DDD)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"))
    private SimpleSoundInstance ess$worldSound(SoundEvent sound,SoundSource source,float volume,float pitch,
            RandomSource random,double x,double y,double z) {
        return WorldSoundEvent.INSTANCE.createSound(WorldSoundEvent.Type.of(sound),
            sound,source,volume,pitch,random,x,y,z);
    }
}
