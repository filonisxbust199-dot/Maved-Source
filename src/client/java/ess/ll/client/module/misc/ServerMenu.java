package ess.ll.client.module.misc;

import ess.ll.client.command.Keys;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.HudNotifications;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import ess.ll.client.util.DelayedInventoryActionManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * ServerMenu: donate items and potions of FunTime, HolyWorld and ReallyWorld on binds. A bind finds the item in
 * the inventory (hotbar first) and throws or uses it through the inventory action queue, which picks the slot
 * and puts the held one back. The lists come from dimasik's FuntimeHelper, HolyWorldHelper and RwHelper; the
 * FunTime potions are told apart by their effects (ServerItemCatalog). The menu itself is ServerMenuScreen.
 */
public final class ServerMenu {
    /** One usable thing: how it is found in the inventory and its bind. */
    public static final class Entry {
        public final String server,name,source;
        public final boolean potion;
        final Item item;
        /** Part of the hover name that tells it apart from other items of the same kind, or null. */
        final String nameHint;
        /** Effects a FunTime potion carries; empty for items. */
        final List<Holder<MobEffect>> effects;
        private final int color;
        /** Built on first use: item components are not bound yet while the client starts. */
        private ItemStack icon;
        public final KeybindSetting bind;
        public final BooleanSetting on;
        Entry(String server,String name,String source,Item item,String nameHint,int color,List<Holder<MobEffect>> effects) {
            this.server=server; this.name=name; this.source=source; this.item=item; this.nameHint=nameHint;
            this.effects=effects; this.potion=!effects.isEmpty();
            this.color=color;
            this.bind=new KeybindSetting(server+" · "+name,-1).visibleWhen(()->false);
            this.on=new BooleanSetting(server+" · "+name+" вкл",true).visibleWhen(()->false);
        }
        /** The real item, a potion in its own colour. */
        public ItemStack icon() {
            if(icon==null) {
                icon=new ItemStack(item);
                if(potion) icon.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(color),
                        effects.stream().map(e->new MobEffectInstance(e,200)).toList(),Optional.empty()));
            }
            return icon;
        }
        boolean matches(ItemStack stack) {
            if(stack.isEmpty() || !stack.is(item)) return false;
            String hover=stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if(nameHint!=null) return hover.contains(nameHint);
            if(!potion) return true;
            if(hover.contains(name.toLowerCase(Locale.ROOT))) return true;
            var contents=stack.get(DataComponents.POTION_CONTENTS);
            if(contents==null) return false;
            Set<Holder<MobEffect>> present=new HashSet<>();
            contents.getAllEffects().forEach(e->present.add(e.getEffect()));
            return present.containsAll(effects);
        }
        /** Items of this kind in the inventory. */
        public int count() {
            var player=Minecraft.getInstance().player;
            if(player==null) return 0;
            int count=0;
            for(int i=0;i<36;i++) { var stack=player.getInventory().getItem(i); if(matches(stack)) count+=stack.getCount(); }
            return count;
        }
    }

    public static final String FUNTIME="FunTime",HOLYWORLD="HolyWorld",REALLYWORLD="ReallyWorld";
    public static final List<String> SERVERS=List.of(FUNTIME,HOLYWORLD,REALLYWORLD);
    public static final List<Entry> ENTRIES=List.of(
        item(FUNTIME,"Явная пыль","Сахар",Items.SUGAR,null),
        item(FUNTIME,"Дезориентация","Око эндера",Items.ENDER_EYE,null),
        item(FUNTIME,"Снежок заморозки","Снежок",Items.SNOWBALL,null),
        item(FUNTIME,"Пласт","Сушёная ламинария",Items.DRIED_KELP,null),
        item(FUNTIME,"Трапка","Незеритовый лом",Items.NETHERITE_SCRAP,null),
        item(FUNTIME,"Огненный смерч","Огненный шар",Items.FIRE_CHARGE,null),
        item(FUNTIME,"Божья аура","Мембрана фантома",Items.PHANTOM_MEMBRANE,null),
        potion("Хлопушка",0xff0000,MobEffects.SLOWNESS,MobEffects.SPEED,MobEffects.BLINDNESS,MobEffects.GLOWING),
        potion("Святая вода",0xffffff,MobEffects.REGENERATION,MobEffects.INVISIBILITY,MobEffects.INSTANT_HEALTH),
        potion("Зелье гнева",0x993333,MobEffects.STRENGTH,MobEffects.SLOWNESS),
        potion("Зелье палладина",0x00ffff,MobEffects.RESISTANCE,MobEffects.FIRE_RESISTANCE,MobEffects.HEALTH_BOOST,MobEffects.INVISIBILITY),
        potion("Зелье ассасина",0x333333,MobEffects.STRENGTH,MobEffects.SPEED,MobEffects.HASTE,MobEffects.INSTANT_DAMAGE),
        potion("Зелье радиации",0x32cd32,MobEffects.POISON,MobEffects.WITHER,MobEffects.SLOWNESS,MobEffects.HUNGER,MobEffects.GLOWING),
        potion("Снотворное",0x484848,MobEffects.WEAKNESS,MobEffects.MINING_FATIGUE,MobEffects.WITHER,MobEffects.BLINDNESS),
        item(HOLYWORLD,"Штучка","Огненный шар",Items.FIRE_CHARGE,null),
        item(HOLYWORLD,"Стан","Звезда Незера",Items.NETHER_STAR,null),
        item(HOLYWORLD,"Взрывная трапка","Осколок призмарина",Items.PRISMARINE_SHARD,null),
        item(HOLYWORLD,"Трапка (хорус)","Хорус",Items.POPPED_CHORUS_FRUIT,null),
        item(HOLYWORLD,"Снежок","Снежок",Items.SNOWBALL,null),
        item(HOLYWORLD,"Рюкзак (шалкер)","Шалкер",Items.SHULKER_BOX,null),
        item(REALLYWORLD,"Ловушка","Сердце моря",Items.HEART_OF_THE_SEA,"ловушка"),
        item(REALLYWORLD,"Эндер ловушка","Сердце моря",Items.HEART_OF_THE_SEA,"эндер"),
        item(REALLYWORLD,"Анти полёт","Звезда фейерверка",Items.FIREWORK_STAR,"анти"),
        item(REALLYWORLD,"Тёмный пульс","Звезда фейерверка",Items.FIREWORK_STAR,"пульс"),
        item(REALLYWORLD,"Каменная ловушка","Звезда фейерверка",Items.FIREWORK_STAR,"камен"),
        item(REALLYWORLD,"Адская ловушка","Незеритовый лом",Items.NETHERITE_SCRAP,"адск"));

    public static final ModeSettings SERVER=new ModeSettings("Сервер",FUNTIME,HOLYWORLD,REALLYWORLD);
    public static final BooleanSetting NOTIFY=new BooleanSetting("Уведомления в островке",true).visibleWhen(()->false);
    public static final BooleanSetting COUNT=new BooleanSetting("Показывать количество",true).visibleWhen(()->false);
    public static final BooleanSetting MISSING=new BooleanSetting("Писать, если предмета нет",true).visibleWhen(()->false);
    public static final ActionSetting OPEN=new ActionSetting("Открыть меню",()->
            Minecraft.getInstance().gui.setScreen(new ess.ll.client.screen.ServerMenuScreen(Minecraft.getInstance().gui.screen())));
    public static final Setting[] SETTINGS;
    static {
        List<Setting> all=new ArrayList<>(List.of(OPEN,SERVER,NOTIFY,COUNT,MISSING));
        for(var e:ENTRIES) { all.add(e.bind); all.add(e.on); }
        SETTINGS=all.toArray(Setting[]::new);
    }
    private static final Set<Integer> HELD=new HashSet<>();

    private ServerMenu() {}

    private static Entry item(String server,String name,String source,Item item,String hint) {
        return new Entry(server,name,source,item,hint,0,List.of());
    }
    @SafeVarargs
    private static Entry potion(String name,int color,Holder<MobEffect>... effects) {
        return new Entry(FUNTIME,name,"Взрывное зелье",Items.SPLASH_POTION,null,color,List.of(effects));
    }

    public static boolean enabled() { return ClientModules.SERVER_MENU!=null && ClientModules.SERVER_MENU.enabled(); }
    public static List<Entry> of(String server,boolean potions) {
        return ENTRIES.stream().filter(e->e.server.equals(server) && e.potion==potions).toList();
    }

    /** Client tick: a pressed bind of the chosen server uses its item, in game only. */
    public static void tick(Minecraft mc) {
        if(!enabled()) { HELD.clear(); return; }
        boolean inGame=mc.player!=null && mc.level!=null && mc.gui.screen()==null && mc.isWindowActive();
        Set<Integer> down=new HashSet<>();
        for(var e:ENTRIES) {
            int key=e.bind.get();
            if(key<0 || !e.server.equals(SERVER.get())) continue;
            boolean pressed=Keys.isDown(key);
            if(pressed) down.add(key);
            if(pressed && !HELD.contains(key) && inGame && e.on.get()) use(e);
        }
        HELD.clear(); HELD.addAll(down);
    }

    /** Finds the item, hotbar first, and throws or uses it through the inventory queue. */
    public static void use(Entry e) {
        var mc=Minecraft.getInstance();
        if(mc.player==null) return;
        Slot found=null;
        for(Slot slot:mc.player.inventoryMenu.slots) {
            if(slot.container!=mc.player.getInventory() || slot.getContainerSlot()>=36 || !e.matches(slot.getItem())) continue;
            if(found==null || slot.getContainerSlot()<9 && found.getContainerSlot()>=9) found=slot;
        }
        if(found==null) {
            ess.ll.client.command.DotCommands.say(e.name+": нет в инвентаре");
            if(MISSING.get()) HudNotifications.queue(Icon.CLOSE,0xffff6b5e,e.name,"Нет в инвентаре");
            return;
        }
        if(mc.player.getCooldowns().isOnCooldown(found.getItem())) {
            ess.ll.client.command.DotCommands.say(e.name+": ещё на кулдауне");
            if(NOTIFY.get()) HudNotifications.queue(Icon.CLOCK,0xffe49a3a,e.name,"Ещё на кулдауне");
            return;
        }
        DelayedInventoryActionManager.INSTANCE.useSlot(found);
        int left=Math.max(0,e.count()-1);
        ess.ll.client.command.DotCommands.say(e.name+": использовано, осталось "+left);
        if(NOTIFY.get()) HudNotifications.queue(Icon.CHECK,Theme.accent(),e.name,"Использовано · осталось "+left);
    }
}
