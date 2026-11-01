package ess.ll.client.util;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.network.PacketEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.phys.BlockHitResult;

public enum ServerLatencyTracker implements MinecraftAccess {
   INSTANCE;

   private final Map<Long, Long> pendingBlocks = new ConcurrentHashMap<>();
   private final Map<Integer, Long> pendingAttacks = new ConcurrentHashMap<>();
   private final Map<Integer, Long> pendingSlots = new ConcurrentHashMap<>();
   private final int[] samples = new int[]{0, 0, 0};
   private volatile long useItemSentMs;
   private volatile long latency;
   private volatile long latencyUpdatedMs;
   private volatile int tabLatency;
   private int sampleIndex;
   private boolean hasSamples;

   public void init() {
      EventBus.register(this);
   }

   public long getLatency() {
      if (mc.isLocalServer()) {
         return 0L;
      }

      long measured = this.latency;
      if (measured > 0L && System.currentTimeMillis() - this.latencyUpdatedMs <= 10000L) {
         return measured;
      }

      int tab = this.tabLatency;
      return tab > 0 ? tab : measured;
   }

   public int getLatencyTicks() {
      return (int)(this.getLatency() / 50L);
   }

   @Subscribe
   public void onPacket(PacketEvent packetEvent) {
      if (packetEvent.isSend()) {
         this.onSend(packetEvent.getPacket());
      } else {
         this.onReceive(packetEvent.getPacket());
      }
   }

   private void onSend(Packet<?> packet) {
      if (packet instanceof ServerboundUseItemOnPacket serverboundUseItemOnPacket) {
         BlockHitResult blockHitResult = serverboundUseItemOnPacket.hitResult();
         BlockPos blockPos = blockHitResult.getBlockPos();
         remember(this.pendingBlocks, blockPos.asLong());
         remember(this.pendingBlocks, blockPos.relative(blockHitResult.getDirection()).asLong());
      } else if (packet instanceof ServerboundPlayerActionPacket serverboundPlayerActionPacket) {
         this.onPlayerAction(serverboundPlayerActionPacket);
      } else if (packet instanceof ServerboundUseItemPacket) {
         this.useItemSentMs = System.currentTimeMillis();
      } else if (packet instanceof ServerboundAttackPacket serverboundAttackPacket) {
         remember(this.pendingAttacks, serverboundAttackPacket.entityId());
      }
   }

   private void onPlayerAction(ServerboundPlayerActionPacket serverboundPlayerActionPacket) {
      switch (serverboundPlayerActionPacket.getAction()) {
         case START_DESTROY_BLOCK:
         case STOP_DESTROY_BLOCK:
            remember(this.pendingBlocks, serverboundPlayerActionPacket.getPos().asLong());
            break;
         case DROP_ITEM:
         case DROP_ALL_ITEMS:
            this.onDropItem();
            break;
         case SWAP_ITEM_WITH_OFFHAND:
            remember(this.pendingSlots, 45);
      }
   }

   private void onDropItem() {
      LocalPlayer player = mc.player;
      if (player != null) {
         remember(this.pendingSlots, 36 + player.getInventory().getSelectedSlot());
      }
   }

   private void onReceive(Packet<?> packet) {
      if (packet instanceof ClientboundBlockUpdatePacket clientboundBlockUpdatePacket) {
         this.confirm(this.pendingBlocks, clientboundBlockUpdatePacket.getPos().asLong());
      } else if (packet instanceof ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket) {
         if (!this.pendingBlocks.isEmpty()) {
            clientboundSectionBlocksUpdatePacket.runUpdates((pos, state) -> this.confirm(this.pendingBlocks, pos.asLong()));
         }
      } else if (packet instanceof ClientboundDamageEventPacket clientboundDamageEventPacket) {
         this.confirm(this.pendingAttacks, clientboundDamageEventPacket.entityId());
      } else if (packet instanceof ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket) {
         if (clientboundContainerSetSlotPacket.getContainerId() == 0) {
            this.confirm(this.pendingSlots, clientboundContainerSetSlotPacket.getSlot());
         }
      } else if (packet instanceof ClientboundAddEntityPacket clientboundAddEntityPacket) {
         this.onAddEntity(clientboundAddEntityPacket);
      } else if (packet instanceof ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket) {
         this.onPlayerInfo(clientboundPlayerInfoUpdatePacket);
      } else if (packet instanceof ClientboundLoginPacket) {
         this.reset();
      }
   }

   private void onAddEntity(ClientboundAddEntityPacket clientboundAddEntityPacket) {
      long sentMs = this.useItemSentMs;
      if (sentMs != 0L) {
         LocalPlayer player = mc.player;
         if (player != null && clientboundAddEntityPacket.getData() == player.getId()) {
            this.useItemSentMs = 0L;
            this.addSample(System.currentTimeMillis() - sentMs);
         }
      }
   }

   private void onPlayerInfo(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket) {
      if (clientboundPlayerInfoUpdatePacket.actions().contains(Action.UPDATE_LATENCY)) {
         UUID profileId = mc.getUser().getProfileId();

         for (var entry : clientboundPlayerInfoUpdatePacket.entries()) {
            if (entry.profileId().equals(profileId)) {
               if (entry.latency() > 0) {
                  this.tabLatency = entry.latency();
               }

               return;
            }
         }
      }
   }

   private static <K> void remember(Map<K, Long> map, K key) {
      if (map.size() > 64) {
         map.clear();
      }

      map.put(key, System.currentTimeMillis());
   }

   private <K> void confirm(Map<K, Long> map, K key) {
      Long sentMs = (Long)map.remove(key);
      if (sentMs != null) {
         this.addSample(System.currentTimeMillis() - sentMs);
      }
   }

   private void addSample(long ms) {
      if (ms >= 0L && ms <= 1000L) {
         int sample = (int)ms;
         if (!this.hasSamples) {
            this.samples[0] = sample;
            this.samples[1] = sample;
            this.samples[2] = sample;
            this.hasSamples = true;
         } else {
            this.samples[this.sampleIndex] = sample;
            this.sampleIndex = (this.sampleIndex + 1) % this.samples.length;
         }

         this.latency = median(this.samples[0], this.samples[1], this.samples[2]);
         this.latencyUpdatedMs = System.currentTimeMillis();
      }
   }

   private void reset() {
      this.pendingBlocks.clear();
      this.pendingAttacks.clear();
      this.pendingSlots.clear();
      this.useItemSentMs = 0L;
      this.latency = 0L;
      this.latencyUpdatedMs = 0L;
      this.tabLatency = 0;
      this.sampleIndex = 0;
      this.hasSamples = false;
   }

   private static int median(int a, int b, int c) {
      return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
   }
}
