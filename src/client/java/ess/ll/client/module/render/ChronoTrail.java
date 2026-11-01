package ess.ll.client.module.render;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GhostModelBatch;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Interpolated historical player meshes: normal, motion blur, soul and comet. */
public final class ChronoTrail {
    public static final ModeSettings MODE=new ModeSettings("Режим","Обычный","Motion Blur","Душа","Комета");
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Theme","White","Custom");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xff8f7cff).visibleWhen(()->COLOR.is("Custom"));
    public static final SliderSetting COUNT=new SliderSetting("Количество",1,30,1,8);
    public static final SliderSetting LIFE=new SliderSetting("Время жизни",.15f,1.5f,.05f,.35f);
    public static final BooleanSetting GLOW=new BooleanSetting("Свечение",true);
    public static final BooleanSetting ARMOR=new BooleanSetting("Отображать броню",true).visibleWhen(()->!MODE.is("Душа"));
    public static final BooleanSetting SELF=new BooleanSetting("Себя",true);
    public static final BooleanSetting OTHERS=new BooleanSetting("Другие игроки",false);
    public static final SliderSetting ALPHA=new SliderSetting("Альфа",.15f,1,.05f,.5f);
    public static final BooleanSetting COMET_SPARKS=new BooleanSetting("Искры",true).visibleWhen(()->MODE.is("Комета"));
    public static final SliderSetting SPARK_AMOUNT=new SliderSetting("Плотность искр",0,3,.1f,1)
            .visibleWhen(()->MODE.is("Комета") && COMET_SPARKS.get());
    public static final BooleanSetting PARTICLES=new BooleanSetting("Частицы",true).visibleWhen(()->!MODE.is("Комета"));
    public static final Setting[] SETTINGS={MODE,COLOR,OWN,COUNT,LIFE,GLOW,ARMOR,SELF,OTHERS,ALPHA,
            COMET_SPARKS,SPARK_AMOUNT,PARTICLES};
    private static final Minecraft MC=Minecraft.getInstance();
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static final Map<Integer,ArrayDeque<ChronoFrame>> HISTORIES=new LinkedHashMap<>();
    private static Object recordedWorld;
    static { GlowFx.add(ChronoTrail::paint); }
    private ChronoTrail() {}
    public static boolean enabled() { return ClientModules.CHRONO_TRAIL!=null && ClientModules.CHRONO_TRAIL.enabled(); }
    private static void clear() { HISTORIES.clear(); recordedWorld=null; }

    private static void paint(GlowFx fx) {
        if(!enabled() || MC.level==null || MC.player==null) { if(!HISTORIES.isEmpty()) clear(); return; }
        if(recordedWorld!=MC.level) { clear(); recordedWorld=MC.level; }
        long now=System.currentTimeMillis();
        float partial=MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if(SELF.get()) record((AbstractClientPlayer)MC.player,partial,now);
        if(OTHERS.get()) for(Entity entity:MC.level.entitiesForRendering()) {
            if(entity instanceof AbstractClientPlayer player && player!=MC.player && player.distanceToSqr(MC.player)<4096)
                record(player,partial,now);
        }
        HISTORIES.entrySet().removeIf(entry->entry.getKey()==MC.player.getId() && !SELF.get() ||
                entry.getKey()!=MC.player.getId() && (!OTHERS.get() || MC.level.getEntity(entry.getKey())==null));
        int keepMs=(int)(LIFE.get()*1000)+120;
        for(ArrayDeque<ChronoFrame> frames:HISTORIES.values())
            while(!frames.isEmpty() && (now-frames.peekFirst().time>keepMs || frames.size()>120)) frames.pollFirst();
        HISTORIES.values().removeIf(ArrayDeque::isEmpty);
        draw(fx,now);
    }
    private static void record(AbstractClientPlayer player,float partial,long now) {
        if(player.isRemoved() || player.isSpectator()) { HISTORIES.remove(player.getId()); return; }
        ArrayDeque<ChronoFrame> frames=HISTORIES.computeIfAbsent(player.getId(),id->new ArrayDeque<>());
        ChronoFrame previous=frames.peekLast();
        if(previous!=null && now-previous.time<20) return;
        ChronoFrame frame=ChronoFrame.capture(player,partial,now,!MODE.is("Душа"));
        if(frame==null) return;
        if(previous!=null && previous.distanceSq(frame)>64) frames.clear();
        frames.addLast(frame);
    }
    private static void draw(GlowFx fx,long now) {
        Vec3 camera=fx.eye;
        boolean firstPerson=MC.options.getCameraType().isFirstPerson();
        boolean blur=MODE.is("Motion Blur"),comet=MODE.is("Комета"),soul=MODE.is("Душа");
        int base=COLOR.is("White")?0xf2f5ff:COLOR.is("Custom")?OWN.get()&0xffffff:Theme.accent()&0xffffff;
        float baseAlpha=ALPHA.get(),duration=LIFE.get()*1000;
        int copies=Math.round(COUNT.get());
        if(blur) copies=Math.min(48,copies*2);
        if(comet) copies=Math.min(54,copies*3);
        boolean particles=comet?COMET_SPARKS.get() && SPARK_AMOUNT.get()>0:PARTICLES.get();
        int drawn=0,armored=0;
        for(var entry:HISTORIES.entrySet()) {
            if(drawn>=120) break;
            if(firstPerson && entry.getKey()==MC.player.getId()) continue;
            ArrayDeque<ChronoFrame> frames=entry.getValue();
            ChronoFrame newest=frames.peekLast();
            if(newest==null) continue;
            float historyFade=smooth((now-frames.peekFirst().time)/180f);
            for(int index=copies;index>=1 && drawn<120;index--) {
                float age=index/(float)(copies+1);
                ChronoFrame frame=ChronoFrame.at(frames,now-(long)(duration*age));
                if(frame==null) continue;
                double separation=Math.sqrt(frame.distanceSq(newest));
                float separate=smooth((float)((separation-.025)/.65));
                if(separate<.01f || camera.distanceToSqr(frame.x,frame.y,frame.z)>4096) continue;
                float fade=(1-age)*(1-age)*smooth(age/.1f)*separate*historyFade;
                float opacity=baseAlpha*fade*(blur?.66f:comet?.54f:1);
                if(opacity<.015f) continue;
                int color=comet?mix(0xf5c4f7,base,age):base;
                if(!soul && !comet) color=mix(color,0xffffff,.35f);
                int tint=Math.clamp((int)(opacity*255),0,255)<<24|color;
                BATCH.begin(); frame.emit(camera,tint,soul,BATCH);
                float modelGlow=GLOW.get()?opacity*(soul?.23f:comet?.32f:.2f):0;
                BATCH.flush(frame.skin,modelGlow,false,false,soul,(now%600000L)*.001f);
                drawn++;
                if(ARMOR.get() && !soul && opacity>=.1f && armored<32 && !frame.armor.isEmpty()) {
                    int armorTint=Math.clamp((int)(opacity*255),0,255)<<24|mix(color,0xffffff,.65f);
                    ChronoArmor.render(frame,camera,armorTint,GLOW.get()?opacity*.08f:0);
                    armored++;
                }
                if(particles) {
                    int amount=comet?Math.min(5,Math.max(1,Math.round(SPARK_AMOUNT.get()*2))):soul?2:1;
                    sparkle(fx,frame,entry.getKey(),index,age,opacity,color,amount,comet);
                    if(!soul && !blur && age<.55f && index%3==0) poseSparks(fx,frame,color,opacity);
                }
            }
        }
    }
    private static void sparkle(GlowFx fx,ChronoFrame frame,int entityId,int index,float age,float opacity,int color,int amount,boolean comet) {
        for(int i=0;i<amount;i++) {
            int hash=entityId*92821+index*40503+i*6151;
            float a=hash01(hash),b=hash01(hash*17+13),c=hash01(hash*31+7);
            double x=frame.x+(a-.5f)*1.05,y=frame.y+.3+b*1.45+age*.12,z=frame.z+(c-.5f)*1.05;
            float half=(.018f+a*.026f)*(1-age*.5f);
            int rgb=comet?0x8fd275:color;
            int strength=Math.clamp((int)(opacity*(comet?150:95)),0,150);
            if(strength<2) continue;
            fx.sprite(new Vec3(x,y,z),half*2.2f,GlowFx.DOT,rgb,strength/765f,.65f,0);
            fx.sprite(new Vec3(x,y,z),half,GlowFx.DOT,mix(rgb,0xffffff,.45f),strength/255f,.85f,0);
        }
    }
    private static void poseSparks(GlowFx fx,ChronoFrame frame,int color,float opacity) {
        int strength=Math.clamp((int)(opacity*78),0,80);
        if(strength<2) return;
        int tint=mix(color,0xffffff,.55f);
        var model=ChronoFrame.model(frame.slimArms);
        fx.sprite(frame.anchor(model.head,0,-.52f,0),.044f,GlowFx.DOT,tint,strength/255f,.8f,0);
        fx.sprite(frame.anchor(model.leftArm,0,.64f,0),.028f,GlowFx.DOT,tint,strength/255f,.8f,0);
        fx.sprite(frame.anchor(model.rightArm,0,.64f,0),.028f,GlowFx.DOT,tint,strength/255f,.8f,0);
    }
    private static float hash01(int value) { value=(value^value>>>13)*1274126177; return ((value^value>>>16)&Integer.MAX_VALUE)/2147483600f; }
    private static float smooth(float v) { float p=Math.clamp(v,0,1); return p*p*(3-2*p); }
    private static int mix(int from,int to,float t) {
        float p=Math.clamp(t,0,1);
        int r=(int)((from>>16&255)+((to>>16&255)-(from>>16&255))*p);
        int g=(int)((from>>8&255)+((to>>8&255)-(from>>8&255))*p);
        int b=(int)((from&255)+((to&255)-(from&255))*p);
        return r<<16|g<<8|b;
    }
}
