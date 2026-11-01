package ess.ll.client.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class CombatFrameMixin {
 @Inject(method="runTick",at=@At("HEAD"))
 private void ess$frame(boolean render,CallbackInfo ci){ess.ll.client.module.combat.CombatRuntime.frame();}
}
