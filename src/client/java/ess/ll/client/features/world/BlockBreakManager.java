package ess.ll.client.features.world;

import ess.ll.client.core.HitResultConstraint;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector2f;
import ess.ll.client.mixin.MultiPlayerGameModeMixin_1262;

public enum BlockBreakManager implements MinecraftAccess {
    INSTANCE;

    private final HitResultConstraint hitResultConstraint = new HitResultConstraint();
    private Object owner;
    private BlockPos targetPosition;
    private int lastKeepAliveTick = -100;
    private int lastRequestTick = -100;
    private int lastProgressTick = -100;
    private float breakProgress;
    private BlockBreakManager.MiningState state = BlockBreakManager.MiningState.IDLE;
    private BlockBreakManager.MiningPolicy policy = BlockBreakManager.MiningPolicy.COMBAT;
    private int ticks;
    private int lastActionTick = -100;
    private int rotationPriority;
    private Action lastAction;
    private BlockPos lastActionPosition;

    public void register() {
        EventBus.register(this);
    }

    public boolean requestBreak(Object var1, BlockPos blockPos) {
        return this.requestBreak(var1, blockPos, BlockBreakManager.MiningPolicy.COMBAT);
    }

    public boolean requestBreak(Object var1, BlockPos blockPos, BlockBreakManager.MiningPolicy var3) {
        return this.requestBreak(var1, blockPos, var3, null);
    }

    public boolean requestBreak(Object var1, BlockPos blockPos, BlockBreakManager.MiningPolicy var3, Vec3 vector) {
        if (mc.player == null || mc.level == null || blockPos == null) {
            return false;
        }
        if (this.owner != null && this.owner != var1) {
            return false;
        }
        if (yieldsToManualAttack(var3)) {
            return false;
        }
        if (!blockPos.equals(this.targetPosition)) {
            this.targetPosition = blockPos.immutable();
            this.breakProgress = 0.0F;
            this.lastProgressTick = mc.player.tickCount;
            this.state = BlockBreakManager.MiningState.IDLE;
        }
        this.policy = var3;
        this.owner = var1;
        this.lastKeepAliveTick = mc.player.tickCount;
        this.lastRequestTick = mc.player.tickCount;
        int var5 = this.needsFinishPriority() ? var3.finishPriority : var3.miningPriority;
        if (var5 < this.rotationPriority) {
            RotationManager.releasePriority(this.rotationPriority);
        }
        this.rotationPriority = var5;
        this.requestRotation(var1, var5, vector == null ? this.aimPoint(this.targetPosition) : vector);
        return true;
    }

    private void requestRotation(Object var1, int var2, Vec3 vector) {
        Vector2f vector2 = this.rotationTo(vector);
        if (this.policy == BlockBreakManager.MiningPolicy.COMBAT) {
            BlockActionQueue.requestRotation(var1, vector2, var2);
        } else {
            RotationManager.rotate(new Rotation(vector2.x, vector2.y), 180.0F, var2, false);
        }
    }

    private static boolean yieldsToManualAttack(BlockBreakManager.MiningPolicy var0) {
        return var0.yieldsToManualAttack && mc.options.keyAttack.isDown();
    }

    public boolean isMining() {
        return this.targetPosition != null && this.state == BlockBreakManager.MiningState.MINING;
    }

    private boolean needsFinishPriority() {
        if (!this.isMining()) {
            return false;
        }
        MultiPlayerGameModeMixin_1262 gameMode2 = (MultiPlayerGameModeMixin_1262) (Object) mc.gameMode;
        boolean var2 = gameMode2.getIsDestroying() && this.targetPosition.equals(gameMode2.getDestroyBlockPos());
        return !var2 || this.willBreakNextTick();
    }

    public int rotationPriority() {
        return this.rotationPriority;
    }

    public String lastActionDescription() {
        return this.lastAction == null ? "-" : this.lastAction + "@" + this.lastActionPosition.toShortString() + " gt=" + this.lastActionTick;
    }

    public boolean willBreakNextTick() {
        return this.isMining() && mc.player != null && mc.level != null ? this.breakProgress + this.blockBreakProgress(this.targetPosition) >= 1.0F : false;
    }

    public float breakProgressPerTick() {
        return this.targetPosition != null && mc.player != null && mc.level != null ? this.blockBreakProgress(this.targetPosition) : 0.0F;
    }

    private float blockBreakProgress(BlockPos blockPos) {
        return mc.level.getBlockState(blockPos).getDestroyProgress(mc.player, mc.level, blockPos);
    }

    public void keepAlive(Object var1) {
        if (this.owner == var1 && this.targetPosition != null && mc.player != null) {
            this.lastKeepAliveTick = mc.player.tickCount;
        }
    }

    public void release(Object var1) {
        if (this.owner == null || this.owner == var1) {
            this.reset();
        }
    }

    public boolean hasTarget() {
        return this.targetPosition != null;
    }

    public boolean isOwnedBy(Object var1) {
        return this.owner == var1 && this.targetPosition != null;
    }

    public boolean canMineTarget() {
        return this.targetPosition != null && mc.player != null && mc.level != null && !mc.options.keyAttack.isDown() && isBreakableBlock(mc.level.getBlockState(this.targetPosition)) && this.isLookingAtTarget();
    }

    public boolean sentActionThisTick() {
        return this.ticks == this.lastActionTick;
    }

    public boolean continueMining() {
        if (!this.canMineTarget() || mc.gameMode == null) {
            return false;
        } else if (mc.gui.screen() == null && mc.gui.overlay() == null && mc.mouseHandler.isMouseGrabbed()) {
            return true;
        } else if (mc.player.isUsingItem()) {
            return true;
        } else if (mc.player.getMainHandItem().has(DataComponents.PIERCING_WEAPON)) {
            return true;
        } else if (!(mc.hitResult instanceof BlockHitResult var1 && this.targetPosition.equals(var1.getBlockPos()))) {
            return true;
        } else {
            if (mc.level.getBlockState(var1.getBlockPos()).isAir()) {
                return true;
            }
            if (mc.gameMode.continueDestroyBlock(var1.getBlockPos(), var1.getDirection())) {
                mc.level.addBreakingBlockEffects(var1.getBlockPos(), var1.getDirection(), false);
                mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            }
            return true;
        }
    }

    public void lockHitResult() {
        if (this.targetPosition != null && this.owner != null && mc.player != null) {
            if (mc.player.tickCount - this.lastRequestTick <= 1) {
                this.hitResultConstraint.lockTo(this.targetPosition);
            }
        }
    }

    public void clearHitResultLock() {
        this.hitResultConstraint.clear();
    }

    public BlockPos targetPosition() {
        return this.targetPosition;
    }

    /**
     * Match the original constrained raycast while mining a selected block.
     */
    public BlockHitResult miningHit() {
        if (this.owner == null || this.targetPosition == null || mc.player == null || mc.level == null || !isBreakableBlock(mc.level.getBlockState(this.targetPosition)))
            return null;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 aim = Vec3.atCenterOf(this.targetPosition);
        if (eye.distanceTo(aim) > mc.player.blockInteractionRange() + .5)
            return null;
        Vec3 ray = aim.subtract(eye), direction = ray.normalize(), start = eye;
        for (int i = 0; i < 64 && start.distanceToSqr(eye) <= ray.lengthSqr() + .01; i++) {
            BlockHitResult hit = mc.level.clip(new ClipContext(start, aim, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK)
                return null;
            if (hit.getBlockPos().equals(this.targetPosition))
                return hit;
            BlockPos blocked = hit.getBlockPos();
            Vec3 at = hit.getLocation();
            double exit = Double.POSITIVE_INFINITY;
            if (direction.x > 1e-8)
                exit = Math.min(exit, (blocked.getX() + 1 - at.x) / direction.x);
            else if (direction.x < -1e-8)
                exit = Math.min(exit, (blocked.getX() - at.x) / direction.x);
            if (direction.y > 1e-8)
                exit = Math.min(exit, (blocked.getY() + 1 - at.y) / direction.y);
            else if (direction.y < -1e-8)
                exit = Math.min(exit, (blocked.getY() - at.y) / direction.y);
            if (direction.z > 1e-8)
                exit = Math.min(exit, (blocked.getZ() + 1 - at.z) / direction.z);
            else if (direction.z < -1e-8)
                exit = Math.min(exit, (blocked.getZ() - at.z) / direction.z);
            if (!Double.isFinite(exit))
                return null;
            start = at.add(direction.scale(Math.max(0, exit) + 1e-4));
        }
        return null;
    }

    public float breakProgress() {
        return this.breakProgress;
    }

    public BlockBreakManager.MiningState state() {
        return this.state;
    }

    public boolean isStalled() {
        return mc.player != null && this.targetPosition != null && elapsedTicks(mc.player.tickCount, this.lastProgressTick) > 20;
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        this.hitResultConstraint.apply(event);
    }

    @Subscribe(0)
    private void onTick(TickEvent event) {
        this.ticks++;
    }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if (event.isSend()) {
            if (event.getPacket() instanceof ServerboundPlayerActionPacket var2) {
                switch (var2.getAction()) {
                    case START_DESTROY_BLOCK:
                    case ABORT_DESTROY_BLOCK:
                    case STOP_DESTROY_BLOCK:
                        this.lastActionTick = this.ticks;
                        this.lastAction = var2.getAction();
                        this.lastActionPosition = var2.getPos();
                }
            }
        }
    }

    @Subscribe(3)
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            this.reset();
        } else if (this.targetPosition != null) {
            if (yieldsToManualAttack(this.policy) || elapsedTicks(mc.player.tickCount, this.lastKeepAliveTick) > 1) {
                this.reset();
            } else if (!isBreakableBlock(mc.level.getBlockState(this.targetPosition))) {
                this.state = BlockBreakManager.MiningState.BROKEN;
            } else {
                this.updateBreakProgress();
                if (!this.isLookingAtTarget()) {
                    this.state = this.isStalled() ? BlockBreakManager.MiningState.STUCK : BlockBreakManager.MiningState.AIMING;
                } else {
                    this.state = this.isStalled() ? BlockBreakManager.MiningState.STUCK : BlockBreakManager.MiningState.MINING;
                }
            }
        }
    }

    public int destroyDelay() {
        return mc.gameMode == null ? 0 : ((MultiPlayerGameModeMixin_1262) (Object) mc.gameMode).getDestroyDelay();
    }

    private boolean isLookingAtTarget() {
        return mc.hitResult instanceof BlockHitResult var1 && this.targetPosition.equals(var1.getBlockPos());
    }

    private void updateBreakProgress() {
        MultiPlayerGameModeMixin_1262 gameMode2 = (MultiPlayerGameModeMixin_1262) (Object) mc.gameMode;
        float var2 = this.targetPosition.equals(gameMode2.getDestroyBlockPos()) ? Mth.clamp(gameMode2.getDestroyProgress(), 0.0F, 1.0F) : 0.0F;
        if (var2 > this.breakProgress + 1.0E-6F) {
            this.lastProgressTick = mc.player.tickCount;
        }
        this.breakProgress = var2;
    }

    public Vec3 aimPoint(BlockPos blockPos) {
        return BlockPlacementManager.usesMatrixRotation() ? BlockPlacementManager.randomizedBlockAim(blockPos) : Vec3.atCenterOf(blockPos);
    }

    private Vector2f rotationTo(Vec3 vector) {
        Vector2f vector2 = PlayerUtils.rotationTo(mc.player.getEyePosition(), vector);
        return BlockPlacementManager.usesMatrixRotation() ? BlockPlacementManager.quantizeRotation(vector2) : vector2;
    }

    private static boolean isBreakableBlock(BlockState blockState) {
        return !blockState.isAir() && !(blockState.getBlock() instanceof LiquidBlock);
    }

    private void reset() {
        if (this.rotationPriority > 0) {
            RotationManager.releasePriority(this.rotationPriority);
        }
        this.rotationPriority = 0;
        this.policy = BlockBreakManager.MiningPolicy.COMBAT;
        this.hitResultConstraint.clear();
        this.owner = null;
        this.targetPosition = null;
        this.lastKeepAliveTick = -100;
        this.lastRequestTick = -100;
        this.lastProgressTick = -100;
        this.breakProgress = 0.0F;
        this.state = BlockBreakManager.MiningState.IDLE;
    }

    private static int elapsedTicks(int var0, int var1) {
        int var2 = var0 - var1;
        return var2 < 0 ? Integer.MAX_VALUE : var2;
    }

    public enum MiningPolicy {
        COMBAT(62, 65, true), UTILITY(999, 999, false);

        private final int miningPriority;
        private final int finishPriority;
        private final boolean yieldsToManualAttack;

        MiningPolicy(int var3, int var4, boolean var5) {
            this.miningPriority = var3;
            this.finishPriority = var4;
            this.yieldsToManualAttack = var5;
        }
    }

    public enum MiningState {
        IDLE, AIMING, MINING, BROKEN, STUCK
    }
}
