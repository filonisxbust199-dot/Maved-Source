package ess.ll.client.features.esp;

import ess.ll.client.setting.ColorSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.Setting;
import ess.ll.client.setting.SettingsContainer;
import ess.ll.client.setting.ToggleSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class EntityEspStyle {
    private final String groupName;
    private final EntityEspStyle.EspTarget target;
    private final ToggleSetting visible;
    private final ToggleSetting nameTag;
    private final ToggleSetting armor;
    private final ToggleSetting heldItems;
    private final ToggleSetting voiceStatus;
    private final ToggleSetting enchantments;
    private final ModeSetting boxMode;
    private final ModeSetting colorMode;
    private final ColorSetting boxColor;
    private final ModeSetting healthBarMode;
    private final ColorSetting healthBarTopColor;
    private final ColorSetting healthBarBottomColor;
    private final ToggleSetting outline;
    private final ColorSetting outlineColor;
    private final NumberSetting<Float> outlineWidth;
    private final ToggleSetting glow;
    private final NumberSetting<Float> glowRadius;
    private final ToggleSetting texturedOutline;
    private final NumberSetting<Float> saturation;
    private final ToggleSetting tracers;
    private final ColorSetting tracerColor;
    private final NumberSetting<Float> tracerWidth;
    private final List<Setting<?>> settings = new ArrayList<>();

    public ColorSetting getHealthBarBottomColor() {
        return this.healthBarBottomColor;
    }

    public ToggleSetting getNameTag() {
        return this.nameTag;
    }

    public NumberSetting<Float> getGlowRadius() {
        return this.glowRadius;
    }

    public boolean hasExtraInfo() {
        return this.isNameTagEnabled() || this.isArmorEnabled() || this.isHeldItemsEnabled() || this.isVoiceStatusEnabled();
    }

    public ToggleSetting getOutline() {
        return this.outline;
    }

    public ModeSetting getHealthBarMode() {
        return this.healthBarMode;
    }

    public boolean isHealthBarEnabled() {
        return this.healthBarMode != null && !this.healthBarMode.is("Отключен");
    }

    public String getGroupName() {
        return this.groupName;
    }

    public ToggleSetting getHeldItems() {
        return this.heldItems;
    }

    public boolean isTracersEnabled() {
        return this.tracers != null && this.tracers.isEnabled();
    }

    public NumberSetting<Float> getSaturation() {
        return this.saturation;
    }

    public ToggleSetting getGlow() {
        return this.glow;
    }

    private <T, S extends Setting<T>> S register(SettingsContainer settingsContainer, String var2, S s) {
        s.withCustomName(var2.isEmpty() ? s.getName() : var2 + "." + s.getName());
        s.withContainer(settingsContainer);
        this.settings.add(s);
        return (S) s;
    }

    public boolean isBoxEnabled() {
        return !this.boxMode.is("Отключено");
    }

    public boolean isNameTagEnabled() {
        return this.nameTag.isEnabled();
    }

    public boolean isVoiceStatusEnabled() {
        return this.voiceStatus != null && this.voiceStatus.isEnabled();
    }

    public boolean isEnchantmentsEnabled() {
        return this.enchantments != null && this.enchantments.isEnabled();
    }

    public NumberSetting<Float> getTracerWidth() {
        return this.tracerWidth;
    }

    public ToggleSetting getVoiceStatus() {
        return this.voiceStatus;
    }

    public float getGlowRadiusValue() {
        return this.glowRadius.getValue();
    }

    public EntityEspStyle(SettingsContainer settingsContainer, String var2, String var3, EntityEspStyle.EspTarget var4, boolean var5, boolean var6, boolean var7) {
        this.groupName = var2;
        this.target = var4;
        boolean var8 = var4 == EntityEspStyle.EspTarget.PLAYER;
        this.visible = this.register(settingsContainer, var3, new ToggleSetting("Отображать").withDefault(var6));
        this.nameTag = this.register(settingsContainer, var3, new ToggleSetting("Ник").withDefault(true));
        if (var8) {
            this.armor = this.register(settingsContainer, var3, new ToggleSetting("Броня").withDefault(true));
            this.enchantments = this.register(settingsContainer, var3, new ToggleSetting("Зачарования").withVisibility(this.armor::isEnabled).withDefault(false));
            this.heldItems = this.register(settingsContainer, var3, new ToggleSetting("Предметы в руках").withDefault(true));
            this.voiceStatus = var5 ? this.register(settingsContainer, var3, new ToggleSetting("Статус войс чата").withDefault(false)) : null;
        } else {
            this.armor = null;
            this.enchantments = null;
            this.heldItems = null;
            this.voiceStatus = null;
        }
        this.boxMode = this.register(settingsContainer, var3, new ModeSetting("Бокс").addModes("Отключено", "Углы", "Квадрат").withDefault("Углы"));
        this.colorMode = this.register(settingsContainer, var3, new ModeSetting("Режим цвета").addModes("Клиентский", "Статичный").withDefault("Клиентский").withVisibility(this::isBoxEnabled));
        this.boxColor = this.register(settingsContainer, var3, new ColorSetting("Цвет бокса").withDefault(new Color(255, 255, 255)).withVisibility(() -> this.isBoxEnabled() && this.colorMode.is("Статичный")));
        if (var4 != EntityEspStyle.EspTarget.ITEM) {
            this.healthBarMode = this.register(settingsContainer, var3, new ModeSetting("Бар здоровья").addModes("Отключен", "Состояние", "Индивидуальный", "Клиентский").withDefault("Отключен"));
            this.healthBarTopColor = this.register(settingsContainer, var3, new ColorSetting("Верхний цвет").withDefault(new Color(0, 255, 0)).withVisibility(() -> this.healthBarMode.is("Индивидуальный")));
            this.healthBarBottomColor = this.register(settingsContainer, var3, new ColorSetting("Нижний цвет").withDefault(new Color(255, 0, 0)).withVisibility(() -> this.healthBarMode.is("Индивидуальный")));
        } else {
            this.healthBarMode = null;
            this.healthBarTopColor = null;
            this.healthBarBottomColor = null;
        }
        // Outline and glow are intentionally excluded from this port.
        this.outline = new ToggleSetting("Обводка").withDefault(false);
        this.texturedOutline = new ToggleSetting("Текстурная обводка").withDefault(false);
        this.outlineColor = new ColorSetting("Цвет обводки").withDefault(Color.WHITE);
        this.outlineWidth = new NumberSetting<Float>("Толщина").withDefault(2.0F);
        this.glow = new ToggleSetting("Свечение").withDefault(false);
        this.glowRadius = new NumberSetting<Float>("Радиус свечения").withDefault(8.0F);
        this.saturation = new NumberSetting<Float>("Насыщение").withDefault(1.5F);
        if (var7) {
            this.tracers = this.register(settingsContainer, var3, new ToggleSetting("Линии").withDefault(false));
            this.tracerColor = this.register(settingsContainer, var3, new ColorSetting("Цвет линий").withDefault(new Color(255, 255, 255)).withVisibility(this.tracers::isEnabled));
            this.tracerWidth = this.register(settingsContainer, var3, new NumberSetting<Float>("Толщина линий").withMin(1.0F).withMax(5.0F).withStep(0.5F).withDefault(2.0F).withVisibility(this.tracers::isEnabled));
        } else {
            this.tracers = null;
            this.tracerColor = null;
            this.tracerWidth = null;
        }
    }

    public boolean isGlowEnabled() {
        return this.glow.isEnabled();
    }

    public boolean isStaticColor() {
        return this.colorMode.is("Статичный");
    }

    public NumberSetting<Float> getOutlineWidth() {
        return this.outlineWidth;
    }

    public ColorSetting getTracerColor() {
        return this.tracerColor;
    }

    public boolean isCornerBox() {
        return this.boxMode.is("Углы");
    }

    public boolean isHeldItemsEnabled() {
        return this.heldItems != null && this.heldItems.isEnabled();
    }

    public boolean isTexturedOutlineEnabled() {
        return this.texturedOutline.isEnabled();
    }

    public ToggleSetting getTracers() {
        return this.tracers;
    }

    public ColorSetting getOutlineColor() {
        return this.outlineColor;
    }

    public ToggleSetting getEnchantments() {
        return this.enchantments;
    }

    public ToggleSetting getVisible() {
        return this.visible;
    }

    public ModeSetting getBoxMode() {
        return this.boxMode;
    }

    public boolean isVisible() {
        return this.visible.isEnabled();
    }

    public ToggleSetting getArmor() {
        return this.armor;
    }

    public ToggleSetting getTexturedOutline() {
        return this.texturedOutline;
    }

    public int tracerArgb() {
        return this.tracerColor.getArgb();
    }

    public boolean isOutlineEnabled() {
        return this.outline.isEnabled();
    }

    public float getSaturationValue() {
        return this.saturation.getValue();
    }

    public ColorSetting getBoxColor() {
        return this.boxColor;
    }

    public boolean isArmorEnabled() {
        return this.armor != null && this.armor.isEnabled();
    }

    public ModeSetting getColorMode() {
        return this.colorMode;
    }

    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    public ColorSetting getHealthBarTopColor() {
        return this.healthBarTopColor;
    }

    public float getTracerWidthValue() {
        return this.tracerWidth.getValue();
    }

    public EntityEspStyle.EspTarget getTarget() {
        return this.target;
    }

    public float getOutlineWidthValue() {
        return this.outlineWidth.getValue();
    }

    public enum EspTarget {
        PLAYER, LIVING, ITEM
    }
}
