package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.HudNotifications;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * GriefJoiner: joins the chosen grief from the server's hub on its own. It uses the compass from the inventory,
 * clicks "Гриф выживание" (the crafting table) in the menu it opens, then looks for the grief with the chosen
 * number (1–88) across the three pages, turning them with the arrows, and clicks it. Every step waits the set
 * delay, so the server sees human-paced clicks; the module turns itself off when done or when a step times out.
 */
public final class GriefJoiner {
    public static final SliderSetting GRIEF=new SliderSetting("Гриф",1,88,1,1);
    public static final SliderSetting DELAY=new SliderSetting("Задержка кликов (мс)",50,1500,25,300);
    public static final SliderSetting TIMEOUT=new SliderSetting("Ожидание меню (с)",1,15,.5f,5);
    public static final BooleanSetting NOTIFY=new BooleanSetting("Уведомления",true);
    public static final Setting[] SETTINGS={GRIEF,DELAY,TIMEOUT,NOTIFY};
    private static final int PAGES=3;
    private enum Step { COMPASS, MODE_MENU, GRIEF_MENU, JOINING }
    private static Step step;
    /** When the next action may run, and when the current wait gives up. */
    private static long nextAt,deadline;
    /** Pages turned so far, and the menu contents last looked at, so a page turn is seen once it lands. */
    private static int pages;
    private static String lastPage;
    private static int target;

    private GriefJoiner() {}

    private static boolean enabled() { return ClientModules.GRIEF_JOINER!=null && ClientModules.GRIEF_JOINER.enabled(); }

    public static void tick(Minecraft mc) {
        if(!enabled()) { step=null; return; }
        if(mc.player==null || mc.gameMode==null) return;
        long now=System.currentTimeMillis();
        if(step==null) {
            step=Step.COMPASS; target=Math.round(GRIEF.get()); pages=0; lastPage=null;
            nextAt=now; deadline=now+timeout();
            notify(Icon.COMPASS,"GriefJoiner","Захожу на гриф #"+target);
        }
        if(now<nextAt) return;
        if(now>deadline) { fail(step==Step.COMPASS || step==Step.MODE_MENU ? "Меню не открылось" : "Гриф #"+target+" не найден"); return; }
        switch (step) {
            case COMPASS -> useCompass(mc,now);
            case MODE_MENU -> {
                Slot slot=find(mc,stack->stack.is(Items.CRAFTING_TABLE) || name(stack).contains("гриф") && name(stack).contains("выжив"));
                if(slot==null) return;
                click(mc,slot);
                lastPage=contents(mc);
                step=Step.GRIEF_MENU; nextAt=now+delay(); deadline=now+timeout();
            }
            case GRIEF_MENU -> pickGrief(mc,now);
            case JOINING -> {
                // The server closes the menu as it moves the player over.
                if(!(mc.gui.screen() instanceof AbstractContainerScreen<?>) || mc.gui.screen() instanceof InventoryScreen) done("Гриф #"+target);
            }
        }
    }

    private static void useCompass(Minecraft mc,long now) {
        Inventory inventory=mc.player.getInventory();
        int slot=-1;
        for(int i=0;i<36 && slot<0;i++) if(inventory.getItem(i).is(Items.COMPASS)) slot=i;
        if(slot<0) { fail("Нет компаса в инвентаре"); return; }
        if(mc.gui.screen()!=null) mc.player.closeContainer();
        if(slot>=9) {
            // Out of the hotbar: swapped into the held slot first (inventory slots 9–35 keep their index in the player menu).
            mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,slot,inventory.getSelectedSlot(),ContainerInput.SWAP,mc.player);
            nextAt=now+delay();
            return;
        }
        inventory.setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        step=Step.MODE_MENU; nextAt=now+delay(); deadline=now+timeout();
    }

    /** Clicks the grief on this page, or turns to the next one once the page has loaded. */
    private static void pickGrief(Minecraft mc,long now) {
        String page=contents(mc);
        if(page==null || page.equals(lastPage)) return;
        Slot grief=find(mc,stack->!isArrow(stack) && numberOf(stack)==target);
        if(grief!=null) {
            click(mc,grief);
            notify(Icon.CHECK,Theme.accent(),"GriefJoiner","Нажал гриф #"+target);
            step=Step.JOINING; nextAt=now+delay(); deadline=now+timeout();
            return;
        }
        Slot next=find(mc,GriefJoiner::isNext);
        if(next==null || ++pages>=PAGES) { fail("Гриф #"+target+" не найден"); return; }
        click(mc,next);
        lastPage=page;
        nextAt=now+delay(); deadline=now+timeout();
    }

    /** The menu's own slots (not the player's inventory) as one string; null while no server menu is open. */
    private static String contents(Minecraft mc) {
        if(!(mc.gui.screen() instanceof AbstractContainerScreen<?>) || mc.gui.screen() instanceof InventoryScreen) return null;
        var sb=new StringBuilder();
        for(Slot slot:mc.player.containerMenu.slots) if(!(slot.container instanceof Inventory)) sb.append(name(slot.getItem())).append('|');
        return sb.toString();
    }

    private static Slot find(Minecraft mc,java.util.function.Predicate<ItemStack> test) {
        if(contents(mc)==null) return null;
        for(Slot slot:mc.player.containerMenu.slots)
            if(!(slot.container instanceof Inventory) && !slot.getItem().isEmpty() && test.test(slot.getItem())) return slot;
        return null;
    }

    private static void click(Minecraft mc,Slot slot) {
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,slot.index,0,ContainerInput.PICKUP,mc.player);
    }

    private static String name(ItemStack stack) { return stack.getHoverName().getString().toLowerCase(Locale.ROOT); }
    private static boolean isArrow(ItemStack stack) { return stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW); }
    /** The forward arrow: named "далее"/"следующая"/"вперёд" or pointing right; the last arrow in the menu otherwise. */
    private static boolean isNext(ItemStack stack) {
        if(!isArrow(stack)) return false;
        String n=name(stack);
        return !(n.contains("назад") || n.contains("пред") || n.contains("←") || n.contains("«"));
    }
    private static final Pattern NUMBER=Pattern.compile("(\\d{1,2})");
    /** The grief's number from its name ("Гриф #12", "ГРИФ-12", "12"), or -1. */
    private static int numberOf(ItemStack stack) {
        Matcher m=NUMBER.matcher(stack.getHoverName().getString());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static long delay() { return Math.round(DELAY.get()); }
    private static long timeout() { return Math.round(TIMEOUT.get()*1000); }

    private static void done(String text) {
        notify(Icon.SERVER,Theme.accent(),"Зашёл",text);
        stop();
    }
    private static void fail(String reason) {
        notify(Icon.CLOSE,0xffff6b5e,"GriefJoiner",reason);
        stop();
    }
    private static void stop() {
        step=null;
        ClientModules.GRIEF_JOINER.setEnabled(false);
    }
    private static void notify(Icon icon,String title,String text) { notify(icon,Theme.accent(),title,text); }
    private static void notify(Icon icon,int color,String title,String text) {
        if(NOTIFY.get()) HudNotifications.queue(icon,color,title,text);
    }
}
