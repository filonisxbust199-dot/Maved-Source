package ess.ll.client.features.world;

import ess.ll.client.core.HitResultConstraint;
import ess.ll.client.event.EventBus;
import ess.ll.client.event.FrameUpdateEvent;
import ess.ll.client.event.HitResultEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationChangeTracker;
import ess.ll.client.util.RotationManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector2f;

public class BlockTrapController implements MinecraftAccess {
    private static final List<Class<? extends Block>> INTERACTABLE_BLOCKS = List.of(DoorBlock.class, TrapDoorBlock.class, FenceGateBlock.class, ButtonBlock.class, LeverBlock.class, NoteBlock.class, CraftingTableBlock.class, AnvilBlock.class, LoomBlock.class, StonecutterBlock.class, CartographyTableBlock.class, GrindstoneBlock.class);
    private static BlockTrapController instance;
    private final HitResultConstraint hitConstraint = new HitResultConstraint();
    private final Deque<BlockTrapController.PlacementTask> queue = new ArrayDeque<>();
    private final List<BlockTrapController.PlacementTask> sortedQueue = new ArrayList<>();
    private final List<BlockPos> completedBlocks = new ArrayList<>(4);
    private boolean rotationPending;
    private boolean allowThroughWalls;
    private boolean silentRotation;
    private int ownerId;
    private int lastPlaceTick = -100;
    private BlockTrapController.PlacementTask currentTask;
    private BlockTrapController.PlacementSpot currentPlacement;

    private boolean isVisibleRotation() {
        return !this.silentRotation;
    }

    private boolean isLookingAtBlock(BlockPos blockPos) {
        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 vector = eyePosition.add(mc.player.getLookAngle().scale(4.5));
        return new AABB(blockPos).clip(eyePosition, vector).isPresent();
    }

    public static boolean isActive() {
        return instance != null && (!instance.queue.isEmpty() || instance.currentTask != null);
    }

    private BlockTrapController.PlacementSpot findPlacement(BlockPos blockPos, boolean var2) {
        if (blockPos != null && mc.player != null && mc.level != null) {
            if (!mc.level.getBlockState(blockPos).canBeReplaced()) {
                return null;
            }
            Vec3 eyePosition = mc.player.getEyePosition();
            float yRot = mc.player.getYRot();
            BlockTrapController.PlacementSpot var5 = null;
            float var6 = Float.MAX_VALUE;
            double var7 = Double.MAX_VALUE;
            for (Direction direction : Direction.values()) {
                BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
                if (isSolidNeighbor(blockPos2)) {
                    Vec3 vector = ExplosionDamageUtils.jitterOnFace(Vec3.atCenterOf(blockPos2).add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5), direction);
                    double distanceSquared = eyePosition.distanceToSqr(vector);
                    if (!(distanceSquared > 20.25)) {
                        Vector2f vector2;
                        if (this.silentRotation) {
                            vector2 = BlockPlacementManager.rotationToFacePoint(blockPos2, direction, vector);
                            if (vector2 == null) {
                                if (!var2 || !ExplosionDamageUtils.isFacingSide(eyePosition, vector, direction)) {
                                    continue;
                                }
                                vector2 = BlockPlacementManager.quantizeRotation(PlayerUtils.rotationTo(eyePosition, vector));
                                if (vector2 == null) {
                                    continue;
                                }
                            }
                        } else {
                            if (!this.isSideVisible(eyePosition, blockPos2, vector, direction, var2)) {
                                continue;
                            }
                            vector2 = PlayerUtils.rotationTo(eyePosition, vector);
                        }
                        vector2.x = yRot + Mth.wrapDegrees(vector2.x - yRot);
                        float var18 = this.rotationDistance(vector2);
                        if (var18 < var6 || Math.abs(var18 - var6) < 0.001 && distanceSquared < var7) {
                            var6 = var18;
                            var7 = distanceSquared;
                            var5 = new BlockTrapController.PlacementSpot(blockPos2, vector, direction, vector2);
                        }
                    }
                }
            }
            return var5;
        } else {
            return null;
        }
    }

    public static boolean hasAdjacentSupport(BlockPos blockPos) {
        if (mc.level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (isSolidNeighbor(blockPos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private boolean enqueue(List<BlockTrapController.PlacementTask> items, boolean var2, boolean var3, int var4, boolean var5, boolean var6) {
        if (mc.player != null && mc.level != null && items != null && !items.isEmpty()) {
            if (isActive() && var4 < this.ownerId) {
                return false;
            }
            this.queue.clear();
            for (BlockTrapController.PlacementTask var8 : items) {
                if (var8 != null && var8.pos != null && var8.item != null) {
                    this.queue.addLast(var8);
                }
            }
            if (this.queue.isEmpty()) {
                return false;
            }
            this.rotationPending = var2;
            this.allowThroughWalls = var3;
            this.silentRotation = var6;
            this.ownerId = var4;
            this.lastPlaceTick = mc.player.tickCount;
            if (var5) {
                this.tickQueue();
            }
            return true;
        } else {
            return false;
        }
    }

    private int compareByRotation(BlockTrapController.PlacementTask var1, BlockTrapController.PlacementTask var2) {
        Vec3 eyePosition = mc.player.getEyePosition();
        return Float.compare(this.rotationDistance(PlayerUtils.rotationTo(eyePosition, Vec3.atCenterOf(var1.pos))), this.rotationDistance(PlayerUtils.rotationTo(eyePosition, Vec3.atCenterOf(var2.pos))));
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            this.hitConstraint.clear();
            this.completedBlocks.clear();
            this.clear();
        } else if (!this.queue.isEmpty() && this.canPlaceNow()) {
            this.clear();
        } else {
            this.tickQueue();
        }
    }

    private void placeCurrent() {
        BlockTrapController.PlacementTask var1 = this.currentTask;
        BlockTrapController.PlacementSpot var2 = this.currentPlacement;
        this.currentTask = null;
        this.currentPlacement = null;
        if (var1 != null && var2 != null) {
            if (mc.level.getBlockState(var1.pos).canBeReplaced()) {
                if (this.isLookingAtBlock(var2.support)) {
                    BlockHitResult blockHitResult = this.toHitResult(var2);
                    if (blockHitResult != null) {
                        if (this.placeBlock(blockHitResult, var1.item, var2.support)) {
                            this.markPlaced(var1.pos);
                            if (this.isVisibleRotation()) {
                                int var4 = 0;
                                while (var4 < 3 && this.tryPlaceAtCrosshair()) {
                                    var4++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean placeBlock(BlockHitResult blockHitResult, Item item2, BlockPos blockPos) {
        int var4 = InventoryUtils.findItemIndex(item2, true);
        if (var4 == -1) {
            return false;
        }
        boolean var5 = this.silentRotation;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(var4);
        if (this.allowThroughWalls) {
            this.hitConstraint.lockTo(blockPos);
        }
        try {
            boolean var7 = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult).consumesAction();
            if (var7) {
                if (var5) {
                    mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                }
                RotationChangeTracker.record();
            }
            return var7;
        } finally {
            this.hitConstraint.clear();
            mc.player.getInventory().setSelectedSlot(selectedSlot);
        }
    }

    private BlockHitResult toHitResult(BlockTrapController.PlacementSpot var1) {
        if (this.allowThroughWalls) {
            return new BlockHitResult(var1.hitPosition, var1.side, var1.support, false);
        } else {
            return this.silentRotation ? BlockPlacementManager.hitFromAnyEyePosition(var1.support, var1.side) : this.rayToFace(mc.player.getEyePosition(), var1.hitPosition, var1.support, var1.side);
        }
    }

    private void markPlaced(BlockPos blockPos) {
        this.queue.removeIf(var1x -> blockPos.equals(var1x.pos));
        this.completedBlocks.add(blockPos);
    }

    public static void cancel(int var0) {
        if (instance != null && instance.ownerId == var0) {
            instance.clear();
        }
    }

    private boolean isSolidSupport(BlockPos blockPos) {
        if (blockPos != null && mc.level != null) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            if (blockState.isAir()) {
                return false;
            }
            if (blockState.hasBlockEntity()) {
                return true;
            }
            Block block = blockState.getBlock();
            for (Class var5 : INTERACTABLE_BLOCKS) {
                if (var5.isInstance(block)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Subscribe
    private void onHitResult(HitResultEvent event) {
        this.hitConstraint.apply(event);
    }

    private void tickQueue() {
        this.queue.removeIf(this::isAlreadyFilled);
        if (this.queue.isEmpty()) {
            this.ownerId = 0;
        } else {
            this.sortedQueue.clear();
            this.sortedQueue.addAll(this.queue);
            this.sortedQueue.sort(this::compareByRotation);
            BlockTrapController.PlacementTask var1 = null;
            BlockTrapController.PlacementSpot var2 = null;
            for (BlockTrapController.PlacementTask var4 : this.sortedQueue) {
                BlockTrapController.PlacementSpot var5 = this.findPlacement(var4.pos, this.allowThroughWalls);
                if (var5 != null) {
                    var1 = var4;
                    var2 = var5;
                    break;
                }
            }
            this.sortedQueue.clear();
            if (var1 == null) {
                this.ownerId = 0;
            } else {
                if (this.rotationPending) {
                    this.rotateTo(var2);
                }
                this.currentTask = var1;
                this.currentPlacement = var2;
            }
        }
    }

    public BlockTrapController() {
        EventBus.register(this);
        instance = this;
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (mc.player != null && mc.level != null && this.currentPlacement != null) {
            if (this.rotationPending) {
                RotationManager.moveCorrectionRequested = true;
            }
            if (this.isSolidSupport(this.currentPlacement.support)) {
                event.setSneak(true);
            }
        }
    }

    private static boolean isSolidNeighbor(BlockPos blockPos) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        return blockState.is(Blocks.COBWEB) || !blockState.canBeReplaced() && blockState.isRedstoneConductor(mc.level, blockPos);
    }

    private boolean isSideVisible(Vec3 vector, BlockPos blockPos, Vec3 vector2, Direction direction, boolean var5) {
        Vec3 vector3 = vector.subtract(vector2);
        Vec3 vector4 = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        if (vector3.dot(vector4) <= 0.01) {
            return false;
        } else {
            return var5 ? true : this.rayToFace(vector, vector2, blockPos, direction) != null;
        }
    }

    private BlockHitResult rayToFace(Vec3 vector, Vec3 vector2, BlockPos blockPos, Direction direction) {
        BlockHitResult blockHitResult = mc.level.clip(new ClipContext(vector, vector2, net.minecraft.world.level.ClipContext.Block.OUTLINE, Fluid.NONE, mc.player));
        if (blockHitResult.getType() != Type.BLOCK) {
            return null;
        } else {
            return blockHitResult.getBlockPos().equals(blockPos) && blockHitResult.getDirection() == direction ? blockHitResult : null;
        }
    }

    private void clear() {
        this.queue.clear();
        this.sortedQueue.clear();
        this.currentTask = null;
        this.currentPlacement = null;
        this.ownerId = 0;
    }

    private float rotationDistance(Vector2f vector) {
        float var2 = Math.abs(Mth.wrapDegrees(vector.x - mc.player.getYRot()));
        float var3 = Math.abs(vector.y - mc.player.getXRot());
        return var2 + var3 * 0.5F;
    }

    private boolean canPlaceNow() {
        int var1 = mc.player.tickCount - this.lastPlaceTick;
        return var1 < 0 || var1 > 2;
    }

    private boolean isAlreadyFilled(BlockTrapController.PlacementTask var1) {
        return !mc.level.getBlockState(var1.pos).canBeReplaced();
    }

    private void rotateTo(BlockTrapController.PlacementSpot var1) {
        RotationManager.rotate(new Rotation(Mth.wrapDegrees(var1.rotation.x), var1.rotation.y), 9999.0F, 63, false);
    }

    @Subscribe
    private void onFrameUpdate(FrameUpdateEvent event) {
        if (mc.player != null && mc.level != null) {
            if (this.currentTask != null && this.rotationPending) {
                BlockTrapController.PlacementSpot var2 = this.findPlacement(this.currentTask.pos, this.allowThroughWalls);
                if (var2 != null) {
                    this.currentPlacement = var2;
                    this.rotateTo(var2);
                }
            }
        }
    }

    public static boolean submit(List<BlockTrapController.PlacementTask> items, boolean var1, boolean var2, int var3, boolean var4, boolean var5) {
        return instance == null ? false : instance.enqueue(items, var1, var2, var3, var4, var5);
    }

    private boolean tryPlaceAtCrosshair() {
        if (this.queue.isEmpty()) {
            return false;
        } else if (!(mc.player.pick(4.5, 1.0F, false) instanceof BlockHitResult var2 && var2.getType() == Type.BLOCK)) {
            return false;
        } else {
            if (this.completedBlocks.contains(var2.getBlockPos())) {
                return false;
            }
            BlockPos blockPos = var2.getBlockPos().relative(var2.getDirection());
            if (!mc.level.getBlockState(blockPos).canBeReplaced()) {
                return false;
            }
            BlockTrapController.PlacementTask var4 = this.findTask(blockPos);
            if (var4 == null) {
                return false;
            }
            if (!this.placeBlock(var2, var4.item, var2.getBlockPos())) {
                return false;
            }
            this.markPlaced(blockPos);
            return true;
        }
    }

    @Subscribe
    private void onPostPlayerInput(PostPlayerInputEvent event) {
        if (mc.player != null && mc.level != null && mc.gameMode != null) {
            this.completedBlocks.clear();
            this.placeCurrent();
        }
    }

    private BlockTrapController.PlacementTask findTask(BlockPos blockPos) {
        for (BlockTrapController.PlacementTask var3 : this.queue) {
            if (blockPos.equals(var3.pos)) {
                return var3;
            }
        }
        return null;
    }

    public static class PlacementTask {
        private final BlockPos pos;
        private final Item item;

        public PlacementTask(BlockPos blockPos, Item item) {
            this.pos = blockPos;
            this.item = item;
        }
    }

    private static class PlacementSpot {
        private final BlockPos support;
        private final Vec3 hitPosition;
        private final Direction side;
        private final Vector2f rotation;

        private PlacementSpot(BlockPos blockPos, Vec3 vector, Direction direction, Vector2f vector2) {
            this.support = blockPos;
            this.hitPosition = vector;
            this.side = direction;
            this.rotation = vector2;
        }
    }
}
