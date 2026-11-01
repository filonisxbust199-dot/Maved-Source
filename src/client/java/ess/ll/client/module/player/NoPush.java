package ess.ll.client.module.player;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * NoPush (from test-26.2): what may not move you. Players walking into you, blocks pushing you out when you stand
 * inside one, flowing water, a fishing rod's hook pulling you, and ladders/vines catching you. Only the local
 * player is affected; bots keep vanilla physics. The hooks are the NoPush mixins.
 */
public final class NoPush {
    public static final MultiBooleanSettings TYPES=new MultiBooleanSettings("Тип","Игроки","Блоки","Вода","Удочка","Лианы");
    public static final Setting[] SETTINGS={TYPES};
    static {
        // Water and climbing change how you move a lot, so they start off.
        TYPES.toggle(TYPES.options.indexOf("Вода"));
        TYPES.toggle(TYPES.options.indexOf("Лианы"));
    }

    private NoPush() {}

    /** Whether this kind of push is cancelled for this entity. */
    public static boolean blocks(Entity entity,String type) {
        return entity!=null && entity==Minecraft.getInstance().player && ClientModules.NO_PUSH!=null && ClientModules.NO_PUSH.enabled() && TYPES.selected(type);
    }
}
