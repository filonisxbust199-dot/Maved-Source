package ess.ll.client.features.crystalv2;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.network.PacketEvent;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Relative;

/** What the server last heard from this client: position, look, the look at the last place, teleports. */
public final class V2Packets {
   public static final V2Packets INSTANCE = new V2Packets();
   public int flagTicks;
   public double LastPosX;
   public double LastPosY;
   public double LastPosZ;
   public float LastYaw;
   public float LastPitch;
   public float PreviousYaw;
   public float PreviousPitch;
   public boolean LastGround;
   public boolean forwardPressed;
   public boolean backPressed;
   public boolean leftPressed;
   public boolean rightPressed;
   public boolean sneakPressed;
   public boolean clickedButNotClosedWithPacket;
   public double yawPlace;
   public double pitchPlace;
   public Simulated simulated;
   public Simulated simulated2;
   public Simulated simulated3;
   private Connection protocolConnection;
   private int protocol = -1;

   private V2Packets() {
      EventBus.register(this);
   }

   @Subscribe
   private void onTick(TickEvent event) {
      this.flagTicks++;
   }

   @Subscribe
   private void onInput(PlayerInputEvent event) {
      this.forwardPressed = event.getForward() >= 0.98F;
      this.backPressed = event.getForward() <= -0.98F;
      this.leftPressed = event.getStrafe() >= 0.98F;
      this.rightPressed = event.getStrafe() <= -0.98F;
      this.sneakPressed = event.isSneak();
   }

   @Subscribe
   private void onPacket(PacketEvent event) {
      switch (event.getPacket()) {
         case ClientboundPlayerPositionPacket pos -> {
            var change = pos.change();
            var flags = pos.relatives();
            this.flagTicks = 0;
            this.LastPosX = flags.contains(Relative.X) ? this.LastPosX + change.position().x : change.position().x;
            this.LastPosY = flags.contains(Relative.Y) ? this.LastPosY + change.position().y : change.position().y;
            this.LastPosZ = flags.contains(Relative.Z) ? this.LastPosZ + change.position().z : change.position().z;
            float yaw = flags.contains(Relative.Y_ROT) ? this.LastYaw + change.yRot() : change.yRot();
            float pitch = flags.contains(Relative.X_ROT) ? this.LastPitch + change.xRot() : change.xRot();
            this.LastYaw = this.PreviousYaw = yaw;
            this.LastPitch = this.PreviousPitch = Mth.clamp(pitch, -90.0F, 90.0F);
         }
         case ServerboundMovePlayerPacket move when event.isSend() -> {
            if (move.hasPosition()) {
               this.LastPosX = move.getX(0.0);
               this.LastPosY = move.getY(0.0);
               this.LastPosZ = move.getZ(0.0);
            }
            if (move.hasRotation()) {
               this.updateRotation(move.getYRot(0.0F), move.getXRot(0.0F));
            }
            this.LastGround = move.isOnGround();
         }
         case ServerboundUseItemOnPacket place when event.isSend() -> {
            this.yawPlace = Math.abs(this.LastYaw - this.PreviousYaw);
            this.pitchPlace = Math.abs(this.LastPitch - this.PreviousPitch);
         }
         case ServerboundContainerClickPacket click when event.isSend() -> this.clickedButNotClosedWithPacket = true;
         case ServerboundContainerClosePacket close when event.isSend() -> this.clickedButNotClosedWithPacket = false;
         case ClientboundContainerClosePacket close -> this.clickedButNotClosedWithPacket = false;
         default -> {
         }
      }
   }

   public void updateRotation(float yaw, float pitch) {
      if (yaw != this.LastYaw || pitch != this.LastPitch) {
         this.PreviousYaw = this.LastYaw;
         this.PreviousPitch = this.LastPitch;
      }

      this.LastYaw = yaw;
      this.LastPitch = pitch;
   }

   public int viaVersionProtocol() {
      Minecraft mc = Minecraft.getInstance();
      Connection connection = mc.getConnection() == null ? null : mc.getConnection().getConnection();
      if (this.protocolConnection == connection && this.protocol != -1) {
         return this.protocol;
      }

      this.protocolConnection = connection;
      this.protocol = this.resolveViaVersionProtocol(connection);
      return this.protocol;
   }

   private int resolveViaVersionProtocol(Connection connection) {
      if (connection != null && connection.isMemoryConnection()) {
         return SharedConstants.getProtocolVersion();
      }

      boolean viaFabricPlusPresent = false;

      try {
         Class<?> implementation = Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
         viaFabricPlusPresent = true;

         try {
            Object instance = implementation.getMethod("impl").invoke(null);
            Object translation = implementation.getMethod("protocolTranslation").invoke(instance);
            Object version = connection == null
               ? translation.getClass().getMethod("targetVersion").invoke(translation)
               : translation.getClass().getMethod("targetVersion", Connection.class).invoke(translation, connection);
            return this.protocolId(version);
         } catch (NoSuchMethodException e) {
            Object instance = implementation.getField("INSTANCE").get(null);
            Object version = connection == null
               ? implementation.getMethod("getTargetVersion").invoke(instance)
               : implementation.getMethod("getTargetVersion", Connection.class).invoke(instance, connection);
            return this.protocolId(version);
         }
      } catch (ClassNotFoundException e) {
      } catch (ReflectiveOperationException | LinkageError e) {
         viaFabricPlusPresent = true;
      }

      try {
         Class<?> translator = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         viaFabricPlusPresent = true;
         Object version = translator.getMethod("getTargetVersion").invoke(null);
         return this.protocolId(version);
      } catch (ClassNotFoundException e) {
      } catch (ReflectiveOperationException | LinkageError e) {
         viaFabricPlusPresent = true;
      }

      return viaFabricPlusPresent ? -1 : SharedConstants.getProtocolVersion();
   }

   private int protocolId(Object version) throws ReflectiveOperationException {
      return version == null ? -1 : ((Number)version.getClass().getMethod("getVersion").invoke(version)).intValue();
   }
}
