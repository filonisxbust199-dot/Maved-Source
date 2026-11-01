package ess.ll.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class EntityEyeDistanceUtils {
   public static float eyeDistanceToBox(Entity from, Entity target) {
      float halfWidth = target.getBbWidth() / 2.0F;
      float dx = (float)Mth.clamp(from.getX() - target.getX(), -halfWidth, halfWidth);
      float dy = (float)Mth.clamp(from.getEyePosition().y() - target.getY(), 0.0, target.getBbHeight());
      float dz = (float)Mth.clamp(from.getZ() - target.getZ(), -halfWidth, halfWidth);
      return (float)from.getEyePosition().distanceTo(target.position().add(dx, dy, dz));
   }

   private EntityEyeDistanceUtils() {
   }
}
