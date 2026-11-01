package ess.ll.client.screen;

import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.UiRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A small, responsive gallery; closing restores the screen it was opened from. */
public final class RenderDemoScreen extends Screen {
    private final Screen parent;
    public RenderDemoScreen(Screen parent) {
        super(Component.literal("Maved / Примеры рендера"));
        this.parent=parent;
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Продолжить"), button -> onClose())
                .bounds(width/2-60, height-28,120,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        UiRenderer.rect(g,0,0,width,height,0xffedeef2);
        float scale=Math.min(1f,Math.min((width-24)/560f,(height-52)/350f));
        scale=Math.max(0.1f,scale);
        g.pose().pushMatrix();
        g.pose().translate((width-560*scale)/2,Math.max(8,(height-40-350*scale)/2));
        g.pose().scale(scale,scale);
        var f=MsdfFont.UI;
        f.draw(g,"MAVED / RENDER LAB",0,0,10,0xff777b86);
        f.draw(g,"Форма. Текст. Тень.",0,22,28,0xff13151b);
        f.draw(g,"MSDF-типографика и плавные выпуклые углы",0,63,11,0xff707580);

        UiRenderer.rounded(g,0,100,268,130,32,0xff101114);
        f.draw(g,"01 / DARK",22,116,10,0xff999ba4);
        f.draw(g,"Белый на чёрном",22,143,20,0xffffffff);
        f.draw(g,"Мягкая геометрия в стиле iOS",22,184,11,0xffb6b8c1);

        UiRenderer.rounded(g,292,100,268,130,32,0xffffffff);
        f.draw(g,"02 / LIGHT",314,116,10,0xff818590);
        f.draw(g,"Чёрный на белом",314,143,20,0xff111216);
        f.draw(g,"Чистый фон, чёткий текст",314,184,11,0xff747884);

        UiRenderer.rect(g,0,254,120,80,0xff000000);
        f.draw(g,"Плитка",18,270,17,0xffffffff);
        f.draw(g,"Без скругления",18,302,9,0xffa2a4ab);

        UiRenderer.shadow(g,148,254,264,80,24,20,0x480b1020);
        UiRenderer.rounded(g,148,254,264,80,24,0xffffffff);
        f.draw(g,"Мягкая тень",170,267,18,0xff16181e);
        f.draw(g,"Размытие + плотность + ARGB",170,300,10,0xff797d89);

        UiRenderer.shadow(g,448,254,80,80,40,12,0x30000000);
        UiRenderer.rounded(g,448,254,80,80,40,0xff16171b);
        f.centered(g,"Aa",488,264,32,0xffffffff);
        g.pose().popMatrix();
        // Draw widgets without Screen's panorama/blur background over the gallery.
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable)
            renderable.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
