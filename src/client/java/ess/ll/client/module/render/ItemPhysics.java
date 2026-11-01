package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * ItemPhysics: dropped items fall tumbling and come to rest lying on the ground instead of hovering and spinning:
 * flat items lie flat, block items sit on a face. ItemPhysicsMixin hands over vanilla's lift and spin in
 * ItemEntityRenderer.submit; here they are replaced by a pose that turns around the item's centre, with each
 * item's angles kept between frames so landing settles it smoothly onto the nearest resting side.
 */
public final class ItemPhysics {
    public static final BooleanSetting TUMBLE=new BooleanSetting("Кувырок в воздухе",true);
    public static final SliderSetting SPEED=new SliderSetting("Скорость кувырка",.2f,3,.1f,1).visibleWhen(TUMBLE::get);
    public static final BooleanSetting RANDOM=new BooleanSetting("Случайный поворот",true);
    public static final Setting[] SETTINGS={TUMBLE,SPEED,RANDOM};

    /** Extra render state: whether the item rests on the ground and which entity it is. */
    public interface State {
        boolean ess$ground();
        int ess$id();
        void ess$set(boolean ground,int id);
    }
    /** Angles of one item between frames. */
    private static final class Pose { float yaw,pitch,settle,age; long seen; }
    private static final Map<Integer,Pose> POSES=new HashMap<>();
    /** The item being submitted now and its pose; null when vanilla draws it. */
    private static ItemEntityRenderState current;
    private static Pose pose;
    private static long lastSweep;

    private ItemPhysics() {}

    public static boolean enabled() { return ClientModules.ITEM_PHYSICS!=null && ClientModules.ITEM_PHYSICS.enabled(); }

    private static boolean flat(AABB box) { return box.maxZ-box.minZ<=.0625; }

    /** Start of ItemEntityRenderer.submit: steps this item's angles. */
    public static void begin(ItemEntityRenderState state) {
        current=null;
        if(!enabled() || !(state instanceof State extra)) return;
        long now=System.currentTimeMillis();
        if(now-lastSweep>2000) { lastSweep=now; POSES.values().removeIf(p->now-p.seen>2000); }
        Pose p=POSES.computeIfAbsent(extra.ess$id(),id->{
            Pose fresh=new Pose();
            fresh.yaw=RANDOM.get() ? (id*137.508f)%360 : 0;
            fresh.age=state.ageInTicks;
            return fresh;
        });
        float dt=Math.clamp(state.ageInTicks-p.age,0,4);
        p.age=state.ageInTicks; p.seen=now;
        boolean flat=flat(state.item.getModelBoundingBox());
        if(extra.ess$ground()) {
            // Settles onto the nearest resting side: lying (90 + k*180) for flat items, any face (k*90) for blocks.
            float step=flat ? 180 : 90,offset=flat ? 90 : 0;
            float rest=Math.round((p.pitch-offset)/step)*step+offset;
            p.pitch+=(rest-p.pitch)*(1-(float)Math.exp(-dt*.6f));
            p.settle=Math.min(1,p.settle+dt*.25f);
        } else {
            if(TUMBLE.get()) p.pitch=(p.pitch+dt*14*SPEED.get())%360;
            p.yaw=(p.yaw+dt*(TUMBLE.get() ? 4*SPEED.get() : 4))%360;
            p.settle=Math.max(0,p.settle-dt*.25f);
        }
        current=state; pose=p;
    }

    /** Vanilla's lift (with the bob); replaced by the item's centre height, down on the ground once it rests. */
    public static void translate(PoseStack stack,float x,float y,float z) {
        if(current==null) { stack.translate(x,y,z); return; }
        AABB box=current.item.getModelBoundingBox();
        float centre=(float)(box.minY+box.maxY)/2;
        float air=(float)-box.minY+.0625f+centre;
        float ground=flat(box) ? (float)box.maxZ+.006f : (float)(box.maxY-box.minY)/2+.002f;
        float s=pose.settle*pose.settle*(3-2*pose.settle);
        stack.translate(0,air+(ground-air)*s,0);
    }

    /** Vanilla's spin; replaced by the item's own yaw and pitch, turning around its centre. */
    public static void rotate(PoseStack stack,Axis axis,float angle) {
        if(current==null) { stack.rotate(axis,angle); return; }
        AABB box=current.item.getModelBoundingBox();
        stack.rotate(Axis.YP,(float)Math.toRadians(pose.yaw));
        stack.rotate(Axis.XP,(float)Math.toRadians(pose.pitch));
        stack.translate(0,-(float)(box.minY+box.maxY)/2,0);
        current=null;
    }
}
