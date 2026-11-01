package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import ess.ll.client.weather.Weather;
import ess.ll.client.weather.WeatherFx;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;

/**
 * WorldTweaks (from Zenith: Weather and World). Weather is the full Zenith weather in ess.ll.client.weather: rain,
 * mist or snow with its own sky (clear, storm, stars), time of day, drops and flakes in the wind, puddles or a wet
 * film with reflections, a storm with lightning, snow cover with footprints, mist and haze. World is light,
 * saturation, time and a coloured fog. Everything is client-side. Hooks: WorldTweaksWeatherMixin,
 * WorldTweaksPrecipitationMixin, WorldTweaksLightMixin, AtmosphereFogMixin, ClientClockInstanceMixin and the
 * Weather* mixins; the passes run from WorldFx.
 */
public final class WorldTweaks {
    private static final BooleanSupplier WEATHER_ON=()->part("Погода");
    public static final MultiBooleanSettings PARTS=new MultiBooleanSettings("Части","Погода","Свет","Насыщенность","Время","Туман");
    public static final ModeSettings WEATHER=new ModeSettings("Погода","Дождь","Туман","Снег").visibleWhen(WEATHER_ON);
    public static final ModeSettings SKY=new ModeSettings("Небо","Погода","Ясно","Шторм","Звёзды").visibleWhen(WEATHER_ON);
    public static final SliderSetting HOUR=new SliderSetting("Время суток",0,24,.25f,12).visibleWhen(WEATHER_ON);
    public static final ModeSettings WATER=new ModeSettings("Вода","Лужи","Плёнка").visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Дождь"));
    public static final SliderSetting DROPS=new SliderSetting("Капли",10,200,5,100)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && !WEATHER.is("Туман"));
    public static final SliderSetting WIND=new SliderSetting("Ветер",0,250,10,100)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && !WEATHER.is("Туман"));
    public static final BooleanSetting STORM=new BooleanSetting("Гроза",true).visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Дождь"));
    public static final SliderSetting SNOW_DEPTH=new SliderSetting("Глубина снега",20,250,5,100)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Снег"));
    public static final SliderSetting MIST_RANGE=new SliderSetting("Дальность тумана",12,160,4,40)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Туман"));
    public static final SliderSetting MIST=new SliderSetting("Дымка",0,200,5,100)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Туман"));
    public static final SliderSetting HAZE=new SliderSetting("Мгла",0,200,5,100)
            .visibleWhen(()->WEATHER_ON.getAsBoolean() && WEATHER.is("Туман"));
    public static final SliderSetting REFLECT=new SliderSetting("Отражения",0,200,5,100).visibleWhen(WEATHER_ON);
    public static final SliderSetting LIGHT=new SliderSetting("Яркость",0,1,.05f,1).visibleWhen(()->part("Свет"));
    public static final SliderSetting SATURATION=new SliderSetting("Насыщенность",0,3,.05f,1).visibleWhen(()->part("Насыщенность"));
    public static final SliderSetting WORLD_HOUR=new SliderSetting("Время мира",0,24,1,12).visibleWhen(()->part("Время"));
    public static final ColorSetting FOG_COLOR=new ColorSetting("Цвет тумана",0xff8b5cf6).visibleWhen(()->part("Туман"));
    public static final SliderSetting FOG_DISTANCE=new SliderSetting("Дальность",10,255,5,80).visibleWhen(()->part("Туман"));
    public static final Setting[] SETTINGS={PARTS,WEATHER,SKY,HOUR,WATER,DROPS,WIND,STORM,SNOW_DEPTH,MIST_RANGE,MIST,HAZE,REFLECT,
            LIGHT,SATURATION,WORLD_HOUR,FOG_COLOR,FOG_DISTANCE};
    static {
        PARTS.toggle(PARTS.options.indexOf("Погода"));
        PARTS.toggle(PARTS.options.indexOf("Время"));
        PARTS.toggle(PARTS.options.indexOf("Туман"));
    }

    private WorldTweaks() {}

    public static boolean active() { return ClientModules.WORLD_TWEAKS!=null && ClientModules.WORLD_TWEAKS.enabled(); }
    public static boolean part(String name) { return PARTS.selected(name); }
    private static boolean on(String name) { return active() && part(name); }

    public static void tick(Minecraft mc) { Weather.tick(mc); }

    // Weather in the world

    public static float rainLevel(float server) { float rain=Weather.rainLevel(); return rain<0 ? server : rain; }
    public static float thunderLevel(float server) { float thunder=Weather.thunderLevel(); return thunder<0 ? server : thunder; }
    public static Biome.Precipitation precipitation(Biome.Precipitation biome) { return Weather.precipitation(biome); }

    // Light, saturation and time

    /** Brightness through the lightmap: the gamma the lightmap is built with is raised up to full bright. */
    public static void lightmap(LightmapRenderState state) {
        if(on("Свет")) state.brightness=Math.max(state.brightness,LIGHT.get()*10);
    }
    /** Saturation of the world part (the weather multiplies its own on top). */
    public static float saturation() { return on("Насыщенность") ? SATURATION.get() : 1; }
    /** Day time in ticks for the overworld clock, or -1 to keep the server's. Hour 6 is tick 0. */
    public static long time() {
        float hour=on("Время") ? WORLD_HOUR.get() : Weather.hour();
        if(hour<0) return -1;
        return Math.floorMod(Math.round(hour*1000)-6000,24000);
    }

    // Fog

    /** Fog distances: the world fog part wins over the weather fog. Not under water or lava. */
    public static void fog(Camera camera,FogData data) {
        if(active() && data!=null && camera.getFluidInCamera()==FogType.NONE) {
            if(part("Туман")) {
                float end=FOG_DISTANCE.get();
                data.environmentalStart=data.environmentalEnd=Float.MAX_VALUE;
                data.renderDistanceStart=2; data.renderDistanceEnd=end;
                data.skyEnd=data.cloudEnd=end;
            } else Weather.fog(camera,data);
        }
        WeatherFx.captureFog(data);
    }
    public static void fogColor(Camera camera,Vector4f color) {
        if(active() && camera.getFluidInCamera()==FogType.NONE) {
            if(part("Туман")) {
                int argb=FOG_COLOR.get();
                color.set(((argb>>16)&255)/255f,((argb>>8)&255)/255f,(argb&255)/255f,color.w);
            } else Weather.fogColor(camera,color);
        }
        WeatherFx.captureFogColor(color);
    }
}
