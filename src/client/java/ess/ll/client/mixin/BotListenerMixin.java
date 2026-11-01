package ess.ll.client.mixin;

import ess.ll.client.bots.Bots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bots: the account the player joins with becomes the main session, so it can be kept online in the background. */
@Mixin(ClientPacketListener.class)
public abstract class BotListenerMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void ess$joined(Minecraft minecraft,Connection connection,CommonListenerCookie cookie,CallbackInfo ci) {
        Bots.listenerCreated((ClientPacketListener)(Object)this,connection,cookie);
    }
}
