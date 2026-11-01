package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.command.Keys;
import ess.ll.client.integration.PlayerArmRenderAccess;
import ess.ll.client.wardrobe.Wardrobe.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * CS-style knives in first person: a sword with a worn "cs:" skin is held the way a knife is (lower and to the side,
 * the karambit in a reverse grip, the daggers one per hand), comes out with a flourish when you switch to it, slashes
 * (the daggers stab) and plays an inspect on the Осмотр ножа key. The animations are keyframed here, not in the model.
 */
public final class CsKnife {
    private CsKnife() {}

    /**
     * Channels of a keyframe row after its time: arm offset (x, y, z in blocks); the arm turned from the shoulder (pitch up,
     * yaw towards the middle of the screen) and the wrist rolled about the forearm (degrees); the knife turned about its spin
     * point (x, y, z degrees); then the butterfly's two handles (degrees, 0 = open).
     */
    private static final int CHANNELS=11;
    private enum Kind {
        // Karambit: the wrist turns the claw to show it, then flicks it twice round the finger ring.
        KARAMBIT(44,13,new float[]{0,-4.3f,0},new float[]{143.5f,-28.9f,-95.7f},new float[][]{
                {0,    0,0,0,          0,0,0,      0,0,0,     0,0},
                {.12f, -.02f,.04f,0,   12,14,-30,  0,0,0,     0,0},
                {.28f, -.02f,.05f,0,   14,16,-75,  0,0,0,     0,0},
                {.40f, -.02f,.05f,0,   12,12,-15,  0,0,0,     0,0},
                {.50f, -.02f,.07f,0,   16,10,15,   0,0,250,   0,0},
                {.58f, -.02f,.05f,0,   13,12,-12,  0,0,360,   0,0},
                {.66f, -.02f,.07f,0,   16,10,15,   0,0,610,   0,0},
                {.74f, -.02f,.05f,0,   13,12,-12,  0,0,720,   0,0},
                {.88f, 0,.02f,0,       5,5,-8,     0,0,720,   0,0},
                {1,    0,0,0,          0,0,0,      0,0,720,   0,0}},new float[][]{
                {0,    0,-.35f,.1f,    -35,-10,60, 0,0,-360,  0,0},
                {.6f,  0,.02f,0,       6,0,-10,    0,0,-30,   0,0},
                {1,    0,0,0,          0,0,0,      0,0,0,     0,0}}),
        // Butterfly: every flick of the wrist swings a handle over, the big one throws the whole knife round.
        BUTTERFLY(56,15,new float[]{0,-3.6f,0},new float[]{-53.9f,51.4f,9.9f},new float[][]{
                {0,    0,0,0,          0,0,0,      0,0,0,     0,0},
                {.10f, -.02f,.04f,0,   12,12,-15,  0,0,0,     0,0},
                {.22f, -.02f,.05f,0,   14,12,35,   0,0,-30,   0,-180},
                {.34f, -.02f,.05f,0,   14,12,-35,  0,0,20,    180,-180},
                {.50f, -.02f,.09f,0,   20,10,40,   0,0,360,   0,0},
                {.62f, -.02f,.05f,0,   14,12,-30,  0,0,330,   0,-180},
                {.74f, -.02f,.05f,0,   14,12,25,   0,0,360,   0,0},
                {.86f, -.02f,.04f,0,   12,12,-70,  0,0,360,   0,0},
                {1,    0,0,0,          0,0,0,      0,0,360,   0,0}},new float[][]{
                {0,    0,-.35f,.1f,    -35,-10,60, 0,0,-180,  180,-180},
                {.5f,  0,.02f,0,       6,0,-30,    0,0,-60,   0,-180},
                {.8f,  0,0,0,          2,0,15,     0,0,0,     0,0},
                {1,    0,0,0,          0,0,0,      0,0,0,     0,0}}),
        // M9: one flat, a toss from a lifted arm, the other flat.
        M9(48,12,new float[]{0,0,0},new float[]{-53.9f,51.4f,9.9f},new float[][]{
                {0,    0,0,0,          0,0,0,      0,0,0,     0,0},
                {.12f, -.02f,.04f,0,   12,14,-25,  0,0,0,     0,0},
                {.28f, -.02f,.05f,0,   14,16,-80,  0,0,0,     0,0},
                {.40f, -.02f,.05f,0,   14,16,-80,  0,0,0,     0,0},
                {.46f, -.02f,.02f,0,   8,14,-30,   0,0,0,     0,0},
                {.54f, -.02f,.12f,0,   26,10,-15,  180,0,0,   0,0},
                {.64f, -.02f,.04f,0,   12,14,-20,  360,0,0,   0,0},
                {.78f, -.02f,.05f,0,   14,16,70,   360,0,0,   0,0},
                {.90f, 0,.02f,0,       5,5,10,     360,0,0,   0,0},
                {1,    0,0,0,          0,0,0,      360,0,0,   0,0}},new float[][]{
                {0,    0,-.35f,.1f,    -35,-10,60, -360,0,0,  0,0},
                {.65f, 0,.02f,0,       6,0,-10,    -20,0,0,   0,0},
                {1,    0,0,0,          0,0,0,      0,0,0,     0,0}}),
        // Daggers: both wrists turn the blades out, twirl them round the bars, turn them in.
        DAGGERS(40,11,new float[]{0,0,0},new float[]{-144f,-60.7f,-114.3f},new float[][]{
                {0,    0,0,0,          0,0,0,      0,0,0,     0,0},
                {.15f, -.02f,.04f,0,   12,16,-50,  0,0,0,     0,0},
                {.40f, -.02f,.04f,0,   12,16,-50,  360,0,0,   0,0},
                {.58f, -.02f,.05f,0,   14,10,60,   360,0,0,   0,0},
                {.80f, -.02f,.05f,0,   14,10,60,   720,0,0,   0,0},
                {1,    0,0,0,          0,0,0,      720,0,0,   0,0}},new float[][]{
                {0,    0,-.35f,.1f,    -35,-10,40, 0,0,0,     0,0},
                {.7f,  0,.02f,0,       6,0,-8,     0,0,0,     0,0},
                {1,    0,0,0,          0,0,0,      0,0,0,     0,0}});

        final int inspectTicks,drawTicks;
        final float[] spin,grip;
        final float[][] inspect,draw;
        Kind(int inspectTicks,int drawTicks,float[] spin,float[] grip,float[][] inspect,float[][] draw) {
            this.inspectTicks=inspectTicks; this.drawTicks=drawTicks; this.spin=spin; this.grip=grip; this.inspect=inspect; this.draw=draw;
        }
        static Kind of(Item item) {
            return switch (item.theme().substring(3)) {
                case "karambit" -> KARAMBIT;
                case "butterfly" -> BUTTERFLY;
                case "m9" -> M9;
                case "daggers" -> DAGGERS;
                default -> null;
            };
        }
    }

    // CS view model: where the knife hand sits compared with the vanilla empty hand; the shoulder the arm turns from
    // and the forearm the wrist rolls about, in view space for the right arm.
    private static final float REST_X=-.06f,REST_Y=.08f,REST_Z=-.3f,SCALE=1.15f;
    private static final float SHOULDER_X=.5f,SHOULDER_Y=-.82f,SHOULDER_Z=-.66f,PALM_X=.7f,PALM_Y=-.52f,PALM_Z=-1.06f;
    private static final org.joml.Vector3f FOREARM=new org.joml.Vector3f();
    private static String heldId;
    private static int heldSlot=-1,swings;
    private static float drawStart=-1000,inspectStart=-1;
    private static boolean keyDown,swinging;
    private static final float[] A=new float[CHANNELS];

    /** The worn CS knife skin for a stack in your hand, or null. */
    static Item knife(ItemStack stack) {
        Item item=WardrobeHeld.skin(stack);
        return item!=null && item.cs() ? item : null;
    }

    /** Client tick: the draw when the knife comes into your hand, the inspect key, and the swings that stop an inspect. */
    public static void tick(Minecraft mc) {
        var player=mc.player;
        if(player==null) { heldId=null; return; }
        float now=player.tickCount;
        Item item=knife(player.getMainHandItem());
        String id=item==null ? null : item.id();
        int slot=player.getInventory().getSelectedSlot();
        if(id!=null && (!id.equals(heldId) || slot!=heldSlot)) { drawStart=now; inspectStart=-1; }
        heldId=id; heldSlot=slot;
        boolean down=Wardrobe.INSPECT.get()>=0 && mc.gui.screen()==null && Keys.isDown(Wardrobe.INSPECT.get());
        if(down && !keyDown && item!=null && now-drawStart>Kind.of(item).drawTicks) inspectStart=now;
        keyDown=down;
        if(player.isSwinging() && !swinging) { swings++; inspectStart=-1; }
        swinging=player.isSwinging();
    }
    /** Starts the inspect now; used by the dev screenshot run. */
    public static void inspect() { var p=Minecraft.getInstance().player; if(p!=null) inspectStart=p.tickCount; }

    /** First person: draws the arm and the knife instead of the sword; false when the hand is not holding a CS knife. */
    public static boolean render(PlayerArmRenderAccess arms,InteractionHand hand,float swing,ItemStack stack,PoseStack pose,SubmitNodeCollector collector,int light) {
        var mc=Minecraft.getInstance();
        var player=mc.player;
        if(player==null || hand!=InteractionHand.MAIN_HAND) return false;
        Item item=knife(stack);
        Kind kind=item==null ? null : Kind.of(item);
        WardrobeModel model=item==null ? null : Wardrobe.model(item);
        if(kind==null || model==null) return false;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(true),now=player.tickCount+partial;
        // The hook's swing can be reshaped by the swing modules; the knife follows the player's own attack.
        swing=player.getSwingAnimation(partial);
        // The draw, else the inspect, else rest; a swing is added on top.
        float d=(now-drawStart)/kind.drawTicks,in=inspectStart<0 ? -1 : (now-inspectStart)/kind.inspectTicks;
        if(in>=1) { inspectStart=-1; in=-1; }
        if(d<1) sample(kind.draw,Math.max(0,d),A);
        else if(in>=0) sample(kind.inspect,in,A);
        else java.util.Arrays.fill(A,0);
        // Breathing: the arm never stands quite still.
        A[1]+=.006f*(float)Math.sin(now*.09f);
        A[3]+=1.2f*(float)Math.sin(now*.09f+.6f);
        A[5]+=2*(float)Math.sin(now*.055f);
        HumanoidArm main=player.getMainArm();
        boolean daggers=kind==Kind.DAGGERS;
        // Slashes alternate direction like the two CS attacks; the daggers stab with alternate hands.
        int dir=swings%2==0 ? 1 : -1;
        hand(arms,kind,model,item,main,daggers && dir<0 ? 0 : swing,dir,pose,collector,light);
        if(daggers && player.getOffhandItem().isEmpty()) hand(arms,kind,model,item,main.getOpposite(),dir<0 ? swing : 0,dir,pose,collector,light);
        return true;
    }

    private static void hand(PlayerArmRenderAccess arms,Kind kind,WardrobeModel model,Item item,HumanoidArm arm,float swing,int dir,
                             PoseStack pose,SubmitNodeCollector collector,int light) {
        float f=arm==HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        try {
            pose.translate(f*(REST_X+A[0]),REST_Y+A[1],REST_Z+A[2]);
            float sx0=f*SHOULDER_X;
            pose.translate(sx0,SHOULDER_Y,SHOULDER_Z);
            float pitch=A[3],yaw=A[4],roll=A[5],lift=0;
            // The attack moves the arm the same way: a sweep from the shoulder with the wrist turning into the cut,
            // or a straight punch for the daggers.
            if(swing>0) {
                float a=(float)Math.sin(swing*Math.PI),b=(float)Math.sin(Math.sqrt(swing)*Math.PI);
                if(kind==Kind.DAGGERS) {
                    pose.translate(f*.03f*a,.03f*a,-.32f*b);
                    pitch+=8*a; roll+=25*a;
                } else {
                    float side=dir;
                    pose.translate(0,0,-.1f*a);
                    yaw+=side*42*b-side*12*a; pitch+=10*a*(side>0 ? 1 : -.4f); roll+=side*40*b;
                    lift=.04f*a*side;
                }
            }
            pose.translate(0,lift,0);
            pose.rotate(Axis.YP.rotationDegrees(f*yaw));
            pose.rotate(Axis.XP.rotationDegrees(pitch));
            // The wrist roll turns the arm about its own length, from the shoulder to the palm.
            FOREARM.set(f*(PALM_X-SHOULDER_X),PALM_Y-SHOULDER_Y,PALM_Z-SHOULDER_Z).normalize();
            pose.rotate(new org.joml.Quaternionf().rotationAxis((float)Math.toRadians(f*roll),FOREARM));
            pose.translate(-sx0,-SHOULDER_Y,-SHOULDER_Z);
            arms.hmi$renderPlayerArm(pose,collector,light,0,0,arm);
            // From the arm into the fist, on the side of it facing you, so the knife sits in front of the arm.
            pose.translate(-f*6/16f-f*.78f*2.6f/16,10.5f/16,-.62f*2.6f/16);
            pose.rotate(Axis.XP.rotationDegrees(kind.grip[0]));
            pose.rotate(Axis.YP.rotationDegrees(f*kind.grip[1]));
            pose.rotate(Axis.ZP.rotationDegrees(f*kind.grip[2]));
            float scale=kind==Kind.DAGGERS ? SCALE*1.35f : SCALE;
            pose.scale(scale,scale,scale);
            float sx=kind.spin[0]/16,sy=kind.spin[1]/16,sz=kind.spin[2]/16;
            pose.translate(sx,sy,sz);
            pose.rotate(Axis.XP.rotationDegrees(A[6]));
            pose.rotate(Axis.YP.rotationDegrees(f*A[7]));
            pose.rotate(Axis.ZP.rotationDegrees(f*A[8]));
            pose.translate(-sx,-sy,-sz);
            float[] bones=rest(model);
            if(kind==Kind.BUTTERFLY) {
                handle(model,bones,"safe",f*A[9]);
                handle(model,bones,"bite",f*A[10]);
            }
            WardrobeRenderer.draw(model,bones,WardrobeModel.FREE,pose,collector,light,OverlayTexture.NO_OVERLAY,true,item.rarity().glows,-1,false);
        } finally { pose.popPose(); }
    }

    private static float[] rest(WardrobeModel model) {
        int n=model.bones();
        float[] p=new float[n*9];
        for(int b=0;b<n;b++) {
            p[b*9]=model.rotation[b*3]; p[b*9+1]=model.rotation[b*3+1]; p[b*9+2]=model.rotation[b*3+2];
            p[b*9+6]=1; p[b*9+7]=1; p[b*9+8]=1;
        }
        return p;
    }
    private static void handle(WardrobeModel model,float[] bones,String name,float degrees) {
        int b=model.bone(name);
        if(b>=0) bones[b*9+2]+=(float)Math.toRadians(degrees);
    }

    /** Keyframes: rows of {time, channels...}; each move starts fast and settles into its key, like the CS view models. */
    private static void sample(float[][] keys,float t,float[] out) {
        int i=0;
        while(i<keys.length-2 && t>keys[i+1][0]) i++;
        float[] a=keys[i],b=keys[Math.min(i+1,keys.length-1)];
        float span=b[0]-a[0],k=span<=0 ? 1 : Math.clamp((t-a[0])/span,0,1);
        k=1-(1-k)*(1-k)*(1-k);
        for(int c=0;c<CHANNELS;c++) out[c]=a[c+1]+(b[c+1]-a[c+1])*k;
    }
}
