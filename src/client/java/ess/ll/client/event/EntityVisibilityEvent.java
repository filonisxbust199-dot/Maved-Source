package ess.ll.client.event;

import net.minecraft.world.entity.Entity;

public class EntityVisibilityEvent extends Event {
   public static final EntityVisibilityEvent INSTANCE = new EntityVisibilityEvent();
   private Entity entity;
   private boolean visible;

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public boolean fire(Entity entity, boolean visible) {
      if (!this.hasListeners()) {
         return visible;
      }

      this.visible = true;
      this.entity = entity;
      super.dispatch();
      return !this.consumeCancelled() ? visible : visible && this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }
}
