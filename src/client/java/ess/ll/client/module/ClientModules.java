package ess.ll.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.module.misc.ClickGuiModule;
import ess.ll.client.module.render.RenderTestModule;
import ess.ll.client.setting.Settings.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared registry for ClickGUI, commands, binds and config serialization. */
public final class ClientModules {
    public static final class Module {
        public final String name,category;
        public final List<Setting> settings;
        private boolean enabled;
        private final BooleanSupplier getter;
        private final Runnable toggler;
        /** Key or mouse code (see Keys), -1 when unbound. */
        public int bind=-1;
        /** Hold mode: enabled only while the bind is held; otherwise the bind toggles. */
        public boolean hold;
        Module(String name,String category,List<Setting> settings,BooleanSupplier getter,Runnable toggler) {
            this.name=name; this.category=category; this.settings=settings; this.getter=getter; this.toggler=toggler;
        }
        public boolean enabled() { return getter==null ? enabled : getter.getAsBoolean(); }
        public void toggle() { setEnabled(!enabled()); }
        public void setEnabled(boolean value) {
            if(enabled()==value) return;
            if(toggler!=null) toggler.run(); else enabled=value;
        }
    }
    private static final List<Module> MODULES=new ArrayList<>();
    public static final SliderSetting TIME=new SliderSetting("Время",0,23900,100,6000);
    public static final SliderSetting BRIGHTNESS=new SliderSetting("Яркость",0,1,.1f,1);
    public static final SliderSetting ZOOM_FACTOR=new SliderSetting("Кратность",1.5f,10,.5f,4);
    public static final SliderSetting ZOOM_SPEED=new SliderSetting("Скорость",2,30,1,12);
    public static final SliderSetting RESPAWN_DELAY=new SliderSetting("Задержка",0,5,.1f,1);
    public static final MultiBooleanSettings HUD_ELEMENTS=new MultiBooleanSettings("Элементы","Watermark","ArrayList","Potions","TargetHud","Coordinates","KeyBinds","Player","Notifications","Armor","Waypoint").addedLater("Waypoint").addedLater("Player").addedLater("Notifications");
    public static final BooleanSetting PLAYER_LYRICS=new BooleanSetting("Текст песни",true).visibleWhen(()->false);
    public static final BooleanSetting NOTE_MODULES=new BooleanSetting("Модули",true).visibleWhen(()->false);
    public static final BooleanSetting NOTE_EFFECTS=new BooleanSetting("Эффекты",true).visibleWhen(()->false);
    public static final BooleanSetting NOTE_FRIENDS=new BooleanSetting("Друзья",true).visibleWhen(()->false);
    // Widget settings are edited in the HUD popup (right click in chat), so they stay out of the ClickGUI card.
    public static final BooleanSetting HUD_GLOW=new BooleanSetting("Свечение иконок",true).visibleWhen(()->false);
    public static final ModeSettings TARGET_STYLE=new ModeSettings("Стиль","Круг","Полоска").visibleWhen(()->false);
    // Settings are saved by name per module, so the potions copy carries a trailing space to stay distinct.
    public static final BooleanSetting WM_FPS=new BooleanSetting("FPS",true).visibleWhen(()->false);
    public static final BooleanSetting WM_PING=new BooleanSetting("Пинг",true).visibleWhen(()->false);
    public static final BooleanSetting WM_TIME=new BooleanSetting("Время",true).visibleWhen(()->false);
    public static final BooleanSetting WM_BPS=new BooleanSetting("Скорость (BPS)",true).visibleWhen(()->false);
    public static final BooleanSetting WM_NICK=new BooleanSetting("Ник",false).visibleWhen(()->false);
    // Not "Заголовок": that name held the old header side as text, and a type change would reject the whole saved state.
    public static final BooleanSetting POT_BLINK=new BooleanSetting("Мигание в конце",true).visibleWhen(()->false);
    public static final SliderSetting POT_THRESHOLD=new SliderSetting("Порог, с",3,30,1,10).visibleWhen(()->false);
    public static final BooleanSetting LIST_LINE=new BooleanSetting("Полоса",true);
    public static final SliderSetting HUD_SIZE=new SliderSetting("Размер",.5f,2,.05f,1);
    public static final BooleanSetting TARGET_PARTICLES=new BooleanSetting("Партиклы цели",true);
    public static final SliderSetting TARGET_PARTICLE_COUNT=new SliderSetting("Количество частиц",1,67,1,16);
    public static final ModeSettings TARGET_PARTICLE_MODE=new ModeSettings("Тип частиц","Светлячки","Классика");
    public static final BooleanSetting TARGET_FROM_HEAD=new BooleanSetting("От головы",true);
    public static final BooleanSetting TARGET_FROM_BAR=new BooleanSetting("От полоски",true);
    public static final SliderSetting GUI_SIZE=new SliderSetting("Размер",.6f,1.4f,.05f,1);
    // The GUI has one layout now; the setting stays for old configs and is hidden.
    public static final ModeSettings GUI_STYLE=new ModeSettings("Стиль","Сайдбар","Окно").visibleWhen(()->false);
    public static final BooleanSetting GUI_SOUNDS=new BooleanSetting("Звуки",true);
    // Theme colours default to the Ice preset; see Theme.PRESETS.
    public static final ColorSetting THEME_ACCENT=new ColorSetting("Акцент",0xff63c8ff),
            THEME_TEXT=new ColorSetting("Основной текст",0xfff4f8ff),
            THEME_SECONDARY=new ColorSetting("Второстепенный текст",0xffaab3c2),
            THEME_HINT=new ColorSetting("Подсказки и иконки",0xff6f7888),
            THEME_BACKGROUND=new ColorSetting("Фон окна",0xff0b0d12),
            THEME_CARD=new ColorSetting("Карточки",0xff12151c),
            THEME_FIELD=new ColorSetting("Поля и переключатели",0xff1b1f29),
            THEME_BORDER=new ColorSetting("Обводка",0x12ffffff,true);
    public static final SliderSetting THEME_RADIUS=new SliderSetting("Скругление",4,16,1,10);
    public static final SliderSetting THEME_OPACITY=new SliderSetting("Непрозрачность",60,100,1,92);
    public static final SliderSetting THEME_SPEED=new SliderSetting("Скорость анимаций",50,200,10,100);
    public static final ModeSettings BACKGROUND_STYLE=new ModeSettings("Стиль фона","Блюр","Обычный");
    public static final SliderSetting GLASS_OPACITY=new SliderSetting("Прозрачность",0,1,.01f,.8f),
            BLUR_POWER=new SliderSetting("Сила блюра",0,30,1,19),
            GLOW_POWER=new SliderSetting("Сила свечения",0,32,1,7);
    public static final Module CUSTOM_TIME, GAMMA, AUTO_JUMP, AUTO_RESPAWN, ZOOM, HUD, THEME, COSMETICS, CLICK_GUI,
            FOG_ATMOSPHERE, DEATH_EFFECT, HANDS, AIMING_ITEMS, CREAMY_KEYS, PVP_SAFE, OPTIMIZATION, JUMP_CIRCLES, NO_DELAY, STREAMER_MODE, AUTO_SORTER, AUTO_RW_TIKVA, AUC_HELPER, TELEGRAM, SERVER_MENU, GRIEF_JOINER, CRYSTAL_POS_ESP, ITEM_SCROLLER, TYPING_ANIMATION, HP_ALERT, BETTER_CHEST, NO_PUSH, RESOURCE_PACK_SPOOF, SHULKER_PREVIEW, AUTO_BREWING, SHADER_ESP, EMOTES, WARDROBE, WORLD_TWEAKS,
            TRAILS, HIT_BUBBLES, HIT_GHOST, CHRONO_TRAIL, CHAMS, MOTION_BLUR, BLOCK_OVERLAY, CROSSHAIR, ITEM_PHYSICS, ARROWS, SHADER_SKY, CUSTOM_CRYSTAL;
    static {
        registerCombat();
        MODULES.add(new Module("RenderTest","Render",RenderTestModule.SETTINGS,RenderTestModule::isEnabled,RenderTestModule::toggle));
        CUSTOM_TIME=add("CustomTime","Render",TIME);
        GAMMA=add("Gamma","Render",BRIGHTNESS);
        AUTO_JUMP=add("AutoJump","Movement");
        AUTO_RESPAWN=add("AutoRespawn","Player",RESPAWN_DELAY);
        ZOOM=add("Zoom","Render",ZOOM_FACTOR,ZOOM_SPEED);
        HUD=add("HUD","Render",HUD_ELEMENTS,LIST_LINE,HUD_SIZE,TARGET_PARTICLES,TARGET_PARTICLE_COUNT,
                TARGET_PARTICLE_MODE,TARGET_FROM_HEAD,TARGET_FROM_BAR,HUD_GLOW,TARGET_STYLE,PLAYER_LYRICS,NOTE_MODULES,NOTE_EFFECTS,NOTE_FRIENDS,WM_FPS,WM_PING,WM_TIME,WM_BPS,WM_NICK,
                POT_BLINK,POT_THRESHOLD);
        FOG_ATMOSPHERE=add("FogAtmosphere","Render",ess.ll.client.module.render.FogAtmosphere.SETTINGS);
        DEATH_EFFECT=add("DeathEffect","Render",ess.ll.client.module.render.DeathEffect.SETTINGS);
        JUMP_CIRCLES=add("JumpCircles","Render",ess.ll.client.module.render.JumpCircles.SETTINGS);
        HANDS=add("Hands","Render",ess.ll.client.module.render.Hands.INSTANCE.settings);
        TRAILS=add("Trails","Render",ess.ll.client.module.render.Trails.SETTINGS);
        HIT_BUBBLES=add("HitBubbles","Render",ess.ll.client.module.render.HitBubbles.SETTINGS);
        HIT_GHOST=add("HitGhost","Render",ess.ll.client.module.render.HitGhost.SETTINGS);
        CHRONO_TRAIL=add("ChronoTrail","Render",ess.ll.client.module.render.ChronoTrail.SETTINGS);
        CHAMS=add("Chams","Render",ess.ll.client.module.render.Chams.SETTINGS);
        MOTION_BLUR=add("MotionBlur","Render",ess.ll.client.module.render.MotionBlur.SETTINGS);
        BLOCK_OVERLAY=add("BlockOverlay","Render",ess.ll.client.module.render.BlockOverlay.SETTINGS);
        SHADER_SKY=add("ShaderSky","Render",ess.ll.client.module.render.ShaderSky.SETTINGS);
        CUSTOM_CRYSTAL=add("CustomCrystal","Render",ess.ll.client.module.render.CustomCrystal.SETTINGS);
        CROSSHAIR=add("Crosshair","Render",ess.ll.client.module.render.Crosshair.SETTINGS);
        ITEM_PHYSICS=add("ItemPhysics","Render",ess.ll.client.module.render.ItemPhysics.SETTINGS);
        ARROWS=add("Arrows","Render",ess.ll.client.module.render.Arrows.SETTINGS);
        CRYSTAL_POS_ESP=add("CrystalPosESP","Render",ess.ll.client.module.render.CrystalPosESP.SETTINGS);
        TYPING_ANIMATION=add("TypingAnimation","Render",ess.ll.client.module.render.TypingAnimation.SETTINGS);
        HP_ALERT=add("HpAlert","Render",ess.ll.client.module.render.HpAlert.SETTINGS);
        WARDROBE=add("Wardrobe","Render",ess.ll.client.wardrobe.Wardrobe.SETTINGS);
        SHADER_ESP=add("ShaderESP","Render",ess.ll.client.module.render.ShaderESP.SETTINGS);
        WORLD_TWEAKS=add("WorldTweaks","Render",ess.ll.client.module.render.WorldTweaks.SETTINGS);
        AIMING_ITEMS=add("AimingItems","Misc",ess.ll.client.module.misc.AimingItems.SETTINGS);
        CREAMY_KEYS=add("CreamyKeys","Misc",ess.ll.client.module.misc.CreamyKeys.SETTINGS);
        PVP_SAFE=add("PvpSafe","Misc",ess.ll.client.module.misc.PvpSafe.SETTINGS);
        OPTIMIZATION=add("Optimization","Misc",ess.ll.client.module.misc.Optimization.SETTINGS);
        NO_DELAY=add("NoDelay","Misc",ess.ll.client.module.misc.NoDelay.SETTINGS);
        TELEGRAM=add("Telegram","Misc",ess.ll.client.telegram.TelegramBot.SETTINGS);
        AUC_HELPER=add("AucHelper","Misc",ess.ll.client.module.misc.AucHelper.SETTINGS);
        AUTO_SORTER=add("AutoSorter","Misc",ess.ll.client.module.misc.AutoSorter.SETTINGS);
        AUTO_RW_TIKVA=add("AutoRwTikva","Misc",ess.ll.client.module.misc.AutoRwTikva.SETTINGS);
        STREAMER_MODE=add("StreamerMode","Misc",ess.ll.client.module.misc.StreamerMode.SETTINGS);
        SERVER_MENU=add("ServerMenu","Misc",ess.ll.client.module.misc.ServerMenu.SETTINGS);
        GRIEF_JOINER=add("GriefJoiner","Misc",ess.ll.client.module.misc.GriefJoiner.SETTINGS);
        ITEM_SCROLLER=add("ItemScroller","Misc",ess.ll.client.module.misc.ItemScroller.SETTINGS);
        BETTER_CHEST=add("BetterChest","Misc",ess.ll.client.module.misc.BetterChest.SETTINGS);
        RESOURCE_PACK_SPOOF=add("ResourcePackSpoof","Misc",ess.ll.client.module.misc.ResourcePackSpoof.SETTINGS);
        SHULKER_PREVIEW=add("ShulkerPreview","Misc",ess.ll.client.module.misc.ShulkerPreview.SETTINGS);
        EMOTES=add("Emotes","Misc",ess.ll.client.module.misc.EmotesModule.SETTINGS);
        NO_PUSH=add("NoPush","Player",ess.ll.client.module.player.NoPush.SETTINGS);
        AUTO_BREWING=add("AutoBrewing","Player",ess.ll.client.module.player.AutoBrewing.SETTINGS);
        // Always on; the ClickGUI shows it as the "Темы" tab instead of a module card.
        THEME=new Module("Theme","Темы",List.of(THEME_ACCENT,THEME_TEXT,THEME_SECONDARY,THEME_HINT,THEME_BACKGROUND,THEME_CARD,
                THEME_FIELD,THEME_BORDER,THEME_RADIUS,THEME_OPACITY,THEME_SPEED,BACKGROUND_STYLE,GLASS_OPACITY,BLUR_POWER,GLOW_POWER),()->true,()->{});
        MODULES.add(THEME);
        // Always on as well; the ClickGUI shows it as the "Cosmetics" tab with previews.
        COSMETICS=new Module("Cosmetics","Cosmetics",List.of(),()->true,()->{});
        MODULES.add(COSMETICS);
        // "Enabled" means the menu is open, so a bind on this module opens the ClickGUI.
        CLICK_GUI=new Module("ClickGUI","Misc",List.of(GUI_STYLE,GUI_SIZE,GUI_SOUNDS),
                ClickGuiModule::isOpen,ClickGuiModule::toggle);
        MODULES.add(CLICK_GUI);
        ZOOM.bind=InputConstants.KEY_C;
        ZOOM.hold=true;
        HUD.setEnabled(true);
        TYPING_ANIMATION.setEnabled(true);
        WARDROBE.setEnabled(true);
    }
    public static boolean hud(String element) {
        return HUD.enabled() && HUD_ELEMENTS.selected(HUD_ELEMENTS.options.indexOf(element));
    }
    private static Module add(String name,String category,Setting... settings) {
        Module m=new Module(name,category,List.of(settings),null,null); MODULES.add(m); return m;
    }
    static void register(Module module) { MODULES.add(module); }
    private static void registerCombat() {
        CombatModules.register();
    }
    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }
    public static Module find(String name) {
        return MODULES.stream().filter(m->m.name.equalsIgnoreCase(name)).findFirst().orElseThrow(()->new IllegalArgumentException("Неизвестный модуль: "+name));
    }
    private ClientModules() {}
}
