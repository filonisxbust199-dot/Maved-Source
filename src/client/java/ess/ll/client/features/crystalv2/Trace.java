package ess.ll.client.features.crystalv2;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Trace(@NotNull Vec3 origin, @NotNull Vec3 direction) implements Cloneable {
   public Trace(@NotNull Player player, double x, double y, double z, float xRot, float yRot) {
      this(new Vec3(x, y, z), calculateDirection(player, xRot, yRot));
   }

   @NotNull
   public static Vec3 calculateDirection(@NotNull Player player, float xRot, float yRot) {
      float rotX = (float)Math.toRadians(xRot);
      float rotY = (float)Math.toRadians(yRot);
      double xz = Mth.cos(rotY);
      return new Vec3(-xz * Mth.sin(rotX), -Mth.sin(rotY), xz * Mth.cos(rotX));
   }

   @Contract(" -> this")
   @NotNull
   public Trace clone() {
      return this;
   }

   @NotNull
   public Vec3 getPointAtDistance(double distance) {
      return this.origin.add(this.direction.scale(distance));
   }

   @NotNull
   public Pair<Vec3, Vec3> closestPointsBetweenLines(@NotNull Trace other) {
      Vec3 n1 = this.direction.cross(other.direction.cross(this.direction));
      Vec3 n2 = other.direction.cross(this.direction.cross(other.direction));
      Vec3 c1 = this.origin.add(this.direction.scale(other.origin.subtract(this.origin).dot(n2) / this.direction.dot(n2)));
      Vec3 c2 = other.origin.add(other.direction.scale(this.origin.subtract(other.origin).dot(n1) / other.direction.dot(n1)));
      return new Pair(c1, c2);
   }
}
