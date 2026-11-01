package ess.ll.client.event;

import java.util.List;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class VisualRemovalEvent extends Event {
   public static final VisualRemovalEvent INSTANCE = new VisualRemovalEvent();
   private VisualRemovalEvent.Type type;
   private List<?> list;

   public boolean cancelIfRemoved(VisualRemovalEvent.Type type, CallbackInfo ci) {
      if (this.isRemoved(type)) {
         ci.cancel();
         return true;
      } else {
         return false;
      }
   }

   public FogType filterFog(FogType fogType, VisualRemovalEvent.Type type) {
      return this.isRemoved(type) ? FogType.NONE : fogType;
   }

   public void filterList(VisualRemovalEvent.Type type, List<?> list) {
      if (this.hasListeners()) {
         this.list = list;
         this.isRemoved(type);
         this.list = null;
      }
   }

   public VisualRemovalEvent.Type getType() {
      return this.type;
   }

   public void setType(VisualRemovalEvent.Type type) {
      this.type = type;
   }

   public void setList(List<?> list) {
      this.list = list;
   }

   public boolean isRemoved(VisualRemovalEvent.Type type) {
      if (!this.hasListeners()) {
         return false;
      }

      this.type = type;
      super.dispatch();
      return this.consumeCancelled();
   }

   public void overrideIfRemoved(VisualRemovalEvent.Type type, CallbackInfoReturnable<Float> cir, float value) {
      if (this.isRemoved(type)) {
         cir.setReturnValue(value);
      }
   }

   public List<?> getList() {
      return this.list;
   }

   public enum Type {
      FIRE,
      NOHURTCAM,
      F5_XUETA,
      SCOREBOARD,
      ITEM_TITLES,
      TOTEM,
      OVERLAY_MESSAGE,
      MINECRAFT_EFFECTS,
      WATER_FOG,
      LAVA_FOG,
      NIGHT_VISION_FOG,
      LAG_MACHINE;
   }
}
