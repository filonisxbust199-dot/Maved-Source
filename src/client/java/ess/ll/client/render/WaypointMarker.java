package ess.ll.client.render;

import ess.ll.client.features.Waypoints;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

/**
 * The active .way point in the world: a beam rising to the sky, a ring on the ground that pulses outwards, a
 * glowing gem over it and a plate with the name and the metres left. A far point is drawn on the same line of
 * sight but no further than NEAR blocks away, at full size, so it stays visible past the render distance.
 */
public final class WaypointMarker {
    /** Past this the marker is pulled in along the view line and scaled down with it. */
    private static final double NEAR=96;
    private static final float BEAM=90;

    private WaypointMarker() {}

    public static void init() { GlowFx.add(WaypointMarker::paint); }

    /** Where the point stands: the top block at its x/z when that chunk is loaded, otherwise at eye level. */
    private static Vec3 base(Waypoints.Point p) {
        var mc=Minecraft.getInstance();
        double y=mc.player.getY();
        if(mc.level.hasChunk(p.x()>>4,p.z()>>4)) y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,p.x(),p.z());
        return new Vec3(p.x()+.5,y,p.z()+.5);
    }

    /** The drawn position (the point itself when near, pulled in along the view line when far) and a size factor. */
    private static double[] placed(Vec3 eye,Vec3 base) {
        double d=base.subtract(eye).length();
        Vec3 at=d>NEAR ? eye.add(base.subtract(eye).scale(NEAR/d)) : base;
        // Beam and gem grow with the drawn distance so they keep a readable width on screen.
        return new double[]{at.x,at.y,at.z,Math.max(1,Math.min(d,NEAR)/20)};
    }

    private static void paint(GlowFx fx) {
        var mc=Minecraft.getInstance();
        Waypoints.Point p=Waypoints.active();
        if(p==null || mc.player==null) return;
        int color=Theme.accent()&0xffffff;
        double[] at=placed(fx.eye,base(p));
        float k=(float)at[3];
        Vec3 foot=new Vec3(at[0],at[1],at[2]);
        double t=System.currentTimeMillis()%1600/1600.0;
        // Beam: bright at the ground, fading into the sky; it always rises well above eye level, so a point in
        // a valley below still shows.
        double height=Math.max(BEAM,fx.eye.y-foot.y+60);
        fx.line(foot,foot.add(0,height,0),.08f*k,color,.95f,0,.9f);
        // Ground ring, and a second one spreading out from it.
        ring(fx,foot.add(0,.06,0),.75*k,.035f*k,color,.9f);
        ring(fx,foot.add(0,.06,0),(.75+t*1.1)*k,.03f*k,color,(float)(.7*(1-t)));
        // The gem floats and bobs over the point.
        Vec3 gem=foot.add(0,2.4*k+.15*Math.sin(System.currentTimeMillis()/400.0),0);
        fx.sprite(gem,.8f*k,GlowFx.DOT,color,1,1,0);
        fx.sprite(gem,1.3f*k,GlowFx.RING,color,.9f,.8f,(float)t);
    }
    private static void ring(GlowFx fx,Vec3 c,double r,float width,int color,float strength) {
        int n=28;
        Vec3 prev=null;
        for(int i=0;i<=n;i++) {
            double a=i*Math.PI*2/n;
            Vec3 q=c.add(Math.cos(a)*r,0,Math.sin(a)*r);
            if(prev!=null) fx.line(prev,q,width,color,strength,strength,.5f);
            prev=q;
        }
    }

    /** The plate over the gem: name and metres, in the ESP font. */
    public static void renderHud(GuiGraphicsExtractor g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        Waypoints.Point p=Waypoints.active();
        if(p==null || mc.player==null) return;
        Vec3 eye=mc.gameRenderer.mainCamera().position();
        double[] at=placed(eye,base(p));
        double k=at[3];
        Vector2f s=GlowFx.screen(new Vec3(at[0],at[1]+4.3*k,at[2]));
        if(s==null) return;
        var font=MsdfFont.ESP;
        String name=p.name(),dist=distance(mc,p);
        float size=7.5f,nw=font.width(name,size),dw=font.width(dist,size*.9f),w=nw+6+dw+14,h=13;
        float x=s.x-w/2,y=s.y-h/2;
        UiRenderer.shadow(g,x,y,w,h,5,8,Theme.withAlpha(Theme.accent(),.35f));
        UiRenderer.rounded(g,x,y,w,h,5,0xdc101318);
        font.mid(g,name,x+7,s.y,size,0xffffffff);
        font.mid(g,dist,x+7+nw+6,s.y,size*.9f,Theme.mix(Theme.accent(),0xffffffff,.35f));
    }

    public static double distanceBlocks(Minecraft mc,Waypoints.Point p) {
        double dx=p.x()+.5-mc.player.getX(),dz=p.z()+.5-mc.player.getZ();
        return Math.sqrt(dx*dx+dz*dz);
    }
    /** Horizontal metres to the point, "1.2 км" past a kilometre. */
    public static String distance(Minecraft mc,Waypoints.Point p) {
        double d=distanceBlocks(mc,p);
        return d>=1000 ? String.format(Locale.ROOT,"%.1f км",d/1000) : Math.round(d)+" м";
    }
    /** Angle to the point relative to where the player looks, radians, 0 = straight ahead, clockwise. */
    public static float bearing(Minecraft mc,Waypoints.Point p) {
        double dx=p.x()+.5-mc.player.getX(),dz=p.z()+.5-mc.player.getZ();
        double target=Math.toDegrees(Math.atan2(-dx,dz));
        return (float)Math.toRadians(((target-mc.player.getYRot())%360+540)%360-180);
    }
}
