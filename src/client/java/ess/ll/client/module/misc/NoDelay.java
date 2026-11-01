package ess.ll.client.module.misc;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.mixin.LivingEntityMixin_1248;
import ess.ll.client.mixin.RightClickDelayAccessor;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

/**
 * NoDelay, ported from test-26.2: removes the pause between jumps while the jump key is held (NoJumpDelay)
 * and the pause between right-clicks, optionally only while throwing experience bottles.
 */
public final class NoDelay {
    public static final MultiBooleanSettings PARTS=new MultiBooleanSettings("Убрать задержку на","На прыжки","На пкм").none();
    public static final BooleanSetting XP_ONLY=new BooleanSetting("Только с опытом",false).visibleWhen(()->PARTS.selected("На пкм"));
    public static final Setting[] SETTINGS={PARTS,XP_ONLY};
    static { PARTS.toggle(0); }
    private static final NoDelay INSTANCE=new NoDelay();
    private static boolean registered;
    private NoDelay() {}

    public static boolean enabled() { return ClientModules.NO_DELAY!=null && ClientModules.NO_DELAY.enabled(); }

    /** Keeps the listener registered only while the module is on. */
    public static void tick() {
        boolean on=enabled();
        if(on==registered) return;
        registered=on;
        if(on) EventBus.register(INSTANCE); else EventBus.unregister(INSTANCE);
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null) return;
        if(PARTS.selected("На прыжки")) ((LivingEntityMixin_1248)mc.player).setjumpingCooldown(0);
        if(PARTS.selected("На пкм") && (!XP_ONLY.get() || throwingExperience(mc))) ((RightClickDelayAccessor)mc).ess$setRightClickDelay(0);
    }

    /** An experience bottle in either hand that is not on cooldown. */
    private static boolean throwingExperience(Minecraft mc) {
        var main=mc.player.getMainHandItem();
        if(main.is(Items.EXPERIENCE_BOTTLE)) return !mc.player.getCooldowns().isOnCooldown(main);
        var off=mc.player.getOffhandItem();
        return off.is(Items.EXPERIENCE_BOTTLE) && !mc.player.getCooldowns().isOnCooldown(off);
    }
}
