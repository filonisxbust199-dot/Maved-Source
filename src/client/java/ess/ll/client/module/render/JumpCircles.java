package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * JumpCircles: a glowing figure spreads out under a player who jumps or lands. It is a grid of quads laid over
 * the blocks under the player (each point rests on the top of the block below it, so the figure drapes over
 * steps and edges), and the jump_circle shader draws the figure on it: its shape, a bright line with a glow
 * around it, a wobble that bends the line, and the fade as it grows.
 */
public final class JumpCircles {
    public static final MultiBooleanSettings WHEN=new MultiBooleanSettings("Когда","Прыжок","Приземление");
    public static final ModeSettings SHAPE=new ModeSettings("Форма","Кольцо","Волны","Звезда","Шестигранник","Цветок","Сердце");
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Тема","Радуга","Свой");
    public static final ColorSetting OWN_COLOR=new ColorSetting("Свой цвет",0xffff5fb4).visibleWhen(()->COLOR.is("Свой"));
    public static final SliderSetting SIZE=new SliderSetting("Размер",.5f,3,.1f,1.3f);
    public static final SliderSetting LIFE=new SliderSetting("Время",.3f,2.5f,.1f,1);
    public static final SliderSetting GLOW=new SliderSetting("Свечение",0,1,.05f,.7f);
    public static final SliderSetting DISTORT=new SliderSetting("Искажение",0,1,.05f,.35f);
    public static final BooleanSetting DRAPE=new BooleanSetting("Облегать блоки",true);
    public static final MultiBooleanSettings WHO=new MultiBooleanSettings("Для","Себя","Игроков").none();
    public static final Setting[] SETTINGS={WHEN,SHAPE,COLOR,OWN_COLOR,SIZE,LIFE,GLOW,DISTORT,DRAPE,WHO};
    static { WHO.toggle(0); }

    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ess-best","pipeline/jump_circle"))
            .withVertexShader(Identifier.fromNamespaceAndPath("ess-best","core/jump_circle"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ess-best","core/jump_circle"))
            .withVertexBinding(0,DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
            .withCull(false).build());
    private static final RenderType TYPE=RenderType.create("ESS jump circle",RenderSetup.builder(PIPELINE).createRenderSetup());
    /** Grid cells along a side; enough for the figure to follow single blocks. */
    private static final int GRID=24;
    private static final JumpCircles INSTANCE=new JumpCircles();
    private static boolean registered;

    /** One figure: where it lies, its grid of heights, colour and age. */
    private record Circle(double x,double y,double z,float radius,float[] heights,int color,boolean landing,int seed,long born,float life) {}
    private final List<Circle> circles=new ArrayList<>();
    /** Per player: on the ground last tick, and ticks in the air. */
    private final Map<Integer,int[]> air=new HashMap<>();
    private final Vector3f point=new Vector3f();

    private JumpCircles() {}

    /** Dev check: figures alive now. */
    public static int count() { return INSTANCE.circles.size(); }
    public static boolean enabled() { return ClientModules.JUMP_CIRCLES!=null && ClientModules.JUMP_CIRCLES.enabled(); }

    /** Client tick: listens while on, and watches players for take-offs and landings. */
    public static void tick(Minecraft mc) {
        boolean on=enabled();
        if(on!=registered) {
            registered=on;
            if(on) EventBus.register(INSTANCE); else { EventBus.unregister(INSTANCE); INSTANCE.circles.clear(); INSTANCE.air.clear(); }
        }
        if(on && mc.level!=null && mc.player!=null && !mc.isPaused()) INSTANCE.watch(mc);
    }

    private void watch(Minecraft mc) {
        Map<Integer,int[]> seen=new HashMap<>();
        for(Player player:mc.level.players()) {
            boolean self=player==mc.player;
            if(self ? !WHO.selected("Себя") : !WHO.selected("Игроков")) continue;
            if(player.isSpectator() || player.isPassenger() || player.getAbilities().flying) continue;
            int[] state=air.getOrDefault(player.getId(),new int[]{player.onGround() ? 1 : 0,0});
            boolean ground=player.onGround();
            if(state[0]==1 && !ground && player.getY()-player.yo>.05 && WHEN.selected("Прыжок")) spawn(mc.level,player.getX(),player.yo,player.getZ(),false);
            if(state[0]==0 && ground && state[1]>=4 && WHEN.selected("Приземление")) spawn(mc.level,player.getX(),player.getY(),player.getZ(),true);
            state[1]=ground ? 0 : state[1]+1;
            state[0]=ground ? 1 : 0;
            seen.put(player.getId(),state);
        }
        air.clear(); air.putAll(seen);
        long now=System.currentTimeMillis();
        circles.removeIf(c->now-c.born()>c.life()*1000);
    }

    /** Lays the grid over the blocks around the feet: every point takes the height of the surface under it. */
    private void spawn(Level level,double x,double y,double z,boolean landing) {
        float radius=SIZE.get()*(landing ? 1.25f : 1);
        float[] heights=new float[(GRID+1)*(GRID+1)];
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int j=0;j<=GRID;j++) for(int i=0;i<=GRID;i++) {
            double px=x+(i/(float)GRID*2-1)*radius,pz=z+(j/(float)GRID*2-1)*radius;
            heights[j*(GRID+1)+i]=DRAPE.get() ? surface(level,pos,px,y,pz) : 0;
        }
        int color=switch (COLOR.get()) {
            case "Радуга" -> java.awt.Color.HSBtoRGB((System.currentTimeMillis()%4000)/4000f,.75f,1);
            case "Свой" -> OWN_COLOR.get();
            default -> Theme.accent();
        };
        circles.add(new Circle(x,y,z,radius,heights,color&0xffffff,landing,ThreadLocalRandom.current().nextInt(1000),System.currentTimeMillis(),LIFE.get()*(landing ? 1.15f : 1)));
        if(circles.size()>48) circles.removeFirst();
    }
    /** Height of the top surface under (x, z) near the feet, relative to the feet; the feet height when there is none. */
    private static float surface(Level level,BlockPos.MutableBlockPos pos,double x,double feet,double z) {
        int top=(int)Math.floor(feet+.5);
        for(int by=top;by>=top-3;by--) {
            pos.set(Math.floor(x),by,Math.floor(z));
            var state=level.getBlockState(pos);
            var shape=state.getCollisionShape(level,pos);
            if(shape.isEmpty()) continue;
            double surface=by+shape.max(net.minecraft.core.Direction.Axis.Y);
            if(surface>feet+.6) return 0;
            return (float)(surface-feet);
        }
        return 0;
    }

    @Subscribe
    private void render(Render3DEvent event) {
        if(circles.isEmpty()) return;
        var eye=event.getCamera().position();
        Matrix4f pose=event.getPoseStack().last().pose();
        VertexConsumer out=event.getVertexCollector().getBuffer(TYPE);
        long now=System.currentTimeMillis();
        int clock=(int)(now%3000*1000/3000);
        int shape=SHAPE.options.indexOf(SHAPE.get());
        float glow=GLOW.get(),distort=DISTORT.get();
        for(Circle c:circles) {
            float t=Math.clamp((now-c.born())/(c.life()*1000f),0,1);
            int progress=Math.round(t*1000);
            float ox=(float)(c.x()-eye.x),oy=(float)(c.y()-eye.y)+.02f,oz=(float)(c.z()-eye.z);
            for(int j=0;j<GRID;j++) for(int i=0;i<GRID;i++) {
                quadCorner(out,pose,c,ox,oy,oz,i,j,progress,shape,clock,glow,distort);
                quadCorner(out,pose,c,ox,oy,oz,i,j+1,progress,shape,clock,glow,distort);
                quadCorner(out,pose,c,ox,oy,oz,i+1,j+1,progress,shape,clock,glow,distort);
                quadCorner(out,pose,c,ox,oy,oz,i+1,j,progress,shape,clock,glow,distort);
            }
        }
    }
    /**
     * One grid point. UV0 is the point in the figure's frame (-1..1); colour alpha is the glow; UV1 packs progress,
     * then wobble strength and seed; UV2 packs the clock, then the shape and the landing flag.
     */
    private void quadCorner(VertexConsumer out,Matrix4f pose,Circle c,float ox,float oy,float oz,int i,int j,int progress,int shape,int clock,float glow,float distort) {
        float u=i/(float)GRID*2-1,v=j/(float)GRID*2-1;
        float h=c.heights()[j*(GRID+1)+i];
        pose.transformPosition(ox+u*c.radius(),oy+h,oz+v*c.radius(),point);
        int alpha=Math.round(glow*255),wobble=Math.round(distort*100)*100+c.seed()%100;
        out.addVertex(point.x,point.y,point.z,alpha<<24|c.color(),u,v,progress|wobble<<16,clock|(shape|(c.landing() ? 16 : 0))<<16,0,1,0);
    }
}
