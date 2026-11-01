package ess.ll.client.features.social;
import net.minecraft.world.entity.player.Player;
public enum FriendManager { INSTANCE;
 public boolean isFriend(Player player) { return ess.ll.client.command.ClientData.isFriend(player.getGameProfile().name()); }
}
