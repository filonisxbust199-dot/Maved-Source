package ess.ll.client.core;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.player.InventoryMove;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class TickTaskScheduler implements MinecraftAccess {
   private static TickTaskScheduler instance;
   private final TickTaskQueue queue = new TickTaskQueue();
   public static int freezeTicks;

   public static TickTaskScheduler getInstance() {
      return instance;
   }

   public static void setFreezeTicks(int ticks) {
      freezeTicks = ticks;
   }

   @Subscribe(0)
   private void onPlayerInput(PlayerInputEvent playerInputEvent) {
      instance.queue.tick();
      if (!ModuleManager.INSTANCE.inventoryMove().isEnabled()) freezeTicks = 0;
      if (freezeTicks > 0 && ModuleManager.INSTANCE.inventoryMove().isEnabled()) {
         playerInputEvent.stopMoving();
         playerInputEvent.setJump(false);
         playerInputEvent.setSneak(false);
         freezeTicks--;
      }
   }

   public static int getFreezeTicks() {
      return freezeTicks;
   }

   public TickTaskQueue getQueue() {
      return this.queue;
   }

   public static void runInInventory(Runnable task) {
      runInInventory(task, 1, true);
   }

   public static void runInInventory(Runnable task, int ticks, boolean closeAfter) {
      int containerId = mc.player.containerMenu.containerId;
      InventoryMove inventoryMove = ModuleManager.INSTANCE.inventoryMove();
      if (!inventoryMove.isEnabled() || !inventoryMove.isMoving() && !isJumpBypass(inventoryMove)) {
         task.run();
         if (closeAfter) {
            PlayerUtils.sendSilently(new ServerboundContainerClosePacket(containerId));
         }
      } else {
         TickTaskQueue tickTaskQueue = getInstance().getQueue();
         tickTaskQueue.clear();
         tickTaskQueue.addTickTask(0, () -> freezeTicks = inventoryMove.delayTicks(ticks), 0).addTickTask(1, () -> {
            task.run();
            if (closeAfter) {
               PlayerUtils.sendSilently(new ServerboundContainerClosePacket(containerId));
            }
         }, 0);
      }
   }

   public static void runInInventory(Runnable task, int ticks) {
      runInInventory(task, ticks, true);
   }

   private static boolean isJumpBypass(InventoryMove inventoryMove) {
      return inventoryMove.isEnabled() && inventoryMove.unusedOption.isEnabled() && mc.options.keyJump.isDown();
   }

   public TickTaskScheduler() {
      instance = this;
      EventBus.register(this);
   }
}
