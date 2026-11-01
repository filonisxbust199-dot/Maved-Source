package ess.ll.client.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.ParseResults;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import ess.ll.client.command.DotSuggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Uses Minecraft's suggestion list, Tab handling, scrolling and narration for local commands. */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private EditBox input;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private boolean keepSuggestions;
    @Shadow private boolean allowSuggestions;
    @Shadow private ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow private boolean currentParseIsCommand;
    @Shadow private boolean currentParseIsMessage;
    @Shadow public abstract void showSuggestions(boolean narrate);
    @Shadow public abstract void hide();

    @Inject(method="updateCommandInfo",at=@At("HEAD"),cancellable=true)
    private void ess$complete(CallbackInfo ci) {
        if(!input.getValue().startsWith(".") || input.getCursorPosition()<1) return;
        ci.cancel();
        if(keepSuggestions) return;
        currentParse=null;
        currentParseIsCommand=false;
        currentParseIsMessage=true;
        hide();
        input.setSuggestion(null);
        commandUsage.clear();
        pendingSuggestions=CompletableFuture.completedFuture(DotSuggestions.complete(input.getValue(),input.getCursorPosition()));
        if(allowSuggestions) showSuggestions(false);
    }
}
