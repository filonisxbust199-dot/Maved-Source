package ess.ll.client.weather;

import ess.ll.client.module.render.WorldTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Zenith's WeatherModule, driven by the WorldTweaks weather settings: a weather mode (rain, mist, snow) is a
 * channel of values (sky palette, clouds, sun, haze, fog, wetness, drops, wind, snow) bent by the time of day and
 * by an optional sky preset (clear, storm, stars). The client world rains or snows as the channel says, the vanilla
 * columns are replaced by Precipitation's drops and flakes, the snow cover, mist puffs and footprints; the sky,
 * wet gloss and mist are full-screen passes (WeatherFx). Turning it on or off fades over about a second.
 */
public final class Weather {
    private static final long START = System.currentTimeMillis();
    private static final long LIGHTNING_SEED = 118049640925L;
    private static WeatherChannel channel, base;
    private static float channelHour = Float.NaN;
    private static WeatherChannel.Data channelData;
    private static SkyPreset channelSky;
    private static final WeatherBatch batch = new WeatherBatch();
    private static final Footprints footprints = new Footprints();
    private static ColumnHeights columns;
    private static int columnsX = Integer.MIN_VALUE, columnsZ = Integer.MIN_VALUE;
    private static long columnsAt;

    /**
     * Fade of the whole weather (900 ms) and of wetness (slow: 4.5 s in, 14 s to dry).
     */
    private static float fadeFrom, wetFrom;
    private static long fadeAt = System.currentTimeMillis(), wetAt = fadeAt;
    private static boolean on;
    private static boolean frosted;
    private static float lastFlash;

    private Weather() {
    }

    /**
     * Settings say weather is wanted (the fades follow this).
     */
    private static boolean wanted() {
        return WorldTweaks.active() && WorldTweaks.part("Погода");
    }

    private static boolean rain() {
        return WorldTweaks.WEATHER.is("Дождь");
    }

    private static boolean mist() {
        return WorldTweaks.WEATHER.is("Туман");
    }

    private static boolean snow() {
        return WorldTweaks.WEATHER.is("Снег");
    }

    /**
     * Client tick: fades start when the wish changes, frost repaints foliage, columns follow the world.
     */
    public static void tick(Minecraft mc) {
        boolean want = wanted();
        if (want != on) {
            fadeFrom = fade();
            wetFrom = wet();
            fadeAt = wetAt = System.currentTimeMillis();
            on = want;
            if (!want) {
                columns = null;
                columnsX = Integer.MIN_VALUE;
                footprints.clear();
            }
        }
        boolean frost = on && channel().flag;
        if (frost != frosted) {
            frosted = frost;
            FrostFoliage.enabled = frost;
            if (mc.level != null)
                mc.execute(mc::reloadResourcePacks);
        }
        if (mc.level != null && active() && channel().floatValue13 > 0) {
            float flash = flash();
            if (flash > .6f && lastFlash <= .6f && mc.player != null) {
                var random = mc.level.getRandom();
                float angle = random.nextFloat() * Mth.TWO_PI, radius = 40 + random.nextFloat() * 90;
                mc.level.playLocalSound(mc.player.getX() + Mth.cos(angle) * radius, mc.player.getY(), mc.player.getZ() + Mth.sin(angle) * radius, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.2f * fade(), .8f + random.nextFloat() * .2f, true);
            }
            lastFlash = flash;
        }
    }

    /**
     * The channel for the current mode, time, sliders and sky, rebuilt only when one of them changes.
     */
    public static WeatherChannel channel() {
        WeatherChannel mode = modeChannel();
        float hour = WorldTweaks.HOUR.get();
        SkyPreset sky = sky();
        WeatherChannel.Data data = data();
        if (channel == null || mode != base || hour != channelHour || !same(channelData, data) || sky != channelSky) {
            channel = mode.build(DayLight.sample(hour), data, sky);
            base = mode;
            channelHour = hour;
            channelData = data;
            channelSky = sky;
        }
        return channel;
    }

    private static WeatherChannel modeChannel() {
        if (snow())
            return WeatherChannel.INSTANCE3;
        if (mist())
            return WeatherChannel.INSTANCE4;
        return WorldTweaks.WATER.is("Лужи") ? WeatherChannel.INSTANCE : WeatherChannel.INSTANCE2;
    }

    private static SkyPreset sky() {
        return switch (WorldTweaks.SKY.get()) {
            case "Ясно" ->
                SkyPreset.INSTANCE;
            case "Шторм" ->
                SkyPreset.INSTANCE2;
            case "Звёзды" ->
                SkyPreset.INSTANCE3;
            default ->
                null;
        };
    }

    private static WeatherChannel.Data data() {
        boolean mist = mist();
        return new WeatherChannel.Data(percent(WorldTweaks.DROPS), percent(WorldTweaks.WIND), percent(WorldTweaks.REFLECT), mist ? percent(WorldTweaks.MIST) : 1, mist ? percent(WorldTweaks.HAZE) : 1, mist ? WorldTweaks.MIST_RANGE.get() : -1, WorldTweaks.STORM.get() && rain());
    }

    private static boolean same(WeatherChannel.Data a, WeatherChannel.Data b) {
        return a != null && a.getFloatValue() == b.getFloatValue() && a.getFloatValue2() == b.getFloatValue2() && a.getFloatValue3() == b.getFloatValue3() && a.getFloatValue4() == b.getFloatValue4() && a.getFloatValue5() == b.getFloatValue5() && a.getFloatValue6() == b.getFloatValue6() && a.isFlag() == b.isFlag();
    }

    private static float percent(ess.ll.client.setting.Settings.SliderSetting slider) {
        return slider.get() / 100f;
    }

    /**
     * Seconds since start, the clock of every animation.
     */
    public static float time() {
        return (System.currentTimeMillis() - START) / 1000f;
    }

    /**
     * 0..1, how much of the weather is shown (smooth over 900 ms).
     */
    public static float fade() {
        float target = on ? 1 : 0;
        float t = Math.clamp((System.currentTimeMillis() - fadeAt) / 900f, 0, 1);
        t = t * t * (3 - 2 * t);
        return fadeFrom + (target - fadeFrom) * t;
    }

    /**
     * Wetness, which soaks in faster than it dries.
     */
    public static float wet() {
        float target = on ? 1 : 0;
        float span = target > wetFrom ? 4500 : 14000;
        float t = Math.clamp((System.currentTimeMillis() - wetAt) / span, 0, 1);
        return wetFrom + (target - wetFrom) * t;
    }

    public static boolean active() {
        var mc = Minecraft.getInstance();
        return fade() > .01f && mc.level != null && mc.player != null;
    }

    public static boolean glossActive() {
        return wet() > .01f && channel().floatValue23 > .001f && Minecraft.getInstance().level != null;
    }

    /**
     * Lightning: a seeded train of flashes, the rate from the channel (storm only).
     */
    public static float flash() {
        return flash(time(), channel().floatValue13) * fade();
    }

    private static float flash(float time, float rate) {
        if (rate <= 0)
            return 0;
        int index = (int) Math.floor(time * rate);
        float best = 0;
        for (int i = index - 1; i <= index; i++) {
            float at = (i + WeatherBatch.hashFloat(LIGHTNING_SEED, i, 0)) / rate;
            float since = time - at;
            if (since < 0)
                continue;
            float strength = .35f + .65f * WeatherBatch.hashFloat(LIGHTNING_SEED, i, 1);
            float pulse = pulse(since, .09f) + pulse(since - .13f, .17f) * .65f;
            best = Math.max(best, Math.min(1, pulse) * strength);
        }
        return best;
    }

    private static float pulse(float t, float decay) {
        return t < 0 ? 0 : (float) (Math.exp(-t / decay) * (1 - Math.exp(-t / .012)));
    }

    // World hooks
    /**
     * Day time in hours from the weather, or -1 (the world part's time wins over it).
     */
    public static float hour() {
        return fade() < .15f ? -1 : channel().floatValue17;
    }

    /**
     * Rain level the client world shows, or -1 to keep the server's.
     */
    public static float rainLevel() {
        float fade = fade();
        if (fade <= .01f)
            return -1;
        WeatherChannel c = channel();
        return c.intValue2 == 0 ? 0 : Math.min(1, c.floatValue18 * fade);
    }

    public static float thunderLevel() {
        float rain = rainLevel();
        if (rain < 0)
            return -1;
        return channel().floatValue13 > 0 ? rain : 0;
    }

    /**
     * Forced precipitation: -1 keeps the biome's.
     */
    public static Biome.Precipitation precipitation(Biome.Precipitation biome) {
        if (rainLevel() < 0)
            return biome;
        return switch (channel().intValue2) {
            case 1 ->
                Biome.Precipitation.RAIN;
            case 2 ->
                Biome.Precipitation.SNOW;
            default ->
                Biome.Precipitation.NONE;
        };
    }

    /**
     * Vanilla rain and snow columns are hidden while the drops of this weather fall.
     */
    public static boolean hidesVanilla() {
        return rainLevel() > 0;
    }

    /**
     * Frost tint of grass and leaves (snow).
     */
    public static float frost() {
        return frosted ? .85f : 0;
    }

    /**
     * Saturation of the graded frame.
     */
    public static float saturation() {
        return Mth.lerp(fade(), 1, channel().floatValue16);
    }

    /**
     * Tint of the graded frame (rgb, and brightness with the lightning flash in a).
     */
    public static void tint(float[] out) {
        float fade = fade();
        WeatherChannel c = channel();
        out[0] = Mth.lerp(fade, 1, c.floatValues11[0]);
        out[1] = Mth.lerp(fade, 1, c.floatValues11[1]);
        out[2] = Mth.lerp(fade, 1, c.floatValues11[2]);
        out[3] = Mth.lerp(fade, 1, c.floatValue15) * (1 + flash() * .85f);
    }

    /**
     * Weather fog: a near start and the channel's distance, in the channel's colour.
     */
    public static boolean fog(Camera camera, FogData data) {
        if (fade() <= .01f || camera.getFluidInCamera() != FogType.NONE)
            return false;
        WeatherChannel c = channel();
        float end = data.renderDistanceEnd > 0 ? Mth.lerp(fade(), data.renderDistanceEnd, c.floatValue14) : c.floatValue14;
        data.environmentalStart = data.environmentalEnd = Float.MAX_VALUE;
        data.renderDistanceStart = 2;
        data.renderDistanceEnd = end;
        data.skyEnd = data.cloudEnd = end;
        return true;
    }

    public static void fogColor(Camera camera, Vector4f color) {
        float fade = fade();
        if (fade <= .01f || camera.getFluidInCamera() != FogType.NONE)
            return;
        int argb = channel().intValue;
        color.x = Mth.lerp(fade, color.x, ((argb >> 16) & 255) / 255f);
        color.y = Mth.lerp(fade, color.y, ((argb >> 8) & 255) / 255f);
        color.z = Mth.lerp(fade, color.z, (argb & 255) / 255f);
        color.w = color.w * Mth.lerp(fade, 1, .9f);
    }

    // Drawing
    /**
     * Dev check: what the weather drew last.
     */
    public static String debug() {
        return "fade " + fade() + " wet " + wet() + " snow indices " + SnowMesh.indices() + " columns " + (columns != null) + " gloss " + glossActive();
    }

    /**
     * After the level, inside the main pass: drops, flakes, ripples, snow cover, mist and footprints.
     */
    public static void render(Matrix4f view) {
        if (!active())
            return;
        var mc = Minecraft.getInstance();
        float fade = fade();
        WeatherChannel c = channel();
        Camera camera = mc.gameRenderer.mainCamera();
        var eye = camera.position();
        boolean needColumns = c.flag || c.intValue2 != 0 || !glossActive();
        ColumnHeights heights = needColumns ? columns(mc, eye.x, eye.z) : null;
        if (needColumns && heights == null)
            return;
        float time = time();
        long now = System.currentTimeMillis();
        batch.beginFrame(camera, 0);
        footprints.update(mc.level, mc.level.players(), c, now);
        SnowMesh.setSheen(percent(WorldTweaks.REFLECT));
        SnowMesh.updateLighting(eye, c, c.floatValue23);
        SnowCover.render(batch, heights, c, fade, footprints, now, eye.x, eye.y, eye.z, percent(WorldTweaks.SNOW_DEPTH));
        footprints.render(batch, c, fade, now, eye.x, eye.y, eye.z);
        if (heights != null && !glossActive())
            MistPuffs.render(batch, heights, c, fade, time, eye.x, eye.y, eye.z);
        if (heights != null)
            Precipitation.renderRain(batch, heights, c, fade, time, eye.x, eye.y, eye.z);
        SnowMesh.render(view);
        batch.draw(view);
    }

    /**
     * Ground heights in a 52-block square around the camera, refreshed every 400 ms and shifted as it moves.
     */
    private static ColumnHeights columns(Minecraft mc, double x, double z) {
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        long now = System.currentTimeMillis();
        if (columns == null) {
            columns = ColumnHeights.capture(mc.level, cx, cz, 52);
            columnsAt = now;
        } else if (now - columnsAt > 400) {
            columns.refresh(mc.level, cx, cz);
            columnsAt = now;
        } else if (cx != columnsX || cz != columnsZ)
            columns.shiftCenter(mc.level, cx, cz);
        SnowChanges.refreshChangedColumns(mc.level, columns);
        columnsX = cx;
        columnsZ = cz;
        return columns;
    }
}
