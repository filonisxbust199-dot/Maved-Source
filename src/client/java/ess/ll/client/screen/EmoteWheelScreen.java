package ess.ll.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.command.Keys;
import ess.ll.client.features.emote.Emotes;
import ess.ll.client.module.misc.EmotesModule;
import ess.ll.client.render.GlassRenderer;
import ess.ll.client.render.Icons;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * The emote wheel (from Zenith): eight slots around a page counter, opened while the wheel key is held. Releasing
 * the key over a slot plays it; left click plays a slot or, on an empty one, opens the picker; right click empties
 * a slot or, off the wheel, stops the emote; the wheel and the arrows turn the 15 pages.
 */
public final class EmoteWheelScreen extends Screen {
    private static final float INNER=54,OUTER=104;
    /** The page stays where it was left between openings. */
    private static int page;
    private final long openedAt=System.nanoTime();
    private final float[] hover=new float[Emotes.PER_PAGE];
    private int hovered=-1;
    private long lastFrame=System.nanoTime();

    public EmoteWheelScreen() { super(Component.literal("Эмоции")); }

    @Override public boolean isPauseScreen() { return false; }

    private float scale() { return Math.min(1,Math.min(width,height)/270f); }
    private float cx() { return width/2f; }
    private float cy() { return height/2f; }

    @Override public void tick() {
        // Letting go of the wheel key plays what is under the cursor.
        int key=EmotesModule.WHEEL.get();
        if(key>=0 && !Keys.isDown(key)) {
            String id=slotId(hovered);
            if(id!=null) EmotesModule.play(id);
            onClose();
        }
    }

    private String slotId(int i) { return i<0 || i>=Emotes.PER_PAGE ? null : Emotes.slot(page*Emotes.PER_PAGE+i); }
    /** Slot under the cursor, by angle, inside the ring; -1 elsewhere. */
    private int slotAt(double mx,double my) {
        double dx=mx-cx(),dy=my-cy(),d=Math.hypot(dx,dy),s=scale();
        if(d<INNER*s || d>OUTER*s+7) return -1;
        double angle=Math.atan2(dy,dx)+Math.PI/2;
        if(angle<0) angle+=Math.PI*2;
        return (int)Math.floor(angle/(Math.PI*2)*Emotes.PER_PAGE)%Emotes.PER_PAGE;
    }
    /** -1 or 1 over the page arrows in the centre, 0 elsewhere. */
    private int arrowAt(double mx,double my) {
        if(Math.abs(my-cy())>11) return 0;
        double dx=mx-cx();
        return dx>=-33 && dx<=-15 ? -1 : dx>=15 && dx<=33 ? 1 : 0;
    }
    private void turn(int direction,boolean wrap) {
        if(direction==0) return;
        int next=page+Integer.signum(direction);
        page=wrap ? Math.floorMod(next,Emotes.PAGES) : Math.clamp(next,0,Emotes.PAGES-1);
        hovered=-1;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        int button=event.button();
        int slot=slotAt(event.x(),event.y());
        if(slot>=0) {
            int index=page*Emotes.PER_PAGE+slot;
            String id=slotId(slot);
            if(id==null) { if(button==InputConstants.MOUSE_BUTTON_LEFT) minecraft.gui.setScreen(new EmotePickerScreen(index)); }
            else if(button==InputConstants.MOUSE_BUTTON_RIGHT) Emotes.clear(index);
            else if(button==InputConstants.MOUSE_BUTTON_LEFT) { EmotesModule.play(id); onClose(); }
            return true;
        }
        int arrow=arrowAt(event.x(),event.y());
        if(arrow!=0 && button==InputConstants.MOUSE_BUTTON_LEFT) turn(arrow,true);
        else if(button==InputConstants.MOUSE_BUTTON_RIGHT) Emotes.stop();
        return true;
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        if(vertical!=0) turn(vertical<0 ? 1 : -1,false);
        return true;
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        long now=System.nanoTime();
        float dt=Math.min(.1f,(now-lastFrame)/1e9f);
        lastFrame=now;
        hovered=slotAt(mouseX,mouseY);
        for(int i=0;i<hover.length;i++) hover[i]+=((i==hovered ? 1 : 0)-hover[i])*Math.min(1,dt*14);
        float open=Math.min(1,(now-openedAt)/1e9f/.28f);
        open=1-(float)Math.pow(1-open,3);
        float s=scale(),cx=cx(),cy=cy(),inner=INNER*s,outer=OUTER*s;
        var font=MsdfFont.UI;

        g.pose().pushMatrix();
        g.pose().translate(cx,cy);
        g.pose().scale(.82f+.18f*open,.82f+.18f*open);
        g.pose().translate(-cx,-cy);
        float r=outer+7;
        UiRenderer.circle(g,cx,cy,r,Theme.withAlpha(Theme.background(),.9f*open));
        UiRenderer.circleOutline(g,cx,cy,r,1,Theme.withAlpha(Theme.border(),(Theme.border()>>>24)/255f*open));

        float mid=(inner+outer)/2;
        for(int i=0;i<Emotes.PER_PAGE;i++) {
            float h=hover[i];
            double angle=-Math.PI/2+Math.PI*2*((i+.5)/Emotes.PER_PAGE);
            float d=mid+h*4,x=cx+(float)Math.cos(angle)*d,y=cy+(float)Math.sin(angle)*d;
            float size=36*s+h*4;
            int tile=Theme.mix(Theme.card(),Theme.mix(Theme.card(),Theme.accent(),.35f),h);
            UiRenderer.rounded(g,x-size/2,y-size/2,size,size,9+h*2,Theme.withAlpha(tile,open*(.85f+.15f*h)));
            if(h>.01f) UiRenderer.outline(g,x-size/2,y-size/2,size,size,9+h*2,1.2f,Theme.withAlpha(Theme.accent(),open*h*.8f));
            String id=slotId(i);
            var emote=id==null ? null : Emotes.find(id);
            if(emote!=null) {
                int icon=Math.round(31*s+h*4);
                g.blit(RenderPipelines.GUI_TEXTURED,emote.icon(),Math.round(x-icon/2f),Math.round(y-icon/2f),0,0,icon,icon,256,256,256,256);
            } else {
                float arm=5+h*1.5f;
                int plus=Theme.withAlpha(Theme.hint(),open*(.62f+.38f*h));
                UiRenderer.rounded(g,x-arm,y-.75f,arm*2,1.5f,.75f,plus);
                UiRenderer.rounded(g,x-.75f,y-arm,1.5f,arm*2,.75f,plus);
            }
        }

        // Centre: page number and the arrows.
        float c=36;
        UiRenderer.circle(g,cx,cy,c,Theme.withAlpha(Theme.card(),.97f*open));
        String number=Integer.toString(page+1);
        font.midCenter(g,number,cx,cy-3,12,Theme.withAlpha(Theme.text(),open));
        font.midCenter(g,"СТРАНИЦА",cx,cy+11,5.5f,Theme.withAlpha(Theme.secondary(),open));
        int arrow=arrowAt(mouseX,mouseY);
        Icons.draw(g,Icon.CHEVRON_LEFT,cx-29,cy-5,10,Theme.withAlpha(arrow<0 ? Theme.accent() : Theme.text(),open*(page>0 ? 1 : .45f)));
        Icons.draw(g,Icon.CHEVRON_RIGHT,cx+19,cy-5,10,Theme.withAlpha(arrow>0 ? Theme.accent() : Theme.text(),open*(page<Emotes.PAGES-1 ? 1 : .45f)));
        g.pose().popMatrix();

        // Caption above the wheel.
        String id=slotId(hovered);
        var emote=id==null ? null : Emotes.find(id);
        String title=hovered<0 ? "Колесо эмоций" : emote==null ? "Пустой слот" : emote.name();
        String hint=hovered<0 ? "Колесо мыши — сменить страницу" : emote==null ? "ЛКМ — выбрать эмоцию" : "Отпустите клавишу — воспроизвести  •  ПКМ — убрать";
        float tw=Math.max(font.width(title,8.5f),font.width(hint,6))+20,ty=cy-outer-38;
        if(Theme.glass()) GlassRenderer.glass(g,cx-tw/2,ty,tw,27,7,GlassRenderer.Style.HUD);
        UiRenderer.rounded(g,cx-tw/2,ty,tw,27,7,Theme.withAlpha(Theme.background(),.88f*open));
        font.midCenter(g,title,cx,ty+9,8.5f,Theme.withAlpha(Theme.text(),open));
        font.midCenter(g,hint,cx,ty+20,6,Theme.withAlpha(Theme.secondary(),open));
        var playing=Emotes.playing();
        if(playing!=null) font.midCenter(g,"Сейчас: "+playing.name()+"  •  ПКМ мимо колеса — остановить",cx,cy+outer+20,6,Theme.withAlpha(Theme.hint(),open));
    }
}
