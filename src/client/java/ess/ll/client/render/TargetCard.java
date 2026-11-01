package ess.ll.client.render;

import ess.ll.client.module.ClientModules;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

/**
 * TargetHud card ported from the dimasik reference, layout and motion unchanged; only the card background is
 * the theme surface. Everything is laid out in the reference's units (the ring card is 220 x 63) and scaled
 * by {@link #SCALE} into HUD units.
 *
 * "Круг": head, name, a row of worn items and an HP ring. When health drops, particles leave the lost arc
 * and orbit along the ring, starting as fast as the ring shrinks and slowly settling on it.
 * "Полоска": a big head, the name and a health bar whose end particles fly off as it shrinks.
 * A hit bursts fireflies out of the head; while targeted, single fireflies keep drifting around the card.
 */
final class TargetCard {
    static final float SCALE=.55f;
    private static final MsdfFont FONT=MsdfFont.UI;
    private static final int ABSORPTION=0xfffff300;
    private static final float RING_W=220, RING_H=63, BAR_H=76;
    /** Where the content starts, past the dark band under the head. */
    private static final float RING_X=71, BAR_X=84;
    private static LivingEntity shown;
    private static boolean ringLayout;
    /** Health for the bar card, the ring's health and its lagging ghost, the change pulse, absorption, item use. */
    private static float barHp, ringHp, ringGhost, pulse, absorb, using, reveal, lastHp=-1;
    /** Previous fractions / values the particle emitters compare against; -1 until the first frame. */
    private static float ringFrac=-1, ringValue=-1, absFrac=-1, absValue=-1, barWidth=-1, barAbsWidth=-1, hurtBefore, idle;
    private static ItemStack usedItem=ItemStack.EMPTY;
    private static final HudRenderer.Roller HP=new HudRenderer.Roller();
    private static final List<Particle> PARTS=new ArrayList<>();
    private static float dt;
    private TargetCard() {}

    private static boolean ringStyle() { return !ClientModules.TARGET_STYLE.is("Полоска"); }
    private static boolean classic() { return ClientModules.TARGET_PARTICLE_MODE.is("Классика"); }
    private static int count() { return Math.max(1,Math.round(ClientModules.TARGET_PARTICLE_COUNT.get())); }

    /** Card size in HUD units. */
    static float[] size(LivingEntity target) {
        if(ringStyle()) return new float[]{RING_W*SCALE,RING_H*SCALE};
        String name=target==null ? "" : target.getName().getString();
        return new float[]{Math.max(200,BAR_X+FONT.width(name,16)+10)*SCALE,BAR_H*SCALE};
    }
    /** Called each frame while the card shows; a new target restarts every animation from its own values. */
    static void update(LivingEntity target,double frameDt) {
        dt=(float)Math.min(.05,frameDt);
        float hp=target.getHealth(),abs=target.getAbsorptionAmount();
        // Switching the style restarts too: ring particles would otherwise hang where the ring used to be.
        if(target!=shown || ringStyle()!=ringLayout) {
            ringLayout=ringStyle();
            barHp=ringHp=ringGhost=hp; pulse=0; lastHp=hp; absorb=abs; using=0; reveal=0; usedItem=ItemStack.EMPTY;
            ringFrac=ringValue=absFrac=absValue=barWidth=barAbsWidth=-1; hurtBefore=0; idle=0;
            PARTS.clear(); HP.init=false;
            shown=target;
        }
        absorb=approach(absorb,abs,3); barHp=approach(barHp,hp,3); reveal=approach(reveal,1,5);
        if(ringStyle()) {
            if(lastHp>=0 && Math.abs(hp-lastHp)>.001f) pulse=1;
            ringHp=approach(ringHp,hp,6); ringGhost=approach(ringGhost,hp,2.6f); pulse=approach(pulse,0,4.5f);
            lastHp=hp;
        }
        HP.update(Math.round(hp),dt);
    }
    /** The card left the screen: particles and emitter memory are dropped. */
    static void reset() { shown=null; PARTS.clear(); reveal=0; }

    static void draw(GuiGraphicsExtractor g,LivingEntity target,float alpha) {
        g.pose().pushMatrix();
        // The reference rounds with plain circular arcs, not the UI's squircle.
        UiRenderer.circular=true;
        try {
            g.pose().scale(SCALE,SCALE);
            if(ringStyle()) ringCard(g,target,alpha); else barCard(g,target,alpha);
        } finally { UiRenderer.circular=false; g.pose().popMatrix(); }
    }

    // ---- "Круг" -------------------------------------------------------------------------------------

    private static void ringCard(GuiGraphicsExtractor g,LivingEntity e,float alpha) {
        float max=Math.max(1,e.getMaxHealth());
        float frac=Math.clamp(ringHp/max,0,1),ghost=Math.clamp(ringGhost/max,0,1),abs=Math.clamp(absorb/max,0,1);
        hitBurst(e,RING_W,RING_H,31.5f,31.5f);
        ringParticles(frac,e.getHealth(),false);
        ringParticles(abs,e.getAbsorptionAmount(),true);
        Theme.plate(g,0,0,RING_W,RING_H,14,alpha);
        Theme.plateBand(g,0,0,RING_W,RING_H,false,63,14,alpha);
        head(g,e,10,10,43,11,alpha);
        FONT.draw(g,fit(e.getName().getString(),105,14),RING_X,14,14,a(Theme.PLATE_TEXT,alpha));
        ItemStack[] slots={e.getItemBySlot(EquipmentSlot.FEET),e.getItemBySlot(EquipmentSlot.LEGS),e.getItemBySlot(EquipmentSlot.CHEST),
                e.getItemBySlot(EquipmentSlot.HEAD),e.getMainHandItem(),e.getOffhandItem()};
        int i=0;
        for(ItemStack stack:slots) if(!stack.isEmpty()) item(g,stack,RING_X+i++*17,33,18,false,alpha);
        int accent=Theme.accent();
        float stroke=2.8f+.55f*pulse;
        ring(g,173,13,38,2.8f,1,a(Theme.withAlpha(accent,.25f),alpha));
        if(ghost>frac+.001f) ring(g,173,13,38,2.8f,ghost,a(Theme.withAlpha(accent,.38f),alpha));
        if(frac>.001f) {
            if(pulse>.01f) ring(g,171.5f,11.5f,41,1.8f,frac,a(Theme.withAlpha(accent,.22f*pulse),alpha));
            ring(g,173,13,38,stroke,frac,a(accent,alpha));
        }
        if(abs>.001f) ring(g,173,13,38,stroke,abs,a(ABSORPTION,alpha));
        // The number sits on the ring's centre.
        float digit=FONT.width("0",13)*1.02f,w=HP.width(digit);
        HP.draw(g,192-w/2,HudRenderer.mid(13,38,13),13,digit,a(Theme.mix(Theme.PLATE_TEXT,accent,.32f*pulse),alpha),false);
        particles(g,alpha);
    }
    /**
     * Ring emitter. A real drop of health (more than 0.05) sends the full count spread over the lost arc; the
     * ring's animated shrink keeps sending a few each frame from the arc it just gave up. Each particle starts
     * on the ring and turns backwards along it at about the speed the ring is shrinking.
     */
    private static void ringParticles(float frac,float value,boolean absorption) {
        float prevFrac=absorption ? absFrac : ringFrac,prevValue=absorption ? absValue : ringValue;
        if(ClientModules.TARGET_PARTICLES.get() && ClientModules.TARGET_FROM_BAR.get()) {
            boolean drop=prevValue>=0 && value<prevValue-.05f,shrink=prevFrac>=0 && frac<prevFrac-.002f;
            if(drop || shrink) {
                float from=prevFrac<0 ? frac : prevFrac,lo=Math.min(frac,from),span=Math.max(.02f,Math.max(frac,from)-lo);
                int n=drop ? count() : Math.max(2,Math.round(count()*.18f));
                var r=ThreadLocalRandom.current();
                double rate=dt>1e-4 ? span/dt*Math.PI*2 : 1.4;
                for(int i=0;i<n;i++) {
                    double at=lo+span*Math.pow(r.nextDouble(),drop ? 1.05 : 2.4);
                    PARTS.add(Particle.orbit(192,32,17.25f+(float)r.nextDouble(-.55,.55),(float)(at*Math.PI*2),
                            (float)(-rate*r.nextDouble(.35,1.05)+r.nextDouble(-.2,.35)),(float)r.nextDouble(-3.5,3.5),
                            absorption ? ABSORPTION : 0,classic()));
                }
            }
        }
        if(absorption) { absFrac=frac; absValue=value; } else { ringFrac=frac; ringValue=value; }
    }

    // ---- "Полоска" -----------------------------------------------------------------------------------

    private static void barCard(GuiGraphicsExtractor g,LivingEntity e,float alpha) {
        float w=Math.max(200,BAR_X+FONT.width(e.getName().getString(),16)+10),max=Math.max(1,e.getMaxHealth());
        float track=w-BAR_X-10,hpW=track*Math.min(barHp/max,1),absW=track*Math.min(absorb/max,1);
        ItemStack use=usedItem(e);
        if(!use.isEmpty()) usedItem=use.copy();
        using=approach(using,use.isEmpty() ? 0 : 1,8);
        hitBurst(e,w,BAR_H,38,38);
        barParticles(hpW,e.getHealth(),false);
        barParticles(absW,e.getAbsorptionAmount(),true);
        float shown=Math.clamp((reveal-.12f)/.88f,0,1);
        Theme.plate(g,0,0,w,BAR_H,10,alpha);
        Theme.plateBand(g,0,0,w,BAR_H,false,76,10,alpha);
        head(g,e,8,8,60,8,alpha);
        useOverlay(g,e,use,alpha);
        int accent=Theme.accent();
        UiRenderer.rounded(g,BAR_X,54,track,8,3,a(Theme.withAlpha(accent,.3f),alpha));
        UiRenderer.rounded(g,BAR_X,54,hpW*shown,8,3,a(accent,alpha));
        if(absW>0) UiRenderer.rounded(g,BAR_X,54,absW*shown,8,3,a(ABSORPTION,alpha));
        FONT.draw(g,e.getName().getString(),BAR_X,12,16,a(Theme.PLATE_TEXT,alpha));
        // The health number rides above the end of the bar with a small arrow under it pointing at the bar.
        String number=String.valueOf((int)e.getHealth());
        float end=BAR_X+hpW*shown-4,nw=FONT.width(number,13);
        FONT.draw(g,number,Math.clamp(end-nw/2,BAR_X+4,Math.max(BAR_X+4,w-10-nw)),27,13,a(Theme.PLATE_TEXT,alpha));
        // Filled triangle pointing down: the play glyph turned a quarter.
        float ax=Math.clamp(end,BAR_X+5,w-15);
        g.pose().pushMatrix();
        try {
            g.pose().translate(ax,48);
            g.pose().rotate((float)(Math.PI/2));
            // The glyph's centre sits at (13.75, 12) of its 24-unit box.
            Icons.draw(g,Icons.Icon.PLAY,-13.75f/24*11,-5.5f,11,a(Theme.PLATE_TEXT,alpha));
        } finally { g.pose().popMatrix(); }
        // Hands and armour stand in a row above the card.
        List<ItemStack> row=new ArrayList<>();
        if(!e.getOffhandItem().isEmpty()) row.add(e.getOffhandItem());
        if(!e.getMainHandItem().isEmpty()) row.add(e.getMainHandItem());
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!e.getItemBySlot(slot).isEmpty()) row.add(e.getItemBySlot(slot));
        for(int i=0;i<row.size();i++) item(g,row.get(i),i*20,-22,20,true,alpha);
        particles(g,alpha);
    }
    /** Eating or drinking: the item and the seconds left cover the head. */
    private static void useOverlay(GuiGraphicsExtractor g,LivingEntity e,ItemStack use,float alpha) {
        float a=using*alpha;
        if(a<=.01f) return;
        UiRenderer.rounded(g,8,8,60,60,8,Theme.withAlpha(Theme.card(),.88f*a));
        ItemStack stack=use.isEmpty() ? usedItem : use;
        if(!stack.isEmpty()) item(g,stack,8+14,8+8,16*1.9f,false,a);
        if(!use.isEmpty()) {
            String left=String.format(Locale.ROOT,"%.1fс",Math.max(0,e.getUseItemRemainingTicks())/20f);
            FONT.draw(g,left,38-FONT.width(left,12)/2,8+43,12,Theme.withAlpha(0xffffffff,a));
        }
    }
    private static ItemStack usedItem(LivingEntity e) {
        if(!e.isUsingItem()) return ItemStack.EMPTY;
        ItemStack stack=e.getUseItem();
        if(stack.isEmpty()) return ItemStack.EMPTY;
        var anim=stack.getUseAnimation();
        return anim==ItemUseAnimation.EAT || anim==ItemUseAnimation.DRINK ? stack : ItemStack.EMPTY;
    }
    /** Bar emitter: particles fly off the shrinking end of the bar (all of them on a real hit). */
    private static void barParticles(float width,float value,boolean absorption) {
        float prevW=absorption ? barAbsWidth : barWidth,prevValue=absorption ? absValue : ringValue;
        if(ClientModules.TARGET_PARTICLES.get() && ClientModules.TARGET_FROM_BAR.get()) {
            boolean drop=prevValue>=0 && value<prevValue-.05f,shrink=prevW>=0 && width<prevW-.35f;
            if(drop || shrink) {
                float lost=Math.max(1.5f,Math.max(width,prevW)-width);
                int n=drop ? count() : Math.max(1,Math.round(count()*.06f));
                var r=ThreadLocalRandom.current();
                int color=absorption ? ABSORPTION : 0;
                for(int i=0;i<n;i++) {
                    float x=BAR_X+width+(float)r.nextDouble(0,lost),y=58+(float)r.nextDouble(-3.5,3.5);
                    if(classic()) PARTS.add(Particle.flyTo(x,y,color));
                    else {
                        double angle=r.nextDouble(-Math.PI*.9,Math.PI*.9),speed=r.nextDouble(20,52);
                        PARTS.add(Particle.free(x,y,(float)(Math.cos(angle)*speed),(float)(Math.sin(angle)*speed-r.nextDouble(6,16)),color));
                    }
                }
            }
        }
        if(absorption) { barAbsWidth=width; absValue=value; } else { barWidth=width; ringValue=value; }
    }

    // ---- shared --------------------------------------------------------------------------------------

    /** A hit bursts particles out of the head; fireflies also keep spawning around the card while it shows. */
    private static void hitBurst(LivingEntity e,float w,float h,float cx,float cy) {
        float hurt=e.hurtTime>0 ? Math.min(.5f,(float)e.hurtTime/Math.max(1,e.hurtDuration)) : 0;
        if(!ClientModules.TARGET_PARTICLES.get()) { hurtBefore=hurt; return; }
        var r=ThreadLocalRandom.current();
        int count=count();
        if(hurt>hurtBefore && ClientModules.TARGET_FROM_HEAD.get()) {
            if(classic()) for(int i=0;i<count;i++) PARTS.add(Particle.flyTo(cx+2,cy+2,0));
            else {
                int ring=Math.max(1,Math.round(count*10f/17f)),rest=Math.max(0,count-ring);
                for(int i=0;i<ring;i++) {
                    double angle=Math.PI*2*i/ring+r.nextDouble(-.3,.3),dist=r.nextDouble(4,20),speed=r.nextDouble(16,34);
                    PARTS.add(Particle.free(cx+(float)(Math.cos(angle)*dist),cy+(float)(Math.sin(angle)*dist),
                            (float)(Math.cos(angle)*speed),(float)(Math.sin(angle)*speed-6),0));
                }
                for(int i=0;i<rest;i++) {
                    double angle=r.nextDouble(Math.PI*2),speed=r.nextDouble(8,22);
                    PARTS.add(Particle.free((float)r.nextDouble(-6,w+6),(float)r.nextDouble(-6,h+6),
                            (float)(Math.cos(angle)*speed),(float)(Math.sin(angle)*speed-5),0));
                }
            }
        }
        hurtBefore=hurt;
        if(classic()) return;
        idle+=dt*2.5f;
        while(idle>=1) {
            idle--;
            if(PARTS.size()>=Math.max(40,count*2)) break;
            double angle=r.nextDouble(Math.PI*2),speed=r.nextDouble(6,16);
            PARTS.add(Particle.free((float)r.nextDouble(-6,w+6),(float)r.nextDouble(-6,h+6),
                    (float)(Math.cos(angle)*speed),(float)(Math.sin(angle)*speed-4),0));
        }
    }
    /** Moves and draws every particle; fireflies are a quadratic halo of discs around a lighter core. */
    private static void particles(GuiGraphicsExtractor g,float alpha) {
        long now=System.currentTimeMillis();
        PARTS.removeIf(p->now-p.born>p.life);
        int accent=Theme.accent();
        for(var p:PARTS) {
            int base=p.color!=0 ? p.color : accent;
            float t=(float)(now-p.born)/p.life;
            if(p.classic && !p.orbit) {
                p.appear+=(1-p.appear)*(1-(float)Math.exp(-dt*10));
                float k=1-(float)Math.exp(-dt*8);
                p.x+=(p.tx-p.x)*k; p.y+=(p.ty-p.y)*k;
                UiRenderer.rounded(g,p.x-6,p.y-6,8,8,3,Theme.withAlpha(base,p.appear*(1-t)*alpha));
                continue;
            }
            p.step(dt,now);
            if(p.classic) { UiRenderer.rounded(g,p.x-6,p.y-6,8,8,3,Theme.withAlpha(base,(1-t)*alpha)); continue; }
            float in=Math.min(t/.15f,1),out=1-Math.max(0,(t-.6f)/.4f);
            float fade=in*in*(3-2*in)*out,flicker=.8f+.2f*(float)Math.sin((now-p.born)*.01f+p.seed);
            float radius=p.size*(.75f+.25f*out),a=alpha*fade*flicker,halo=radius*2.2f;
            for(int i=(int)halo;i>0;i--) {
                float k=1-i/halo,glow=a*.11f*k*k;
                if(glow>.004f) disc(g,p.x,p.y,radius+i,Theme.withAlpha(base,glow));
            }
            disc(g,p.x,p.y,radius,Theme.withAlpha(Theme.mix(base,0xffffffff,p.tint*.5f),a*.7f));
        }
    }
    /**
     * The reference's ring: a size x size box, centre line at size/2 - stroke/2 - 0.35 and a stroke in card
     * units. The icon arc draws its centre line at 9/24 of its box with the stroke in 1/24ths, so it is
     * redrawn in a box that puts the same circle in the same place.
     */
    private static void ring(GuiGraphicsExtractor g,float x,float y,float size,float stroke,float progress,int color) {
        float radius=size/2-stroke/2-.35f,box=radius*24/9,c=size/2;
        Icons.arc(g,x+c-box/2,y+c-box/2,box,stroke*24/box,progress,color);
    }
    private static void disc(GuiGraphicsExtractor g,float x,float y,float radius,int color) {
        float box=radius*2/.875f;
        Icons.draw(g,Icons.Icon.DOT,x-box/2,y-box/2,box,color);
    }
    private static void head(GuiGraphicsExtractor g,LivingEntity e,float x,float y,float size,float radius,float alpha) {
        if(e instanceof AbstractClientPlayer player) Avatar.draw(g,player.getSkin(),x,y,size,radius,alpha);
        else {
            UiRenderer.rounded(g,x,y,size,size,radius,Theme.withAlpha(Theme.field(),.9f*alpha));
            float icon=size*.45f;
            Icons.draw(g,e instanceof net.minecraft.world.entity.monster.Enemy ? Icons.Icon.SWORD : Icons.Icon.USER,
                    x+size/2-icon/2,y+size/2-icon/2,icon,1.8f,a(Theme.secondary(),alpha));
        }
    }
    private static void item(GuiGraphicsExtractor g,ItemStack stack,float x,float y,float size,boolean details,float alpha) {
        if(alpha<.05f) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(x,y);
            g.pose().scale(size/16,size/16);
            g.item(stack,0,0);
            if(details) g.itemDecorations(Minecraft.getInstance().font,stack,0,0);
        } finally { g.pose().popMatrix(); }
    }
    private static String fit(String s,float width,float size) {
        if(FONT.width(s,size)<=width) return s;
        while(s.length()>1 && FONT.width(s+"…",size)>width) s=s.substring(0,s.length()-1);
        return s+"…";
    }
    private static int a(int argb,float alpha) { return Theme.withAlpha(argb,((argb>>>24)/255f)*alpha); }
    private static float approach(float v,float target,float speed) { return v+(target-v)*(1-(float)Math.exp(-dt*speed)); }

    /** One particle in card units; lifetimes are wall-clock, as in the reference. */
    private static final class Particle {
        final long born=System.currentTimeMillis();
        long life; float size,seed,tint,x,y,vx,vy; int color; boolean classic,orbit;
        float cx,cy,radius,rest,angle,spin,push,tx,ty,appear;

        static Particle free(float x,float y,float vx,float vy,int color) {
            var p=new Particle(); var r=ThreadLocalRandom.current();
            p.x=x; p.y=y; p.vx=vx; p.vy=vy; p.color=color;
            p.life=900+r.nextLong(700); p.size=1.8f+r.nextFloat()*1.8f;
            return p;
        }
        static Particle orbit(float cx,float cy,float radius,float angle,float spin,float push,int color,boolean classic) {
            var p=new Particle(); var r=ThreadLocalRandom.current();
            p.orbit=true; p.classic=classic; p.color=color;
            p.cx=cx; p.cy=cy; p.radius=p.rest=radius; p.angle=angle; p.spin=spin; p.push=push;
            p.x=cx+(float)Math.sin(angle)*radius; p.y=cy-(float)Math.cos(angle)*radius;
            if(classic) { p.life=1250+r.nextLong(750); p.size=4; }
            else { p.life=900+r.nextLong(700); p.size=1.8f+r.nextFloat()*1.8f; }
            return p;
        }
        /** "Классика": a square that eases from its start towards a random point up to 75 units away. */
        static Particle flyTo(float x,float y,int color) {
            var p=new Particle(); var r=ThreadLocalRandom.current();
            p.classic=true; p.color=color; p.x=x; p.y=y;
            p.tx=x-(float)r.nextDouble(-75,75); p.ty=y-(float)r.nextDouble(-75,75);
            p.life=1250+r.nextLong(750); p.size=4;
            return p;
        }
        Particle() { var r=ThreadLocalRandom.current(); seed=r.nextFloat()*10; tint=r.nextFloat(); }

        void step(float dt,long now) {
            if(orbit) {
                angle+=spin*dt; radius+=push*dt;
                radius+=(rest-radius)*Math.min(1,dt*10);
                spin*=(float)Math.exp(-dt*.7); push*=(float)Math.exp(-dt*3.2);
                float wobble=(float)Math.sin(now*.004+seed*4)*.4f;
                x=cx+(float)Math.sin(angle)*(radius+wobble);
                y=cy-(float)Math.cos(angle)*(radius+wobble);
            } else {
                vx*=(float)Math.exp(-dt*.55); vy*=(float)Math.exp(-dt*.55);
                vy+=18*dt;
                x+=vx*dt+(float)Math.sin(now*.002+seed*4)*6*dt;
                y+=vy*dt+(float)Math.cos(now*.0017+seed*3)*4*dt;
            }
        }
    }
}
