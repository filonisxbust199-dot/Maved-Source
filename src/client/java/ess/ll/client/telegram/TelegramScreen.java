package ess.ll.client.telegram;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Icons;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.render.UiSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The Telegram menu: three steps top to bottom, each with its own short guide: make a bot and paste its token,
 * start it, send it the code to link your chat; then the options and what you can write to the bot.
 */
public final class TelegramScreen extends Screen {
    private static final float PW=320,PAD=12,R=10;
    private record Hot(float x,float y,float w,float h,Runnable click) {}

    private final Screen parent;
    private final long openedAt=System.nanoTime();
    private long lastFrame=System.nanoTime();
    private float s=1,mx,my,dt;
    private GuiGraphicsExtractor g;
    private final Map<String,Float> hovers=new HashMap<>();
    private List<Hot> hots=new ArrayList<>(),drawing=new ArrayList<>();
    private boolean editing;
    private String typed="";
    private String toast="";
    private long toastAt;

    public TelegramScreen(Screen parent) {
        super(Component.literal("Telegram"));
        this.parent=parent;
    }

    private float ease(String id,boolean on) {
        float v=hovers.getOrDefault(id,0f);
        v+=((on ? 1 : 0)-v)*(float)(1-Math.exp(-dt*14));
        hovers.put(id,v);
        return v;
    }
    private boolean over(float x,float y,float w,float h) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    private float hot(String id,float x,float y,float w,float h,Runnable click) {
        drawing.add(new Hot(x,y,w,h,click));
        return ease(id,over(x,y,w,h));
    }
    private void say(String text) { toast=text; toastAt=System.currentTimeMillis(); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        if(minecraft.level!=null) extractBlurredBackground(graphics);
        else super.extractBackground(graphics,mouseX,mouseY,delta);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        g=graphics;
        long frame=System.nanoTime(); dt=(float)Math.min(.1,(frame-lastFrame)/1e9); lastFrame=frame;
        float open=(float)Math.clamp((frame-openedAt)/1e9/.25,0,1);
        open=1-(1-open)*(1-open)*(1-open);
        float ph=height();
        s=Math.min(1.6f,Math.min(width/(PW+24),height/(ph+24)));
        mx=mouseX/s; my=mouseY/s;
        drawing=new ArrayList<>();
        UiRenderer.rect(g,0,0,width,height,Theme.withAlpha(0xff04060b,.35f*open));
        g.pose().pushMatrix();
        try {
            g.pose().scale(s,s);
            float x=(width/s-PW)/2,y=(height/s-ph)/2+(1-open)*10;
            panel(x,y,ph,open);
        } finally { g.pose().popMatrix(); }
        hots=drawing;
    }

    private static final float HEAD=34,STEP1=92,STEP2=58,STEP3=66,OPTS=74,GAP=8;
    private float height() { return HEAD+STEP1+STEP2+STEP3+OPTS+GAP*4+PAD*2; }

    private void panel(float x,float y,float h,float alpha) {
        var font=MsdfFont.UI;
        Theme.panel(g,x,y,PW,h,R,alpha);
        float ix=x+PAD,iw=PW-PAD*2,cy=y+PAD;
        // Header: the icon, the name, where the bot stands, the close cross.
        UiRenderer.rounded(g,ix,cy+3,22,22,7,Theme.withAlpha(Theme.accent(),.18f));
        Icons.draw(g,Icons.Icon.ROBOT,ix+5,cy+8,12,Theme.accent());
        font.mid(g,"Telegram",ix+30,cy+10,10,Theme.text());
        font.mid(g,"управление клиентом из Telegram",ix+30,cy+21,6.5f,Theme.hint());
        var st=TelegramBot.state();
        String pill=switch (st) {
            case RUNNING -> "в сети"+(TelegramBot.botName().isEmpty() ? "" : " · @"+TelegramBot.botName());
            case STARTING -> "запускается...";
            case ERROR -> "ошибка";
            default -> "остановлен";
        };
        int pc=switch (st) { case RUNNING -> 0xff4fe38a; case STARTING -> 0xffffd25a; case ERROR -> 0xffff6b5e; default -> Theme.hint(); };
        float pw=font.width(pill,6.5f)+18,px=ix+iw-pw-24;
        UiRenderer.rounded(g,px,cy+7,pw,14,7,Theme.withAlpha(pc,.16f));
        UiRenderer.rounded(g,px+6,cy+12,4,4,2,pc);
        font.mid(g,pill,px+13,cy+14,6.5f,pc);
        float ch=hot("close",ix+iw-18,cy+5,18,18,this::onClose);
        UiRenderer.rounded(g,ix+iw-18,cy+5,18,18,6,Theme.withAlpha(Theme.field(),.6f+.4f*ch));
        Icons.draw(g,Icons.Icon.CLOSE,ix+iw-14,cy+9,10,Theme.mix(Theme.hint(),Theme.text(),ch));
        cy+=HEAD+GAP;

        // 1. The token.
        card(ix,cy,iw,STEP1);
        step(ix,cy,"1","Создай бота и вставь токен");
        font.mid(g,"В Telegram открой @BotFather, напиши /newbot, придумай имя и ник —",ix+10,cy+30,6.5f,Theme.secondary());
        font.mid(g,"он пришлёт токен вида 123456:AAF... Скопируй его сюда.",ix+10,cy+39,6.5f,Theme.secondary());
        float fy=cy+48,fw=iw-20-118;
        float fh=hot("field",ix+10,fy,fw,20,()->{ editing=true; typed=""; });
        boolean has=TelegramBot.hasToken();
        UiRenderer.rounded(g,ix+10,fy,fw,20,6,editing ? Theme.withAlpha(Theme.accent(),.9f) : Theme.border());
        UiRenderer.rounded(g,ix+10.8f,fy+.8f,fw-1.6f,18.4f,5.4f,Theme.mix(Theme.field(),Theme.card(),.3f-.3f*fh));
        String shown=editing ? (typed.isEmpty() ? "вставь или впиши токен, Enter — сохранить" : typed+(System.currentTimeMillis()/500%2==0 ? "|" : ""))
                : has ? TelegramBot.maskedToken() : "токен не задан";
        while(font.width(shown,7)>fw-16 && shown.length()>4) shown=shown.substring(1);
        font.mid(g,shown,ix+18,fy+10,7,editing && typed.isEmpty() || !editing && !has ? Theme.hint() : Theme.text());
        float bx=ix+10+fw+6;
        button("paste","Вставить",bx,fy,56,false,()->{
            String clip=minecraft.keyboardHandler.getClipboard().trim();
            if(!clip.matches("\\d{5,}:[\\w-]{20,}")) { say("В буфере не токен: скопируй его из @BotFather"); return; }
            TelegramBot.setToken(clip); editing=false; say("Токен сохранён");
        });
        button("clear","Удалить",bx+60,fy,52,false,()->{ TelegramBot.setToken(""); editing=false; say("Токен удалён"); });
        if(has && !editing) font.mid(g,"Токен хранится только на этом компьютере.",ix+10,cy+80,6,Theme.hint());
        cy+=STEP1+GAP;

        // 2. Start and stop: the module switch.
        card(ix,cy,iw,STEP2);
        step(ix,cy,"2","Запусти бота");
        boolean on=ClientModules.TELEGRAM.enabled();
        String err=TelegramBot.error();
        String line=st==TelegramBot.State.ERROR && !err.isEmpty() ? err
                : on ? (st==TelegramBot.State.RUNNING ? "Бот работает, пока включён модуль Telegram." : "Подключаюсь к Telegram...")
                : "Бот работает, пока игра открыта и модуль включён.";
        font.mid(g,line,ix+10,cy+30,6.5f,st==TelegramBot.State.ERROR ? 0xffff6b5e : Theme.secondary());
        button("start",on ? "Остановить" : "Запустить",ix+10,cy+37,82,!on,()->{
            if(!on && !TelegramBot.hasToken()) { say("Сначала вставь токен"); return; }
            ClientModules.TELEGRAM.setEnabled(!on);
            if(!on) TelegramBot.start(); else TelegramBot.stop();
            UiSounds.toggle(!on);
        });
        cy+=STEP2+GAP;

        // 3. The link code.
        card(ix,cy,iw,STEP3);
        step(ix,cy,"3","Привяжи свой чат");
        if(TelegramBot.linked()) {
            font.mid(g,"Чат привязан: "+(TelegramBot.linkedName().isEmpty() ? "да" : TelegramBot.linkedName()),ix+10,cy+31,7.5f,0xff4fe38a);
            font.mid(g,"Команды принимаются только из этого чата.",ix+10,cy+42,6.5f,Theme.secondary());
            button("unlink","Отвязать",ix+iw-10-62,cy+24,62,false,()->{ TelegramBot.unlink(); say("Чат отвязан: отправь боту новый код"); });
        } else if(st==TelegramBot.State.RUNNING && !TelegramBot.code().isEmpty()) {
            String code=TelegramBot.code().substring(0,3)+" "+TelegramBot.code().substring(3);
            font.mid(g,code,ix+10,cy+36,15,Theme.accent());
            font.mid(g,"Открой своего бота"+(TelegramBot.botName().isEmpty() ? "" : " @"+TelegramBot.botName())+" и отправь ему этот код.",ix+10,cy+52,6.5f,Theme.secondary());
            button("copy","Копировать",ix+iw-10-70,cy+27,70,false,()->{ minecraft.keyboardHandler.setClipboard(TelegramBot.code()); say("Код скопирован"); });
        } else font.mid(g,"Код появится здесь, когда бот запустится.",ix+10,cy+33,6.5f,Theme.hint());
        cy+=STEP3+GAP;

        // Options and what to write.
        card(ix,cy,iw,OPTS);
        font.mid(g,"Пересылать чат игры",ix+10,cy+12,7,Theme.text());
        toggle("forward",ix+iw-34,cy+6,TelegramBot.FORWARD_CHAT.get(),TelegramBot.FORWARD_CHAT::toggle);
        font.mid(g,"Уведомления о смерти и кике",ix+10,cy+29,7,Theme.text());
        toggle("notify",ix+iw-34,cy+23,TelegramBot.NOTIFY.get(),TelegramBot.NOTIFY::toggle);
        UiRenderer.rect(g,ix+10,cy+40,iw-20,.6f,Theme.border());
        font.mid(g,"В боте: текст — в чат игры  ·  !home — команда сервера",ix+10,cy+50,6.5f,Theme.secondary());
        font.mid(g,".toggle Aura — клиент  ·  #goto 0 0 — Baritone  ·  /help",ix+10,cy+61,6.5f,Theme.secondary());

        if(System.currentTimeMillis()-toastAt<2500 && !toast.isEmpty()) {
            float tw=font.width(toast,7)+20,tx=x+PW/2-tw/2,ty=y+h+8;
            UiRenderer.rounded(g,tx,ty,tw,18,9,0xe0101318);
            font.midCenter(g,toast,x+PW/2,ty+9,7,0xffffffff);
        }
    }
    private void card(float x,float y,float w,float h) {
        UiRenderer.rounded(g,x,y,w,h,8,Theme.withAlpha(Theme.card(),.7f));
    }
    private void step(float x,float y,String number,String title) {
        UiRenderer.rounded(g,x+10,y+8,14,14,7,Theme.withAlpha(Theme.accent(),.2f));
        MsdfFont.UI.midCenter(g,number,x+17,y+15,7,Theme.accent());
        MsdfFont.UI.mid(g,title,x+30,y+15,8,Theme.text());
    }
    private void button(String id,String label,float x,float y,float w,boolean primary,Runnable click) {
        float h=hot(id,x,y,w,20,click);
        int bg=primary ? Theme.mix(Theme.accent(),0xffffffff,.12f*h) : Theme.mix(Theme.field(),Theme.text(),.08f+.08f*h);
        UiRenderer.rounded(g,x,y,w,20,6,bg);
        MsdfFont.UI.midCenter(g,label,x+w/2,y+10,7,primary ? Theme.onAccent() : Theme.text());
    }
    private void toggle(String id,float x,float y,boolean on,Runnable click) {
        float t=ease(id+"-on",on);
        hot(id,x,y,24,12,()->{ click.run(); UiSounds.toggle(!on); });
        UiRenderer.rounded(g,x,y,24,12,6,Theme.mix(Theme.field(),Theme.accent(),t));
        UiRenderer.rounded(g,x+2+12*t,y+2,8,8,4,0xffffffff);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event,doubleClick);
        float x=(float)event.x()/s,y=(float)event.y()/s;
        for(int i=hots.size()-1;i>=0;i--) {
            Hot h=hots.get(i);
            if(x>=h.x && x<h.x+h.w && y>=h.y && y<h.y+h.h) {
                if(h.click!=null) { UiSounds.select(); h.click.run(); }
                return true;
            }
        }
        // A click anywhere else leaves the token field.
        editing=false;
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(editing) {
            int key=event.key();
            if(key==InputConstants.KEY_ESCAPE) { editing=false; return true; }
            if(key==InputConstants.KEY_RETURN || key==InputConstants.KEY_NUMPADENTER) {
                if(typed.trim().matches("\\d{5,}:[\\w-]{20,}")) { TelegramBot.setToken(typed.trim()); say("Токен сохранён"); editing=false; }
                else say("Это не похоже на токен бота");
                return true;
            }
            if(key==InputConstants.KEY_BACKSPACE) { typed=typed.isEmpty() ? "" : typed.substring(0,typed.length()-1); return true; }
            if(event.isPaste()) { typed+=minecraft.keyboardHandler.getClipboard().trim(); return true; }
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if(!editing) return false;
        String c=event.codepointAsString();
        if(c.matches("[\\w:-]") && typed.length()<80) typed+=c;
        return true;
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    /** Dev check: types a token into the field as if pasted. */
    public void typeForTest(String text) { editing=true; typed=text; }
}
