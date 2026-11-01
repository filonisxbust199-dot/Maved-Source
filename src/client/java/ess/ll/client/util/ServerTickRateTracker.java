package ess.ll.client.util;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.network.PacketEvent;
import java.util.Arrays;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Mth;

public enum ServerTickRateTracker implements MinecraftAccess {
   INSTANCE;

   private final float[] tickRates = new float[20];
   private int sampleCount = 0;
   private long lastTimeUpdateMs = -1L;
   private float lagAverage = 0.0F;

   public void init() {
      EventBus.register(this);
      Arrays.fill(this.tickRates, 0.0F);
   }

   public float getTps() {
      float count = 0.0F;
      float total = 0.0F;

      for (float rate : this.tickRates) {
         if (rate > 0.0F) {
            total += rate;
            count++;
         }
      }

      return Mth.clamp(total / count, 0.0F, 20.0F);
   }

   public void onTimeUpdate() {
      if (this.lastTimeUpdateMs != -1L) {
         float seconds = (float)(System.currentTimeMillis() - this.lastTimeUpdateMs) / 1000.0F;
         float rate = 20.0F / seconds;
         float clamped = Mth.clamp(rate, 0.0F, 20.0F);
         this.tickRates[this.sampleCount % this.tickRates.length] = clamped;
         this.sampleCount++;
         float missing = 20.0F - clamped;
         this.lagAverage = this.lagAverage * 0.9F + missing * 0.1F;
      }

      this.lastTimeUpdateMs = System.currentTimeMillis();
   }

   @Subscribe
   public void onPacket(PacketEvent packetEvent) {
      if (packetEvent.getPacket() instanceof ClientboundSetTimePacket) {
         this.onTimeUpdate();
      }
   }

   public float[] getTickRates() {
      return this.tickRates;
   }

   public int getSampleCount() {
      return this.sampleCount;
   }

   public long getLastTimeUpdateMs() {
      return this.lastTimeUpdateMs;
   }

   public float getLagAverage() {
      return this.lagAverage;
   }
}
