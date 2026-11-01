package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Trails: a glowing trace behind moving players. Every tick a player moves, their position is kept; the trace
 * runs through the kept positions up to where the player is drawn this frame and fades with age. It is a line
 * at the waist, a band from the feet up, a line with a bright head, or particles dropped along the way.
 */
public final class Trails {
    private static final String RIBBON = "Лента", SHEET = "Шлейф", PARTICLES = "Частицы", COMET = "Комета";
    public static final ModeSettings MODE = new ModeSettings("Вид", RIBBON, SHEET, PARTICLES, COMET);
    public static final MultiBooleanSettings WHO = new MultiBooleanSettings("Для", "Себя", "Игроков", "Цели").none();
    public static final SliderSetting LENGTH = new SliderSetting("Длина, с", .3f, 3, .1f, 1.2f);
    public static final SliderSetting WIDTH = new SliderSetting("Ширина", .3f, 3, .1f, 1);
    public static final ModeSettings COLOR = new ModeSettings("Цвет", "Тема", "Градиент", "Радуга", "Свой");
    public static final ColorSetting OWN = new ColorSetting("Свой цвет", 0xff8f7cff).visibleWhen(() -> COLOR.is("Свой") || COLOR.is("Градиент"));
    public static final ColorSetting SECOND = new ColorSetting("Второй цвет", 0xff4fd6ff).visibleWhen(() -> COLOR.is("Градиент"));
    public static final SliderSetting GLOW = new SliderSetting("Свечение", 0, 1, .05f, .7f);
    public static final Setting[] SETTINGS = { MODE, WHO, LENGTH, WIDTH, COLOR, OWN, SECOND, GLOW };

    static {
        WHO.toggle(0);
        GlowFx.add(Trails::paint);
    }

    /**
     * A kept position, when it was kept, and for particles a drift.
     */
    private record Point(Vec3 pos, long time, Vec3 drift) {
    }

    private static final Map<Integer, ArrayDeque<Point>> TRAILS = new HashMap<>();

    /**
     * Close to the camera a trace would cover the view (your own, in first person).
     */
    private static final double NEAR = 1.1;

    private Trails() {
    }

    public static boolean enabled() {
        return ClientModules.TRAILS != null && ClientModules.TRAILS.enabled();
    }

    /**
     * Dev check: kept points of all traces.
     */
    public static int points() {
        return TRAILS.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    private static boolean wanted(Minecraft mc, Player player) {
        if (player.isSpectator() || player.isInvisible())
            return false;
        if (player == mc.player)
            return WHO.selected("Себя");
        if (WHO.selected("Игроков"))
            return true;
        return WHO.selected("Цели") && ess.ll.client.module.combat.AttackAura.target == player;
    }

    /**
     * Client tick: keeps the position of every followed player that moved, and drops what is too old.
     */
    public static void tick(Minecraft mc) {
        if (!enabled() || mc.level == null || mc.player == null) {
            TRAILS.clear();
            return;
        }
        if (mc.isPaused())
            return;
        long now = System.currentTimeMillis(), keep = (long) (LENGTH.get() * 1000);
        var random = ThreadLocalRandom.current();
        Map<Integer, ArrayDeque<Point>> seen = new HashMap<>();
        for (Player player : mc.level.players()) {
            if (!wanted(mc, player))
                continue;
            var points = TRAILS.getOrDefault(player.getId(), new ArrayDeque<>());
            Vec3 at = player.position();
            Point last = points.peekLast();
            // A teleport would draw a line across the map.
            if (last != null && last.pos.distanceToSqr(at) > 36)
                points.clear();
            if (last == null || last.pos.distanceToSqr(at) > 1e-4) {
                Vec3 drift = new Vec3(random.nextDouble(-.4, .4), random.nextDouble(-.2, .9), random.nextDouble(-.4, .4)).scale(player.getBbHeight());
                points.addLast(new Point(at, now, drift));
            }
            while (!points.isEmpty() && now - points.peekFirst().time > keep) points.removeFirst();
            if (points.size() > 200)
                points.removeFirst();
            seen.put(player.getId(), points);
        }
        TRAILS.clear();
        TRAILS.putAll(seen);
    }

    private static int color(float along, int index) {
        return switch (COLOR.get()) {
            case "Градиент" ->
                Theme.mix(OWN.get(), SECOND.get(), along);
            case "Радуга" ->
                java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f + along * .6f, .7f, 1);
            case "Свой" ->
                OWN.get();
            default ->
                Theme.mix(Theme.accent(), Theme.accentLight(), along * .6f);
        };
    }

    private static void paint(GlowFx fx) {
        if (!enabled() || TRAILS.isEmpty())
            return;
        var mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        float keep = LENGTH.get() * 1000, width = WIDTH.get(), glow = GLOW.get();
        for (var entry : TRAILS.entrySet()) {
            if (!(mc.level.getEntity(entry.getKey()) instanceof Player player))
                continue;
            List<Point> points = new ArrayList<>(entry.getValue());
            if (points.isEmpty())
                continue;
            Vec3 head = player.getPosition(fx.partial);
            // The newest kept point is where the player was last tick; the trace ends where it is drawn now.
            points.add(new Point(head, now, Vec3.ZERO));
            float height = player.getBbHeight();
            switch (MODE.get()) {
                case SHEET ->
                    sheet(fx, points, now, keep, height, glow);
                case PARTICLES ->
                    particles(fx, points, now, keep, width, glow);
                default ->
                    line(fx, points, now, keep, height, width, glow, MODE.is(COMET));
            }
        }
    }

    private static float age(Point p, long now, float keep) {
        return Math.clamp((now - p.time) / keep, 0, 1);
    }

    private static void line(GlowFx fx, List<Point> points, long now, float keep, float height, float width, float glow, boolean comet) {
        int n = points.size();
        List<Vec3> at = new ArrayList<>(n);
        float[] widths = new float[n], strengths = new float[n];
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            Point p = points.get(i);
            Vec3 pos = p.pos.add(0, height * .5, 0);
            float fresh = 1 - age(p, now, keep);
            at.add(pos);
            widths[i] = .045f * width * (comet ? .25f + .75f * fresh : 1);
            strengths[i] = fresh * fresh * (pos.distanceTo(fx.eye) < NEAR ? 0 : 1);
            colors[i] = color(1 - fresh, i);
        }
        fx.strip(at, widths, colors, strengths, glow);
        if (comet && at.getLast().distanceTo(fx.eye) > NEAR)
            fx.sprite(at.getLast(), .24f * width, GlowFx.DOT, colors[n - 1], 1, glow, 0);
    }

    private static void sheet(GlowFx fx, List<Point> points, long now, float keep, float height, float glow) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Point a = points.get(i), b = points.get(i + 1);
            if (a.pos.distanceTo(fx.eye) < NEAR || b.pos.distanceTo(fx.eye) < NEAR)
                continue;
            float sa = 1 - age(a, now, keep), sb = 1 - age(b, now, keep);
            sa *= sa * .8f;
            sb *= sb * .8f;
            int ca = color(1 - sa, i), cb = color(1 - sb, i + 1);
            double bottom = .05, top = height * .9;
            fx.vertex(a.pos.x, a.pos.y + bottom, a.pos.z, ca, sa, 0, 0, GlowFx.SHEET, glow, 0);
            fx.vertex(a.pos.x, a.pos.y + top, a.pos.z, ca, sa, 0, 1, GlowFx.SHEET, glow, 0);
            fx.vertex(b.pos.x, b.pos.y + top, b.pos.z, cb, sb, 1, 1, GlowFx.SHEET, glow, 0);
            fx.vertex(b.pos.x, b.pos.y + bottom, b.pos.z, cb, sb, 1, 0, GlowFx.SHEET, glow, 0);
            fx.quadDone();
        }
    }

    /**
     * Each kept point leaves a particle that drifts up off the body and shrinks away.
     */
    private static void particles(GlowFx fx, List<Point> points, long now, float keep, float width, float glow) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Point p = points.get(i);
            float t = age(p, now, keep);
            Vec3 pos = p.pos.add(p.drift.x * .5, p.drift.y * .5 + .3 + t * .35, p.drift.z * .5);
            if (pos.distanceTo(fx.eye) < NEAR)
                continue;
            fx.sprite(pos, .15f * width * (1 - t * .5f), GlowFx.DOT, color(t, i), 1 - t, glow, 0);
        }
    }
}
