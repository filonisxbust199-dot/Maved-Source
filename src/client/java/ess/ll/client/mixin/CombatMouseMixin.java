package ess.ll.client.mixin;
import ess.ll.client.event.MouseTurnEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(MouseHandler.class)
public class CombatMouseMixin {
 @Redirect(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
 private void ess$turn(LocalPlayer player,double x,double y){MouseTurnEvent.INSTANCE.fire(player,x,y);}
}
