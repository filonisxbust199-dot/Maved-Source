package ess.ll.client.module.combat;

import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.social.FriendManager;
import ess.ll.client.module.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Friend hit-result filtering ported from test-26.2, with a final attack guard. */
public final class NoFriendDamage extends ClientModule {
    public NoFriendDamage() {
        super(ModuleCategory.Combat, ModuleSubCategory.Other);
        setDescription("Блокирует удары по игрокам из .friend.");
    }

    public static boolean isFriend(Entity entity) {
        return entity instanceof Player player && FriendManager.INSTANCE.isFriend(player);
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        if (event.getType() == HitResultEvent.Type.ENTITY && isFriend(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
