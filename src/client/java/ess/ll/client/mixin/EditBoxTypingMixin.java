package ess.ll.client.mixin;

import ess.ll.client.module.render.TypingAnimation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** TypingAnimation in vanilla text boxes (chat, container search): the text before and after the cursor. */
@Mixin(EditBox.class)
public abstract class EditBoxTypingMixin {
    @Shadow private String value;
    @Shadow private int displayPos;
    @Shadow private int cursorPos;

    private static final String TEXT="Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V";

    @Redirect(method="extractWidgetRenderState",at=@At(value="INVOKE",target=TEXT,ordinal=0))
    private void ess$beforeCursor(GuiGraphicsExtractor g,Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow) {
        if(TypingAnimation.enabled()) TypingAnimation.editBox(g,font,this,value,displayPos,text,x,y,color,shadow);
        else g.text(font,text,x,y,color,shadow);
    }

    @Redirect(method="extractWidgetRenderState",at=@At(value="INVOKE",target=TEXT,ordinal=1))
    private void ess$afterCursor(GuiGraphicsExtractor g,Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow) {
        if(TypingAnimation.enabled()) TypingAnimation.editBox(g,font,this,value,cursorPos,text,x,y,color,shadow);
        else g.text(font,text,x,y,color,shadow);
    }
}
