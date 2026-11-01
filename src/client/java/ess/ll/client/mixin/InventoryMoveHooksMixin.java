package ess.ll.client.mixin;

import ess.ll.client.event.ContainerClickEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class InventoryMoveHooksMixin {
    @Inject(method="handleContainerInput",at=@At("HEAD"),cancellable=true)
    private void ess$queuedInventoryClick(int containerId,int slot,int button,ContainerInput input,Player player,CallbackInfo ci) {
        ContainerClickEvent.INSTANCE.fire(containerId,slot,button,input,player,ci);
    }
}
