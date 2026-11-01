package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Theme;
import ess.ll.client.render.targetfx.internal.GpuUniformBufferHelper;
import ess.ll.client.setting.Settings.*;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

/**
 * ShaderESP (from Zenith): targets glow through the vanilla entity outline, but the outline post chain is replaced
 * by ess-best:esp_glow, which draws a crisp outline, a soft halo and a fill that leans to the edges. Each target
 * glows in its own colour (theme, custom, by health or rainbow). Hooks: ShaderEspMixin (who glows and in what
 * colour) and ShaderEspChainMixin (the chain and its live settings).
 */
public final class ShaderESP {
    public static final MultiBooleanSettings TARGETS=new MultiBooleanSettings("Цели","Игроки","Без брони","Враждебные","Мирные","Предметы","Кристаллы");
    public static final BooleanSetting IGNORE_SELF=new BooleanSetting("Игнорировать себя",true);
    public static final SliderSetting MAX_DISTANCE=new SliderSetting("Дальность",6,240,1,90);
    public static final BooleanSetting THROUGH_WALLS=new BooleanSetting("Сквозь стены",true);
    public static final ModeSettings COLOR_MODE=new ModeSettings("Цвет","Тема","Свой","Здоровье","Радуга");
    public static final ColorSetting COLOR=new ColorSetting("Свой цвет",0xff8b5cf6).visibleWhen(()->COLOR_MODE.is("Свой"));
    public static final SliderSetting OUTLINE_WIDTH=new SliderSetting("Толщина обводки",0,8,.2f,2.4f);
    public static final SliderSetting GLOW_RADIUS=new SliderSetting("Радиус свечения",0,48,1,14);
    public static final SliderSetting GLOW_STRENGTH=new SliderSetting("Сила свечения",0,3,.05f,1.15f);
    public static final SliderSetting FILL_OPACITY=new SliderSetting("Заливка",0,1,.05f,.35f);
    public static final BooleanSetting PULSE=new BooleanSetting("Пульсация",true);
    public static final Setting[] SETTINGS={TARGETS,IGNORE_SELF,MAX_DISTANCE,THROUGH_WALLS,COLOR_MODE,COLOR,OUTLINE_WIDTH,GLOW_RADIUS,GLOW_STRENGTH,FILL_OPACITY,PULSE};
    static {
        TARGETS.toggle(TARGETS.options.indexOf("Мирные"));
        TARGETS.toggle(TARGETS.options.indexOf("Предметы"));
    }

    public static final Identifier CHAIN=Identifier.fromNamespaceAndPath("ess-best","esp_glow");
    private static final int LOW_HEALTH=0xffff3b30,FULL_HEALTH=0xff34c758;
    /** Visibility through walls, cached for one frame. */
    private static final Int2BooleanOpenHashMap VISIBLE=new Int2BooleanOpenHashMap();
    private static long visibleFrame=Long.MIN_VALUE;
    private static Vec3 visibleFrom=Vec3.ZERO;
    private static GpuBufferSlice uniforms;

    private ShaderESP() {}

    public static boolean active() { return ClientModules.SHADER_ESP!=null && ClientModules.SHADER_ESP.enabled(); }

    /** Whether the entity glows with the ShaderESP outline. */
    public static boolean glows(Entity entity) {
        var mc=Minecraft.getInstance();
        if(!active() || mc.player==null || mc.level==null || entity==null || entity.isRemoved()) return false;
        if(IGNORE_SELF.get() && entity==mc.player) return false;
        if(entity==mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return false;
        float range=MAX_DISTANCE.get();
        if(mc.player.distanceToSqr(entity)>range*range || !targeted(entity)) return false;
        return THROUGH_WALLS.get() || visible(mc,entity);
    }
    private static boolean targeted(Entity entity) {
        if(entity instanceof Player player) return TARGETS.selected(player.getArmorValue()==0 ? "Без брони" : "Игроки");
        if(entity instanceof EndCrystal) return TARGETS.selected("Кристаллы");
        if(entity instanceof ItemEntity) return TARGETS.selected("Предметы");
        if(entity instanceof Enemy) return TARGETS.selected("Враждебные");
        if(entity instanceof Animal) return TARGETS.selected("Мирные");
        return entity instanceof LivingEntity && TARGETS.selected("Враждебные");
    }
    private static boolean visible(Minecraft mc,Entity entity) {
        Vec3 camera=mc.gameRenderer.mainCamera().position();
        long frame=mc.level.getGameTime()<<32 ^ Float.floatToIntBits(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))&0xffffffffL;
        if(frame!=visibleFrame || !camera.equals(visibleFrom)) { VISIBLE.clear(); visibleFrame=frame; visibleFrom=camera; }
        int id=entity.getId();
        if(VISIBLE.containsKey(id)) return VISIBLE.get(id);
        AABB box=entity.getBoundingBox();
        Vec3 centre=box.getCenter();
        boolean seen=sees(mc,camera,centre) || sees(mc,camera,new Vec3(centre.x,Math.min(box.maxY-.05,entity.getEyeY()),centre.z))
                || sees(mc,camera,new Vec3(centre.x,box.minY+.1,centre.z));
        VISIBLE.put(id,seen);
        return seen;
    }
    private static boolean sees(Minecraft mc,Vec3 from,Vec3 to) {
        var hit=mc.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        return hit.getType()==HitResult.Type.MISS || hit.getLocation().distanceToSqr(from)+1e-4>=to.distanceToSqr(from);
    }

    /** Outline colour (rgb) of a glowing entity. Pure black would read as empty, so it is lifted a little. */
    public static int color(Entity entity) {
        int argb=switch (COLOR_MODE.get()) {
            case "Свой" -> COLOR.get();
            case "Здоровье" -> health(entity);
            case "Радуга" -> Color.HSBtoRGB(((System.currentTimeMillis()/20+(entity==null ? 0 : entity.getId()*150L))%360)/360f,.7f,1);
            default -> Theme.accent();
        };
        int r=argb>>16&255,g=argb>>8&255,b=argb&255;
        return Math.max(r,Math.max(g,b))>=8 ? argb&0xffffff : 0x080808;
    }
    private static int health(Entity entity) {
        if(!(entity instanceof LivingEntity living)) return COLOR.get();
        float t=Math.clamp(living.getHealth()/Math.max(living.getMaxHealth(),1),0,1);
        return Theme.mix(LOW_HEALTH,FULL_HEALTH,t);
    }
    private static float pulse() {
        if(!PULSE.get()) return 1;
        return .86f+.14f*(float)Math.sin(System.currentTimeMillis()*.0013%(Math.PI*2));
    }

    /** Uploads this frame's settings; the chain's EssEsp block is bound to them instead of its json defaults. */
    public static void upload() {
        float radius=Math.max(GLOW_RADIUS.get(),OUTLINE_WIDTH.get()+1);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            uniforms=GpuUniformBufferHelper.getGpuBufferSlice2(Std140Builder.onStack(stack,32)
                    .putVec4(OUTLINE_WIDTH.get(),GLOW_STRENGTH.get(),FILL_OPACITY.get(),pulse())
                    .putVec4(radius,0,0,0).get());
        }
    }
    public static GpuBufferSlice uniforms() { return uniforms; }
}
