package ess.ll.client.util;

public final class TrailPoint {
   public double x;
   public double y;
   public double z;
   public float alpha = 1.0F;
   public final long time;

   public TrailPoint(double x, double y, double z, long time) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.time = time;
   }

   public void set(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }
}
