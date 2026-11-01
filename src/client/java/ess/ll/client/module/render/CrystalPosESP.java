package ess.ll.client.module.render;

import ess.ll.client.command.ClientData;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

/**
 * CrystalPosESP: where a crystal you place now pays off. Every obsidian or bedrock block in reach with room for a
 * crystal is weighed by the damage the blast deals to the nearest enemy against the damage it deals to you; the
 * best spots are outlined green (worth it), yellow (weak) or red (hurts you too much or would kill you), with
 * the two numbers over them. Friends from .friend are never the target.
 */
public final class CrystalPosESP {
    public static final SliderSetting RANGE = new SliderSetting("Дальность установки", 2, 6, .5f, 4.5f);
    public static final SliderSetting TARGET_RANGE = new SliderSetting("Дальность цели", 4, 16, 1, 12);
    public static final SliderSetting MIN_DAMAGE = new SliderSetting("Хороший урон", 1, 20, .5f, 6);
    public static final SliderSetting MAX_SELF = new SliderSetting("Макс. урон по себе", 1, 20, .5f, 6);
    public static final SliderSetting COUNT = new SliderSetting("Сколько показывать", 1, 10, 1, 3);
    public static final BooleanSetting LABELS = new BooleanSetting("Цифры урона", true);
    public static final SliderSetting GLOW = new SliderSetting("Свечение", 0, 1, .05f, .6f);
    public static final Setting[] SETTINGS = { RANGE, TARGET_RANGE, MIN_DAMAGE, MAX_SELF, COUNT, LABELS, GLOW };

    static {
        GlowFx.add(CrystalPosESP::paint);
    }

    private static final int GREEN = 0xff4fd18b, YELLOW = 0xffffd34f, RED = 0xffff4d4d;

    private record Spot(BlockPos base, float target, float self, int color) {
    }

    private static List<Spot> spots = List.of();
    private static int ticks;

    private CrystalPosESP() {
    }

    public static boolean enabled() {
        return ClientModules.CRYSTAL_POS_ESP != null && ClientModules.CRYSTAL_POS_ESP.enabled();
    }

    /**
     * Dev check: spots shown now.
     */
    public static int shown() {
        return spots.size();
    }

    /**
     * The blast check casts rays, so spots are weighed every other tick rather than each frame.
     */
    public static void tick(Minecraft mc) {
        if (!enabled() || mc.player == null || mc.level == null) {
            spots = List.of();
            return;
        }
        if (ticks++ % 2 != 0)
            return;
        Player me = mc.player, target = null;
        double best = TARGET_RANGE.get() * TARGET_RANGE.get();
        for (Player p : mc.level.players()) {
            if (p == me || p.isSpectator() || !p.isAlive() || ClientData.isFriend(p.getGameProfile().name()))
                continue;
            double d = p.distanceToSqr(me);
            if (d < best) {
                best = d;
                target = p;
            }
        }
        if (target == null) {
            spots = List.of();
            return;
        }
        Vec3 eye = me.getEyePosition();
        float range = RANGE.get(), health = me.getHealth() + me.getAbsorptionAmount();
        int r = (int) Math.ceil(range);
        BlockPos at = me.blockPosition();
        List<Spot> found = new ArrayList<>();
        for (BlockPos base : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
            if (!placeable(mc, base))
                continue;
            Vec3 blast = new Vec3(base.getX() + .5, base.getY() + 1, base.getZ() + .5);
            if (eye.distanceToSqr(base.getX() + .5, base.getY() + 1, base.getZ() + .5) > range * range)
                continue;
            float toTarget = ExplosionDamageUtils.damageTo(blast, target);
            if (toTarget < .5f)
                continue;
            float toSelf = ExplosionDamageUtils.damageTo(blast, me);
            int color = toSelf >= health || toSelf > MAX_SELF.get() ? RED : toTarget >= MIN_DAMAGE.get() && toTarget > toSelf ? GREEN : YELLOW;
            found.add(new Spot(base.immutable(), toTarget, toSelf, color));
        }
        found.sort(Comparator.comparingDouble((Spot s) -> s.color == RED ? 1 : 0).thenComparingDouble(s -> -(s.target - s.self)));
        spots = found.subList(0, Math.min(found.size(), Math.round(COUNT.get())));
    }

    /**
     * Obsidian or bedrock with air above and no entity in the crystal's place.
     */
    private static boolean placeable(Minecraft mc, BlockPos base) {
        var state = mc.level.getBlockState(base);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK))
            return false;
        BlockPos up = base.above();
        if (!mc.level.getBlockState(up).isAir())
            return false;
        return mc.level.getEntities(null, new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1, up.getY() + 2, up.getZ() + 1)).isEmpty();
    }

    /**
     * The top of the block filled and its edges outlined, so the spot reads from any side.
     */
    private static void paint(GlowFx fx) {
        if (!enabled() || spots.isEmpty())
            return;
        float glow = GLOW.get();
        for (Spot spot : spots) {
            double x0 = spot.base.getX(), y = spot.base.getY() + 1.003, z0 = spot.base.getZ(), x1 = x0 + 1, z1 = z0 + 1;
            for (double[] q : new double[][] { { x0, z0 }, { x1, z0 }, { x1, z1 }, { x0, z1 } }) fx.vertex(q[0], y, q[1], spot.color & 0xffffff, .22f, 0, 0, GlowFx.FILL, 0, 1);
            fx.quadDone();
            Vec3 a = new Vec3(x0, y, z0), b = new Vec3(x1, y, z0), c = new Vec3(x1, y, z1), d = new Vec3(x0, y, z1);
            Vec3[] ring = { a, b, c, d, a };
            for (int i = 0; i < 4; i++) fx.line(ring[i], ring[i + 1], .025f, spot.color & 0xffffff, 1, 1, glow);
        }
    }

    /**
     * Damage to the target and to you over each spot.
     */
    public static void renderHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!enabled() || !LABELS.get() || spots.isEmpty())
            return;
        var font = MsdfFont.ESP;
        for (Spot spot : spots) {
            Vector2f at = GlowFx.screen(new Vec3(spot.base.getX() + .5, spot.base.getY() + 1.35, spot.base.getZ() + .5));
            if (at == null)
                continue;
            String text = String.format(Locale.ROOT, "%.1f", spot.target), self = String.format(Locale.ROOT, " / %.1f", spot.self);
            float size = 7, w = font.width(text, size) + font.width(self, size * .85f);
            UiRenderer.rounded(g, at.x - w / 2 - 4, at.y - size / 2 - 2.5f, w + 8, size + 5, 3, 0xc0101318);
            font.mid(g, text, at.x - w / 2, at.y, size, spot.color);
            font.mid(g, self, at.x - w / 2 + font.width(text, size), at.y, size * .85f, Theme.withAlpha(0xffffffff, .6f));
        }
    }
}
