package ess.ll.client.module.render;

import ess.ll.client.mixin.MultiPlayerGameModeMixin_1262;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BlockOverlay: the outline of the block under the crosshair, in place of vanilla's. Lines run along the edges
 * of the block's own shape (stairs, slabs and fences keep their form), with an optional glow; moving to another
 * block the outline glides there, stretching between the two shapes, and it fades out when nothing is aimed at.
 * It can fill the block lightly and show the breaking progress as a fill rising from the bottom.
 */
public final class BlockOverlay {
    public static final ModeSettings MODE=new ModeSettings("Режим","Делофт","Shader","Глоу","Уголки");
    public static final ModeSettings SHADER_STYLE=new ModeSettings("Шейдер","Solar","Плазма","Рельеф","Соты").visibleWhen(()->MODE.is("Shader"));
    public static final ModeSettings COVERAGE=new ModeSettings("Область","Грань","Весь блок").visibleWhen(()->!MODE.is("Делофт"));
    public static final SliderSetting CORNER_LENGTH=new SliderSetting("Длина уголков",.1f,.45f,.01f,.25f).visibleWhen(()->MODE.is("Уголки"));
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Тема","Свой");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xff8f7cff).visibleWhen(()->COLOR.is("Свой"));
    public static final SliderSetting WIDTH=new SliderSetting("Толщина",.002f,.025f,.001f,.008f).visibleWhen(()->MODE.is("Глоу")||MODE.is("Уголки"));
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true);
    public static final SliderSetting GLOW_POWER=new SliderSetting("Сила свечения",0,2,.05f,.8f).visibleWhen(()->GLOW.get()&&(MODE.is("Глоу")||MODE.is("Уголки")));
    public static final SliderSetting SMOOTH=new SliderSetting("Плавность",0,1,.05f,.6f);
    public static final BooleanSetting FILL=new BooleanSetting("Заливка",false);
    public static final SliderSetting FILL_ALPHA=new SliderSetting("Прозрачность заливки",0,.6f,.02f,.22f)
            .visibleWhen(()->FILL.get() || MODE.is("Глоу") || MODE.is("Уголки"));
    public static final BooleanSetting BREAK=new BooleanSetting("Прогресс ломания",true);
    public static final SliderSetting SPEED=new SliderSetting("Скорость",.1f,3,.1f,1).visibleWhen(()->MODE.is("Shader"));
    public static final SliderSetting PATTERN_SIZE=new SliderSetting("Размер узора",.5f,4,.1f,1.5f).visibleWhen(()->MODE.is("Shader"));
    public static final SliderSetting SHADER_OPACITY=new SliderSetting("Непрозрачность шейдера",.05f,1,.05f,.4f).visibleWhen(()->MODE.is("Shader"));
    public static final Setting[] SETTINGS={MODE,SHADER_STYLE,COVERAGE,CORNER_LENGTH,COLOR,OWN,WIDTH,GLOW,GLOW_POWER,SMOOTH,FILL,FILL_ALPHA,BREAK,SPEED,PATTERN_SIZE,SHADER_OPACITY};
    static { GlowFx.add(BlockOverlay::paint); }

    /** Edges of the shape shown, in its own block's coordinates, and that shape's bounds. */
    private static final List<double[]> EDGES=new ArrayList<>();
    private static AABB shapeBounds;
    private static BlockPos shown;
    private static VoxelShape shownShape;
    /** The box drawn now (gliding to the aimed one), and how visible the outline is. */
    private static double[] box;
    private static float visible;
    private static long last=System.nanoTime();
    private static final float[] faceWeights=new float[6];
    private static Direction lastFace=Direction.UP;
    private static double animationTime;
    private static BlockPos externalTarget;
    private static Direction externalFace=Direction.UP;

    private BlockOverlay() {}

    public static boolean enabled() { return ClientModules.BLOCK_OVERLAY!=null && ClientModules.BLOCK_OVERLAY.enabled(); }
    /** Dev check: how visible the outline is now. */
    public static float visibility() { return visible; }
    public static void highlight(BlockPos pos,Direction face) { externalTarget=pos; externalFace=face==null?Direction.UP:face; }

    private static void paint(GlowFx fx) {
        long now=System.nanoTime();
        float dt=Math.min(.1f,(now-last)/1e9f);
        last=now;
        if(!enabled()) { box=null; visible=0; shown=null; shownShape=null; EDGES.clear(); Arrays.fill(faceWeights,0); animationTime=0; return; }
        var mc=Minecraft.getInstance();
        BlockPos aimed=null;
        VoxelShape shape=null;
        Direction face=lastFace;
        BlockPos requested=externalTarget;
        externalTarget=null;
        if(requested==null && mc.hitResult instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK) {
            requested=hit.getBlockPos(); face=hit.getDirection();
        } else if(requested!=null) face=externalFace;
        if(requested!=null) {
            var state=mc.level.getBlockState(requested);
            if(!state.isAir() && mc.level.getWorldBorder().isWithinBounds(requested)) {
                VoxelShape found=state.getShape(mc.level,requested,CollisionContext.of(mc.player));
                if(!found.isEmpty()) { aimed=requested; shape=found; lastFace=face; }
            }
        }
        if(aimed!=null && (!aimed.equals(shown) || shape!=shownShape)) {
            shown=aimed; shownShape=shape;
            shapeBounds=shape.bounds();
            EDGES.clear();
            shape.forAllEdges((x1,y1,z1,x2,y2,z2)->EDGES.add(new double[]{x1,y1,z1,x2,y2,z2}));
        }
        visible=approach(visible,aimed!=null ? 1 : 0,dt,aimed!=null ? 22 : 12);
        if(shown==null || visible<.004f) { if(aimed==null) { shown=null; box=null; } return; }
        double[] target={shown.getX()+shapeBounds.minX,shown.getY()+shapeBounds.minY,shown.getZ()+shapeBounds.minZ,
                shown.getX()+shapeBounds.maxX,shown.getY()+shapeBounds.maxY,shown.getZ()+shapeBounds.maxZ};
        float follow=SMOOTH.get()<.01f ? 1 : 1-(float)Math.exp(-dt*(4+(1-SMOOTH.get())*40));
        if(box==null) box=target.clone();
        else for(int i=0;i<6;i++) box[i]+=(target[i]-box[i])*follow;

        int color=(COLOR.is("Свой") ? OWN.get() : Theme.accent())&0xffffff;
        float glow=GLOW.get() ? GLOW_POWER.get() : 0;
        animationTime+=dt*SPEED.get();
        if(!MODE.is("Делофт")) {
            float blend=1-(float)Math.exp(-24*dt);
            Vec3 eye=fx.eye;
            for(Direction d:Direction.values()) {
                boolean wanted=COVERAGE.is("Весь блок") ? visibleFace(d,eye) : d==face;
                int i=d.ordinal();
                faceWeights[i]+=((wanted?1:0)-faceWeights[i])*blend;
            }
            int secondary=COLOR.is("Свой")?lift(color,.45f):Theme.mix(color,Theme.background(),.4f)&0xffffff;
            int style=MODE.is("Глоу")?5:MODE.is("Уголки")?6:
                    SHADER_STYLE.is("Solar")?-1:SHADER_STYLE.is("Рельеф")?1:SHADER_STYLE.is("Соты")?3:0;
            BlockOverlayGpu.draw(box,eye,faceWeights,style,(float)animationTime,PATTERN_SIZE.get(),
                    color,secondary,MODE.is("Shader")?SHADER_OPACITY.get()*visible:FILL_ALPHA.get()*visible,
                    WIDTH.get(),glow,CORNER_LENGTH.get());
            return;
        }
        Vec3 centre=new Vec3((box[0]+box[3])/2,(box[1]+box[4])/2,(box[2]+box[5])/2);
        float width=(float)(WIDTH.get()*Math.max(1.5,centre.distanceTo(fx.eye)));
        for(double[] e:EDGES) {
            Vec3 a=map(e[0],e[1],e[2]),b=map(e[3],e[4],e[5]);
            // Nudged towards the camera so the faces next to the edge do not cut into it.
            Vec3 toEye=fx.eye.subtract(a.add(b).scale(.5)).normalize().scale(.004);
            a=a.add(toEye);
            b=b.add(toEye);
            fx.line(a,b,width,color,visible,visible,glow);
        }
        float progress=breaking(mc);
        fill(fx,box[1],box[4],color,.26f*visible);
        if(progress>0) fill(fx,box[1],box[1]+(box[4]-box[1])*progress,color,(.25f+.2f*glow)*visible);
    }

    /** How far the aimed block is broken, 0 when it is not being broken. */
    private static float breaking(Minecraft mc) {
        if(!BREAK.get() || mc.gameMode==null || !mc.gameMode.isDestroying()) return 0;
        var mode=(MultiPlayerGameModeMixin_1262)mc.gameMode;
        return shown.equals(mode.getDestroyBlockPos()) ? Math.clamp(mode.getDestroyProgress(),0,1) : 0;
    }

    /** A point of the shown shape placed in the gliding box. */
    private static Vec3 map(double x,double y,double z) {
        return new Vec3(axis(x,shapeBounds.minX,shapeBounds.maxX,box[0],box[3]),axis(y,shapeBounds.minY,shapeBounds.maxY,box[1],box[4]),
                axis(z,shapeBounds.minZ,shapeBounds.maxZ,box[2],box[5]));
    }
    private static double axis(double v,double from,double to,double min,double max) {
        return to-from<1e-5 ? (min+max)/2 : min+(v-from)/(to-from)*(max-min);
    }

    /** The gliding box filled between two heights, a little larger so its faces do not flicker with the block's. */
    private static void fill(GlowFx fx,double y0,double y1,int color,float strength) {
        double g=.003,x0=box[0]-g,z0=box[2]-g,x1=box[3]+g,z1=box[5]+g;
        y0-=g; y1+=g;
        double[][] faces={
            {x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1},{x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0},
            {x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0},{x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1},
            {x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0},{x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1}};
        for(double[] f:faces) {
            for(int i=0;i<4;i++) fx.vertex(f[i*3],f[i*3+1],f[i*3+2],color,strength,0,0,GlowFx.FILL,0,1);
            fx.quadDone();
        }
    }

    private static float approach(float value,float target,float dt,float speed) {
        return value+(target-value)*(1-(float)Math.exp(-dt*speed));
    }
    private static boolean visibleFace(Direction d,Vec3 camera) {
        return switch (d) {
            case DOWN -> camera.y<=box[1]; case UP -> camera.y>=box[4];
            case NORTH -> camera.z<=box[2]; case SOUTH -> camera.z>=box[5];
            case WEST -> camera.x<=box[0]; case EAST -> camera.x>=box[3];
        };
    }
    private static int lift(int c,float amount) {
        int r=(int)((c>>16&255)+(255-(c>>16&255))*amount);
        int g=(int)((c>>8&255)+(255-(c>>8&255))*amount);
        int b=(int)((c&255)+(255-(c&255))*amount);
        return r<<16|g<<8|b;
    }
}
