package ess.ll.client.render;

import ess.ll.client.features.social.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityESP name plates. "В строку": head, name, HP and gear in one dark row with a thin HP line under it.
 * "Пилюля": round head, name and an HP badge, with the gear in its own row above. Sizes are GUI pixels.
 */
public final class EspPlates {
    private EspPlates() {}
    private static final int BACKGROUND=0xe0101318, FRIEND_BACKGROUND=0xe0103d24, SLOT=0x12ffffff, TEXT=0xffffffff, FRIEND=0xff4fd18b;
    /** EntityESP's "Блюр фона" and "Свечение фона", set before each frame's plates. */
    public static boolean blur, glow;

    /**
     * Every ESP background goes through here: frosted glass with blur on, a soft halo with glow on (green for
     * friends, the accent otherwise), and a green fill for friends.
     */
    public static void background(GuiGraphicsExtractor g,float x,float y,float w,float h,float radius,int color,boolean friend) {
        if(friend && color==BACKGROUND) color=FRIEND_BACKGROUND;
        if(glow) UiRenderer.shadow(g,x,y,w,h,radius,6,Theme.withAlpha(friend ? FRIEND : Theme.accent(),.4f*((color>>>24)/255f)));
        if(blur) GlassRenderer.glass(g,x,y,w,h,radius,Theme.withAlpha(color,.6f*((color>>>24)/255f)),1);
        else UiRenderer.rounded(g,x,y,w,h,radius,color);
    }
    private static final float H=11, PAD=2.5f, HEAD=8, ITEM=9, NAME=6.7f;

    /** @param cx horizontal centre of the entity box, top its top edge on screen */
    public static void draw(GuiGraphicsExtractor g,MsdfFont font,Entity e,boolean pill,boolean gear,float cx,float top) {
        Component name=name(e);
        LivingEntity living=e instanceof LivingEntity l ? l : null;
        boolean friend=e instanceof Player p && FriendManager.INSTANCE.isFriend(p);
        List<ItemStack> items=gear && e instanceof Player p ? gear(p) : List.of();
        if(pill) pill(g,font,e,living,name,friend,items,cx,top);
        else row(g,font,e,living,name,friend,items,cx,top);
    }
    /** The name with the server's own prefix (donate rank glyphs, colours); items carry their count. */
    public static Component name(Entity e) {
        if(e instanceof LivingEntity living) return ess.ll.client.util.PlayerUtils.displayName(living);
        if(e instanceof ItemEntity item) {
            Component n=item.getItem().getStyledHoverName();
            if(n.getString().length()>32) n=item.getItem().getItemName();
            return item.getItem().getCount()>1 ? Component.empty().append(n).append(" ["+item.getItem().getCount()+"]") : n;
        }
        return e.getDisplayName();
    }

    /*
     * Names are drawn in the ESP font with the colours the server gave them: component styles and the legacy §
     * codes some servers leave inside team prefixes are both read. Letters the font lacks (rank icons servers
     * put into their resource pack, symbols) fall back to the vanilla font, so donate badges still show.
     * A friend's name is green.
     */
    private record Run(String text,int color,boolean vanilla,Style style) {}
    private static final float GLYPH=7f;
    private static List<Run> runs(Component name) {
        List<Run> runs=new ArrayList<>();
        StringBuilder sb=new StringBuilder();
        int[] color={TEXT};
        boolean[] vanilla={false};
        Style[] style={Style.EMPTY};
        StringDecomposer.iterateFormatted(name,Style.EMPTY,(index,st,cp)->{
            int c=st.getColor()==null ? TEXT : 0xff000000|st.getColor().getValue();
            boolean v=!st.getFont().equals(FontDescription.DEFAULT) || !MsdfFont.ESP.has(cp) && cp!=' ';
            if(sb.length()>0 && (c!=color[0] || v!=vanilla[0] || v && !st.equals(style[0]))) {
                runs.add(new Run(sb.toString(),color[0],vanilla[0],style[0])); sb.setLength(0);
            }
            color[0]=c; vanilla[0]=v; style[0]=st;
            sb.appendCodePoint(cp);
            return true;
        });
        if(sb.length()>0) runs.add(new Run(sb.toString(),color[0],vanilla[0],style[0]));
        return runs;
    }
    public static float nameWidth(Component name,float size) {
        var vanilla=Minecraft.getInstance().font;
        float w=0;
        for(Run r:runs(name)) w+=r.vanilla ? vanilla.width(Component.literal(r.text).withStyle(r.style))*size/GLYPH : MsdfFont.ESP.width(r.text,size);
        return w;
    }
    public static void drawName(GuiGraphicsExtractor g,Component name,float x,float midY,float size,boolean friend) {
        var vanilla=Minecraft.getInstance().font;
        float px=x,k=size/GLYPH;
        for(Run r:runs(name)) {
            if(r.vanilla) {
                Component part=Component.literal(r.text).withStyle(r.style);
                g.pose().pushMatrix();
                try {
                    g.pose().translate(px,midY-3.5f*k);
                    g.pose().scale(k,k);
                    g.text(vanilla,part,0,0,0xffffffff,false);
                } finally { g.pose().popMatrix(); }
                px+=vanilla.width(part)*k;
            } else {
                MsdfFont.ESP.mid(g,r.text,px,midY,size,friend ? FRIEND : r.color);
                px+=MsdfFont.ESP.width(r.text,size);
            }
        }
    }

    /** Armour from helmet to boots, then the main hand. */
    private static List<ItemStack> gear(Player p) {
        List<ItemStack> list=new ArrayList<>();
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND})
            list.add(p.getItemBySlot(slot));
        if(list.stream().allMatch(ItemStack::isEmpty)) return List.of();
        return list;
    }
    private static float ratio(LivingEntity l) { return Math.clamp(l.getHealth()/Math.max(1,l.getMaxHealth()),0,1); }
    private static int healthColor(float r) { return r>.5f ? Theme.accent() : r>.25f ? 0xffe89a2e : 0xffe0454f; }
    private static String hp(LivingEntity l) { return String.valueOf((int)Math.ceil(l.getHealth())); }

    // ---- one row ----------------------------------------------------------------------------
    private static void row(GuiGraphicsExtractor g,MsdfFont font,Entity e,LivingEntity living,Component name,boolean friend,List<ItemStack> items,float cx,float top) {
        boolean head=e instanceof AbstractClientPlayer;
        String hp=living==null ? null : hp(living);
        float w=PAD+(head ? HEAD+3 : 1.5f)+nameWidth(name,NAME)+(hp==null ? 0 : 3+font.width(hp,6.5f))
                +(items.isEmpty() ? 0 : 3+1+3+items.size()*ITEM+(items.size()-1))+PAD+1.5f;
        float x=cx-w/2, y=top-H-3;
        background(g,x,y,w,H,4,BACKGROUND,friend);
        float px=x+PAD;
        if(head) { Avatar.draw(g,((AbstractClientPlayer)e).getSkin(),px,y+(H-HEAD)/2,HEAD,2,1); px+=HEAD+3; }
        else px+=1.5f;
        drawName(g,name,px,y+H/2,NAME,friend);
        px+=nameWidth(name,NAME);
        if(hp!=null) {
            int color=healthColor(ratio(living));
            px+=3;
            font.mid(g,hp,px,y+H/2,6.5f,color);
            px+=font.width(hp,6.5f);
        }
        if(!items.isEmpty()) {
            px+=3;
            UiRenderer.rect(g,px,y+3,.6f,H-6,0x1fffffff);
            px+=1+3;
            for(ItemStack stack:items) { slot(g,stack,px,y+(H-ITEM)/2,SLOT,false); px+=ITEM+1; }
        }
    }

    // ---- pill with gear above ------------------------------------------------------------------
    private static void pill(GuiGraphicsExtractor g,MsdfFont font,Entity e,LivingEntity living,Component name,boolean friend,List<ItemStack> items,float cx,float top) {
        boolean head=e instanceof AbstractClientPlayer;
        float h=10, head8=h-2;
        String hp=living==null ? null : hp(living);
        float badge=hp==null ? 0 : 3+4.5f+1.5f+font.width(hp,6.2f)+3.5f;
        float w=1+(head ? head8+3 : 3)+nameWidth(name,NAME)+3+badge+(hp==null ? 1 : 1);
        float x=cx-w/2, y=top-h-3;
        background(g,x,y,w,h,h/2,BACKGROUND,friend);
        float px=x+1;
        if(head) { Avatar.draw(g,((AbstractClientPlayer)e).getSkin(),px,y+1,head8,head8/2,1); px+=head8+3; }
        else px+=3;
        drawName(g,name,px,y+h/2,NAME,friend);
        px+=nameWidth(name,NAME)+3;
        if(hp!=null) {
            int color=healthColor(ratio(living));
            float bw=badge-3, bh=h-2, bx=x+w-1-bw, by=y+1;
            UiRenderer.shadow(g,bx,by,bw,bh,bh/2,3,Theme.withAlpha(color,.5f));
            UiRenderer.rounded(g,bx,by,bw,bh,bh/2,color);
            // Heart and number centred together in the badge; digits are centred on their cap height.
            float content=4.5f+1.5f+font.width(hp,6.2f), sx=bx+(bw-content)/2, cy=by+bh/2;
            Icons.draw(g,Icons.Icon.HEART_FILLED,sx,cy-2.25f,4.5f,TEXT);
            font.mid(g,hp,sx+4.5f+1.5f,cy,6.2f,TEXT);
        }
        if(!items.isEmpty()) {
            float rw=items.size()*ITEM+(items.size()-1)*1.5f, ix=cx-rw/2, iy=y-ITEM-2;
            for(ItemStack stack:items) { slot(g,stack,ix,iy,friend ? 0xcc103d24 : 0xcc101318,true); ix+=ITEM+1.5f; }
        }
    }

    /** Item tile with a durability line under damaged gear. */
    /** Tiles inside a plate (own=false) sit on its background already, so only standalone ones get blur and glow. */
    private static void slot(GuiGraphicsExtractor g,ItemStack stack,float x,float y,int tile,boolean own) {
        if(own) background(g,x,y,ITEM,ITEM,2.4f,tile,false);
        else UiRenderer.rounded(g,x,y,ITEM,ITEM,2.4f,tile);
        if(stack.isEmpty()) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(x+ITEM/2,y+ITEM/2-.3f);
            g.pose().scale(7.4f/16,7.4f/16);
            g.item(stack,-8,-8);
        } finally { g.pose().popMatrix(); }
        if(stack.isDamageableItem() && stack.isDamaged()) {
            float left=1-stack.getDamageValue()/(float)Math.max(1,stack.getMaxDamage());
            int color=left>.5f ? 0xff5cd06f : left>.25f ? 0xffffc36b : 0xffff5f6d;
            UiRenderer.rect(g,x+1.2f,y+ITEM-1.4f,(ITEM-2.4f)*left,.8f,color);
        }
    }
}
