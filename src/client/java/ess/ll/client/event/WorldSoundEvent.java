package ess.ll.client.event;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class WorldSoundEvent extends Event {
   public static final WorldSoundEvent INSTANCE = new WorldSoundEvent();
   private Vec3 position;
   private WorldSoundEvent.Type type;
   private float volume;
   private float reduction;

   public WorldSoundEvent.Type getType() {
      return this.type;
   }

   public Vec3 getPosition() {
      return this.position;
   }

   public void setType(WorldSoundEvent.Type type) {
      this.type = type;
   }

   public float fireVolume(WorldSoundEvent.Type type, float volume, Vec3 vec) {
      if (type == null) {
         return volume;
      }

      this.position = vec;
      this.type = type;
      this.volume = volume;
      this.reduction = 1.0F;
      super.dispatch();
      return this.consumeCancelled() ? volume * (1.0F - Mth.clamp(this.reduction, 0.0F, 1.0F)) : volume;
   }

   public float getReduction() {
      return this.reduction;
   }

   public float getVolume() {
      return this.volume;
   }

   public void setPosition(Vec3 vec) {
      this.position = vec;
   }

   public SimpleSoundInstance createSound(
      WorldSoundEvent.Type type, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, RandomSource randomSource, double x, double y, double z
   ) {
      float adjustedVolume = this.fireVolume(type, volume, new Vec3(x, y, z));
      return new SimpleSoundInstance(soundEvent, soundSource, adjustedVolume, pitch, randomSource, x, y, z);
   }

   public void setVolume(float volume) {
      this.volume = volume;
   }

   public void setReduction(float reduction) {
      this.reduction = reduction;
   }

   public enum Type {
      WATER(SoundEvents.WATER_AMBIENT),
      WATER_DRIP(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER),
      DRAGON_SUMMON(SoundEvents.ENDER_DRAGON_GROWL, SoundEvents.ENDER_DRAGON_DEATH, SoundEvents.ENDER_DRAGON_AMBIENT),
      WITHER_RELEASED(SoundEvents.WITHER_SPAWN),
      FIREWORK(SoundEvents.FIREWORK_ROCKET_LAUNCH),
      EXPERIENCE_BOTTLE(SoundEvents.EXPERIENCE_BOTTLE_THROW);

      private final SoundEvent[] sounds;

      Type(SoundEvent... sounds) {
         this.sounds = sounds;
      }

      public static WorldSoundEvent.Type of(SoundEvent soundEvent) {
         if (soundEvent == null) {
            return null;
         }

         for (WorldSoundEvent.Type type : values()) {
            for (SoundEvent soundEvent2 : type.sounds) {
               if (soundEvent2.equals(soundEvent)) {
                  return type;
               }
            }
         }

         return null;
      }
   }
}
