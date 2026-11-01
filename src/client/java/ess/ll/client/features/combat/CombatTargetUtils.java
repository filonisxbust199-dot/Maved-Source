package ess.ll.client.features.combat;

import ess.ll.client.features.social.FriendManager;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.AntiBot;
import ess.ll.client.util.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class CombatTargetUtils implements MinecraftAccess {
    public static boolean isEnemy(Player player, AntiBot antiBot) {
        return isValidPlayer(player) && !FriendManager.INSTANCE.isFriend(player) && (antiBot == null || !antiBot.isBot(player));
    }

    public static boolean isValidPlayer(Player player2) {
        return player2 != mc.player && !(player2 instanceof LocalPlayer) && player2.isAlive() && !player2.isDeadOrDying() && !player2.isSpectator();
    }

    public static List<Player> findEnemiesInRange(float var0) {
        ArrayList items = new ArrayList();
        if (mc.player != null && mc.level != null) {
            AntiBot antiBot2 = ModuleManager.INSTANCE.antiBot();
            EntityExtensionAccess player2 = (EntityExtensionAccess) mc.player;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Player var6 && isEnemy(var6, antiBot2) && !(player2.getDistanceEyePos(var6) > var0)) {
                    items.add(var6);
                }
            }
            return items;
        } else {
            return items;
        }
    }

    public static boolean isEnemy(Player player) {
        return isEnemy(player, ModuleManager.INSTANCE.antiBot());
    }

    public static List<Player> findFriendsInRange(float var0) {
        ArrayList items = new ArrayList();
        if (mc.player != null && mc.level != null) {
            EntityExtensionAccess player2 = (EntityExtensionAccess) mc.player;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Player var5 && isValidPlayer(var5) && FriendManager.INSTANCE.isFriend(var5) && !(player2.getDistanceEyePos(var5) > var0)) {
                    items.add(var5);
                }
            }
            return items;
        } else {
            return items;
        }
    }

    private CombatTargetUtils() {
    }
}
