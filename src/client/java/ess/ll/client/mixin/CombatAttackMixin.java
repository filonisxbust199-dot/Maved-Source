package ess.ll.client.mixin;
import ess.ll.client.event.AttackEntityEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)
public class CombatAttackMixin {
 @Inject(method="attack",at=@At("HEAD"),cancellable=true)
 private void ess$pre(Player player,Entity target,CallbackInfo ci){
  var modules=ess.ll.client.util.ModuleManager.INSTANCE;
  if(modules.noFriendDamage().isEnabled() && target instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal
    && ess.ll.client.module.combat.CrystalAura.risksFriend(target.position())) {
   ci.cancel();
   return;
  }
  if(ess.ll.client.module.combat.NoFriendDamage.isFriend(target)
    && (modules.noFriendDamage().isEnabled() || modules.aura().isEnabled() || modules.crystalAura().isEnabled() || modules.crystalAuraV2().isEnabled())) {
   ci.cancel();
   return;
  }
  AttackEntityEvent.INSTANCE.firePre(target,ci);
 }
 @Inject(method="attack",at=@At("TAIL"))
 private void ess$post(Player player,Entity target,CallbackInfo ci){AttackEntityEvent.INSTANCE.firePost(target);}
}
