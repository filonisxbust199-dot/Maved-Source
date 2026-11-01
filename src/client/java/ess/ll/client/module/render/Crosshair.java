package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Crosshair: the client's own crosshair in place of vanilla's: a cross, a dot, a circle or four corners. It
 * spreads while moving, sprinting and in the air, turns to its own colour over a living target, and a ring
 * around it fills while the attack recharges.
 */
public final class Crosshair {
    public static final ModeSettings STYLE=new ModeSettings("Стиль","Крест","Точка","Круг","Уголки");
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Тема","Белый","Свой");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xffffffff).visibleWhen(()->COLOR.is("Свой"));
    public static final SliderSetting LENGTH=new SliderSetting("Длина",1,10,.5f,4).visibleWhen(()->!STYLE.is("Точка"));
    public static final SliderSetting THICKNESS=new SliderSetting("Толщина",.5f,3,.25f,1);
    public static final SliderSetting GAP=new SliderSetting("Зазор",0,8,.5f,2).visibleWhen(()->!STYLE.is("Точка"));
    public static final BooleanSetting DOT=new BooleanSetting("Точка в центре",false).visibleWhen(()->!STYLE.is("Точка"));
    public static final BooleanSetting DYNAMIC=new BooleanSetting("Расход в движении",true);
    public static final BooleanSetting COOLDOWN=new BooleanSetting("Кулдаун удара",true);
    public static final BooleanSetting TARGET=new BooleanSetting("Подсветка цели",true);
    public static final ColorSetting TARGET_COLOR=new ColorSetting("Цвет на цели",0xffff5a5a).visibleWhen(TARGET::get);
    public static final BooleanSetting OUTLINE=new BooleanSetting("Обводка",true);
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true);
    public static final Setting[] SETTINGS={STYLE,COLOR,OWN,LENGTH,THICKNESS,GAP,DOT,DYNAMIC,COOLDOWN,TARGET,TARGET_COLOR,OUTLINE,GLOW};

    private static float spread,onTarget,cooldownAlpha;
    private static long last=System.nanoTime();

    private Crosshair() {}

    public static boolean enabled() { return ClientModules.CROSSHAIR!=null && ClientModules.CROSSHAIR.enabled(); }

    /** Draws in vanilla's place; vanilla's own is drawn when the module is off. */
    public static boolean render(GuiGraphicsExtractor g,DeltaTracker delta) {
        if(!enabled()) return false;
        var mc=Minecraft.getInstance();
        var player=mc.player;
        long now=System.nanoTime();
        float dt=Math.min(.1f,(now-last)/1e9f);
        last=now;
        if(player==null || !mc.options.getCameraType().isFirstPerson() || player.isSpectator()) return true;

        float want=0;
        if(DYNAMIC.get()) {
            double speed=player.getDeltaMovement().horizontalDistance();
            if(speed>.02) want+=2;
            if(player.isSprinting()) want+=2;
            if(!player.onGround() && !player.getAbilities().flying) want+=3;
            if(player.isCrouching()) want-=1;
        }
        spread=approach(spread,want,dt,12);
        boolean aiming=TARGET.get() && mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive();
        onTarget=approach(onTarget,aiming ? 1 : 0,dt,16);
        float charge=player.getAttackStrengthScale(delta.getGameTimeDeltaPartialTick(false));
        cooldownAlpha=approach(cooldownAlpha,COOLDOWN.get() && charge<.999f ? 1 : 0,dt,charge<.999f ? 20 : 6);

        int base=switch (COLOR.get()) { case "Белый" -> 0xffffffff; case "Свой" -> OWN.get(); default -> Theme.accent(); };
        int color=Theme.mix(base,TARGET_COLOR.get(),onTarget);
        float cx=g.guiWidth()/2f,cy=g.guiHeight()/2f;
        float t=THICKNESS.get(),length=LENGTH.get(),gap=GAP.get()+spread;

        switch (STYLE.get()) {
            case "Точка" -> dot(g,cx,cy,t*1.6f,color);
            case "Круг" -> {
                float r=gap+length*.5f+t;
                if(OUTLINE.get()) Icons.arc(g,cx-r-.5f,cy-r-.5f,2*r+1,(t+1)*24/(2*r+1),1,0x99000000);
                if(GLOW.get()) UiRenderer.shadow(g,cx-r,cy-r,2*r,2*r,r,4,Theme.withAlpha(color,.35f));
                Icons.arc(g,cx-r,cy-r,2*r,t*24/(2*r),1,color);
            }
            case "Уголки" -> {
                float o=gap+t,l=Math.max(2,length*.7f);
                for(int sx=-1;sx<=1;sx+=2) for(int sy=-1;sy<=1;sy+=2) {
                    float x=cx+sx*o,y=cy+sy*o;
                    bar(g,sx<0 ? x-l : x,sy<0 ? y-t : y,l,t,color);
                    bar(g,sx<0 ? x-t : x,sy<0 ? y-l : y,t,l,color);
                }
            }
            default -> {
                bar(g,cx-t/2,cy-gap-length,t,length,color);
                bar(g,cx-t/2,cy+gap,t,length,color);
                bar(g,cx-gap-length,cy-t/2,length,t,color);
                bar(g,cx+gap,cy-t/2,length,t,color);
            }
        }
        if(DOT.get() && !STYLE.is("Точка")) dot(g,cx,cy,t*.9f,color);
        if(cooldownAlpha>.01f) {
            float r=gap+length+t+4;
            Icons.arc(g,cx-r,cy-r,2*r,1.6f*24/(2*r),1,Theme.withAlpha(0x000000,.35f*cooldownAlpha));
            Icons.arc(g,cx-r,cy-r,2*r,1.6f*24/(2*r),Math.clamp(charge,0,1),Theme.withAlpha(color,.9f*cooldownAlpha));
        }
        return true;
    }

    private static void bar(GuiGraphicsExtractor g,float x,float y,float w,float h,int color) {
        if(OUTLINE.get()) UiRenderer.rounded(g,x-.5f,y-.5f,w+1,h+1,Math.min(w,h)/2+.5f,0x99000000);
        if(GLOW.get()) UiRenderer.shadow(g,x-1,y-1,w+2,h+2,Math.min(w,h)/2+1,4,Theme.withAlpha(color,.4f));
        UiRenderer.rounded(g,x,y,w,h,Math.min(w,h)/2,color);
    }
    private static void dot(GuiGraphicsExtractor g,float cx,float cy,float size,int color) {
        bar(g,cx-size/2,cy-size/2,size,size,color);
    }
    private static float approach(float value,float target,float dt,float speed) {
        return value+(target-value)*(1-(float)Math.exp(-dt*speed));
    }
}
