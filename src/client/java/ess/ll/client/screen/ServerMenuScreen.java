package ess.ll.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.command.Keys;
import ess.ll.client.module.misc.ServerMenu;
import ess.ll.client.module.misc.ServerMenu.Entry;
import ess.ll.client.render.*;
import ess.ll.client.setting.Settings.BooleanSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The ServerMenu window, in the ClickGUI's own sizes and theme: a server switch, tabs for items, potions and
 * other settings, and a card per item with its icon, bind and switch. A click on a bind waits for a key (Esc
 * cancels, Backspace clears); a right click on a card switches the item off.
 * The window opens with a short rise, the selection of server and tab glides to its place, and the cards come
 * in one after another whenever the server or tab changes.
 */
public final class ServerMenuScreen extends Screen {
    private static final float W=470,H=320,PAD=12,BODY_TOP=70,COLS=4,GAP=6,CARD_H=58;
    private static final double OPEN_SECONDS=.24,CARD_SECONDS=.26,STAGGER=.035;
    private static final int[] SERVER_COLORS={0xffff9f43,0xffb884ff,0xffff5f7a};
    private record Hit(float x,float y,float w,float h,Runnable left,Runnable right) {}
    private final Screen parent;
    private List<Hit> hits=new ArrayList<>(),drawing;
    private final Map<Object,Float> anims=new HashMap<>();
    private String tab="items";
    /** The bind waiting for a key, or null. */
    private Entry waiting;
    private float scale=1,left,top,mx,my,dt,alpha=1;
    private long lastFrame=System.nanoTime(),opened=System.nanoTime(),switched=System.nanoTime();

    public ServerMenuScreen(Screen parent) {
        super(Component.literal("ServerMenu"));
        this.parent=parent;
    }

    @Override public boolean isPauseScreen() { return false; }
    /** Dev check: shows a server and a tab. */
    public void showForTest(String server,String tab) {
        ServerMenu.SERVER.select(ServerMenu.SERVERS.indexOf(server));
        this.tab=tab;
        switched=System.nanoTime();
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        if(minecraft.level!=null) extractBlurredBackground(g);
    }

    private float anim(Object key,float target,float speed) {
        Float stored=anims.get(key);
        float v=stored==null ? target : stored;
        v+=(target-v)*(float)(1-Math.exp(-dt*speed*Theme.speed()));
        if(Math.abs(target-v)<.001f) v=target;
        anims.put(key,v);
        return v;
    }
    private static float ease(double t) { t=Math.clamp(t,0,1); return (float)(1-Math.pow(1-t,3)); }
    private boolean over(float x,float y,float w,float h) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    private void hit(float x,float y,float w,float h,Runnable left,Runnable right) { drawing.add(new Hit(x,y,w,h,left,right)); }
    private static int mix(int a,int b,float t) { return Theme.mix(a,b,t); }
    /** A colour with the current fade applied. */
    private int c(int argb) { return Theme.withAlpha(argb,((argb>>>24)/255f)*alpha); }
    private void select(String newTab) {
        if(tab.equals(newTab)) return;
        tab=newTab; waiting=null; switched=System.nanoTime();
        UiSounds.select();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        long now=System.nanoTime(); dt=(float)Math.min(.1,(now-lastFrame)/1e9); lastFrame=now;
        scale=Math.max(.1f,Math.min(Math.min((width-24)/W,(height-24)/H),1f));
        left=(width-W*scale)/2; top=(height-H*scale)/2;
        mx=(mouseX-left)/scale; my=(mouseY-top)/scale;
        drawing=new ArrayList<>();
        float open=ease((now-opened)/1e9*Theme.speed()/OPEN_SECONDS);
        alpha=open;
        g.pose().pushMatrix();
        try {
            // Rises a little and grows from 96% as it opens.
            float grow=.96f+.04f*open;
            g.pose().translate(left+W*scale/2,top+H*scale/2+(1-open)*10);
            g.pose().scale(scale*grow,scale*grow);
            g.pose().translate(-W/2,-H/2);
            Theme.panel(g,0,0,W,H,Theme.radius()+8,open);
            drawHeader(g);
            drawTabs(g);
            double since=(now-switched)/1e9*Theme.speed();
            if(tab.equals("other")) drawOther(g,since); else drawItems(g,since);
        } finally { g.pose().popMatrix(); }
        hits=drawing;
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        var font=MsdfFont.UI;
        UiRenderer.rounded(g,14,12,22,22,Math.max(4,Theme.radius()-3),c(Theme.accent()));
        Icons.draw(g,Icons.Icon.MARK,18,16,14,c(Theme.onAccent()));
        font.mid(g,"ServerMenu",42,19,11,c(Theme.text()));
        font.mid(g,"Предметы сервера по бинду · "+ServerMenu.SERVER.get(),42,29,6.5f,c(Theme.hint()));
        // Server switch: one selection plate that glides to the chosen server.
        float[] widths=new float[3];
        float total=0;
        for(int i=0;i<3;i++) { widths[i]=font.width(ServerMenu.SERVERS.get(i),7.5f)+20; total+=widths[i]; }
        float sx=W-12-17-6-total-6,sy=13;
        UiRenderer.rounded(g,sx,sy,total+6,19,7,c(Theme.field()));
        int chosen=Math.max(0,ServerMenu.SERVERS.indexOf(ServerMenu.SERVER.get()));
        float cx=sx+3,targetX=cx;
        for(int i=0;i<chosen;i++) targetX+=widths[i];
        float selX=anim("server-x",targetX,18),selW=anim("server-w",widths[chosen],18);
        UiRenderer.rounded(g,selX,sy+2,selW,15,5,c(mix(Theme.field(),Theme.text(),.14f)));
        for(int i=0;i<3;i++) {
            String name=ServerMenu.SERVERS.get(i);
            float hover=anim(List.of("server",i),over(cx,sy+2,widths[i],15)?1:0,16),sel=anim(List.of("server-on",i),i==chosen?1:0,16);
            if(hover*(1-sel)>.01f) UiRenderer.rounded(g,cx,sy+2,widths[i],15,5,c(Theme.withAlpha(Theme.text(),.05f*hover*(1-sel))));
            float dot=4+1.2f*sel;
            UiRenderer.rounded(g,cx+9-dot/2,sy+9.5f-dot/2,dot,dot,dot/2,c(SERVER_COLORS[i]));
            font.mid(g,name,cx+14,sy+10.5f,7.5f,c(mix(Theme.secondary(),Theme.text(),Math.max(sel,hover))));
            final int index=i;
            hit(cx,sy+2,widths[i],15,()->{
                if(ServerMenu.SERVER.get().equals(name)) return;
                ServerMenu.SERVER.select(index); waiting=null; switched=System.nanoTime();
                if(tab.equals("potions") && ServerMenu.of(name,true).isEmpty()) tab="items";
                UiSounds.select();
            },null);
            cx+=widths[i];
        }
        // Close.
        float x=W-12-17,y=14;
        float hover=anim("close",over(x,y,17,17)?1:0,16);
        UiRenderer.rounded(g,x,y,17,17,6,c(mix(Theme.field(),0xffff6b5e,.3f*hover)));
        g.pose().pushMatrix();
        try {
            // The cross turns a quarter as the cursor comes over it.
            g.pose().translate(x+8.5f,y+8.5f);
            g.pose().rotate((float)(Math.PI/2*hover));
            Icons.draw(g,Icons.Icon.CLOSE,-4,-4,8,c(mix(Theme.hint(),Theme.text(),hover)));
        } finally { g.pose().popMatrix(); }
        hit(x,y,17,17,this::onClose,null);
    }

    private void drawTabs(GuiGraphicsExtractor g) {
        var font=MsdfFont.UI;
        String server=ServerMenu.SERVER.get();
        int items=ServerMenu.of(server,false).size(),potions=ServerMenu.of(server,true).size();
        Object[][] tabs={{"items","Предметы",Icons.Icon.CUBE,items},{"potions","Зелья",Icons.Icon.FLASK,potions},{"other","Другое",Icons.Icon.GRID,-1}};
        float[] xs=new float[tabs.length],ws=new float[tabs.length];
        float x=PAD,y=44;
        for(int i=0;i<tabs.length;i++) {
            String label=(String)tabs[i][1];
            int count=(int)tabs[i][3];
            xs[i]=x;
            ws[i]=8+10+4+font.width(label,8)+(count>=0 ? 6+font.width(String.valueOf(count),6.5f)+8 : 0)+8;
            x+=ws[i]+4;
        }
        int chosen=0;
        for(int i=0;i<tabs.length;i++) if(tabs[i][0].equals(tab)) chosen=i;
        // One highlight that glides under the chosen tab.
        float hx=anim("tab-x",xs[chosen],18),hw=anim("tab-w",ws[chosen],18);
        UiRenderer.rounded(g,hx,y,hw,20,7,c(Theme.withAlpha(Theme.accent(),.14f)));
        for(int i=0;i<tabs.length;i++) {
            String id=(String)tabs[i][0],label=(String)tabs[i][1];
            int count=(int)tabs[i][3];
            boolean disabled=count==0,on=i==chosen;
            float hover=anim(List.of("tab",id),!disabled && over(xs[i],y,ws[i],20)?1:0,16),sel=anim(List.of("tab-on",id),on?1:0,16);
            int fg=disabled ? Theme.withAlpha(Theme.hint(),.4f) : mix(mix(Theme.hint(),Theme.secondary(),hover),Theme.accent(),sel);
            Icons.draw(g,(Icons.Icon)tabs[i][2],xs[i]+8,y+5-hover,10,2.4f,c(fg));
            font.mid(g,label,xs[i]+22,y+10.5f,8,c(fg));
            if(count>=0) {
                String badge=String.valueOf(count);
                float bx=xs[i]+22+font.width(label,8)+6,bw=font.width(badge,6.5f)+8;
                UiRenderer.rounded(g,bx,y+5,bw,10,4,c(mix(Theme.field(),Theme.withAlpha(Theme.accent(),.22f),sel)));
                font.midCenter(g,badge,bx+bw/2,y+10,6.5f,c(mix(Theme.hint(),Theme.accent(),sel)));
            }
            if(!disabled) hit(xs[i],y,ws[i],20,()->select(id),null);
        }
        String hint;
        if(tab.equals("other")) hint="";
        else if(waiting!=null) hint="Нажми клавишу · Esc отмена · Backspace снять";
        else {
            var list=ServerMenu.of(server,tab.equals("potions"));
            long bound=list.stream().filter(e->e.bind.get()>=0).count();
            hint=bound+" из "+list.size()+" на биндах · ПКМ по карточке — выкл";
        }
        font.mid(g,hint,W-PAD-font.width(hint,6.5f),54,6.5f,c(waiting!=null ? Theme.accent() : Theme.hint()));
    }

    private void drawItems(GuiGraphicsExtractor g,double since) {
        var font=MsdfFont.UI;
        var list=ServerMenu.of(ServerMenu.SERVER.get(),tab.equals("potions"));
        float cw=(W-PAD*2-GAP*(COLS-1))/COLS;
        float saved=alpha;
        for(int i=0;i<list.size();i++) {
            Entry e=list.get(i);
            float appear=ease((since-i*STAGGER)/CARD_SECONDS);
            if(appear<=0) continue;
            float x=PAD+(i%(int)COLS)*(cw+GAP),baseY=BODY_TOP+(i/(int)COLS)*(CARD_H+GAP);
            boolean on=e.on.get();
            float hover=anim(e,over(x,baseY,cw,CARD_H)?1:0,14),off=anim(List.of(e,"off"),on?0:1,14);
            float y=baseY+(1-appear)*10-hover*1.5f;
            alpha=saved*appear;
            int edge=mix(Theme.border(),Theme.withAlpha(Theme.accent(),.55f),hover);
            UiRenderer.rounded(g,x,y,cw,CARD_H,Theme.radius(),c(Theme.plate(Theme.card())));
            UiRenderer.outline(g,x,y,cw,CARD_H,Theme.radius(),.75f,c(edge));
            float dim=1-.5f*off;
            // Icon: the real item, the potions in their own colours; it grows a little under the cursor.
            UiRenderer.rounded(g,x+8,y+8,26,26,7,c(Theme.withAlpha(mix(Theme.field(),Theme.accent(),.14f*hover),dim)));
            if(appear>.5f) {
                float size=20*(1+.08f*hover);
                g.pose().pushMatrix();
                try {
                    g.pose().translate(x+21-size/2,y+21-size/2);
                    g.pose().scale(size/16,size/16);
                    g.item(e.icon(),0,0);
                } finally { g.pose().popMatrix(); }
            }
            float tx=x+40,room=cw-40-8;
            font.mid(g,fit(e.name,7.5f,room),tx,y+16,7.5f,c(Theme.withAlpha(Theme.text(),dim)));
            font.mid(g,fit(e.source,6.5f,room),tx,y+25.5f,6.5f,c(Theme.withAlpha(Theme.hint(),dim)));
            float by=y+CARD_H-8-13;
            // Bind pill; while it waits for a key it pulses.
            boolean wait=waiting==e;
            String key=wait ? "..." : e.bind.get()<0 ? "—" : Keys.name(e.bind.get());
            float bw=Math.max(30,font.width(key,6.5f)+20);
            float bh=anim(List.of(e,"bind"),over(x+8,by,bw,13)?1:0,16);
            float pulse=wait ? (float)(.5+.5*Math.sin(System.nanoTime()/1e9*7)) : 0;
            int bfill=wait ? Theme.withAlpha(Theme.accent(),.7f+.3f*pulse) : e.bind.get()>=0 ? Theme.withAlpha(Theme.accent(),.14f+.08f*bh) : mix(Theme.field(),Theme.text(),.06f*bh);
            UiRenderer.rounded(g,x+8,by,bw,13,5,c(bfill));
            int bfg=wait ? Theme.onAccent() : e.bind.get()>=0 ? Theme.accent() : mix(Theme.secondary(),Theme.text(),bh);
            Icons.draw(g,Icons.Icon.KEYBOARD,x+13,by+3.5f,6,2.4f,c(bfg));
            font.mid(g,key,x+22,by+6.5f,6.5f,c(bfg));
            hit(x+8,by,bw,13,()->{ waiting=waiting==e ? null : e; UiSounds.select(); },null);
            // Count in the inventory, between the bind and the switch.
            if(ServerMenu.COUNT.get() && minecraft.player!=null) {
                String count=e.count()+" шт";
                float cwid=font.width(count,6)+8,cx=x+cw-8-18-5-cwid;
                if(cx>x+8+bw+4) {
                    UiRenderer.rounded(g,cx,by+1.5f,cwid,10,4,c(Theme.field()));
                    font.midCenter(g,count,cx+cwid/2,by+6.5f,6,c(e.count()>0 ? Theme.secondary() : Theme.hint()));
                }
            }
            toggle(g,List.of(e,"switch"),x+cw-8-18,by+1.5f,18,10,on);
            hit(x+cw-8-22,by-2,26,17,()->{ e.on.toggle(); UiSounds.toggle(e.on.get()); },null);
            hit(x,y,cw,CARD_H,null,()->{ e.on.toggle(); UiSounds.toggle(e.on.get()); });
        }
        alpha=saved;
    }

    private void drawOther(GuiGraphicsExtractor g,double since) {
        var font=MsdfFont.UI;
        float cw=(W-PAD*2-8)/2;
        Object[][] cards={{"Вид",List.of(ServerMenu.COUNT)},{"Уведомления",List.of(ServerMenu.NOTIFY,ServerMenu.MISSING)}};
        int rowsMax=0;
        for(Object[] card:cards) rowsMax=Math.max(rowsMax,((List<?>)card[1]).size());
        float h=9*2+16+rowsMax*17;
        float saved=alpha;
        for(int c=0;c<cards.length;c++) {
            @SuppressWarnings("unchecked") List<BooleanSetting> rows=(List<BooleanSetting>)cards[c][1];
            float appear=ease((since-c*STAGGER*2)/CARD_SECONDS);
            if(appear<=0) continue;
            alpha=saved*appear;
            float x=PAD+c*(cw+8),y=BODY_TOP+(1-appear)*10;
            UiRenderer.rounded(g,x,y,cw,h,Theme.radius(),c(Theme.plate(Theme.card())));
            UiRenderer.outline(g,x,y,cw,h,Theme.radius(),.75f,c(Theme.border()));
            font.mid(g,(String)cards[c][0],x+9,y+9+8,9,c(Theme.text()));
            float ry=y+9+16;
            for(var row:rows) {
                float hover=anim(row,over(x+9,ry,cw-18,17)?1:0,16);
                font.mid(g,row.name(),x+9+hover,ry+8.5f,7.5f,c(mix(Theme.secondary(),Theme.text(),hover)));
                toggle(g,row,x+cw-9-18,ry+3.5f,18,10,row.get());
                hit(x+9,ry,cw-18,17,()->{ row.toggle(); UiSounds.toggle(row.get()); },null);
                ry+=17;
            }
        }
        alpha=saved*ease((since-.1)/CARD_SECONDS);
        font.mid(g,"Бинды работают для выбранного сервера, когда модуль ServerMenu включён",PAD,BODY_TOP+h+14,6.5f,c(Theme.hint()));
        alpha=saved;
    }

    /** Switch whose knob slides and stretches a little on the way, as in the ClickGUI. */
    private void toggle(GuiGraphicsExtractor g,Object key,float x,float y,float w,float h,boolean on) {
        float v=anim(List.of(key,"v"),on?1:0,14);
        UiRenderer.rounded(g,x,y,w,h,h/2,c(mix(Theme.field(),Theme.accent(),v)));
        float d=h-4,stretch=2.5f*(float)Math.sin(Math.PI*v);
        UiRenderer.rounded(g,x+2+(w-4-d)*v-stretch*v,y+2,d+stretch,d,d/2,c(mix(Theme.hint(),Theme.onAccent(),v)));
    }
    private static String fit(String text,float size,float width) {
        var font=MsdfFont.UI;
        if(font.width(text,size)<=width) return text;
        while(text.length()>1 && font.width(text+"...",size)>width) text=text.substring(0,text.length()-1);
        return text+"...";
    }

    // ---------------------------------------------------------------- input

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        int button=event.button();
        if(waiting!=null && button!=InputConstants.MOUSE_BUTTON_LEFT && button!=InputConstants.MOUSE_BUTTON_RIGHT
                && button<=Keys.MOUSE_BUTTONS) {
            waiting.bind.set(Keys.MOUSE_BASE+button); waiting=null; UiSounds.bindSet(); return true;
        }
        float k=scale;
        float x=(float)((event.x()-left)/k),y=(float)((event.y()-top)/k);
        for(int i=hits.size()-1;i>=0;i--) {
            Hit h=hits.get(i);
            if(x<h.x() || x>=h.x()+h.w() || y<h.y() || y>=h.y()+h.h()) continue;
            Runnable action=button==InputConstants.MOUSE_BUTTON_LEFT ? h.left() : button==InputConstants.MOUSE_BUTTON_RIGHT ? h.right() : null;
            if(action==null) continue;
            action.run();
            return true;
        }
        if(button==InputConstants.MOUSE_BUTTON_LEFT) waiting=null;
        return x>=0 && x<W && y>=0 && y<H || super.mouseClicked(event,doubleClick);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int key=event.key();
        if(waiting!=null) {
            if(key==InputConstants.KEY_ESCAPE) waiting=null;
            else if(key==InputConstants.KEY_BACKSPACE || key==InputConstants.KEY_DELETE) { waiting.bind.set(-1); waiting=null; }
            else if(Keys.isValid(key)) { waiting.bind.set(key); waiting=null; UiSounds.bindSet(); }
            return true;
        }
        return super.keyPressed(event);
    }
}
