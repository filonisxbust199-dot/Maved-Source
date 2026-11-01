package ess.ll.client.util;

import ess.ll.client.event.HeldItemRenderEvent;

import ess.ll.client.integration.LevelRendererAccess;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.ui.GuiHitTestUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Vector2f;
import ess.ll.client.mixin.DeltaTracker$TimerMixin_1312;
import ess.ll.client.mixin.LivingEntityMixin_1248;

public final class PlayerUtils implements MinecraftAccess {
   public static final Pattern NAME_PATTERN = Pattern.compile(PlayerUtils.NAME_REGEX);
   private static final float SCOREBOARD_MAX_HEALTH = 20.0F;
   private static final double HALF_TURN_YAW = 180.0;
   private static final double PI_YAW = Math.PI;
   private static final float YAW_OFFSET = 90.0F;
   private static final double HALF_TURN_PITCH = 180.0;
   private static final double PI_PITCH = Math.PI;
   private static final double MIN_PITCH = -90.0;
   private static final double MAX_PITCH = 90.0;
   private static final float SEARCH_HEIGHT = 6.0F;
   private static final double SQUARE_X = 2.0;
   private static final double SQUARE_Z = 2.0;
   private static final double SQUARE_Y = 2.0;
   private static final double SQUARE_RADIUS = 2.0;
   private static final double SQUARE_INNER_RADIUS = 2.0;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";
   private static final String NAME_REGEX = "^[A-zА-я0-9_]{3,16}$";

   public static float tickDelta() {
      return ((DeltaTracker$TimerMixin_1312)mc.getDeltaTracker()).tickDelta();
   }

   public static ItemStack renderedMainHand(Player player) {
      return HeldItemRenderEvent.INSTANCE.fireMainHandStack(player, player.getMainHandItem());
   }

   public static boolean isInSphere(int x, int y, int z, int blockX, int blockY, int blockZ, float radius, boolean shell) {
      double distanceSq = Math.pow(x - blockX, SQUARE_X) + Math.pow(z - blockZ, SQUARE_Z) + Math.pow(y - blockY, SQUARE_Y);
      return distanceSq < Math.pow(radius, SQUARE_RADIUS) && (!shell || distanceSq >= Math.pow(radius - 1.0F, SQUARE_INNER_RADIUS));
   }

   public static float maxHealth(LivingEntity livingEntity) {
      return ModuleManager.INSTANCE.scoreboardHealth().isEnabled() && livingEntity instanceof Player ? scoreboardHealth(livingEntity)[1] : livingEntity.getMaxHealth();
   }

   public static void dropHeldItem(Minecraft client) {
      if (client != null && client.player != null && client.gameMode != null && mc.player != null) {
         client.gameMode
            .handleContainerInput(client.player.containerMenu.containerId, 36 + mc.player.getInventory().getSelectedSlot(), 1, ContainerInput.THROW, client.player);
      }
   }

   public static float[] scoreboardHealth(LivingEntity livingEntity) {
      float[] health = new float[]{livingEntity.getHealth(), livingEntity.getMaxHealth()};
      if (mc.level == null) {
         return health;
      }

      Scoreboard scoreboard = mc.level.getScoreboard();
      Objective displayObjective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
      if (displayObjective == null) {
         return health;
      }

      String scoreboardName = livingEntity.getScoreboardName();

      for (PlayerScoreEntry playerScoreEntry : scoreboard.listPlayerScores(displayObjective)) {
         if (playerScoreEntry.owner().equals(scoreboardName)) {
            health[0] = playerScoreEntry.value();
            health[1] = SCOREBOARD_MAX_HEALTH;
            break;
         }
      }

      return health;
   }

   public static List<BlockPos> blocksAround(BlockPos pos, float radius, float height, boolean shell, boolean sphere, int yOffset, boolean all) {
      ArrayList blocks = new ArrayList();
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();

      for (int i = x - (int)radius; i <= x + radius; i++) {
         for (int j = z - (int)radius; j <= z + radius; j++) {
            int minY = sphere ? y - (int)radius : y;
            int maxY = sphere ? y + (int)radius : y + (int)height;

            for (int k = minY; k < maxY; k++) {
               if (isInSphere(x, y, z, i, k, j, radius, shell) || all) {
                  blocks.add(new BlockPos(i, k + yOffset, j));
               }
            }
         }
      }

      return blocks;
   }

   private PlayerUtils() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static double distanceTo(Entity entity, BlockPos pos) {
      return GuiHitTestUtils.distance(entity.getX(), entity.getY(), entity.getZ(), pos.getX(), pos.getY(), pos.getZ());
   }

   public static double clamp(double min, double max, double value) {
      return Math.clamp(value, min, max);
   }

   public static Component teamName(PlayerTeam playerTeam) {
      return teamName(playerTeam, true, true, true);
   }

   public static boolean sendSilently(Packet<?> packet) {
      return ess.ll.client.module.combat.CombatRuntime.sendWithoutEvents(packet);
   }

   public static Component displayName(LivingEntity livingEntity) {
      return displayName(livingEntity, true, true, true);
   }

   private static Component joinName(Component prefix, Component name, Component suffix) {
      MutableComponent mutableComponent = Component.empty();
      if (prefix != null) {
         String prefixText = prefix.getString();
         if (prefixText.length() < 64) {
            mutableComponent.append(prefix);
         }
      }

      if (name != null) {
         mutableComponent.append(name);
      }

      if (suffix != null) {
         String suffixText = suffix.getString();
         if (suffixText.length() < 64) {
            String text = suffix.getString().replace('&', '§');
            if (!ChatFormatting.stripFormatting(text).isBlank()) {
               mutableComponent.append(suffix);
            }
         }
      }

      return mutableComponent;
   }

   public static boolean isInView(Entity entity) {
      if (entity == null) {
         return false;
      }

      if (entity.getBoundingBox() == null) {
         return false;
      }

      if (mc.getEntityRenderDispatcher() == null) {
         return false;
      }

      Frustum frustum = mc.levelRenderer instanceof LevelRendererAccess access
            ? access.frustum() : ess.ll.client.event.Render3DEvent.INSTANCE.getFrustum();
      return frustum == null || frustum.isVisible(entity.getBoundingBox());
   }

   public static Vector2f rotationTo(Vec3 from, Vec3 to) {
      Vec3 delta = to.subtract(from);
      double horizontal = Math.hypot(delta.x(), delta.z());
      float yaw = (float)(Mth.atan2(delta.z(), delta.x()) * HALF_TURN_YAW / PI_YAW) - YAW_OFFSET;
      float pitch = (float)(-(Mth.atan2(delta.y(), horizontal) * HALF_TURN_PITCH / PI_PITCH));
      yaw = wrapDegrees(yaw);
      pitch = (float)clamp(MIN_PITCH, MAX_PITCH, pitch);
      return new Vector2f(yaw, pitch);
   }

   public static boolean isInView(AABB aabb) {
      if (aabb == null) {
         return false;
      }

      if (mc.getEntityRenderDispatcher() == null) {
         return false;
      }

      Frustum frustum = mc.levelRenderer instanceof LevelRendererAccess access
            ? access.frustum() : ess.ll.client.event.Render3DEvent.INSTANCE.getFrustum();
      return frustum == null || frustum.isVisible(aabb);
   }

   public static boolean isGliding() {
      return mc.player == null ? false : ((LivingEntityMixin_1248)(Object)mc.player).glidingtick() > 2;
   }

   public static Component teamName(PlayerTeam playerTeam, boolean withPrefix, boolean withName, boolean withSuffix) {
      boolean hasTeam = playerTeam != null;
      Component prefix = withPrefix && hasTeam ? playerTeam.getPlayerPrefix() : null;
      Component suffix = withSuffix && hasTeam ? playerTeam.getPlayerSuffix() : null;
      MutableComponent name = null;
      if (withName) {
         MutableComponent teamDisplay = hasTeam ? playerTeam.getFormattedDisplayName() : null;
         TextColor textColor = teamDisplay != null ? teamDisplay.getStyle().getColor() : null;
         MutableComponent literal = Component.literal(firstTeamMember(playerTeam));
         if (textColor != null) {
            literal.setStyle(Style.EMPTY.withColor(textColor));
         }

         name = literal;
      }

      return joinName(prefix, name, suffix);
   }

   public static float health(LivingEntity livingEntity) {
      return ModuleManager.INSTANCE.scoreboardHealth().isEnabled() && livingEntity instanceof Player ? scoreboardHealth(livingEntity)[0] : livingEntity.getHealth();
   }

   public static boolean isInvalidName(String text) {
      return text == null ? true : !NAME_PATTERN.matcher(text).matches();
   }

   public static float wrapDegrees(float degrees) {
      return Mth.wrapDegrees(degrees);
   }

   public static BlockPos blockPosition() {
      return mc.player == null ? BlockPos.ZERO : BlockPos.containing(Math.floor(mc.player.getX()), Math.floor(mc.player.getY()), Math.floor(mc.player.getZ()));
   }

   public static ItemStack mainHandItem(LivingEntity livingEntity) {
      if (livingEntity instanceof Player player) {
         return renderedMainHand(player);
      } else {
         return livingEntity == null ? ItemStack.EMPTY : livingEntity.getMainHandItem();
      }
   }

   public static Component displayName(LivingEntity livingEntity, boolean withPrefix, boolean withName, boolean withSuffix) {
      PlayerTeam team = livingEntity.getTeam();
      boolean hasTeam = team != null;
      Component prefix = withPrefix && hasTeam ? team.getPlayerPrefix() : null;
      Component suffix = withSuffix && hasTeam ? team.getPlayerSuffix() : null;
      MutableComponent name = null;
      if (withName) {
         TextColor color = livingEntity.getDisplayName().getStyle().getColor();
         MutableComponent literal = Component.literal(livingEntity.getName().getString());
         if (color != null) {
            literal.setStyle(Style.EMPTY.withColor(color));
         }

         name = literal;
      }

      return joinName(prefix, name, suffix);
   }

   public static void dropAll(Minecraft client) {
      if (client != null && client.player != null && client.gameMode != null) {
         for (int i = 0; i < client.player.containerMenu.slots.size(); i++) {
            if (!client.player.containerMenu.slots.get(i).getItem().isEmpty()) {
               client.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 1, ContainerInput.THROW, client.player);
            }
         }
      }
   }

   public static String firstTeamMember(PlayerTeam playerTeam) {
      if (playerTeam == null) {
         return "";
      } else {
         var players = playerTeam.getPlayers();
         if (players != null && !players.isEmpty()) {
            String text = (String)players.iterator().next();
            return text == null ? "" : text;
         } else {
            return "";
         }
      }
   }

   public static BlockPos findNearestBlock(float radius, Block block) {
      return blocksAround(blockPosition(), radius, SEARCH_HEIGHT, false, true, 0, false)
         .stream()
         .filter(pos -> mc.level.getBlockState(pos).getBlock() == block)
         .min(Comparator.comparing(candidate -> distanceTo(mc.player, candidate)))
         .orElse(null);
   }

   public static boolean isGlidingFor(int ticks) {
      return mc.player == null ? false : ((LivingEntityMixin_1248)(Object)mc.player).glidingtick() > ticks;
   }

   public static int ping(Player player) {
      if (mc.getConnection() != null && player != null) {
         PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
         return playerInfo == null ? -1 : playerInfo.getLatency();
      } else {
         return -1;
      }
   }
}
