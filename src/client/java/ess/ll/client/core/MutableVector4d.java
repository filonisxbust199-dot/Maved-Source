package ess.ll.client.core;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector2dc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4dc;
import org.joml.Vector4fc;
import org.joml.Vector4ic;

public class MutableVector4d {
   public double x;
   public double y;
   public double z;
   private static final double NEGATE = -1.0;

   public MutableVector4d set(Vector4dc vector4dc) {
      this.x = vector4dc.x();
      this.y = vector4dc.y();
      this.z = vector4dc.z();
      return this;
   }

   public double getZ() {
      return this.z;
   }

   public MutableVector4d set(Vector4ic vector4ic) {
      this.x = vector4ic.x();
      this.y = vector4ic.y();
      this.z = vector4ic.z();
      return this;
   }

   public MutableVector4d set(float[] values) {
      this.x = values[0];
      this.y = values[1];
      this.z = values[2];
      return this;
   }

   public MutableVector4d multiply(MutableVector4d mutableVector4d) {
      return this.multiply(mutableVector4d.x, mutableVector4d.y, mutableVector4d.z);
   }

   public MutableVector4d set(Vector3fc vector3fc) {
      this.x = vector3fc.x();
      this.y = vector3fc.y();
      this.z = vector3fc.z();
      return this;
   }

   public MutableVector4d set(double value) {
      this.x = value;
      this.y = value;
      this.z = value;
      return this;
   }

   public MutableVector4d set(Vector2dc vector2dc, double z) {
      this.x = vector2dc.x();
      this.y = vector2dc.y();
      this.z = z;
      return this;
   }

   public MutableVector4d set(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public MutableVector4d set(Vec3 vec) {
      this.x = vec.x();
      this.y = vec.y();
      this.z = vec.z();
      return this;
   }

   public MutableVector4d set(double[] values) {
      this.x = values[0];
      this.y = values[1];
      this.z = values[2];
      return this;
   }

   public MutableVector4d multiply(double x, double y, double z) {
      return new MutableVector4d(this.x * x, this.y * y, this.z * z);
   }

   public MutableVector4d set(Vector3ic vector3ic) {
      this.x = vector3ic.x();
      this.y = vector3ic.y();
      this.z = vector3ic.z();
      return this;
   }

   public MutableVector4d subtract(double x, double y, double z) {
      return this.add(-x, -y, -z);
   }

   public MutableVector4d negate() {
      return this.multiply(NEGATE);
   }

   public MutableVector4d multiply(double factor) {
      return this.multiply(factor, factor, factor);
   }

   public MutableVector4d add(double value) {
      return this.add(value, value, value);
   }

   public MutableVector4d() {
   }

   public MutableVector4d(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public MutableVector4d subtract(double value) {
      return this.subtract(value, value, value);
   }

   public MutableVector4d set(Vector4fc vector4fc) {
      this.x = vector4fc.x();
      this.y = vector4fc.y();
      this.z = vector4fc.z();
      return this;
   }

   public MutableVector4d horizontal() {
      return new MutableVector4d(this.x, 0.0, this.z);
   }

   public double getX() {
      return this.x;
   }

   public MutableVector4d add(double x, double y, double z) {
      return new MutableVector4d(this.x + x, this.y + y, this.z + z);
   }

   public MutableVector4d set(Vector2fc vector2fc, double z) {
      this.x = vector2fc.x();
      this.y = vector2fc.y();
      this.z = z;
      return this;
   }

   public MutableVector4d set(Vector3dc vector3dc) {
      if (vector3dc == this) {
         return this;
      }

      this.x = vector3dc.x();
      this.y = vector3dc.y();
      this.z = vector3dc.z();
      return this;
   }

   public MutableVector4d subtract(MutableVector4d mutableVector4d) {
      return this.subtract(mutableVector4d.x, mutableVector4d.y, mutableVector4d.z);
   }

   public MutableVector4d set(Vector2ic vector2ic, double z) {
      this.x = vector2ic.x();
      this.y = vector2ic.y();
      this.z = z;
      return this;
   }

   public double getY() {
      return this.y;
   }

   public MutableVector4d(double value) {
      this.x = value;
      this.y = value;
      this.z = value;
   }

   public MutableVector4d add(MutableVector4d mutableVector4d) {
      return this.add(mutableVector4d.x, mutableVector4d.y, mutableVector4d.z);
   }
}
