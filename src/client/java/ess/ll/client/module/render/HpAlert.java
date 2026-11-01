package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HpAlert: warns when health runs low. "Мигание" pulses the edges of the screen red (warmer, towards yellow,
 * with the yellow tint); "Надпись" slides a plate with a beating heart in at the top: "Здоровье подходит к
 * концу!" and the health left. Both fade in and out instead of switching.
 */
public final class HpAlert {
    public static final ModeSettings MODE=new ModeSettings("Как предупреждать","Мигание","Надпись","Оба");
    public static final SliderSetting HEALTH=new SliderSetting("Порог здоровья",1,20,.5f,6);
    public static final ModeSettings TINT=new ModeSettings("Оттенок","Красный","Жёлтый").visibleWhen(()->!MODE.is("Надпись"));
    public static final SliderSetting STRENGTH=new SliderSetting("Сила мигания",.1f,1,.05f,.6f).visibleWhen(()->!MODE.is("Надпись"));
    public static final BooleanSetting ABSORPTION=new BooleanSetting("Учитывать золотые сердца",true);
    public static final Setting[] SETTINGS={MODE,HEALTH,TINT,STRENGTH,ABSORPTION};
    private static float shown;
    private static long last=System.nanoTime();

    private HpAlert() {}

    public static boolean enabled() { return ClientModules.HP_ALERT!=null && ClientModules.HP_ALERT.enabled(); }
    /** Dev check: how far the alert is faded in. */
    public static float shown() { return shown; }

    public static void render(GuiGraphicsExtractor g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        long now=System.nanoTime();
        float dt=Math.min(.1f,(now-last)/1e9f);
        last=now;
        float hp=mc.player==null ? 99 : mc.player.getHealth()+(ABSORPTION.get() ? mc.player.getAbsorptionAmount() : 0);
        boolean low=enabled() && mc.player!=null && mc.player.isAlive() && !mc.player.isCreative() && !mc.player.isSpectator() && hp<=HEALTH.get();
        shown+=((low ? 1 : 0)-shown)*(1-(float)Math.exp(-dt*8));
        if(shown<.01f) return;
        double t=now/1e9;
        // Lower health beats faster.
        float rate=1.2f+2.2f*(1-Math.clamp(hp/Math.max(1,HEALTH.get()),0,1));
        float pulse=(float)(.5+.5*Math.sin(t*Math.PI*2*rate));
        if(!MODE.is("Надпись")) flash(g,pulse);
        if(!MODE.is("Мигание")) banner(g,hp,pulse,t);
    }

    /** The screen's edges glow and pulse; the middle stays clear so the fight is still visible. */
    private static void flash(GuiGraphicsExtractor g,float pulse) {
        int color=TINT.is("Жёлтый") ? 0xffff9a2e : 0xffff2b2b;
        float a=shown*STRENGTH.get()*(.45f+.55f*pulse);
        int w=g.guiWidth(),h=g.guiHeight();
        float edge=Math.min(w,h)*.22f;
        int steps=14;
        for(int i=0;i<steps;i++) {
            float f=i/(float)steps,d=edge*f,s=edge/steps+1;
            int c=Theme.withAlpha(color,a*(1-f)*(1-f)*.55f);
            UiRenderer.rect(g,d,d,w-2*d,s,c);
            UiRenderer.rect(g,d,h-d-s,w-2*d,s,c);
            UiRenderer.rect(g,d,d+s,s,h-2*d-2*s,c);
            UiRenderer.rect(g,w-d-s,d+s,s,h-2*d-2*s,c);
        }
        UiRenderer.rect(g,0,0,w,h,Theme.withAlpha(color,a*.08f));
    }

    /** Plate at the top: a beating heart, the warning and the health left. */
    private static void banner(GuiGraphicsExtractor g,float hp,float pulse,double t) {
        var font=MsdfFont.UI;
        String title="Здоровье подходит к концу!",sub=String.format(Locale.ROOT,"Низкое здоровье · %.1f HP",hp);
        float ts=9,ss=6.8f,tw=Math.max(font.width(title,ts),font.width(sub,ss));
        float w=22+8+tw+12,h=30,x=(g.guiWidth()-w)/2,y=8-(1-shown)*18;
        float a=shown;
        UiRenderer.shadow(g,x,y,w,h,h/2,10,Theme.withAlpha(0xffff3b3b,.35f*a*(.6f+.4f*pulse)));
        UiRenderer.rounded(g,x,y,w,h,h/2,Theme.withAlpha(0xff101318,.86f*a));
        float heart=12*(1+.16f*pulse),hx=x+8+11-heart/2,hy=y+h/2-heart/2;
        UiRenderer.shadow(g,hx,hy,heart,heart,heart/2,5,Theme.withAlpha(0xffff3b3b,.5f*a*pulse));
        Icons.draw(g,Icons.Icon.HEART_FILLED,hx,hy,heart,Theme.withAlpha(0xffff4d4d,a));
        float tx=x+8+22+4;
        font.mid(g,title,tx,y+11,ts,Theme.withAlpha(0xffffffff,a));
        font.mid(g,sub,tx,y+21.5f,ss,Theme.withAlpha(0xffff8a8a,a*.9f));
    }
}
