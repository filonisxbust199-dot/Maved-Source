package ess.ll.client.network;

import ess.ll.client.event.Event;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class PacketEvent extends Event {
   public static final PacketEvent INSTANCE = new PacketEvent();
   private Packet<?> packet;
   private PacketEvent.Direction direction;
   private Connection connection;

   public void fireCancellable(Packet<?> packet, PacketEvent.Direction direction, CallbackInfo ci, Connection connection) {
      if (this.isBlocked(packet, direction, connection)) {
         ci.cancel();
      }
   }

   public boolean isBlocked(Packet<?> packet, PacketEvent.Direction direction, Connection connection) {
      return this.fire(packet, direction, connection) == null;
   }

   public void setConnection(Connection connection) {
      this.connection = connection;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setDirection(PacketEvent.Direction direction) {
      this.direction = direction;
   }

   public boolean isSend() {
      return this.direction == PacketEvent.Direction.SEND;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public Connection getConnection() {
      return this.connection;
   }

   public PacketEvent.Direction getDirection() {
      return this.direction;
   }

   public Packet<?> fire(Packet<?> packet, PacketEvent.Direction direction, Connection connection) {
      PacketEvent packetEvent = new PacketEvent();
      packetEvent.packet = packet;
      packetEvent.direction = direction;
      packetEvent.connection = connection;
      if (true) {
         packetEvent.dispatch();
      }

      return packetEvent.consumeCancelled() ? null : packetEvent.packet;
   }

   public boolean isReceive() {
      return this.direction == PacketEvent.Direction.RECEIVE;
   }

   public enum Direction {
      RECEIVE,
      SEND;
   }
}
