package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * BetterChest: a bar over chests, shulkers, barrels, hoppers and dispensers with a search field and three
 * buttons. The search lights up the slots whose item matches by name or id and dims the rest; "Взять всё"
 * moves every stack of the container into the inventory, "Сложить всё" every inventory stack into the container,
 * "Выкинуть всё" throws the container's stacks out. Each stack is its own click, queued with a short delay so a
 * server sees ordinary clicks. While the search is focused, keys type into it: E or a hotbar number do not close
 * the chest or move items.
 */
public final class BetterChest {
    public static final BooleanSetting SEARCH=new BooleanSetting("Поиск",true);
    public static final BooleanSetting BUTTONS=new BooleanSetting("Кнопки",true);
    public static final SliderSetting DELAY=new SliderSetting("Задержка кликов (мс)",0,200,5,25).visibleWhen(BUTTONS::get);
    public static final BooleanSetting DIM=new BooleanSetting("Затемнять остальное",true).visibleWhen(SEARCH::get);
    public static final Setting[] SETTINGS={SEARCH,BUTTONS,DELAY,DIM};
    private static final float BAR_H=22,PAD=3,FIELD_W=96,TEXT=7.5f;
    private static final String[] LABELS={"Взять","Сложить","Выкинуть"};
    private static final Icon[] ICONS={Icon.CHEVRON_DOWN,Icon.CHEVRON_DOWN,Icon.TRASH};

    /** Queued clicks: slot index and the kind of click. */
    private record Click(int container,int slot,ContainerInput input,int button) {}
    private static final Deque<Click> QUEUE=new ArrayDeque<>();
    private static long nextClick;

    private BetterChest() {}

    public static boolean enabled() { return ClientModules.BETTER_CHEST!=null && ClientModules.BETTER_CHEST.enabled(); }

    /** Screens that get the bar: storage containers, not the inventory, furnaces or crafting. */
    public static boolean applies(AbstractContainerScreen<?> screen) {
        var menu=screen.getMenu();
        return enabled() && (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu || menu instanceof DispenserMenu);
    }

    // ---- layout -------------------------------------------------------------------------------
    private static float buttonW(int i) { return 7+9+5+MsdfFont.UI.width(LABELS[i],TEXT)+8; }
    private static float barW() {
        float w=PAD;
        if(SEARCH.get()) w+=FIELD_W+PAD;
        if(BUTTONS.get()) for(int i=0;i<3;i++) w+=buttonW(i)+PAD;
        return w;
    }
    /** Bar's left and top for a container panel at (left, top) of the given width. */
    public static float[] bar(int left,int top,int width) {
        float w=barW();
        return new float[]{left+width/2f-w/2,Math.max(2,top-BAR_H-5),w};
    }

    /** The search box, placed inside the bar's field; vanilla handles typing, so TypingAnimation applies. */
    public static EditBox searchBox(int left,int top,int width) {
        float[] b=bar(left,top,width);
        var box=new EditBox(Minecraft.getInstance().font,Math.round(b[0]+PAD+18),Math.round(b[1]+PAD+(BAR_H-2*PAD-8)/2f+.5f),
                Math.round(FIELD_W-24),10,Component.literal("Поиск"));
        box.setBordered(false);
        box.setMaxLength(40);
        box.setHint(Component.literal("Поиск").withColor(0xff8a8fa0));
        box.setTextColor(0xffffffff);
        return box;
    }

    // ---- drawing ------------------------------------------------------------------------------
    public static void render(GuiGraphicsExtractor g,AbstractContainerScreen<?> screen,EditBox box,int left,int top,int width,int mouseX,int mouseY,float partial) {
        String query=box==null ? "" : box.getValue().trim().toLowerCase(Locale.ROOT);
        if(SEARCH.get() && !query.isEmpty()) highlight(g,screen,left,top,query);
        float[] b=bar(left,top,width);
        float x=b[0],y=b[1],w=b[2];
        UiRenderer.shadow(g,x,y,w,BAR_H,8,8,0x55000000);
        UiRenderer.rounded(g,x,y,w,BAR_H,8,0xe0101318);
        float px=x+PAD;
        if(SEARCH.get()) {
            boolean focused=box!=null && box.isFocused();
            float fh=BAR_H-2*PAD;
            UiRenderer.rounded(g,px,y+PAD,FIELD_W,fh,6,focused ? Theme.withAlpha(Theme.accent(),.9f) : 0x22ffffff);
            UiRenderer.rounded(g,px+.8f,y+PAD+.8f,FIELD_W-1.6f,fh-1.6f,5.4f,0xff181b22);
            Icons.draw(g,Icon.SEARCH,px+6,y+BAR_H/2-4,8,focused ? Theme.accent() : 0xff8a8fa0);
            if(box!=null) box.extractRenderState(g,mouseX,mouseY,partial);
            px+=FIELD_W+PAD;
        }
        if(BUTTONS.get()) for(int i=0;i<3;i++) {
            float bw=buttonW(i),bh=BAR_H-2*PAD,by=y+PAD;
            boolean over=mouseX>=px && mouseX<px+bw && mouseY>=by && mouseY<by+bh;
            UiRenderer.rounded(g,px,by,bw,bh,6,over ? 0x2effffff : 0x14ffffff);
            int iconColor=i==2 ? 0xffff6b5e : Theme.accent();
            float ix=px+7,iy=y+BAR_H/2-4.5f;
            if(i==1) {
                // "Сложить" points up: the chevron turned over.
                g.pose().pushMatrix();
                g.pose().translate(ix+4.5f,iy+4.5f);
                g.pose().rotate((float)Math.PI);
                Icons.draw(g,ICONS[i],-4.5f,-4.5f,9,iconColor);
                g.pose().popMatrix();
            } else Icons.draw(g,ICONS[i],ix,iy,9,iconColor);
            MsdfFont.UI.mid(g,LABELS[i],px+7+9+5,y+BAR_H/2,TEXT,over ? 0xffffffff : 0xffe8e9ee);
            px+=bw+PAD;
        }
    }

    /** Matching slots get an accent frame and glow; with dimming on the other slots darken (items keep their layer above). */
    private static void highlight(GuiGraphicsExtractor g,AbstractContainerScreen<?> screen,int left,int top,String query) {
        int accent=Theme.accent();
        for(Slot slot:screen.getMenu().slots) {
            if(slot.container instanceof Inventory) continue;
            float sx=left+slot.x,sy=top+slot.y;
            if(slot.hasItem() && matches(slot.getItem(),query)) {
                UiRenderer.shadow(g,sx-1,sy-1,18,18,3,5,Theme.withAlpha(accent,.6f));
                UiRenderer.rounded(g,sx-1,sy-1,18,18,3,Theme.withAlpha(accent,.16f));
                UiRenderer.rect(g,sx-1,sy-1,18,1,accent); UiRenderer.rect(g,sx-1,sy+16,18,1,accent);
                UiRenderer.rect(g,sx-1,sy,1,16,accent); UiRenderer.rect(g,sx+16,sy,1,16,accent);
            } else if(DIM.get()) UiRenderer.rect(g,sx,sy,16,16,0xa0101318);
        }
    }
    public static boolean matches(ItemStack stack,String query) {
        if(stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replace('_',' ').contains(query);
    }

    // ---- input --------------------------------------------------------------------------------
    /** A click on one of the buttons; true when it was one. */
    public static boolean click(AbstractContainerScreen<?> screen,int left,int top,int width,double mx,double my) {
        if(!BUTTONS.get()) return false;
        float[] b=bar(left,top,width);
        float px=b[0]+PAD+(SEARCH.get() ? FIELD_W+PAD : 0),by=b[1]+PAD,bh=BAR_H-2*PAD;
        for(int i=0;i<3;i++) {
            float bw=buttonW(i);
            if(mx>=px && mx<px+bw && my>=by && my<by+bh) { run(screen,i); return true; }
            px+=bw+PAD;
        }
        return mx>=b[0] && mx<b[0]+b[2] && my>=b[1] && my<b[1]+BAR_H;
    }

    private static void run(AbstractContainerScreen<?> screen,int action) {
        var mc=Minecraft.getInstance();
        if(mc.player==null) return;
        ess.ll.client.render.UiSounds.select();
        var menu=screen.getMenu();
        QUEUE.clear();
        for(Slot slot:menu.slots) {
            boolean inventory=slot.container instanceof Inventory;
            if(!slot.hasItem()) continue;
            if(action==0 && !inventory) QUEUE.add(new Click(menu.containerId,slot.index,ContainerInput.QUICK_MOVE,0));
            if(action==1 && inventory) QUEUE.add(new Click(menu.containerId,slot.index,ContainerInput.QUICK_MOVE,0));
            if(action==2 && !inventory) QUEUE.add(new Click(menu.containerId,slot.index,ContainerInput.THROW,1));
        }
        nextClick=0;
        tick(mc);
    }

    /** Runs the queued clicks that are due; with no delay the whole queue goes at once. */
    public static void tick(Minecraft mc) {
        if(QUEUE.isEmpty()) return;
        if(mc.player==null || mc.gameMode==null) { QUEUE.clear(); return; }
        long now=System.currentTimeMillis();
        long delay=Math.round(DELAY.get());
        while(!QUEUE.isEmpty() && now>=nextClick) {
            Click c=QUEUE.poll();
            if(mc.player.containerMenu.containerId!=c.container) { QUEUE.clear(); return; }
            mc.gameMode.handleContainerInput(c.container,c.slot,c.button,c.input,mc.player);
            if(delay>0) nextClick=now+delay;
        }
    }
    /** Dev check: presses button 0–2 (take, store, throw). */
    public static void runForTest(AbstractContainerScreen<?> screen,int action) { run(screen,action); }
    /** Dev check: clicks still queued. */
    public static int queued() { return QUEUE.size(); }
}
