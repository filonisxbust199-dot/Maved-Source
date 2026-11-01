package ess.ll.client.event;

import net.minecraft.world.entity.Entity;

public class EntityOutlineColorEvent extends Event {
   public static final EntityOutlineColorEvent INSTANCE = new EntityOutlineColorEvent();
   private Entity entity;
   private int color;

   public int getColor() {
      return this.color;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public int fire(Entity entity, int color) {
      this.entity = entity;
      this.color = color;
      super.dispatch();
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
