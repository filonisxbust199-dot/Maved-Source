package ess.ll.client.module.player;

import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.module.*;
import net.minecraft.world.level.block.Blocks;

/** RaycastManager continues the interaction ray past rejected blocks. */
public final class WebIgnore extends ClientModule {
    public WebIgnore() {
        super(ModuleCategory.Player, ModuleSubCategory.Other);
        setDescription("Позволяет ломать, ставить блоки и взаимодействовать сквозь паутину.");
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        if (event.getType() == HitResultEvent.Type.BLOCK && event.getState().is(Blocks.COBWEB)) {
            event.setCancelled(true);
        }
    }
}
