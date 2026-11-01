package ess.ll.client.module.movement;

import ess.ll.client.event.CobwebCollisionEvent;
import ess.ll.client.event.EntityMoveEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.MovementUtils;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ess.ll.client.mixin.ClientLevelMixin_1241;
import ess.ll.client.mixin.EntityMixin_1159;

public class NoWeb extends ClientModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("QuickClient");
    private static final NoWeb.WebSpeeds LEGACY_SPEEDS = new NoWeb.WebSpeeds(0.078, 0.058, 0.056, 0.046, 0.054, 0.0499, 0.072);
    private static final NoWeb.WebSpeeds MODERN_SPEEDS = new NoWeb.WebSpeeds(0.115, 0.1, 0.068, 0.058, 0.064, 0.068, 0.085);
    private static final NoWeb.ModernWebSpeeds NEW_WEB_SPEEDS = new NoWeb.ModernWebSpeeds(0.15, 0.0599, 0.1599, 0.0108);
    private static final Vec3 WEB_SLOWDOWN = new Vec3(0.25, 0.05F, 0.25);
    private final ModeSetting mode;
    private boolean jumpInput;
    private boolean sneakInput;
    private boolean inWebNow;
    private Vec3 pendingMotion;
    private boolean breakQueued;
    private boolean movingUp;

    private NoWeb.WebSpeeds currentSpeeds() {
        return this.mode.is("1.17.1") ? LEGACY_SPEEDS : MODERN_SPEEDS;
    }

    private boolean isNoCollisionMode() {
        return this.mode.is("GrimCollision") || this.mode.is("NoCollision");
    }

    @Override
    public void onEnable() {
        this.jumpInput = false;
        this.sneakInput = false;
        this.inWebNow = false;
        super.onEnable();
    }

    @Subscribe
    private void onCobwebCollision(CobwebCollisionEvent event) {
        if (this.isNoCollisionMode()) {
            if (this.mode.is("GrimCollision")) {
                this.breakWeb(event.getPos().immutable());
            }
            event.cancel();
        }
    }

    private double sweepHorizontal(double var1, double[] var3, double var4) {
        if (!(var1 <= 0.0) && mc.player != null && mc.level != null) {
            AABB bounds = mc.player.getBoundingBox().move(var3[0] * var4, 0.0, var3[1] * var4);
            double var7 = this.ceilingAbove(bounds, var1) - 0.001 - bounds.minY;
            if (var7 >= var1) {
                return var1;
            } else {
                return var7 >= -this.pendingMotion.y ? var7 : Math.min(var1, this.pendingMotion.y);
            }
        } else {
            return var1;
        }
    }

    private double verticalSpeed(NoWeb.WebSpeeds var1) {
        if (!this.sneakInput && !this.isCriticalWindow()) {
            return this.isAirborne() ? var1.airborneHorizontal() : var1.groundHorizontal();
        } else {
            return var1.descendHorizontal();
        }
    }

    private double floorBelow(AABB bounds, double var2, double var4) {
        double minY2 = bounds.minY;
        double NEGATIVE_INFINITY2 = Double.NEGATIVE_INFINITY;
        MutableBlockPos blockPos = new MutableBlockPos();
        int var11 = Mth.floor(minY2 + 0.6);
        for (int var12 = Mth.floor(bounds.minX); var12 <= Mth.floor(bounds.maxX); var12++) {
            for (int var13 = Mth.floor(bounds.minZ); var13 <= Mth.floor(bounds.maxZ); var13++) {
                for (int var14 = Mth.floor(minY2); var14 <= var11; var14++) {
                    blockPos.set(var12, var14, var13);
                    VoxelShape collisionShape = mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos);
                    if (!collisionShape.isEmpty()) {
                        NEGATIVE_INFINITY2 = Math.max(NEGATIVE_INFINITY2, var14 + collisionShape.max(Axis.Y));
                    }
                }
            }
        }
        double var16 = NEGATIVE_INFINITY2 - minY2 + 0.001;
        if (!(var16 <= 0.0) && !(var16 > var2 + var4)) {
            var16 = Math.max(var16, var2);
            return mc.level.noCollision(mc.player, bounds.move(0.0, var16, 0.0)) ? var16 : 0.0;
        } else {
            return 0.0;
        }
    }

    private AABB verticalStepBounds(double[] var1, double var2) {
        if (var2 != 0.0 && (this.jumpInput || this.sneakInput)) {
            if (mc.player != null && mc.level != null) {
                AABB bounds = mc.player.getBoundingBox().move(var1[0] * var2, 0.0, var1[1] * var2);
                return mc.level.noCollision(mc.player, bounds) ? null : bounds;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public NoWeb() {
        super(ModuleCategory.Movement, ModuleSubCategory.Other);
        this.mode = new ModeSetting("Мод").addModes("1.21.11", "GrimCollision", "NoCollision", "1.18.2", "1.17.1").withDefault("1.21.11").withContainer(this);
        this.pendingMotion = Vec3.ZERO;
        this.setName("No Web");
    }

    private boolean isCriticalWindow() {
        return ModuleManager.INSTANCE.criticals().isWebCriticalWindowActive();
    }

    private Vec3 legacyMotion(NoWeb.WebSpeeds var1) {
        double[] var2 = MovementUtils.moveDirectionVector();
        boolean var3 = var2[0] != 0.0 || var2[1] != 0.0;
        double var4 = var3 ? this.verticalSpeed(var1) : 0.0;
        AABB bounds = var3 ? this.verticalStepBounds(var2, var4) : null;
        this.breakQueued = !var3 || bounds != null;
        double var7 = this.breakQueued ? var1.idleAscend() : var1.ascend();
        double var9 = this.breakQueued ? var1.idleDescend() : var1.descend();
        double var11 = MovementUtils.axis(this.jumpInput, this.sneakInput, var7, var9);
        double var13 = bounds != null && var11 > 0.0 && this.isAirborne() ? this.floorBelow(bounds, var11, 0.015) : 0.0;
        this.movingUp = var13 > 0.0;
        if (this.movingUp) {
            var11 = var13;
        } else if (this.breakQueued) {
            var4 = 0.0;
        }
        return new Vec3(var2[0] * var4, var11, var2[1] * var4);
    }

    private Direction breakSide(BlockPos blockPos) {
        Vec3 eyePosition = mc.player.getEyePosition();
        if (eyePosition.y >= blockPos.getY() + 1) {
            return Direction.UP;
        } else if (eyePosition.y <= blockPos.getY()) {
            return Direction.DOWN;
        } else if (eyePosition.x <= blockPos.getX()) {
            return Direction.WEST;
        } else if (eyePosition.x >= blockPos.getX() + 1) {
            return Direction.EAST;
        } else {
            return eyePosition.z <= blockPos.getZ() ? Direction.NORTH : Direction.SOUTH;
        }
    }

    private double clearHeadroom(double var1, double var3) {
        if (!(var1 >= var3) && mc.player != null && mc.level != null) {
            return mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, var1, 0.0)) ? var1 : var3;
        } else {
            return var1;
        }
    }

    private double ceilingAbove(AABB bounds, double var2) {
        double NEGATIVE_INFINITY2 = Double.NEGATIVE_INFINITY;
        MutableBlockPos blockPos = new MutableBlockPos();
        int var7 = Mth.floor(bounds.maxY + var2);
        for (int var8 = Mth.floor(bounds.minX); var8 <= Mth.floor(bounds.maxX); var8++) {
            for (int var9 = Mth.floor(bounds.minZ); var9 <= Mth.floor(bounds.maxZ); var9++) {
                for (int var10 = Mth.floor(bounds.minY); var10 <= var7; var10++) {
                    if (mc.level.getBlockState(blockPos.set(var8, var10, var9)).is(Blocks.COBWEB)) {
                        NEGATIVE_INFINITY2 = Math.max(NEGATIVE_INFINITY2, var10 + 1.0);
                    }
                }
            }
        }
        return NEGATIVE_INFINITY2;
    }

    private void sendAction(Action action, BlockPos blockPos, Direction direction) {
        try (BlockStatePredictionHandler blockStatePredictionHandler = ((ClientLevelMixin_1241) (Object) mc.level).predictionHandler().startPredicting()) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(action, blockPos, direction, blockStatePredictionHandler.currentSequence()));
        }
    }

    private void breakWeb(BlockPos blockPos) {
        Direction direction = this.breakSide(blockPos);
        this.sendAction(Action.STOP_DESTROY_BLOCK, blockPos, direction);
    }

    @Subscribe(4)
    private void onPlayerInput(PlayerInputEvent event) {
        if (!this.isNoCollisionMode()) {
            this.jumpInput = event.isJump();
            this.sneakInput = event.isSneak();
            this.inWebNow = mc.player != null && WEB_SLOWDOWN.equals(((EntityMixin_1159) (Object) mc.player).getStuckSpeedMultiplier());
            if (this.inWebNow) {
                this.pendingMotion = mc.player.getDeltaMovement().multiply(WEB_SLOWDOWN);
            }
            if (this.isInWeb()) {
                event.setSneak(false);
            }
        }
    }

    @Subscribe
    private void onEntityMove(EntityMoveEvent event) {
        if (!this.isNoCollisionMode() && this.isInWeb()) {
            Vec3 vector = this.mode.is("1.21.11") ? this.modernMotion(NEW_WEB_SPEEDS) : this.legacyMotion(this.currentSpeeds());
            this.inWebNow = false;
            event.setMovement(vector);
        }
    }

    private double cappedHorizontal(double[] var1, double var2, double var4) {
        double var6 = Math.abs(var1[0]);
        double var8 = Math.abs(var1[1]);
        double var10 = Math.max(0.0, var2 + Math.signum(var1[0]) * this.pendingMotion.x);
        double var12 = Math.max(0.0, var2 + Math.signum(var1[1]) * this.pendingMotion.z);
        double var14 = Math.min((var10 + var4) / var6, (var12 + var4) / var8);
        if (!(var14 * var6 <= var10) && !(var14 * var8 <= var12)) {
            double var16 = var6 * var10 + var8 * var12;
            return var16 + Math.sqrt(Math.max(0.0, var16 * var16 - var10 * var10 - var12 * var12 + var4 * var4));
        } else {
            return var14;
        }
    }

    private Vec3 modernMotion(NoWeb.ModernWebSpeeds var1) {
        double[] var2 = MovementUtils.moveDirectionVector();
        boolean var3 = var2[0] != 0.0 || var2[1] != 0.0;
        double var4 = this.pendingMotion.y + var1.above();
        double var6 = this.pendingMotion.y - var1.below();
        double var8 = var3 ? this.cappedHorizontal(var2, var1.half(), var1.budget()) : 0.0;
        AABB bounds = var3 ? this.verticalStepBounds(var2, var8) : null;
        this.breakQueued = !var3 || bounds != null;
        double var11 = this.breakQueued ? var1.budget() : 0.0;
        double var13 = !this.jumpInput && !this.sneakInput ? Mth.clamp(0.0, var6, var4) : MovementUtils.axis(this.jumpInput, this.sneakInput, var4 + var11, var11 - var6);
        double var15 = bounds != null && var13 > 0.0 && this.isAirborne() ? this.floorBelow(bounds, var13, 0.0) : 0.0;
        this.movingUp = var15 > 0.0;
        if (this.movingUp) {
            var13 = var15;
            double var17 = var15 - var4;
            var8 = this.cappedHorizontal(var2, var1.half(), Math.sqrt(Math.max(0.0, var1.budget() * var1.budget() - var17 * var17)));
        } else {
            if (this.breakQueued) {
                var8 = 0.0;
            }
            var13 = this.clearHeadroom(this.sweepHorizontal(var13, var2, var8), var6);
        }
        return new Vec3(var2[0] * var8, var13, var2[1] * var8);
    }

    private boolean isAirborne() {
        return mc.player != null && !mc.player.onGround();
    }

    private boolean isInWeb() {
        return this.mode.is("1.21.11") ? this.inWebNow : WorldCollisionUtils.isPlayerInWeb();
    }

    private record ModernWebSpeeds(double half, double above, double below, double budget) {
    }

    private record WebSpeeds(double groundHorizontal, double airborneHorizontal, double descendHorizontal, double ascend, double descend, double idleAscend, double idleDescend) {
    }
}
