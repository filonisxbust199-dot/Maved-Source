package ess.ll.client.mixin;

import ess.ll.client.screen.AltAccounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every connection, from any screen, becomes the current account's last server. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method="startConnecting",at=@At("HEAD"))
    private static void ess$remember(Screen parent,Minecraft minecraft,ServerAddress address,ServerData data,boolean quickPlay,TransferState transfer,CallbackInfo ci) {
        if(data!=null && transfer==null) AltAccounts.played(data);
    }
}
