package ess.ll.client.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class RaycastEvent extends Event {
   public static final RaycastEvent INSTANCE = new RaycastEvent();
   private RaycastEvent.Type type;
   private Entity entity;
   private HitResult result;
   private double range;
   private float tickDelta;
   private boolean includeFluids;

   public Entity getEntity() {
      return this.entity;
   }

   public EntityHitResult fireEntity(HitResult hitResult) {
      this.type = RaycastEvent.Type.ENTITY;
      this.result = hitResult;
      super.dispatch();
      return this.result instanceof EntityHitResult range ? range : null;
   }

   public void fireBlock(Entity entity, double range, float tickDelta, boolean includeFluids, CallbackInfoReturnable<HitResult> cir) {
      this.type = RaycastEvent.Type.BLOCK;
      this.entity = entity;
      this.result = (HitResult)cir.getReturnValue();
      this.range = range;
      this.tickDelta = tickDelta;
      this.includeFluids = includeFluids;
      super.dispatch();
      if (this.consumeCancelled()) {
         cir.setReturnValue(this.result);
      }
   }

   public void setResult(HitResult hitResult) {
      this.result = hitResult;
   }

   public boolean isIncludeFluids() {
      return this.includeFluids;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public RaycastEvent.Type getType() {
      return this.type;
   }

   public double getRange() {
      return this.range;
   }

   public void setType(RaycastEvent.Type type) {
      this.type = type;
   }

   public void setIncludeFluids(boolean includeFluids) {
      this.includeFluids = includeFluids;
   }

   public void setRange(double range) {
      this.range = range;
   }

   public void setTickDelta(float tickDelta) {
      this.tickDelta = tickDelta;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public HitResult getResult() {
      return this.result;
   }

   public enum Type {
      BLOCK,
      ENTITY;
   }
}
