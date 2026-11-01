package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.ClientParticleManager;
import ess.ll.client.render.HudNotifications;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Optimization: removes every particle (vanilla and the client's own), skips drawing far entities and block
 * entities, caps the frame rate while the window is in the background. On enable the island lists the modules
 * and video settings that still cost frames, so the player knows what else to turn off.
 */
public final class Optimization {
    public static final MultiBooleanSettings PARTS=new MultiBooleanSettings("Оптимизировать",
            "Все частицы","Дальние сущности","Дальние блок-сущности","FPS в фоне");
    public static final SliderSetting ENTITY_RANGE=new SliderSetting("Дальность сущностей",16,128,4,64)
            .visibleWhen(()->PARTS.selected("Дальние сущности"));
    public static final SliderSetting BLOCK_RANGE=new SliderSetting("Дальность блок-сущностей",16,128,4,48)
            .visibleWhen(()->PARTS.selected("Дальние блок-сущности"));
    public static final SliderSetting BACKGROUND_FPS=new SliderSetting("FPS в фоне",5,60,5,15)
            .visibleWhen(()->PARTS.selected("FPS в фоне"));
    public static final BooleanSetting TIPS=new BooleanSetting("Подсказки в островке",true);
    public static final Setting[] SETTINGS={PARTS,ENTITY_RANGE,BLOCK_RANGE,BACKGROUND_FPS,TIPS};
    /** Client modules whose effects cost the most frames. */
    private static final List<String> HEAVY=List.of("FogAtmosphere","DeathEffect",
            "Particles","BlockESP","RenderTest");
    private static boolean wasOn;

    private Optimization() {}

    public static boolean enabled() { return ClientModules.OPTIMIZATION!=null && ClientModules.OPTIMIZATION.enabled(); }
    public static boolean noParticles() { return enabled() && PARTS.selected("Все частицы"); }

    /** Runs every client tick: clears the client particles while removal is on and shows the tips on enable. */
    public static void tick() {
        boolean on=enabled();
        if(on && !wasOn && TIPS.get()) tips(Minecraft.getInstance());
        wasOn=on;
        if(noParticles()) ClientParticleManager.INSTANCE.clear();
    }

    /** Entities other than players further than the set range are not drawn. */
    public static boolean cullEntity(Entity entity,double camX,double camY,double camZ) {
        if(!enabled() || !PARTS.selected("Дальние сущности") || entity instanceof Player) return false;
        var mc=Minecraft.getInstance();
        if(entity==mc.getCameraEntity() || mc.player!=null && entity.hasPassenger(mc.player)) return false;
        double range=ENTITY_RANGE.get();
        return entity.distanceToSqr(camX,camY,camZ)>range*range;
    }

    public static boolean cullBlockEntity(double distanceSqr) {
        if(!enabled() || !PARTS.selected("Дальние блок-сущности")) return false;
        double range=BLOCK_RANGE.get();
        return distanceSqr>range*range;
    }

    /** Frame limit while the window is not focused, or the given limit when that part is off. */
    public static int framerate(int limit) {
        if(!enabled() || !PARTS.selected("FPS в фоне") || Minecraft.getInstance().isWindowActive()) return limit;
        return Math.min(limit,Math.round(BACKGROUND_FPS.get()));
    }

    private static void tips(Minecraft mc) {
        List<String> modules=new ArrayList<>();
        for(String name:HEAVY)
            for(var m:ClientModules.all()) if(m.name.equalsIgnoreCase(name) && m.enabled()) modules.add(m.name);
        List<String> video=new ArrayList<>();
        var o=mc.options;
        if(o.renderDistance().get()>12) video.add("прорисовка до 12");
        if(o.simulationDistance().get()>8) video.add("симуляция до 8");
        if(o.graphicsPreset().get()==GraphicsPreset.FABULOUS || o.improvedTransparency().get()) video.add("без улучшенной прозрачности");
        if(o.getCloudStatus()!=CloudStatus.OFF) video.add("без облаков");
        if(o.entityShadows().get()) video.add("без теней сущностей");
        if(o.biomeBlendRadius().get()>2) video.add("смешивание биомов до 2");
        if(o.enableVsync().get()) video.add("без V-Sync");
        if(modules.isEmpty() && video.isEmpty()) {
            HudNotifications.queue(Icon.BOLT,Theme.accent(),"Optimization","Всё уже настроено на FPS");
            return;
        }
        for(int i=0;i<modules.size();i+=3)
            HudNotifications.queue(Icon.BOLT,0xffe49a3a,"Выключи модули",String.join(", ",modules.subList(i,Math.min(modules.size(),i+3))));
        for(int i=0;i<video.size();i+=3)
            HudNotifications.queue(Icon.MONITOR,0xffe49a3a,"Поменяй в настройках",String.join(", ",video.subList(i,Math.min(video.size(),i+3))));
    }
}
