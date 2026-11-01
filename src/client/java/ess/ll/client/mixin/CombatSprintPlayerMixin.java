package ess.ll.client.mixin;

import ess.ll.client.event.PlayerMovementActionEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve the source AutoSprint's attack knockback sprint reset behavior. */
@Mixin(Player.class)
public abstract class CombatSprintPlayerMixin {
    @Redirect(method="causeExtraKnockback",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void ess$resetSprint(Player player,boolean sprinting) {
        if(player==net.minecraft.client.Minecraft.getInstance().player)
            PlayerMovementActionEvent.INSTANCE.fireSprint(player,sprinting);
        else player.setSprinting(sprinting);
    }

    @Redirect(method="causeExtraKnockback",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void ess$knockbackVelocity(Player player,Vec3 velocity) {
        if(player==net.minecraft.client.Minecraft.getInstance().player)
            PlayerMovementActionEvent.INSTANCE.fireVelocity(player,velocity);
        else player.setDeltaMovement(velocity);
    }
}
