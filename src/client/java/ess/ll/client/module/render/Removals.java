package ess.ll.client.module.render;

import ess.ll.client.event.MobEffectVisibilityEvent;
import ess.ll.client.event.NauseaStrengthEvent;
import ess.ll.client.event.NightVisionEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.VisualRemovalEvent;
import ess.ll.client.event.WeatherStateEvent;
import ess.ll.client.event.WorldSoundEvent;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import java.util.List;
import net.minecraft.client.renderer.state.MapRenderState.MapDecorationRenderState;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public class Removals extends ClientModule {
    private final MultiToggleSetting removedVisuals;
    private final ToggleSetting removeSounds;
    private final MultiToggleSetting removedSounds;
    private final NumberSetting<Float> soundReduction;
    private boolean hideNegativeEffects;
    private boolean hideFireOverlay;
    private boolean hideHurtCamera;
    private boolean disableCameraCollision;
    private boolean hideScoreboard;
    private boolean hideItemTitles;
    private boolean hideOverlayMessage;
    private boolean hideEffectList;
    private boolean hideWaterFog;
    private boolean hideLavaFog;
    private boolean hideWeather;

    @Subscribe
    private void onVisualRemoval(VisualRemovalEvent event) {
        boolean var2 = switch (event.getType()) {
            case FIRE ->
                this.hideFireOverlay;
            case NOHURTCAM ->
                this.hideHurtCamera;
            case F5_XUETA ->
                this.disableCameraCollision;
            case SCOREBOARD ->
                this.hideScoreboard;
            case TOTEM ->
                this.removedVisuals.isSelected("Поп тотема");
            case ITEM_TITLES ->
                this.hideItemTitles;
            case OVERLAY_MESSAGE ->
                this.hideOverlayMessage;
            case MINECRAFT_EFFECTS ->
                this.hideEffectList;
            case WATER_FOG ->
                this.hideWaterFog;
            case LAVA_FOG ->
                this.hideLavaFog;
            case NIGHT_VISION_FOG ->
                false;
            case LAG_MACHINE ->
                this.trimList(event.getList());
        };
        if (var2) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    private void onNauseaStrength(NauseaStrengthEvent event) {
        if (this.hideNegativeEffects) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    private void onMobEffectVisibility(MobEffectVisibilityEvent event) {
        if (this.hideNegativeEffects) {
            MobEffect effect = event.getEffect();
            if (effect != null) {
                if (effect.equals(MobEffects.NAUSEA.value()) || effect.equals(MobEffects.DARKNESS.value())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Subscribe
    private void onWorldSound(WorldSoundEvent event) {
        if (this.removeSounds.isEnabled()) {
            event.setReduction(this.soundReduction.getValue());
            boolean var2 = switch (event.getType()) {
                case WATER ->
                    this.removedSounds.isSelected("Течение воды");
                case WATER_DRIP ->
                    this.removedSounds.isSelected("Капанье воды");
                case DRAGON_SUMMON ->
                    this.removedSounds.isSelected("Призыв дракона");
                case WITHER_RELEASED ->
                    this.removedSounds.isSelected("Призыв визера");
                case FIREWORK ->
                    this.removedSounds.isSelected("Запуск фейерверка");
                case EXPERIENCE_BOTTLE ->
                    this.removedSounds.isSelected("Бутылочка опыта");
            };
            if (var2) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        this.hideNegativeEffects = this.removedVisuals.isSelected("Плохие эффекты");
        this.hideHurtCamera = this.removedVisuals.isSelected("Тряска экрана");
        this.hideFireOverlay = this.removedVisuals.isSelected("Огонь на экране");
        this.disableCameraCollision = this.removedVisuals.isSelected("Ограничение от f5");
        this.hideScoreboard = this.removedVisuals.isSelected("Скорборд");
        this.hideItemTitles = this.removedVisuals.isSelected("Предметные Тайтлы");
        this.hideOverlayMessage = this.removedVisuals.isSelected("Оверлейное сообщение");
        this.hideEffectList = this.removedVisuals.isSelected("Список эффектов");
        this.hideWaterFog = this.removedVisuals.isSelected("Затемнение в воде");
        this.hideLavaFog = this.removedVisuals.isSelected("Затемнение в лаве");
        this.hideWeather = this.removedVisuals.isSelected("Погода");
    }

    @Subscribe
    private void onWeatherState(WeatherStateEvent event) {
        if (this.hideWeather) {
            event.setCancelled(true);
        }
    }

    public Removals() {
        super(ModuleCategory.Render, ModuleSubCategory.Other);
        this.removedVisuals = new MultiToggleSetting("Типы").addOption(NamedOption.of("Огонь на экране", true)).addOption(NamedOption.of("Тряска экрана", true)).addOption(NamedOption.of("Ограничение от f5", true)).addOption(NamedOption.of("Предметные Тайтлы", false)).addOption(NamedOption.of("Скорборд", false)).addOption(NamedOption.of("Оверлейное сообщение", false)).addOption(NamedOption.of("Поп тотема", false)).addOption(NamedOption.of("Список эффектов", false)).addOption(NamedOption.of("Плохие эффекты", true)).addOption(NamedOption.of("Затемнение в воде", false)).addOption(NamedOption.of("Затемнение в лаве", false)).addOption(NamedOption.of("Погода", false)).addOption(NamedOption.of("Лаг Машина", true)).withContainer(this);
        this.removeSounds = new ToggleSetting("Убрать звуки").setValue(false).withContainer(this);
        this.removedSounds = new MultiToggleSetting("Звуки").addOption(NamedOption.of("Течение воды", false)).addOption(NamedOption.of("Капанье воды", false)).addOption(NamedOption.of("Призыв визера", false)).addOption(NamedOption.of("Призыв дракона", true)).addOption(NamedOption.of("Запуск фейерверка", false)).addOption(NamedOption.of("Бутылочка опыта", false)).withVisibility(() -> this.removeSounds.isEnabled()).withContainer(this);
        this.soundReduction = new NumberSetting<Float>("Сила заглушения").withMin(0.0F).withMax(1.0F).withStep(0.1F).withDefault(0.5F).withVisibility(() -> this.removeSounds.isEnabled()).withContainer(this);
        this.hideNegativeEffects = false;
        this.hideFireOverlay = false;
        this.hideHurtCamera = false;
        this.disableCameraCollision = false;
        this.hideScoreboard = false;
        this.hideItemTitles = false;
        this.hideOverlayMessage = false;
        this.hideEffectList = false;
        this.hideWaterFog = false;
        this.hideLavaFog = false;
        this.hideWeather = false;
        this.setName("Removals");
    }

    private boolean trimList(List<?> items) {
        if (this.removedVisuals.isSelected("Лаг Машина") && items != null) {
            if (items.size() > 16) {
                items.subList(16, items.size()).clear();
            }
            for (Object var3 : items) {
                MapDecorationRenderState mapDecorationRenderState = (MapDecorationRenderState) var3;
                if (mapDecorationRenderState.name != null && mapDecorationRenderState.name.getString(33).length() > 32) {
                    mapDecorationRenderState.name = null;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Subscribe
    private void onNightVision(NightVisionEvent event) {
        if (this.hideNegativeEffects) {
            event.setCancelled(true);
        }
    }
}
