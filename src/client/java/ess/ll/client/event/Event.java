package ess.ll.client.event;

public abstract class Event {
   private boolean cancelled = false;
   public EventBus.ListenerCache listenerCache;

   public void dispatch() {
      EventListener[] eventListeners = EventBus.listenersOf(this);

      for (int i = 0; i < eventListeners.length; i++) {
         eventListeners[i].fire(this);
      }
   }

   protected boolean consumeCancelled() {
      if (!this.cancelled) {
         return false;
      }

      this.cancelled = false;
      return true;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public final boolean hasListeners() {
      return EventBus.listenersOf(this).length != 0;
   }
}
