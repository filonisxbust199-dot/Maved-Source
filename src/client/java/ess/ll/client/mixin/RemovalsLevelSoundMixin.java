package ess.ll.client.mixin;

import ess.ll.client.event.WorldSoundEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelEventHandler.class)
public abstract class RemovalsLevelSoundMixin {
    @Redirect(method="levelEvent",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void ess$experienceBottleSound(ClientLevel level,BlockPos pos,SoundEvent sound,SoundSource source,
            float volume,float pitch,boolean distanceDelay,int event,BlockPos origin,int data) {
        if(event==2002 && data==0 && sound.equals(SoundEvents.SPLASH_POTION_BREAK))
            volume=WorldSoundEvent.INSTANCE.fireVolume(WorldSoundEvent.Type.EXPERIENCE_BOTTLE,volume,Vec3.atCenterOf(pos));
        level.playLocalSound(pos,sound,source,volume,pitch,distanceDelay);
    }
}
