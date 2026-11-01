package ess.ll.client.event;

public class SelectedSlotEvent extends Event {
   public static final SelectedSlotEvent INSTANCE = new SelectedSlotEvent();
   private int slot;

   public int getSlot() {
      return this.slot;
   }

   public void fire(int slot) {
      this.slot = slot;
      super.dispatch();
   }
}
