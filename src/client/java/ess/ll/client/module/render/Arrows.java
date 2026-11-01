package ess.ll.client.module.render;

import ess.ll.client.command.ClientData;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Icons;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Arrows: arrows on a circle around the crosshair point to other players, as on a map seen from above with
 * forward at the top. They glide when the player or you move, fade in and out, are coloured by distance (or
 * green for friends) and can carry the name and the distance. By default only players off screen get one.
 */
public final class Arrows {
    public static final SliderSetting RADIUS=new SliderSetting("Радиус",20,120,1,48);
    public static final SliderSetting SIZE=new SliderSetting("Размер",5,20,.5f,12);
    public static final SliderSetting RANGE=new SliderSetting("Дальность",8,256,4,96);
    public static final ModeSettings COLOR=new ModeSettings("Цвет","По дистанции","Тема","Свой");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xffff5a5a).visibleWhen(()->COLOR.is("Свой"));
    public static final BooleanSetting FRIENDS=new BooleanSetting("Друзья зелёным",true);
    public static final BooleanSetting LABELS=new BooleanSetting("Ник и дистанция",true);
    public static final BooleanSetting OFFSCREEN=new BooleanSetting("Только вне экрана",true);
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true);
    public static final Setting[] SETTINGS={RADIUS,SIZE,RANGE,COLOR,OWN,FRIENDS,LABELS,OFFSCREEN,GLOW};
    private static final int NEAR=0xffff4d4d,MIDDLE=0xffffd34f,FAR=0xffffffff,FRIEND=0xff5be37a;
    /** Per player: the angle drawn (gliding to the real one) and how visible the arrow is. */
    private static final Map<Integer,float[]> SHOWN=new HashMap<>();
    private static long last=System.nanoTime();

    private Arrows() {}

    public static boolean enabled() { return ClientModules.ARROWS!=null && ClientModules.ARROWS.enabled(); }
    /** Dev check: arrows drawn now. */
    public static int shown() { return (int)SHOWN.values().stream().filter(s->s[1]>.05f).count(); }

    private static int color(Player player,double distance) {
        if(FRIENDS.get() && ClientData.isFriend(player.getGameProfile().name())) return FRIEND;
        return switch (COLOR.get()) {
            case "Тема" -> Theme.accent();
            case "Свой" -> OWN.get();
            default -> distance<16 ? Theme.mix(NEAR,MIDDLE,(float)(distance/16)) : Theme.mix(MIDDLE,FAR,(float)Math.min(1,(distance-16)/48));
        };
    }

    public static void render(GuiGraphicsExtractor g,DeltaTracker delta) {
        long now=System.nanoTime();
        float dt=Math.min(.1f,(now-last)/1e9f);
        last=now;
        var mc=Minecraft.getInstance();
        if(!enabled() || mc.player==null || mc.level==null) { SHOWN.clear(); return; }
        float partial=delta.getGameTimeDeltaPartialTick(false);
        var camera=mc.gameRenderer.mainCamera();
        Vec3 eye=camera.position();
        float yaw=camera.yRot();
        float cx=g.guiWidth()/2f,cy=g.guiHeight()/2f,radius=RADIUS.get(),size=SIZE.get();
        Map<Integer,float[]> seen=new HashMap<>();
        for(Player player:mc.level.players()) {
            if(player==mc.player || player.isSpectator()) continue;
            Vec3 pos=player.getPosition(partial);
            double distance=pos.distanceTo(eye);
            float[] state=SHOWN.get(player.getId());
            boolean wanted=distance<=RANGE.get() && !(OFFSCREEN.get() && GlowFx.screen(pos.add(0,player.getBbHeight()/2,0))!=null);
            // Straight up on the screen is where the camera looks.
            float angle=(float)Math.toDegrees(Math.atan2(pos.z-eye.z,pos.x-eye.x))-yaw-90;
            if(state==null) { if(!wanted) continue; state=new float[]{angle,0}; }
            float diff=((angle-state[0])%360+540)%360-180;
            state[0]+=diff*(1-(float)Math.exp(-dt*14));
            state[1]+=((wanted ? 1 : 0)-state[1])*(1-(float)Math.exp(-dt*(wanted ? 12 : 8)));
            if(!wanted && state[1]<.02f) continue;
            seen.put(player.getId(),state);
            draw(g,player,cx,cy,radius,size,state[0],state[1],distance);
        }
        SHOWN.clear(); SHOWN.putAll(seen);
    }

    private static void draw(GuiGraphicsExtractor g,Player player,float cx,float cy,float radius,float size,float angle,float alpha,double distance) {
        int color=color(player,distance);
        double a=Math.toRadians(angle);
        // Grows out of the circle as it appears.
        float r=radius+(1-alpha)*8;
        float x=cx+(float)Math.sin(a)*r,y=cy-(float)Math.cos(a)*r;
        if(GLOW.get()) UiRenderer.shadow(g,x-size*.55f,y-size*.55f,size*1.1f,size*1.1f,size*.55f,size*.6f,Theme.withAlpha(color,.45f*alpha));
        g.pose().pushMatrix();
        try {
            g.pose().translate(x,y);
            g.pose().rotate((float)a);
            Icons.draw(g,Icons.Icon.NAV,-size/2,-size/2,size,Theme.withAlpha(color,alpha));
        } finally { g.pose().popMatrix(); }
        if(!LABELS.get()) return;
        var font=MsdfFont.UI;
        float lx=cx+(float)Math.sin(a)*(r+size*.9f+9),ly=cy-(float)Math.cos(a)*(r+size*.9f+9);
        String name=player.getGameProfile().name(),meters=Math.round(distance)+" м";
        float w=Math.max(font.width(name,6.5f),font.width(meters,5.5f))+8;
        UiRenderer.rounded(g,lx-w/2,ly-8,w,16,5,Theme.withAlpha(Theme.background(),.75f*alpha));
        font.midCenter(g,name,lx,ly-3,6.5f,Theme.withAlpha(Theme.text(),alpha));
        font.midCenter(g,meters,lx,ly+4,5.5f,Theme.withAlpha(color,alpha));
    }
}
