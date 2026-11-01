package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * ItemScroller: moving items between a container and the inventory without clicking every stack. The wheel over
 * a stack moves one item to the other side (with Shift the whole stack); holding Shift and the left button while
 * dragging moves every stack the cursor passes over.
 */
public final class ItemScroller {
    public static final BooleanSetting SCROLL=new BooleanSetting("Прокрутка колёсиком",true);
    public static final BooleanSetting DRAG=new BooleanSetting("Shift + перетаскивание",true);
    public static final Setting[] SETTINGS={SCROLL,DRAG};

    /**
     * Slots already moved during the current Shift drag. Kept by identity: in the creative inventory every slot of
     * the inventory tab has index 0.
     */
    private static final Set<Slot> DRAGGED=Collections.newSetFromMap(new IdentityHashMap<>());

    private ItemScroller() {}

    public static boolean enabled() { return ClientModules.ITEM_SCROLLER!=null && ClientModules.ITEM_SCROLLER.enabled(); }

    /** Wheel over a slot: true when it moved something, so the screen does not scroll. */
    public static boolean scroll(AbstractContainerScreen<?> screen,Slot slot,double amount) {
        var mc=Minecraft.getInstance();
        if(!enabled() || !SCROLL.get() || slot==null || !slot.hasItem() || amount==0 || mc.player==null || mc.gameMode==null) return false;
        if(!screen.getMenu().getCarried().isEmpty() || screen instanceof CreativeModeInventoryScreen) return false;
        if(shift()) { quickMove(screen,slot); return true; }
        Slot target=target(screen,slot);
        if(target==null) return false;
        int id=screen.getMenu().containerId;
        // Pick the stack up, drop one item on the other side, put the rest back.
        mc.gameMode.handleContainerInput(id,slot.index,0,ContainerInput.PICKUP,mc.player);
        mc.gameMode.handleContainerInput(id,target.index,1,ContainerInput.PICKUP,mc.player);
        mc.gameMode.handleContainerInput(id,slot.index,0,ContainerInput.PICKUP,mc.player);
        return true;
    }

    /** Shift + left drag: each stack under the cursor moves once. */
    public static boolean drag(AbstractContainerScreen<?> screen,Slot slot,int button,boolean shift) {
        var mc=Minecraft.getInstance();
        if(!enabled() || !DRAG.get() || button!=0 || !shift || slot==null || mc.player==null || mc.gameMode==null) return false;
        if(!screen.getMenu().getCarried().isEmpty()) return false;
        if(slot.hasItem() && DRAGGED.add(slot)) quickMove(screen,slot);
        return true;
    }
    public static void release() { DRAGGED.clear(); }

    /** Vanilla's own Shift state: it is the one kept up to date while a screen is open. */
    private static boolean shift() { return Minecraft.getInstance().hasShiftDown(); }
    /**
     * A Shift click made by the screen itself, like a real one: it goes to the screen's own menu (the player's open
     * container can still be the last one for a moment), and the creative inventory, whose slots do not go through
     * container clicks, moves the stack its own way.
     */
    private static void quickMove(AbstractContainerScreen<?> screen,Slot slot) {
        ((ess.ll.client.mixin.ContainerSlotClickInvoker)screen).ess$slotClicked(slot,slot.index,0,ContainerInput.QUICK_MOVE);
    }

    /**
     * Sides a slot belongs to: the container's own slots, the hotbar, the main inventory, and the rest (armour,
     * offhand, crafting) that never trades.
     * Over a container the other side of any inventory slot is the container (and back); in the player's own
     * inventory, where there is no container, the hotbar and the main inventory trade items.
     */
    private static int side(Slot slot) {
        // The 2x2 crafting grid and its result are not a side to move into.
        if(slot.container instanceof net.minecraft.world.inventory.CraftingContainer || slot.container instanceof net.minecraft.world.inventory.ResultContainer) return 3;
        if(!(slot.container instanceof Inventory)) return 0;
        int i=slot.getContainerSlot();
        return i<9 ? 1 : i<36 ? 2 : 3;
    }
    private static boolean otherSide(Slot from,Slot to,boolean container) {
        int a=side(from),b=side(to);
        if(a==3 || b==3) return false;
        if(container) return (a==0)!=(b==0);
        return a!=b;
    }

    /** A slot on the other side that takes one more of this item: a matching stack with room first, then an empty one. */
    private static Slot target(AbstractContainerScreen<?> screen,Slot from) {
        ItemStack stack=from.getItem();
        var slots=screen.getMenu().slots;
        boolean container=slots.stream().anyMatch(s->side(s)==0);
        Slot empty=null;
        for(Slot s:slots) {
            if(s==from || !otherSide(from,s,container) || !s.mayPlace(stack)) continue;
            ItemStack there=s.getItem();
            if(there.isEmpty()) { if(empty==null) empty=s; }
            else if(ItemStack.isSameItemSameComponents(there,stack) && there.getCount()<s.getMaxStackSize(there)) return s;
        }
        return empty;
    }
}
