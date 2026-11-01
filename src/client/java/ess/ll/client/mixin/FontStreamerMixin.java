package ess.ll.client.mixin;

import ess.ll.client.module.misc.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** StreamerMode: everything the game draws or measures with its font goes through the replacements. */
@Mixin(Font.class)
public abstract class FontStreamerMixin {
    @ModifyVariable(method="prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",at=@At("HEAD"),argsOnly=true)
    private String ess$streamerText(String text) { return StreamerMode.apply(text); }
    @ModifyVariable(method="prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",at=@At("HEAD"),argsOnly=true)
    private FormattedCharSequence ess$streamerSequence(FormattedCharSequence text) { return StreamerMode.apply(text); }
    @ModifyVariable(method="prepare8xTextOutline",at=@At("HEAD"),argsOnly=true)
    private FormattedCharSequence ess$streamerOutline(FormattedCharSequence text) { return StreamerMode.apply(text); }
    @ModifyVariable(method="width(Ljava/lang/String;)I",at=@At("HEAD"),argsOnly=true)
    private String ess$streamerWidth(String text) { return StreamerMode.apply(text); }
    @ModifyVariable(method="width(Lnet/minecraft/util/FormattedCharSequence;)I",at=@At("HEAD"),argsOnly=true)
    private FormattedCharSequence ess$streamerSequenceWidth(FormattedCharSequence text) { return StreamerMode.apply(text); }
    // Scoreboard and tab widths are measured on components: measure what will be drawn.
    @Inject(method="width(Lnet/minecraft/network/chat/FormattedText;)I",at=@At("HEAD"),cancellable=true)
    private void ess$streamerTextWidth(FormattedText text,CallbackInfoReturnable<Integer> cir) {
        if(StreamerMode.enabled()) cir.setReturnValue(((Font)(Object)this).width(Language.getInstance().getVisualOrder(text)));
    }
}
