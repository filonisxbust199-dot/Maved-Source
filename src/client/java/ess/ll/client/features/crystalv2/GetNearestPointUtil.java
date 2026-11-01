package ess.ll.client.features.crystalv2;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class GetNearestPointUtil implements V2Access {
   public static Vec3 getNearestPoint(AABB box, double expand) {
      Vec3 eye = mc.player.getEyePosition();
      double x = Math.max(box.minX + expand, Math.min(eye.x, box.maxX - expand));
      double y = Math.max(box.minY + expand, Math.min(eye.y, box.maxY - expand));
      double z = Math.max(box.minZ + expand, Math.min(eye.z, box.maxZ - expand));
      return new Vec3(x, y, z);
   }

   @NotNull
   public static Vec3 cutBoxToVector(double x, double y, double z, @NotNull AABB box) {
      return new Vec3(Mth.clamp(x, box.minX, box.maxX), Mth.clamp(y, box.minY, box.maxY), Mth.clamp(z, box.minZ, box.maxZ));
   }

   public GetNearestPointUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
