package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import net.minecraft.resources.Identifier;

/** Crystal model controls and the original ESS custom crystal skins. */
public final class CustomCrystal {
    public static final SliderSetting SIZE = new SliderSetting("Размер", .2f, 3f, .05f, 1f);
    public static final SliderSetting SPIN = new SliderSetting("Скорость вращения", 0f, 4f, .05f, 1f);
    public static final SliderSetting BOB = new SliderSetting("Высота покачивания", 0f, 3f, .05f, 1f);
    public static final BooleanSetting BASE = new BooleanSetting("Подставка", true);
    public static final BooleanSetting BEAM = new BooleanSetting("Луч", true);
    public static final ModeSettings TEXTURE = new ModeSettings("Текстура", "Ванильная", "Своя", "Шейдер", "Астральная");
    public static final ModeSettings CUSTOM_TEXTURE = new ModeSettings("Файл", "mellstroy", "mellstroy_shock", "aqua", "ember", "void", "prism")
            .visibleWhen(() -> TEXTURE.is("Своя"));
    public static final ModeSettings SHADER_STYLE = new ModeSettings("Стиль заливки", "Шейдер", "Заливка")
            .visibleWhen(() -> TEXTURE.is("Шейдер"));
    public static final ModeSettings SHADER_MODE = new ModeSettings("Шейдер", "Туманность", "Электроразряд", "Северное сияние", "Сетка", "Магма", "Голограмма", "Сингулярность")
            .visibleWhen(() -> TEXTURE.is("Шейдер") && SHADER_STYLE.is("Шейдер"));
    public static final ModeSettings SHADER_ANIMATION = new ModeSettings("Анимация шейдера", "Вращение", "Пульсация", "Сканирование", "Вспышки")
            .visibleWhen(() -> TEXTURE.is("Шейдер") && SHADER_STYLE.is("Шейдер"));
    public static final SliderSetting SHADER_SPEED = new SliderSetting("Скорость шейдера", .1f, 4f, .1f, 1f)
            .visibleWhen(() -> TEXTURE.is("Шейдер") && SHADER_STYLE.is("Шейдер"));
    public static final SliderSetting SHADER_OPACITY = new SliderSetting("Прозрачность", 10f, 100f, 1f, 85f)
            .visibleWhen(() -> TEXTURE.is("Шейдер"));
    public static final ModeSettings SHADER_COLOR = new ModeSettings("Цвет шейдера", "Тема", "Свой")
            .visibleWhen(() -> TEXTURE.is("Шейдер") && (SHADER_STYLE.is("Заливка") || !SHADER_MODE.is("Магма")));
    public static final ColorSetting SHADER_CUSTOM_COLOR = new ColorSetting("Свой цвет", 0xff8d7aff)
            .visibleWhen(() -> TEXTURE.is("Шейдер") && SHADER_COLOR.is("Свой") && (SHADER_STYLE.is("Заливка") || !SHADER_MODE.is("Магма")));
    public static final ColorSetting ASTRAL_COLOR = new ColorSetting("Цвет астрала", 0xffff42f5)
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final ModeSettings ASTRAL_SHAPE = new ModeSettings("Подрежим астрала", "Классика", "Вихрь", "Созвездие", "Контур", "Грани", "Ядро")
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final ModeSettings ASTRAL_ANIMATION = new ModeSettings("Анимация астрала", "Вращение", "Пульсация", "Маятник", "Резонанс", "Гироскоп", "Парение", "Кувырок")
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final SliderSetting ASTRAL_SPEED = new SliderSetting("Скорость эффектов", 0f, 3f, .05f, 1f)
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final SliderSetting ASTRAL_LINE_WIDTH = new SliderSetting("Толщина линий", .5f, 2f, .05f, 1f)
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final SliderSetting ASTRAL_BRIGHTNESS = new SliderSetting("Яркость астрала", .2f, 1.5f, .05f, 1.05f)
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final BooleanSetting ASTRAL_THROUGH_WALLS = new BooleanSetting("Астрал сквозь стены", true)
            .visibleWhen(() -> TEXTURE.is("Астральная"));
    public static final BooleanSetting EXPLOSION = new BooleanSetting("Анимация взрыва", true)
            .visibleWhen(() -> TEXTURE.is("Астральная") || TEXTURE.is("Шейдер"));
    public static final ModeSettings EXPLOSION_TYPE = new ModeSettings("Эффект взрыва", "Подъём", "Разлёт", "Импульс", "Схлопывание", "Вихрь", "Расщепление", "След")
            .visibleWhen(() -> EXPLOSION.get() && (TEXTURE.is("Астральная") || TEXTURE.is("Шейдер")));
    public static final SliderSetting EXPLOSION_TIME = new SliderSetting("Длительность взрыва", .3f, 1.5f, .05f, .85f)
            .visibleWhen(() -> EXPLOSION.get() && (TEXTURE.is("Астральная") || TEXTURE.is("Шейдер")));
    public static final Setting[] SETTINGS = {SIZE, SPIN, BOB, BASE, BEAM, TEXTURE, CUSTOM_TEXTURE, SHADER_STYLE,
            SHADER_MODE, SHADER_ANIMATION, SHADER_SPEED, SHADER_OPACITY, SHADER_COLOR, SHADER_CUSTOM_COLOR,
            ASTRAL_COLOR, ASTRAL_SHAPE, ASTRAL_ANIMATION, ASTRAL_SPEED, ASTRAL_LINE_WIDTH, ASTRAL_BRIGHTNESS,
            ASTRAL_THROUGH_WALLS, EXPLOSION, EXPLOSION_TYPE, EXPLOSION_TIME};

    private CustomCrystal() {}

    public static boolean enabled() {
        return ClientModules.CUSTOM_CRYSTAL != null && ClientModules.CUSTOM_CRYSTAL.enabled();
    }

    public static float size() { return enabled() ? SIZE.get() : 1f; }
    public static float spin() { return enabled() ? SPIN.get() : 1f; }
    public static float bob() { return enabled() ? BOB.get() : 1f; }
    public static boolean base() { return !enabled() || (!astral() && BASE.get()); }
    public static boolean beam() { return !enabled() || BEAM.get(); }
    public static boolean astral() { return enabled() && TEXTURE.is("Астральная"); }
    public static boolean effectsActive() { return enabled() && (TEXTURE.is("Астральная") || TEXTURE.is("Шейдер")); }

    public static Identifier textureOverride() {
        if (!enabled()) return null;
        if (TEXTURE.is("Своя")) return Identifier.fromNamespaceAndPath("ess-best", "textures/crystal/" + CUSTOM_TEXTURE.get() + ".png");
        if (TEXTURE.is("Шейдер")) return CrystalShaderTexture.ID;
        return null;
    }

    public static int shaderRgb() {
        return (SHADER_COLOR.is("Свой") ? SHADER_CUSTOM_COLOR.get() : Theme.accent()) & 0xffffff;
    }

    public static void renderTick() {
        if (enabled() && TEXTURE.is("Шейдер")) CrystalShaderTexture.update();
        if (enabled() && TEXTURE.is("Астральная")) CrystalAstralTexture.ensure();
    }
}
