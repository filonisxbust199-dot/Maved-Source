package ess.ll.client.module.combat;

import ess.ll.client.event.EntityVisibilityEvent;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ToggleSetting;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AntiBot extends ClientModule {
    private final ToggleSetting hideBots;

    private boolean hasOfflineUuid(Player player) {
        UUID uUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName().getString()).getBytes(StandardCharsets.UTF_8));
        return player.getUUID().equals(uUID);
    }

    @Subscribe(1)
    private void onHitResult(HitResultEvent event) {
        if (this.hideBots.isEnabled()) {
            if (event.getType() == HitResultEvent.Type.ENTITY && event.getEntity() instanceof Player var3 && this.isBot(var3)) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        this.updateVerifiedPlayers();
    }

    private void updateVerifiedPlayers() {
        ClientPacketListener connection = mc.getConnection();
        LocalPlayer player2 = mc.player;
        if (connection != null && player2 != null) {
            boolean var3 = this.hasOfflineUuid(player2);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Player var6) {
                    EntityExtensionAccess entityExtensionAccess = (EntityExtensionAccess) var6;
                    if (!entityExtensionAccess.botEntity()) {
                        if (var6 instanceof LocalPlayer) {
                            entityExtensionAccess.setBotEntity(true);
                        } else {
                            boolean var8 = var3 ? this.hasOfflineUuid(var6) : connection.getPlayerInfo(var6.getUUID()) != null;
                            if (var8) {
                                entityExtensionAccess.setBotEntity(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public AntiBot() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.hideBots = new ToggleSetting("Убирать с мира").withDefault(false).withContainer(this);
        this.setName("Anti Bot");
    }

    public boolean isBot(Entity entity) {
        return entity == null ? false : this.isEnabled() && !((EntityExtensionAccess) entity).botEntity();
    }

    @Subscribe(1)
    private void onEntityVisibility(EntityVisibilityEvent event) {
        if (this.hideBots.isEnabled()) {
            if (event.getEntity() instanceof Player var3 && this.isBot(var3)) {
                event.setVisible(false);
                event.cancel();
            }
        }
    }
}
