package ess.ll.client.render;

import ess.ll.client.module.render.RenderTestModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import static ess.ll.client.render.Gradient.Axis.*;

/** Non-interactive examples, only rendered when RenderTest is enabled. */
public final class RenderDemoHud {
    private static final long START = System.nanoTime();
    private static final int BLUE=0xff65ceff, WHITE=0xfff8fcff, INK=0xff102b40;
    private RenderDemoHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!RenderTestModule.isEnabled()) return;
        var client=Minecraft.getInstance();
        if(client.level==null || client.player==null) return;
        double time=RenderTestModule.ANIMATION.get() ? (System.nanoTime()-START)/1_000_000_000.0*RenderTestModule.SPEED.get() : 0;
        int BLUE=switch (RenderTestModule.PALETTE.get()) {
            case "Ice" -> 0xffb8edff;
            case "Mono" -> 0xff8d969e;
            default -> RenderDemoHud.BLUE;
        };
        float scale=Math.min(1f,Math.min((g.guiWidth()-32f)/492f,(g.guiHeight()-62f)/266f));
        if(scale<=0) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(12,14);
            g.pose().scale(scale,scale);
            var font=MsdfFont.UI;
            font.draw(g,"MAVED / LIVE",14,10,10,BLUE);
            font.draw(g,"Голубой + белый",14,27,19,Gradient.flow(BLUE,WHITE,HORIZONTAL,14,190,time*.12));
            UiRenderer.flowingLine(g,14,57,210,1.2f,time*.18);

            if(RenderTestModule.EXAMPLES.selected(0)) {
            Gradient backgroundFlow=Gradient.flow(BLUE,WHITE,HORIZONTAL,14,200,time*.10);
            UiRenderer.panel(g,14,70,100,48,13,Gradient.linear(backgroundFlow.at(14,70),backgroundFlow.at(114,70),HORIZONTAL,14,100),UiRenderer.Shadow.DENSE);
            font.draw(g,"Horizontal",24,78,12,INK);
            font.draw(g,"Градиент фона",24,98,8,0xff34556c);
            UiRenderer.panel(g,124,70,100,48,13,Gradient.linear(WHITE,BLUE,VERTICAL,70,48),UiRenderer.Shadow.DENSE);
            font.draw(g,"Vertical",134,78,12,INK);
            font.draw(g,"Градиент фона",134,98,8,0xff34556c);

            font.draw(g,"Градиент текста",14,128,16,Gradient.linear(BLUE,WHITE,HORIZONTAL,14,160));
            font.draw(g,"Переливание MSDF",14,150,16,Gradient.flow(BLUE,WHITE,HORIZONTAL,14,195,time*.20));
            font.draw(g,"Вертикальный",14,173,11,Gradient.linear(WHITE,BLUE,VERTICAL,176,12));
            }

            if(RenderTestModule.EXAMPLES.selected(1)) {
            UiRenderer.Shadow[] types=UiRenderer.Shadow.values();
            String[] labels={"Soft","Dense","Tight","Glow"};
            for(int i=0;i<types.length;i++) {
                float x=14+i*54;
                UiRenderer.panel(g,x,204,48,30,9,Gradient.linear(0xffb1e8ff,WHITE,VERTICAL,204,30),types[i]);
                font.centered(g,labels[i],x+24,212,9,INK);
            }
            }
            UiRenderer.flowingLine(g,14,247,210,1.4f,time*.24);
            UiRenderer.flowingLine(g,14,253,210,.7f,time*.24+.22);

            // Keep the original monochrome examples alongside the new effects.
            if(RenderTestModule.EXAMPLES.selected(2)) {
            g.nextStratum();
            g.pose().translate(254,0);
            font.draw(g,"MAVED / CLASSIC",14,10,10,WHITE);

            UiRenderer.rounded(g,14,34,210,65,20,0xff101114);
            font.draw(g,"Белый на чёрном",28,43,16,0xffffffff);
            font.draw(g,"Плавные выпуклые углы",28,73,9,0xffb6b8c1);

            UiRenderer.rounded(g,14,110,210,65,20,0xffffffff);
            font.draw(g,"Чёрный на белом",28,119,16,0xff111216);
            font.draw(g,"Чистый фон, чёткий текст",28,149,9,0xff747884);

            UiRenderer.rect(g,14,190,62,60,0xff000000);
            font.draw(g,"Плитка",22,202,12,0xffffffff);
            font.draw(g,"Без углов",22,227,7,0xffa2a4ab);

            UiRenderer.panel(g,88,190,84,60,16,Gradient.solid(0xffffffff),UiRenderer.Shadow.SOFT);
            font.draw(g,"Мягкая",98,201,12,0xff16181e);
            font.draw(g,"тень",98,219,12,0xff16181e);

            UiRenderer.panel(g,184,190,40,60,20,Gradient.solid(0xff16171b),UiRenderer.Shadow.SOFT);
            font.centered(g,"Aa",204,203,20,0xffffffff);
            }
        } finally { g.pose().popMatrix(); }
    }
}
