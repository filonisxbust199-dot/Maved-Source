package ess.ll.client.event;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HeldItemRenderEvent extends Event {
   public static final HeldItemRenderEvent INSTANCE = new HeldItemRenderEvent();
   private HeldItemRenderEvent.Kind kind;
   private int originalSlot;
   private int slot;
   private Player player;
   private LivingEntity livingEntity;
   private HumanoidArm arm;
   private ItemStack originalStack;
   private ItemStack stack;

   public ItemStack fireStackInArm(LivingEntity livingEntity, HumanoidArm humanoidArm, ItemStack stack) {
      if (!this.hasListeners()) {
         return stack == null ? ItemStack.EMPTY : stack;
      }

      this.kind = HeldItemRenderEvent.Kind.STACK_IN_ARM;
      this.player = null;
      this.livingEntity = livingEntity;
      this.arm = humanoidArm;
      this.originalStack = stack == null ? ItemStack.EMPTY : stack;
      this.stack = this.originalStack;
      super.dispatch();
      return this.stack;
   }

   public int fireSelectedSlot(int selectedSlot) {
      if (!this.hasListeners()) {
         return selectedSlot;
      }

      this.kind = HeldItemRenderEvent.Kind.SELECTED_SLOT;
      this.originalSlot = selectedSlot;
      this.slot = selectedSlot;
      this.originalStack = ItemStack.EMPTY;
      this.stack = ItemStack.EMPTY;
      super.dispatch();
      return this.slot;
   }

   public void setOriginalStack(ItemStack stack) {
      this.originalStack = stack;
   }

   public void setPlayer(Player player) {
      this.player = player;
   }

   public LivingEntity getLivingEntity() {
      return this.livingEntity;
   }

   public HumanoidArm getArm() {
      return this.arm;
   }

   public ItemStack getOriginalStack() {
      return this.originalStack;
   }

   public int getSlot() {
      return this.slot;
   }

   public ItemStack fireMainHandStack(Player player, ItemStack stack) {
      if (!this.hasListeners()) {
         return stack == null ? ItemStack.EMPTY : stack;
      }

      this.kind = HeldItemRenderEvent.Kind.MAIN_HAND_STACK;
      this.player = player;
      this.originalStack = stack == null ? ItemStack.EMPTY : stack;
      this.stack = this.originalStack;
      super.dispatch();
      return this.stack;
   }

   public void setArm(HumanoidArm humanoidArm) {
      this.arm = humanoidArm;
   }

   public void setKind(HeldItemRenderEvent.Kind kind) {
      this.kind = kind;
   }

   public HeldItemRenderEvent.Kind getKind() {
      return this.kind;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public void setOriginalSlot(int originalSlot) {
      this.originalSlot = originalSlot;
   }

   public void setLivingEntity(LivingEntity livingEntity) {
      this.livingEntity = livingEntity;
   }

   public Player getPlayer() {
      return this.player;
   }

   public int getOriginalSlot() {
      return this.originalSlot;
   }

   public void setSlot(int slot) {
      this.slot = slot;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public enum Kind {
      SELECTED_SLOT,
      MAIN_HAND_STACK,
      STACK_IN_ARM;
   }
}
