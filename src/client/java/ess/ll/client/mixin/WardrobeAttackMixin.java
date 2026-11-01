package ess.ll.client.mixin;

import ess.ll.client.wardrobe.WardrobeHeld;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wardrobe: hits with a skinned weapon burst its hit particle. */
@Mixin(MultiPlayerGameMode.class)
public abstract class WardrobeAttackMixin {
    @Inject(method="attack",at=@At("HEAD"))
    private void ess$weaponHit(Player player,Entity target,CallbackInfo ci) {
        WardrobeHeld.attack(target);
    }
}
