package ess.ll.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.features.emote.Emotes;
import ess.ll.client.features.emote.Emotes.Emote;
import ess.ll.client.module.render.TypingAnimation;
import ess.ll.client.render.GlassRenderer;
import ess.ll.client.render.Icons;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Picks the emote for one wheel slot (from Zenith): a searchable grid of every emote with its picture, and your
 * own player on the right playing the emote under the cursor. A click puts the emote into the slot.
 */
public final class EmotePickerScreen extends Screen {
    private static final float CARD_W=62,CARD_H=74,GAP=6,PREVIEW_W=150,HEADER=34;
    private final int slot;
    private final long openedAt=System.nanoTime();
    private String query="";
    private List<Emote> shown=List.of();
    private float scroll,scrollTarget;
    private Emote previewed;

    public EmotePickerScreen(int slot) {
        super(Component.literal("Выбор эмоции"));
        this.slot=slot;
        filter();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { Emotes.preview(null); }

    private void filter() {
        String q=query.trim().toLowerCase(Locale.ROOT);
        shown=Emotes.all().stream().filter(e->q.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(q) || e.id().contains(q)).toList();
        scroll=scrollTarget=0;
    }

    // Layout.
    private float panelW() { return Math.min(560,width-30); }
    private float panelH() { return Math.min(340,height-30); }
    private float panelX() { return (width-panelW())/2; }
    private float panelY() { return (height-panelH())/2; }
    private float gridX() { return panelX()+12; }
    private float gridY() { return panelY()+HEADER+26; }
    private float gridW() { return panelW()-24-PREVIEW_W-10; }
    private float gridH() { return panelY()+panelH()-12-gridY(); }
    private int columns() { return Math.max(1,(int)((gridW()+GAP)/(CARD_W+GAP))); }
    private float maxScroll() {
        int rows=(shown.size()+columns()-1)/columns();
        return Math.max(0,rows*(CARD_H+GAP)-GAP-gridH());
    }
    private int cardAt(double mx,double my) {
        if(mx<gridX() || mx>gridX()+gridW() || my<gridY() || my>gridY()+gridH()) return -1;
        int col=(int)((mx-gridX())/(CARD_W+GAP)),row=(int)((my-gridY()+scroll)/(CARD_H+GAP));
        if(col>=columns() || mx-gridX()-col*(CARD_W+GAP)>CARD_W || my-gridY()+scroll-row*(CARD_H+GAP)>CARD_H) return -1;
        int i=row*columns()+col;
        return i<shown.size() ? i : -1;
    }
    private boolean overClose(double mx,double my) {
        float x=panelX()+panelW()-28,y=panelY()+8;
        return mx>=x && mx<=x+18 && my>=y && my<=y+18;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT) return true;
        if(overClose(event.x(),event.y())) { onClose(); return true; }
        int card=cardAt(event.x(),event.y());
        if(card>=0) {
            Emotes.assign(slot,shown.get(card).id());
            onClose();
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        scrollTarget=Math.clamp(scrollTarget-(float)vertical*(CARD_H+GAP)*.75f,0,maxScroll());
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==InputConstants.KEY_BACKSPACE && !query.isEmpty()) {
            query=query.substring(0,query.offsetByCodePoints(query.length(),-1));
            filter();
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event) {
        String text=event.codepointAsString();
        if(text.codePoints().allMatch(cp->Character.isLetterOrDigit(cp) || cp==' ' || cp=='_' || cp=='-') && query.codePointCount(0,query.length())<24) {
            query+=text;
            filter();
        }
        return true;
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        UiRenderer.rect(g,0,0,width,height,0x66000000);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        super.extractRenderState(g,mouseX,mouseY,delta);
        float open=Math.min(1,(System.nanoTime()-openedAt)/1e9f/.25f);
        open=1-(float)Math.pow(1-open,3);
        scroll+=(scrollTarget-scroll)*.35f;
        var font=MsdfFont.UI;
        float px=panelX(),py=panelY()+(1-open)*8,pw=panelW(),ph=panelH(),radius=Theme.radius()+2;
        if(Theme.glass()) GlassRenderer.glass(g,px,py,pw,ph,radius,GlassRenderer.Style.PANEL);
        Theme.panel(g,px,py,pw,ph,radius,open);

        // Header: title, the slot it fills, close button.
        font.mid(g,"Выбор эмоции",px+14,py+HEADER/2f,10,Theme.withAlpha(Theme.text(),open));
        int page=slot/Emotes.PER_PAGE+1,place=slot%Emotes.PER_PAGE+1;
        font.mid(g,"страница "+page+" · слот "+place+" · "+shown.size()+" эмоций",px+14+font.width("Выбор эмоции",10)+10,py+HEADER/2f+1,6.5f,Theme.withAlpha(Theme.hint(),open));
        boolean closeHover=overClose(mouseX,mouseY);
        UiRenderer.rounded(g,px+pw-28,py+8,18,18,5,Theme.withAlpha(closeHover ? Theme.mix(Theme.field(),Theme.accent(),.3f) : Theme.field(),open));
        Icons.draw(g,Icon.CLOSE,px+pw-24,py+12,10,Theme.withAlpha(Theme.text(),open));

        // Search.
        float sx=gridX(),sy=py+HEADER,sw=gridW();
        UiRenderer.rounded(g,sx,sy,sw,18,6,Theme.withAlpha(Theme.field(),open));
        if(query.isEmpty()) font.mid(g,"Поиск эмоции...",sx+8,sy+9,7,Theme.withAlpha(Theme.hint(),open));
        else TypingAnimation.mid(g,font,this,query,sx+8,sy+9,7,Theme.withAlpha(Theme.text(),open));
        boolean caret=(System.currentTimeMillis()/530)%2==0;
        if(caret) UiRenderer.rect(g,sx+8+font.width(query,7)+1,sy+4.5f,.8f,9,Theme.withAlpha(Theme.accent(),open));

        // Grid.
        int hovered=cardAt(mouseX,mouseY);
        if(hovered>=0 && shown.get(hovered)!=previewed) { previewed=shown.get(hovered); Emotes.preview(previewed); }
        float gx=gridX(),gy=gridY(),gw=gridW(),gh=gridH();
        g.enableScissor(Math.round(gx),Math.round(gy),Math.round(gx+gw),Math.round(gy+gh));
        int cols=columns();
        for(int i=0;i<shown.size();i++) {
            float x=gx+(i%cols)*(CARD_W+GAP),y=gy+(i/cols)*(CARD_H+GAP)-scroll;
            if(y+CARD_H<gy || y>gy+gh) continue;
            Emote e=shown.get(i);
            boolean over=i==hovered,worn=e.id().equals(Emotes.slot(slot));
            int card=over ? Theme.mix(Theme.card(),Theme.accent(),.22f) : Theme.card();
            UiRenderer.rounded(g,x,y,CARD_W,CARD_H,7,Theme.withAlpha(card,open));
            if(over || worn) UiRenderer.outline(g,x,y,CARD_W,CARD_H,7,1,Theme.withAlpha(Theme.accent(),open*(worn ? 1 : .7f)));
            int icon=50;
            g.blit(RenderPipelines.GUI_TEXTURED,e.icon(),Math.round(x+(CARD_W-icon)/2),Math.round(y+4),0,0,icon,icon,256,256,256,256);
            String name=e.name();
            float size=6;
            while(font.width(name,size)>CARD_W-6 && name.length()>4) name=name.substring(0,name.length()-4)+"...";
            font.midCenter(g,name,x+CARD_W/2,y+CARD_H-9,size,Theme.withAlpha(over ? Theme.text() : Theme.secondary(),open));
        }
        g.disableScissor();
        if(shown.isEmpty()) font.midCenter(g,"Ничего не найдено",gx+gw/2,gy+gh/2,8,Theme.withAlpha(Theme.hint(),open));
        float max=maxScroll();
        if(max>0) {
            float track=gh,thumb=Math.max(20,track*gh/(gh+max)),ty=gy+(track-thumb)*(scroll/max);
            UiRenderer.rounded(g,gx+gw+3,ty,2.5f,thumb,1.25f,Theme.withAlpha(Theme.hint(),open*.6f));
        }

        // Preview: your player playing the emote under the cursor.
        float vx=px+pw-12-PREVIEW_W,vy=py+HEADER,vh=ph-HEADER-12;
        UiRenderer.rounded(g,vx,vy,PREVIEW_W,vh,8,Theme.withAlpha(Theme.card(),open));
        var player=minecraft.player;
        if(player!=null) {
            var state=minecraft.getEntityRenderDispatcher().getRenderer(player).createRenderState(player,1);
            state.shadowPieces.clear(); state.outlineColor=0; state.nameTag=null;
            if(state instanceof AvatarRenderState avatar) avatar.id=Emotes.PREVIEW_ID;
            if(state instanceof LivingEntityRenderState living) {
                living.bodyRot=180+(float)Math.sin(System.nanoTime()/1e9*.6)*18; living.yRot=0; living.xRot=0;
                living.walkAnimationSpeed=0;
                living.boundingBoxWidth/=living.scale; living.boundingBoxHeight/=living.scale; living.scale=1;
            }
            float box=vh-26;
            Quaternionf rotation=new Quaternionf().rotateZ((float)Math.PI);
            g.entity(state,box*.34f,new Vector3f(0,.95f,0),rotation,null,Math.round(vx+1),Math.round(vy+1),Math.round(vx+PREVIEW_W-1),Math.round(vy+vh-26));
        }
        String title=previewed==null ? "Наведите на эмоцию" : previewed.name();
        font.midCenter(g,title,vx+PREVIEW_W/2,vy+vh-18,7.5f,Theme.withAlpha(Theme.text(),open));
        if(previewed!=null) font.midCenter(g,"автор: "+previewed.author(),vx+PREVIEW_W/2,vy+vh-8,5.5f,Theme.withAlpha(Theme.hint(),open));
    }
}
