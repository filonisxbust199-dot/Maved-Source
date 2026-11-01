package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * AucHelper: on an auction page (any container whose items carry a price in their description) the cheapest lot of
 * each kind you picked is framed in your colour. Lots are compared with their own kind: the same item, and with
 * "Учитывать чары" the same enchantments too, so a Sharpness V sword is not beaten by a bare one. Stacks compare by
 * price per piece.
 */
public final class AucHelper {
    public static final MultiBooleanSettings KINDS=new MultiBooleanSettings("Предметы","Броня","Тотемы","Золотые яблоки",
            "Чарованные яблоки","Мечи","Головы игроков","Книги чар").none();
    public static final BooleanSetting ENCHANTS=new BooleanSetting("Учитывать чары",true);
    public static final ColorSetting COLOR=new ColorSetting("Цвет",0xff4fe38a);
    public static final BooleanSetting PRICE=new BooleanSetting("Цена за штуку",true);
    public static final Setting[] SETTINGS={KINDS,ENCHANTS,COLOR,PRICE};
    static { for(int i=0;i<KINDS.options.size();i++) KINDS.toggle(i); }

    // "Цена: 1 500$", "Стоимость - 12.5к", "Price: $2,000"...
    private static final Pattern PRICE_LINE=Pattern.compile("(?iu)(цен|стоим|price|cost|\\$|₽|монет|coins)");
    private static final Pattern NUMBER=Pattern.compile("(?iu)(\\d[\\d\\s.,']*)\\s*(к|k|кк|kk|м|m|млн|b|млрд)?(?![\\p{L}])");

    private AucHelper() {}

    public static boolean enabled() { return ClientModules.AUC_HELPER!=null && ClientModules.AUC_HELPER.enabled(); }

    /** The picked kind of a lot, or null when it is not one of them. */
    static String kind(ItemStack stack) {
        if(stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR))
            return KINDS.selected("Броня") ? "armor" : null;
        if(stack.is(Items.TOTEM_OF_UNDYING)) return KINDS.selected("Тотемы") ? "totem" : null;
        if(stack.is(Items.GOLDEN_APPLE)) return KINDS.selected("Золотые яблоки") ? "gapple" : null;
        if(stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return KINDS.selected("Чарованные яблоки") ? "egapple" : null;
        if(stack.is(ItemTags.SWORDS)) return KINDS.selected("Мечи") ? "sword" : null;
        if(stack.is(Items.PLAYER_HEAD)) return KINDS.selected("Головы игроков") ? "head" : null;
        if(stack.is(Items.ENCHANTED_BOOK)) return KINDS.selected("Книги чар") ? "book" : null;
        return null;
    }
    /** Lots compete within a group: the item, its enchantments when they count, a head's name. */
    static String group(ItemStack stack) {
        StringBuilder key=new StringBuilder(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(stack.is(Items.PLAYER_HEAD)) key.append('|').append(stack.getHoverName().getString());
        boolean book=stack.is(Items.ENCHANTED_BOOK);
        if(ENCHANTS.get() || book) {
            ItemEnchantments enchants=stack.getOrDefault(book ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
            List<String> list=new ArrayList<>();
            for(var e:enchants.entrySet()) list.add(e.getKey().unwrapKey().map(k->k.identifier().getPath()).orElse("?")+e.getIntValue());
            list.sort(null);
            key.append('|').append(String.join(",",list));
        }
        return key.toString();
    }
    /** The price written in the lot's description, or -1. */
    static double price(ItemStack stack) {
        var lore=stack.get(DataComponents.LORE);
        if(lore==null) return -1;
        for(var line:lore.lines()) {
            String text=line.getString();
            if(!PRICE_LINE.matcher(text).find()) continue;
            // The number after the colon when there is one: "Продавец: Steve2009" must not read as a price.
            int colon=text.indexOf(':');
            if(colon<0) colon=text.indexOf('-');
            Matcher m=NUMBER.matcher(colon>=0 ? text.substring(colon+1) : text);
            if(m.find()) {
                double value=number(m.group(1),m.group(2));
                if(value>0) return value;
            }
        }
        return -1;
    }
    private static double number(String digits,String suffix) {
        String d=digits.replaceAll("[\\s']","").trim();
        // "1,500" and "1.500" are thousands; "12.5" or "1,25" before a suffix are fractions.
        if(suffix!=null) d=d.replace(',','.');
        else d=d.replaceAll("[.,](?=\\d{3}(\\D|$))","").replace(',','.');
        int last=d.lastIndexOf('.');
        if(last>=0) d=d.substring(0,last).replace(".","")+d.substring(last);
        double v;
        try { v=Double.parseDouble(d); } catch(NumberFormatException e) { return -1; }
        if(suffix==null) return v;
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "к", "k" -> v*1e3;
            case "кк", "kk", "м", "m", "млн" -> v*1e6;
            case "b", "млрд" -> v*1e9;
            default -> v;
        };
    }

    /** Draws the frames over the container; called after the screen is drawn. */
    public static void render(GuiGraphicsExtractor g,AbstractContainerScreen<?> screen,int left,int top) {
        if(!enabled()) return;
        Map<String,Slot> best=new HashMap<>();
        Map<String,Double> bestPrice=new HashMap<>();
        Map<String,Integer> count=new HashMap<>();
        for(Slot slot:screen.getMenu().slots) {
            if(slot.container instanceof Inventory || !slot.hasItem()) continue;
            ItemStack stack=slot.getItem();
            if(kind(stack)==null) continue;
            double price=price(stack);
            if(price<=0) continue;
            double each=price/Math.max(1,stack.getCount());
            String group=group(stack);
            count.merge(group,1,Integer::sum);
            if(each<bestPrice.getOrDefault(group,Double.MAX_VALUE)) { bestPrice.put(group,each); best.put(group,slot); }
        }
        int color=COLOR.get();
        for(var e:best.entrySet()) {
            Slot slot=e.getValue();
            float x=left+slot.x,y=top+slot.y;
            // Drawn over the items, so only a frame: a solid ring on the slot edge and a fainter one outside it.
            frame(g,x-1,y-1,18,color);
            frame(g,x-2,y-2,20,Theme.withAlpha(color,.45f));
            if(PRICE.get()) {
                String label=compact(bestPrice.get(e.getKey()));
                float w=MsdfFont.UI.width(label,5.5f)+4;
                UiRenderer.rounded(g,x+8-w/2,y+10,w,7,2,0xe0101318);
                MsdfFont.UI.midCenter(g,label,x+8,y+13.5f,5.5f,color);
            }
        }
    }
    private static void frame(GuiGraphicsExtractor g,float x,float y,float size,int color) {
        UiRenderer.rect(g,x,y,size,1,color); UiRenderer.rect(g,x,y+size-1,size,1,color);
        UiRenderer.rect(g,x,y+1,1,size-2,color); UiRenderer.rect(g,x+size-1,y+1,1,size-2,color);
    }
    private static String compact(double v) {
        if(v>=1e6) return trim(v/1e6)+"м";
        if(v>=1e4) return trim(v/1e3)+"к";
        return trim(v);
    }
    private static String trim(double v) {
        return v>=100 || v==Math.floor(v) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT,"%.1f",v);
    }

    /** Dev check: the kind and group, and the read price. */
    public static String kindForTest(ItemStack stack) { return kind(stack)+" "+group(stack); }
    public static double priceForTest(ItemStack stack) { return price(stack); }

    /** .search: the held item's name for /ah search. */
    public static String searchName(ItemStack stack) {
        String name=stack.getHoverName().getString().replaceAll("§.","").trim();
        return name.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() : name;
    }
}
