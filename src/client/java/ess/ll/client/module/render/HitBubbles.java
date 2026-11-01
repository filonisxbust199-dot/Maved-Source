package ess.ll.client.module.render;

import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

/**
 * HitBubbles: a glowing ring spreads out where your hit lands, a critical hit adds rays bursting out of it, and
 * the damage dealt rises from the spot as a number (a critical one bigger and in its own colour). The damage is
 * read from the target's health over the next ticks, so it is what the server really took off.
 */
public final class HitBubbles {
    public static final SliderSetting SIZE=new SliderSetting("Размер",.3f,2,.05f,1);
    public static final SliderSetting LIFE=new SliderSetting("Время",.2f,1.5f,.05f,.55f);
    public static final SliderSetting GLOW=new SliderSetting("Свечение",0,1,.05f,.75f);
    public static final ModeSettings COLOR=new ModeSettings("Цвет","Тема","Свой");
    public static final ColorSetting OWN=new ColorSetting("Свой цвет",0xff8f7cff).visibleWhen(()->COLOR.is("Свой"));
    public static final BooleanSetting CRITS=new BooleanSetting("Искры на крите",true);
    public static final ColorSetting CRIT_COLOR=new ColorSetting("Цвет крита",0xffffd34f);
    public static final BooleanSetting NUMBERS=new BooleanSetting("Цифры урона",true);
    public static final SliderSetting NUMBER_SIZE=new SliderSetting("Размер цифр",.5f,2,.05f,1).visibleWhen(NUMBERS::get);
    public static final BooleanSetting ONLY_PLAYERS=new BooleanSetting("Только по игрокам",false);
    public static final Setting[] SETTINGS={SIZE,LIFE,GLOW,COLOR,OWN,CRITS,CRIT_COLOR,NUMBERS,NUMBER_SIZE,ONLY_PLAYERS};
    static { GlowFx.add(HitBubbles::paint); }

    private static final HitBubbles INSTANCE=new HitBubbles();
    private static boolean registered;

    private static final class Bubble {
        final Vec3 pos; final boolean crit; final long born; final float life; final int color;
        Bubble(Vec3 pos,boolean crit,long born,float life,int color) { this.pos=pos; this.crit=crit; this.born=born; this.life=life; this.color=color; }
        float t(long now) { return Math.clamp((now-born)/(life*1000),0,1); }
    }
    /** A hit waiting to see how much health it took. */
    private static final class Pending {
        final int target; final Vec3 pos; final boolean crit; final float health; int ticks;
        Pending(int target,Vec3 pos,boolean crit,float health) { this.target=target; this.pos=pos; this.crit=crit; this.health=health; }
    }
    private static final class Number {
        final Vec3 pos; final String text; final boolean crit; final long born; final float drift;
        Number(Vec3 pos,String text,boolean crit,long born,float drift) { this.pos=pos; this.text=text; this.crit=crit; this.born=born; this.drift=drift; }
    }
    private static final List<Bubble> BUBBLES=new ArrayList<>();
    private static final List<Pending> PENDING=new ArrayList<>();
    private static final List<Number> NUMBERS_SHOWN=new ArrayList<>();
    /** Whether the hit about to land is a critical one; decided before the attack resets the swing. */
    private boolean critNext;

    private HitBubbles() {}

    public static boolean enabled() { return ClientModules.HIT_BUBBLES!=null && ClientModules.HIT_BUBBLES.enabled(); }
    /** Dev check: bubbles and numbers alive now. */
    public static int alive() { return BUBBLES.size()+NUMBERS_SHOWN.size(); }

    public static void tick(Minecraft mc) {
        boolean on=enabled();
        if(on!=registered) {
            registered=on;
            if(on) EventBus.register(INSTANCE); else { EventBus.unregister(INSTANCE); BUBBLES.clear(); PENDING.clear(); NUMBERS_SHOWN.clear(); }
        }
        if(!on || mc.level==null) return;
        long now=System.currentTimeMillis();
        BUBBLES.removeIf(b->b.t(now)>=1);
        NUMBERS_SHOWN.removeIf(n->now-n.born>1100);
        for(Iterator<Pending> it=PENDING.iterator();it.hasNext();) {
            Pending p=it.next();
            if(!(mc.level.getEntity(p.target) instanceof LivingEntity living) || ++p.ticks>12) { it.remove(); continue; }
            float lost=p.health-(living.getHealth()+living.getAbsorptionAmount());
            if(lost<.05f) continue;
            it.remove();
            if(NUMBERS.get()) {
                String text=lost>=10 ? String.valueOf(Math.round(lost)) : String.format(Locale.ROOT,"%.1f",lost);
                NUMBERS_SHOWN.add(new Number(p.pos,text,p.crit,now,(float)ThreadLocalRandom.current().nextDouble(-1,1)));
            }
        }
    }

    /** Before the swing resets: is this a critical hit by vanilla's rules. Afterwards: spawns the bubble. */
    @Subscribe
    private void attack(AttackEntityEvent event) {
        var mc=Minecraft.getInstance();
        Player player=mc.player;
        Entity target=event.getEntity();
        if(player==null || target==null) return;
        if(event.getPhase()==AttackEntityEvent.Phase.PRE) {
            critNext=player.getAttackStrengthScale(.5f)>.9f && player.fallDistance>0 && !player.onGround() && !player.onClimbable()
                    && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
            return;
        }
        if(!(target instanceof LivingEntity living) || ONLY_PLAYERS.get() && !(target instanceof Player)) return;
        Vec3 eye=player.getEyePosition();
        Vec3 at;
        if(mc.hitResult instanceof EntityHitResult hit && hit.getEntity()==target) at=hit.getLocation();
        else at=target.getBoundingBox().clip(eye,eye.add(player.getViewVector(1).scale(8))).orElse(target.getBoundingBox().getCenter());
        // Pulled towards the eye so the target's own model does not hide it.
        Vec3 toEye=eye.subtract(at);
        if(toEye.lengthSqr()>1e-6) at=at.add(toEye.normalize().scale(.18));
        boolean crit=critNext && CRITS.get();
        int color=crit ? CRIT_COLOR.get() : COLOR.is("Свой") ? OWN.get() : Theme.accent();
        long now=System.currentTimeMillis();
        BUBBLES.add(new Bubble(at,crit,now,LIFE.get()*(crit ? 1.25f : 1),color));
        if(BUBBLES.size()>32) BUBBLES.removeFirst();
        PENDING.add(new Pending(target.getId(),at,crit,living.getHealth()+living.getAbsorptionAmount()));
    }

    private static void paint(GlowFx fx) {
        if(!enabled() || BUBBLES.isEmpty()) return;
        long now=System.currentTimeMillis();
        float size=SIZE.get(),glow=GLOW.get();
        for(Bubble b:BUBBLES) {
            float t=b.t(now);
            fx.sprite(b.pos,.55f*size*(b.crit ? 1.3f : 1),b.crit ? GlowFx.BURST : GlowFx.RING,b.color,1,glow,t);
        }
    }

    /** The numbers rise from the hit, pop in, drift a little sideways and fade. */
    public static void renderHud(GuiGraphicsExtractor g,DeltaTracker delta) {
        if(NUMBERS_SHOWN.isEmpty() || !enabled()) return;
        long now=System.currentTimeMillis();
        var font=MsdfFont.ESP;
        for(Number n:NUMBERS_SHOWN) {
            float t=Math.clamp((now-n.born)/1100f,0,1);
            Vector2f at=GlowFx.screen(n.pos.add(0,t*.6,0));
            if(at==null) continue;
            // Grows past its size in the first moment, then settles.
            float pop=t<.12f ? .6f+.55f*(1-(float)Math.pow(1-t/.12f,3)) : 1.15f-.15f*Math.min(1,(t-.12f)/.15f);
            float fade=t<.7f ? 1 : 1-(t-.7f)/.3f;
            float size=(n.crit ? 13 : 10)*NUMBER_SIZE.get()*pop;
            int color=n.crit ? CRIT_COLOR.get() : 0xffffffff;
            float x=at.x+n.drift*t*14,w=font.width(n.text,size);
            UiRenderer.shadow(g,x-w/2-4,at.y-size/2-3,w+8,size+6,size/2,8,Theme.withAlpha(n.crit ? color : Theme.accent(),.45f*fade));
            font.midCenter(g,n.text,x,at.y,size,Theme.withAlpha(color,fade));
        }
    }
}
