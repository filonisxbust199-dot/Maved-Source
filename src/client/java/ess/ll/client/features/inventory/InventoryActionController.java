package ess.ll.client.features.inventory;

import ess.ll.client.event.PlayerUpdateEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

public interface InventoryActionController {
    void useItem(InteractionHand interactionHand, Item item);

    void useItem(Item item);

    void use(InteractionHand interactionHand, Object var2, float var3, float var4);

    void useSlot(Slot slot, float var2, float var3);

    void useItem(Item item, float var2, float var3);

    InventoryActionSequence swapSlots(int var1, int var2);

    void useSlot(Slot slot);

    void abort();

    void onPlayerUpdate(PlayerUpdateEvent event);

    void useSlot(InteractionHand interactionHand, Slot slot);
}
