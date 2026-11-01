package ess.ll.client.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ContainerClickEvent extends Event {
   public static final ContainerClickEvent INSTANCE = new ContainerClickEvent();
   private Player player;
   private int containerId;
   private int slot;
   private int button;
   private ContainerInput input;

   public Player getPlayer() {
      return this.player;
   }

   public void setContainerId(int containerId) {
      this.containerId = containerId;
   }

   public void fire(int containerId, int slot, int button, ContainerInput containerInput, Player player, CallbackInfo ci) {
      this.containerId = containerId;
      this.slot = slot;
      this.button = button;
      this.input = containerInput;
      this.player = player;
      super.dispatch();
      if (this.consumeCancelled()) {
         ci.cancel();
      }
   }

   public void setPlayer(Player player) {
      this.player = player;
   }

   public ContainerInput getInput() {
      return this.input;
   }

   public void setSlot(int slot) {
      this.slot = slot;
   }

   public int getButton() {
      return this.button;
   }

   public void setInput(ContainerInput containerInput) {
      this.input = containerInput;
   }

   public void setButton(int button) {
      this.button = button;
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getSlot() {
      return this.slot;
   }
}
