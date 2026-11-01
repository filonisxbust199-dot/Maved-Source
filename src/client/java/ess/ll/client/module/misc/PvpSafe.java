package ess.ll.client.module.misc;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.mixin.BossOverlayAccessor;
import ess.ll.client.module.ClientModules;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.setting.Settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

/**
 * PvpSafe, ported from dimasik: while the server shows a PvP boss bar, leaving the server is blocked,
 * both the pause-menu button (PvpSafeMixin) and exit commands like /hub, /limbo, /an, /anarchy.
 */
public final class PvpSafe {
    public static final Setting[] SETTINGS={};
    private static final PvpSafe INSTANCE=new PvpSafe();
    private static boolean registered;
    private long lastPvpBossBarMillis;
    private PvpSafe() {}

    public static boolean enabled() { return ClientModules.PVP_SAFE!=null && ClientModules.PVP_SAFE.enabled(); }

    /** Keeps the event listeners registered only while the module is on. */
    public static void tick() {
        boolean on=enabled();
        if(on==registered) return;
        registered=on;
        if(on) EventBus.register(INSTANCE); else EventBus.unregister(INSTANCE);
    }

    public static boolean isPvpProtectionActive() {
        return enabled() && INSTANCE.isRecentlyInPvp();
    }

    public static boolean isExitCommand(String text) {
        if(text==null || text.isEmpty()) return false;
        String command=text.toLowerCase().trim();
        if(command.startsWith("/")) command=command.substring(1).trim();
        String head=command.split("\\s+",2)[0];
        return head.equals("hub") || head.equals("limbo") || head.equals("anarchy") || head.startsWith("an")
                || command.matches(".*\\b(hub|limbo|anarchy)\\b.*");
    }

    @Subscribe
    private void onTick(TickEvent event) { updatePvpBossBarState(); }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if(!event.isSend() || !isRecentlyInPvp()) return;
        String text=switch (event.getPacket()) {
            case ServerboundChatPacket packet -> packet.message();
            case ServerboundChatCommandSignedPacket packet -> packet.command();
            case ServerboundChatCommandPacket packet -> packet.command();
            default -> null;
        };
        if(text!=null && isExitCommand(text)) event.cancel();
    }

    private boolean isRecentlyInPvp() {
        updatePvpBossBarState();
        return System.currentTimeMillis()-lastPvpBossBarMillis<500L;
    }

    private void updatePvpBossBarState() {
        var mc=Minecraft.getInstance();
        if(mc.gui==null || mc.gui.hud==null) return;
        for(var bar:((BossOverlayAccessor)mc.gui.hud.getBossOverlay()).ess$events().values()) {
            String name=bar.getName().getString().toLowerCase();
            if(name.contains("pvp") || name.contains("пвп")) { lastPvpBossBarMillis=System.currentTimeMillis(); return; }
        }
    }
}
