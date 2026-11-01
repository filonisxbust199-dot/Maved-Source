package ess.ll.client.util;

public class ElapsedTimer {
   public long time = System.currentTimeMillis();

   public boolean hasElapsed(double ms) {
      return System.currentTimeMillis() - ms >= this.time;
   }

   public void setTime(long time) {
      this.time = time;
   }

   public boolean isPast() {
      return this.time < System.currentTimeMillis();
   }

   public long elapsed() {
      return System.currentTimeMillis() - this.time;
   }

   public void reset() {
      this.time = System.currentTimeMillis();
   }

   public long getTime() {
      return this.time;
   }

   public void setDelay(long ms) {
      this.time = System.currentTimeMillis() + ms;
   }

   public boolean isPending() {
      return System.currentTimeMillis() - this.time <= 0L;
   }

   public boolean resetIfElapsed(double ms) {
      boolean elapsed = this.hasElapsed(ms);
      if (elapsed) {
         this.reset();
      }

      return elapsed;
   }

   public boolean isOlderThan(long ms) {
      return System.currentTimeMillis() - this.time > ms;
   }
}
