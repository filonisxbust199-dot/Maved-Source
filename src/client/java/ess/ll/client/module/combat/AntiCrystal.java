package ess.ll.client.module.combat;

import com.mojang.datafixers.util.Pair;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.CombatTargetUtils;
import ess.ll.client.features.crystalv2.CalculateFovUtil;
import ess.ll.client.features.crystalv2.DidRayTraceHitUtil;
import ess.ll.client.features.crystalv2.IsInWebUtil;
import ess.ll.client.features.crystalv2.LastData;
import ess.ll.client.features.crystalv2.Rotation;
import ess.ll.client.features.crystalv2.V2Packets;
import ess.ll.client.features.crystalv2.V2Visuals;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** From maved-26.2: covers the obsidian near you an enemy could put a crystal on with a block of your own. */
public class AntiCrystal extends ClientModule {
   private static final int ROTATION_PRIORITY = 1001;
   private static final double TARGET_RANGE = 14.0;
   private final V2Packets pc = V2Packets.INSTANCE;
   private final NumberSetting<Float> minDamage = new NumberSetting<Float>("Мин дамаг по себе").withMin(1.0F).withMax(20.0F).withStep(0.5F)
      .withDefault(8.0F).withContainer(this);
   private final V2Visuals visuals = new V2Visuals();
   private Rotation rotation;
   private LastData lastData;

   public AntiCrystal() {
      super(ModuleCategory.Combat, ModuleSubCategory.Utility);
      this.setName("AntiCrystal");
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.rotation = null;
      this.lastData = null;
   }

   @Subscribe
   private void onPostInput(PostPlayerInputEvent event) {
      if (this.rotation != null && this.pc.viaVersionProtocol() >= 768) {
         this.rotate(this.rotation);
      }
   }

   @Subscribe
   private void onInput(PlayerInputEvent event) {
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         return;
      }

      this.lastData = null;
      this.rotation = null;
      Player target = this.findTarget();
      int slot = this.blockSlot();
      if (slot == -1 || target == null) {
         return;
      }

      LocalPlayer self = mc.player;
      this.lastData = this.findDangerPos(target);
      BlockPos blockPos = this.lastData.blockPos();
      Direction direction = this.lastData.face();
      if (blockPos == null || direction == null) {
         this.lastData = null;
         return;
      }

      Rotation rot = CalculateFovUtil.calculate(IsInWebUtil.getNearestFacePoint(blockPos, direction, mc.player.getEyePosition(1.0F)));
      float look = Math.abs(this.pc.LastYaw - this.pc.PreviousYaw);
      if (Math.abs(look - this.pc.yawPlace) < 1.0E-4F && this.pc.yawPlace != 0.0 && look != 0.0F) {
         rot.setYaw(rot.getYaw() + 0.005F);
      }

      this.rotate(rot);
      if (Math.abs(look - this.pc.yawPlace) >= 1.0E-4F || this.pc.yawPlace == 0.0 || look == 0.0F || look <= 2.0F) {
         boolean legacy = this.pc.viaVersionProtocol() > 0 && this.pc.viaVersionProtocol() < 768;
         if (legacy) {
            mc.getConnection().send(new PosRot(this.pc.LastPosX, this.pc.LastPosY, this.pc.LastPosZ,
               mc.player.getYRot() + -Mth.wrapDegrees(mc.player.getYRot() - rot.getYaw()), rot.getPitch(), this.pc.LastGround, false));
         }

         int prevSlot = self.getInventory().getSelectedSlot();
         self.getInventory().setSelectedSlot(slot);
         mc.gameMode.useItemOn(self, InteractionHand.MAIN_HAND, new BlockHitResult(DidRayTraceHitUtil.lookToBlock(rot, blockPos, 8.0), direction, blockPos, false));
         self.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
         if (prevSlot != slot) {
            self.getInventory().setSelectedSlot(prevSlot);
         }

         if (legacy) {
            mc.getConnection().send(new PosRot(this.pc.LastPosX, this.pc.LastPosY, this.pc.LastPosZ, mc.player.getYRot(), mc.player.getXRot(), this.pc.LastGround, false));
         }

         if (!this.visuals.has(blockPos)) {
            this.visuals.add(blockPos.relative(direction), 0.2F, 0.8F, 0.2F);
         }

         this.lastData = null;
         this.rotation = rot;
      }
   }

   private void rotate(Rotation rotation) {
      RotationManager.rotate(new ess.ll.client.util.Rotation(rotation.getYaw(), rotation.getPitch()), 360.0F, ROTATION_PRIORITY, false);
      RotationManager.moveCorrectionRequested = true;
   }

   private Player findTarget() {
      var antiBot = ModuleManager.INSTANCE.antiBot();
      Player best = null;
      double bestDistance = TARGET_RANGE;

      for (Player player : mc.level.players()) {
         double distance = mc.player.distanceTo(player);
         if (distance <= bestDistance && CombatTargetUtils.isEnemy(player, antiBot)) {
            best = player;
            bestDistance = distance;
         }
      }

      return best;
   }

   private int blockSlot() {
      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getItem(slot).getItem() instanceof BlockItem block
            && block.getBlock().defaultBlockState().isSolid()
            && block.getBlock() != Blocks.COBWEB) {
            return slot;
         }
      }

      return -1;
   }

   public LastData findDangerPos(Player target) {
      BlockPos bestPanPos = null;
      double bestSelfDamage = 0.0;
      Direction bestFace = null;
      BlockPos basePos = mc.player.blockPosition();
      MutableBlockPos checkPos = new MutableBlockPos();

      for (int x = -5; x <= 5; x++) {
         int px = basePos.getX() + x;

         for (int y = -5; y <= 5; y++) {
            int py = basePos.getY() + y;

            for (int z = -5; z <= 5; z++) {
               checkPos.set(px, py, basePos.getZ() + z);
               if (!(checkPos.getY() >= mc.player.getY() - 0.5)) {
                  BlockState state = mc.level.getBlockState(checkPos);
                  if (state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK) {
                     Pair<Double, Direction> face = IsInWebUtil.getBestVisibleFaceAntiCrystal(checkPos, mc.player, this.minDamage.getValue().floatValue(), true);
                     if (face.getFirst() != 0.0 && face.getSecond() != null) {
                        Vec3 rotatePoint = IsInWebUtil.getFacePoint(checkPos, face.getSecond());
                        var aim = target.getInterpolation() == null ? null : target.getInterpolation().target();
                        Vec3 from = aim == null ? target.position() : aim.position();
                        Rotation rotTarget = CalculateFovUtil.calculate(from.add(0.0, target.getEyeHeight(target.getPose()), 0.0), rotatePoint);
                        BlockHitResult raycastTarget = DidRayTraceHitUtil.raycastBlock(rotTarget, target, checkPos, 6.0);
                        if (raycastTarget != null && !(face.getFirst() < bestSelfDamage)) {
                           bestSelfDamage = face.getFirst();
                           bestFace = face.getSecond();
                           bestPanPos = new BlockPos(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                        }
                     }
                  }
               }
            }
         }
      }

      return new LastData(bestPanPos, bestFace);
   }

   public LastData getLastData() {
      return this.isEnabled() ? this.lastData : null;
   }
}
