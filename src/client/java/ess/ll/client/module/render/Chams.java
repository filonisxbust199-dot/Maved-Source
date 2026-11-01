package ess.ll.client.module.render;

import ess.ll.client.command.ClientData;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GhostModelBatch;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Theme;
import ess.ll.client.render.WorldVertexCollector;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** ESS Chams target selection, flat fill, luminous model and blur silhouette. */
public final class Chams {
    public static final ModeSettings MODE=new ModeSettings("Режим","Свечение","Flat","Блюр");
    public static final MultiBooleanSettings TARGETS=new MultiBooleanSettings("Цели","Игроки","Голые","Невидимые","Друзья","Себя");
    public static final BooleanSetting THEME_COLOR=new BooleanSetting("Цвет из темы",false).visibleWhen(()->!MODE.is("Блюр"));
    public static final ColorSetting COLOR=new ColorSetting("Цвет",0xff78aaff,true).visibleWhen(()->!MODE.is("Блюр") && !THEME_COLOR.get());
    public static final BooleanSetting DAMAGE=new BooleanSetting("Индикация урона",true);
    public static final ColorSetting DAMAGE_COLOR=new ColorSetting("Цвет урона",0x99ff4b55,true).visibleWhen(DAMAGE::get);
    public static final BooleanSetting THROUGH_WALLS=new BooleanSetting("Сквозь стены",false);
    public static final BooleanSetting THROUGH_ARMOR=new BooleanSetting("Сквозь броню",false).visibleWhen(()->MODE.is("Свечение"));
    public static final SliderSetting FILL_OPACITY=new SliderSetting("Плотность заливки",0,100,1,100).visibleWhen(()->MODE.is("Свечение"));
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true).visibleWhen(()->MODE.is("Свечение"));
    public static final SliderSetting GLOW_RADIUS=new SliderSetting("Радиус свечения",2,64,1,24).visibleWhen(()->MODE.is("Свечение") && GLOW.get());
    public static final SliderSetting GLOW_STRENGTH=new SliderSetting("Сила свечения",.1f,3,.1f,1.6f).visibleWhen(()->MODE.is("Свечение") && GLOW.get());
    public static final BooleanSetting ADDITIVE=new BooleanSetting("Аддитивный блендинг",true).visibleWhen(()->MODE.is("Flat"));
    public static final ColorSetting BLUR_COLOR=new ColorSetting("Цвет блюра",0xff78aaff,true).visibleWhen(()->MODE.is("Блюр"));
    public static final SliderSetting BLUR_RADIUS=new SliderSetting("Сила блюра",1,12,1,5).visibleWhen(()->MODE.is("Блюр"));
    public static final SliderSetting BLUR_ALPHA=new SliderSetting("Прозрачность цвета",0,100,1,28).visibleWhen(()->MODE.is("Блюр"));
    public static final BooleanSetting BLUR_MIRROR=new BooleanSetting("Отзеркаливание",false).visibleWhen(()->MODE.is("Блюр"));
    public static final Setting[] SETTINGS={MODE,TARGETS,THEME_COLOR,COLOR,DAMAGE,DAMAGE_COLOR,THROUGH_WALLS,
            THROUGH_ARMOR,FILL_OPACITY,GLOW,GLOW_RADIUS,GLOW_STRENGTH,ADDITIVE,BLUR_COLOR,BLUR_RADIUS,BLUR_ALPHA,BLUR_MIRROR};

    private static final Minecraft MC=Minecraft.getInstance();
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static final List<AbstractClientPlayer> VISIBLE=new ArrayList<>();
    /** TargetESP's geometry for this frame, held back until the chams pass is done. */
    private static final WorldVertexCollector LATE=WorldVertexCollector.late();
    private static boolean initialised;
    private static boolean worldSubmission;
    static {
        TARGETS.toggle(TARGETS.options.indexOf("Невидимые"));
        TARGETS.toggle(TARGETS.options.indexOf("Друзья"));
        TARGETS.toggle(TARGETS.options.indexOf("Себя"));
    }
    private Chams() {}
    public static void initialize() {
        if(initialised) return;
        initialised=true;
        ChamsBlur.initialize();
        ChamsGlow.initialize();
        // Chams is rendered after the vanilla entity pass, when the scene depth is final; TargetESP follows it.
        GlowFx.add(fx->{ safePaint(fx); drawLate(); });
    }
    public static boolean enabled() { return ClientModules.CHAMS!=null && ClientModules.CHAMS.enabled(); }
    /** Where TargetESP draws while Chams is on, so it lands on top of the chams; null when it can draw as usual. */
    public static WorldVertexCollector lateCollector() { return enabled()?LATE:null; }
    /** TargetESP's world effects, held back while Chams is on, drawn over the chams. */
    private static void drawLate() {
        LATE.drawNow();
        if(enabled()) ess.ll.client.module.combat.CombatRuntime.renderTargetEffects();
    }
    public static void beginWorldSubmission() { worldSubmission=true; }
    public static void endWorldSubmission() { worldSubmission=false; }
    /** The vanilla body is replaced by the Flat pass or the blur silhouette in the world only. */
    public static boolean replacesBody(AvatarRenderState state) {
        if(!worldSubmission || !enabled() || MODE.is("Свечение") || MC.level==null) return false;
        Entity entity=MC.level.getEntity(state.id);
        return entity instanceof AbstractClientPlayer player && targets(player);
    }
    public static void tick() {
        if(!enabled() || !MODE.is("Блюр")) ChamsBlur.release();
        if(!enabled() || !MODE.is("Свечение")) ChamsGlow.release();
    }
    private static void safePaint(GlowFx fx) {
        try { paint(fx); }
        catch(RuntimeException e) {
            ClientModules.CHAMS.setEnabled(false);
            System.err.println("[ESS] Chams disabled after render failure: "+e);
            ChamsBlur.release(); ChamsGlow.release();
        }
    }
    private static boolean targets(AbstractClientPlayer player) {
        if(player.isSpectator()) return false;
        if(player==MC.player) return TARGETS.selected("Себя") && !MC.options.getCameraType().isFirstPerson();
        if(ClientData.isFriend(player.getGameProfile().name())) return TARGETS.selected("Друзья");
        if(player.isInvisible()) return TARGETS.selected("Невидимые");
        boolean naked=player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        return TARGETS.selected(naked?"Голые":"Игроки");
    }
    private static void paint(GlowFx fx) {
        if(!enabled() || MC.level==null || MC.player==null) return;
        boolean blur=MODE.is("Блюр");
        if(blur) ChamsBlur.begin();
        boolean glow=MODE.is("Свечение");
        if(glow) ChamsGlow.begin();
        VISIBLE.clear();
        for(Entity entity:MC.level.entitiesForRendering()) {
            if(entity instanceof AbstractClientPlayer player && targets(player) && player.distanceToSqr(MC.player)<4096) {
                if(glow && THROUGH_ARMOR.get() && !THROUGH_WALLS.get() && !terrainVisible(player,fx.eye)) continue;
                VISIBLE.add(player);
                if(VISIBLE.size()>=48) break;
            }
        }
        long stamp=System.currentTimeMillis();
        for(AbstractClientPlayer player:VISIBLE) {
            ChronoFrame frame=ChronoFrame.capture(player,fx.partial,stamp,glow && !THROUGH_ARMOR.get());
            if(frame==null) continue;
            int base=DAMAGE.get() && player.hurtTime>0?DAMAGE_COLOR.get():MODE.is("Блюр")?BLUR_COLOR.get():THEME_COLOR.get()?Theme.accent():COLOR.get();
            int rgb=base&0xffffff;
            boolean through=THROUGH_WALLS.get();
            if(MODE.is("Flat")) {
                BATCH.begin(); frame.emit(fx.eye,base|0xff000000,false,BATCH);
                BATCH.flushFlat(frame.skin,(base>>>24)/255f,through,ADDITIVE.get());
            } else if(MODE.is("Свечение")) {
                ChamsGlow.render(frame,fx.eye,rgb);
            } else {
                // The glass post effect uses the scene colour under the silhouette.
                ChamsBlur.render(frame,fx.eye,base,through);
            }
        }
        if(blur) ChamsBlur.finish();
        if(glow) ChamsGlow.finish();
        VISIBLE.clear();
    }
    /** Keep the through-armor option from revealing a player behind solid terrain. */
    private static boolean terrainVisible(AbstractClientPlayer player,Vec3 eye) {
        var box=player.getBoundingBox();
        double x=(box.minX+box.maxX)*.5,z=(box.minZ+box.maxZ)*.5;
        return clearRay(eye,new Vec3(x,box.minY+.15,z)) || clearRay(eye,new Vec3(x,(box.minY+box.maxY)*.5,z))
                || clearRay(eye,new Vec3(x,box.maxY-.1,z));
    }
    private static boolean clearRay(Vec3 eye,Vec3 target) {
        var hit=MC.level.clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MC.player));
        return hit.getType()==HitResult.Type.MISS || hit.getLocation().distanceToSqr(eye)+1e-4>=target.distanceToSqr(eye);
    }
}
