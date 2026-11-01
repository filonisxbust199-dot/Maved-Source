package ess.ll.client.module.render;

import ess.ll.client.core.SimpleVoiceChatCompatibility;
import ess.ll.client.event.HudRenderEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.esp.EntityEspStyle;
import ess.ll.client.features.social.FriendManager;
import ess.ll.client.module.*;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.render.targetfx.internal.ScreenProjectionMath;
import ess.ll.client.render.EspPlates;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.UiRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/**
 * One shared appearance style with a separate entity-category selector.
 */
public final class EntityESP extends ClientModule {
    private final EntityEspStyle appearance;
    private final MultiToggleSetting shown;

    /**
     * Name plate look: one row with head, HP and gear; a pill with gear above it; or the old flat label.
     */
    private final ModeSetting plate;

    /**
     * Frosted glass behind every plate and tile, and a soft halo around them.
     */
    private final ToggleSetting blur, glow;
    private final MsdfFont font = MsdfFont.ESP;

    public EntityESP() {
        super(ModuleCategory.Render, ModuleSubCategory.World);
        shown = new MultiToggleSetting("Кого отображать").addOption(NamedOption.of("Игроки", true)).addOption(NamedOption.of("Друзья", true)).addOption(NamedOption.of("Себя", false)).addOption(NamedOption.of("Мобы", false)).addOption(NamedOption.of("Жители", false)).addOption(NamedOption.of("Предметы", true));
        shown.withContainer(this);
        plate = new ModeSetting("Плашка").addModes("В строку", "Пилюля", "Классика").withDefault("В строку");
        plate.withContainer(this);
        blur = new ToggleSetting("Блюр фона").withDefault(true).withContainer(this);
        glow = new ToggleSetting("Свечение фона").withDefault(true).withContainer(this);
        appearance = new EntityEspStyle(this, "Общие", "", EntityEspStyle.EspTarget.PLAYER, SimpleVoiceChatCompatibility.isReady(), true, true);
        setName("EntityESP");
    }

    private EntityEspStyle style(Entity e) {
        if (e instanceof ItemEntity)
            return shown.isSelected("Предметы") ? appearance : null;
        if (e instanceof Player p) {
            if (e == mc.player)
                return shown.isSelected("Себя") ? appearance : null;
            if (ess.ll.client.util.ModuleManager.INSTANCE.antiBot().isBot(p))
                return null;
            return shown.isSelected(FriendManager.INSTANCE.isFriend(p) ? "Друзья" : "Игроки") ? appearance : null;
        }
        if (e instanceof Villager || e instanceof WanderingTrader)
            return shown.isSelected("Жители") ? appearance : null;
        return e instanceof Mob && shown.isSelected("Мобы") ? appearance : null;
    }

    @Subscribe
    private void onHud(HudRenderEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null)
            return;
        GuiGraphicsExtractor g = event.getGraphics();
        EspPlates.blur = blur.isEnabled();
        EspPlates.glow = glow.isEnabled();
        // Use the live camera and HUD frame's delta together. The world-submit
        // matrices can be a frame behind the HUD when a player walks/turns.
        var camera = mc.gameRenderer.mainCamera();
        Vec3 origin = camera.position();
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Quaternionf inverseRotation = new Quaternionf(camera.rotation()).conjugate();
        Matrix4f cameraEffects = ScreenProjectionMath.cameraEffectsCopy();
        float aspect = (float) mc.getWindow().getWidth() / mc.getWindow().getHeight();
        float fovScale = (float) (1.0 / Math.tan(Math.toRadians(camera.getFov()) / 2.0));
        ArrayList<Entity> visible = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) visible.add(e);
        if (!mc.options.getCameraType().isFirstPerson() && visible.stream().noneMatch(e -> e == mc.player))
            visible.add(mc.player);
        for (Entity e : visible) {
            if (e == null || !e.isAlive() || e.isRemoved())
                continue;
            EntityEspStyle entityEspStyle = style(e);
            if (entityEspStyle == null || !entityEspStyle.isVisible())
                continue;
            if (e == mc.player && mc.options.getCameraType().isFirstPerson())
                continue;
            if (e.distanceToSqr(origin) > 256 * 256)
                continue;
            Vec3 pos = e.getPosition(partial);
            AABB current = e.getBoundingBox();
            double halfWidth = (current.maxX - current.minX) / 2, halfDepth = (current.maxZ - current.minZ) / 2;
            AABB box = new AABB(pos.x - halfWidth, pos.y, pos.z - halfDepth, pos.x + halfWidth, pos.y + current.maxY - current.minY + .2 - (e.isShiftKeyDown() ? .1 : 0), pos.z + halfDepth);
            float x = Float.MAX_VALUE, y = Float.MAX_VALUE, r = -Float.MAX_VALUE, b = -Float.MAX_VALUE;
            boolean behind = false;
            for (int i = 0; i < 8; i++) {
                Vector3f v = new Vector3f((float) (((i & 1) == 0 ? box.minX : box.maxX) - origin.x), (float) (((i & 2) == 0 ? box.minY : box.maxY) - origin.y), (float) (((i & 4) == 0 ? box.minZ : box.maxZ) - origin.z));
                v.rotate(inverseRotation);
                cameraEffects.transformPosition(v);
                if (v.z >= -.001f) {
                    behind = true;
                    break;
                }
                float sx = (.5f + v.x * fovScale / aspect / (-v.z) * .5f) * g.guiWidth();
                float sy = (.5f - v.y * fovScale / (-v.z) * .5f) * g.guiHeight();
                x = Math.min(x, sx);
                y = Math.min(y, sy);
                r = Math.max(r, sx);
                b = Math.max(b, sy);
            }
            if (behind || r < 0 || b < 0 || x > g.guiWidth() || y > g.guiHeight() || r - x < 2 || b - y < 3)
                continue;
            draw(g, e, entityEspStyle, x, y, r, b);
        }
    }

    private void draw(GuiGraphicsExtractor g, Entity e, EntityEspStyle entityEspStyle, float x, float y, float r, float b) {
        int color = entityEspStyle.isStaticColor() ? entityEspStyle.getBoxColor().getArgb() : 0xff58caff;
        if (entityEspStyle.isBoxEnabled()) {
            if (entityEspStyle.isCornerBox())
                corners(g, x, y, r, b, color);
            else
                frame(g, x, y, r, b, color);
        }
        if (entityEspStyle.isHealthBarEnabled() && e instanceof LivingEntity living) {
            float health = Math.clamp(living.getHealth() / Math.max(1, living.getMaxHealth()), 0, 1);
            int high = entityEspStyle.getHealthBarMode().is("Индивидуальный") ? entityEspStyle.getHealthBarTopColor().getArgb() : entityEspStyle.getHealthBarMode().is("Клиентский") ? color : 0xff00ff00;
            int low = entityEspStyle.getHealthBarMode().is("Индивидуальный") ? entityEspStyle.getHealthBarBottomColor().getArgb() : 0xffff4040;
            int red = (int) (((high >> 16) & 255) * health + ((low >> 16) & 255) * (1 - health));
            int green = (int) (((high >> 8) & 255) * health + ((low >> 8) & 255) * (1 - health));
            int blue = (int) ((high & 255) * health + (low & 255) * (1 - health));
            UiRenderer.rect(g, x - 5, y, 2, b - y, 0xb0000000);
            UiRenderer.rect(g, x - 5, b - (b - y) * health, 2, (b - y) * health, 0xff000000 | red << 16 | green << 8 | blue);
        }
        if (!plate.is("Классика")) {
            if (entityEspStyle.isNameTagEnabled())
                EspPlates.draw(g, font, e, plate.is("Пилюля"), entityEspStyle.isArmorEnabled(), (x + r) / 2, y);
            if (entityEspStyle.isTracersEnabled())
                line(g, g.guiWidth() / 2f, g.guiHeight(), (x + r) / 2, (y + b) / 2, entityEspStyle.tracerArgb(), entityEspStyle.getTracerWidthValue());
            return;
        }
        if (entityEspStyle.isNameTagEnabled()) {
            var name = net.minecraft.network.chat.Component.empty().append(EspPlates.name(e));
            if (e instanceof LivingEntity living)
                name.append(" [" + String.format(java.util.Locale.ROOT, "%.1f", living.getHealth()) + "]");
            float size = 6.7f, tw = EspPlates.nameWidth(name, size), px = (x + r - tw) / 2, py = y - 9;
            boolean friend = e instanceof Player player && FriendManager.INSTANCE.isFriend(player);
            float plateY = py - 1.5f, plateHeight = 8.5f;
            EspPlates.background(g, px - 4, plateY, tw + 8, plateHeight, 1.5f, friend ? 0xa0145a30 : 0x641e1e1e, friend);
            EspPlates.drawName(g, name, px, plateY + plateHeight / 2, size, friend);
        }
        if (e instanceof Player p)
            drawEquipment(g, p, entityEspStyle, x, y, r, b);
        if (entityEspStyle.isTracersEnabled())
            line(g, g.guiWidth() / 2f, g.guiHeight(), (x + r) / 2, (y + b) / 2, entityEspStyle.tracerArgb(), entityEspStyle.getTracerWidthValue());
    }

    private void drawEquipment(GuiGraphicsExtractor g, Player p, EntityEspStyle entityEspStyle, float x, float y, float r, float b) {
        if (!entityEspStyle.isArmorEnabled() && !entityEspStyle.isHeldItemsEnabled())
            return;
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if (entityEspStyle.isArmorEnabled()) {
            stacks.add(p.getOffhandItem());
            stacks.add(p.getMainHandItem());
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) stacks.add(p.getItemBySlot(slot));
            stacks.removeIf(ItemStack::isEmpty);
            float tile = 13f, step = 15f;
            float ix = (x + r - (stacks.size() * step - 2)) / 2, iy = y - 25;
            for (ItemStack stack : stacks) {
                EspPlates.background(g, ix, iy, tile, tile, 3, 0x641e1e1e, false);
                g.pose().pushMatrix();
                g.pose().translate(ix + .9f, iy + .9f);
                g.pose().scale(.7f, .7f);
                g.item(stack, 0, 0);
                g.pose().popMatrix();
                if (stack.isDamaged()) {
                    float fraction = 1 - stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage());
                    UiRenderer.rect(g, ix + 1, iy + 11, 11 * fraction, 1.5f, 0xff62da8d);
                }
                if (stack.getCount() > 1) {
                    String count = Integer.toString(stack.getCount());
                    font.midCenter(g, count, ix + tile / 2, iy + tile / 2, 5.5f, 0xffffffff);
                }
                if (entityEspStyle.isEnchantmentsEnabled() && stack.isEnchanted()) {
                    float ey = iy - 6;
                    for (var enchant : stack.getEnchantments().keySet()) {
                        int level = stack.getEnchantments().getLevel(enchant);
                        String id = enchant.getRegisteredName();
                        String shortName = enchantName(id);
                        if (shortName == null)
                            continue;
                        String label = shortName + level;
                        font.draw(g, label, ix + tile / 2 - font.width(label, 5.5f) / 2, ey, 5.5f, 0xffffffff);
                        ey -= 5.5f;
                    }
                }
                ix += step;
            }
        }
        if (entityEspStyle.isHeldItemsEnabled()) {
            float center = (x + r) / 2, yy = b + 5;
            for (ItemStack stack : new ItemStack[] { p.getMainHandItem(), p.getOffhandItem() }) {
                if (stack.isEmpty())
                    continue;
                String label = stack.getStyledHoverName().getString();
                float width = font.width(label, 6.7f);
                float plateY = yy - 1.5f, plateHeight = 8.5f;
                EspPlates.background(g, center - width / 2 - 4, plateY, width + 8, plateHeight, 1.5f, 0x641e1e1e, false);
                font.mid(g, label, center - width / 2, plateY + plateHeight / 2, 6.7f, 0xffffffff);
                yy += 9;
            }
        }
    }

    private static String enchantName(String id) {
        if (id.contains("unbreaking"))
            return "Un";
        if (id.contains("sharpness"))
            return "Sh";
        if (id.contains("protection"))
            return "Pr";
        if (id.contains("mending"))
            return "Me";
        if (id.contains("depth_strider"))
            return "DS";
        if (id.contains("respiration"))
            return "Re";
        if (id.contains("efficiency"))
            return "Ef";
        if (id.contains("feather_falling"))
            return "FF";
        if (id.contains("aqua_affinity"))
            return "AA";
        return null;
    }

    private static void frame(GuiGraphicsExtractor g, float x, float y, float r, float b, int c) {
        UiRenderer.rect(g, x, y, r - x, 1, c);
        UiRenderer.rect(g, x, b - 1, r - x, 1, c);
        UiRenderer.rect(g, x, y, 1, b - y, c);
        UiRenderer.rect(g, r - 1, y, 1, b - y, c);
    }

    private static void corners(GuiGraphicsExtractor g, float x, float y, float r, float b, int c) {
        float w = Math.min(8, (r - x) / 3), h = Math.min(8, (b - y) / 3);
        for (float xx : new float[] { x, r - w }) for (float yy : new float[] { y, b - h }) {
            UiRenderer.rect(g, xx, yy, w, 1, c);
            UiRenderer.rect(g, xx == x ? xx : r - 1, yy, 1, h, c);
        }
    }

    private static void line(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int c, float t) {
        float dx = x2 - x1, dy = y2 - y1, len = (float) Math.hypot(dx, dy);
        if (len <= 0)
            return;
        g.pose().pushMatrix();
        g.pose().translate(x1, y1);
        g.pose().rotate((float) Math.atan2(dy, dx));
        UiRenderer.rect(g, 0, -t / 2, len, t, c);
        g.pose().popMatrix();
    }
}
