package ess.ll.client.module.combat;

import ess.ll.client.core.SimulatedPlayerInput;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.CombatTargetUtils;
import ess.ll.client.features.combat.ExplosionDamageUtils;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.features.world.BlockPlacementTracker;
import ess.ll.client.features.world.BlockTrapController;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import ess.ll.client.util.RaycastUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class WebTrap extends ClientModule {
    private final ModeSetting mode;
    private final ToggleSetting rageMode;
    private final ToggleSetting matrixBypass;
    private int placedCount;
    private int failCount;
    private int cooldownTick;
    private BlockPos lastTargetPos;
    private boolean webbing;
    private Player planPlayer;
    private int webbingFrom;

    /**
     * A target that keeps running never ends the webbing: the crystals stop waiting after a second.
     */
    private static final int CRYSTAL_WAIT_TICKS = 20;

    private List<BlockTrapController.PlacementTask> buildTrap(Player player, BlockPos blockPos) {
        List items = new ArrayList<>();
        this.placedCount = 0;
        boolean sx = CrystalAura.isAnyCrystalAuraEnabled();
        if (this.mode.is("Multi")) {
            this.tryEnqueue(items, blockPos, player, sx);
            this.tryEnqueue(items, blockPos.above(), player, sx);
            this.tryEnqueue(items, blockPos.offset(1, 0, 0), player, sx);
            this.tryEnqueue(items, blockPos.offset(-1, 0, 0), player, sx);
            this.tryEnqueue(items, blockPos.offset(0, 0, 1), player, sx);
            this.tryEnqueue(items, blockPos.offset(0, 0, -1), player, sx);
            this.tryEnqueue(items, blockPos.offset(1, 1, 0), player, sx);
            this.tryEnqueue(items, blockPos.offset(-1, 1, 0), player, sx);
            this.tryEnqueue(items, blockPos.offset(0, 1, 1), player, sx);
            this.tryEnqueue(items, blockPos.offset(0, 1, -1), player, sx);
            return items;
        }
        BlockPos blockPos2 = blockPos.above();
        if (!this.isCobweb(blockPos2) && !this.isCobweb(blockPos)) {
            if (!BlockTrapController.hasAdjacentSupport(blockPos2) || !this.tryEnqueue(items, blockPos2, player, sx)) {
                this.tryEnqueue(items, blockPos, player, sx);
            }
            return items;
        } else {
            return items;
        }
    }

    private Player findTarget() {
        Player player2 = null;
        double var2 = Double.MAX_VALUE;
        AntiBot antiBot2 = ModuleManager.INSTANCE.antiBot();
        for (Player player3 : mc.level.players()) {
            if (CombatTargetUtils.isEnemy(player3, antiBot2) && this.canSee(player3)) {
                double distanceSquared = player3.position().distanceToSqr(mc.player.position());
                if (distanceSquared < var2) {
                    var2 = distanceSquared;
                    player2 = player3;
                }
            }
        }
        return player2;
    }

    private BlockPos predictFeet(Player player) {
        SimulatedPlayerInput simulatedPlayerInput = SimulatedPlayerInput.predict(player, 7);
        Vec3 vector = simulatedPlayerInput.position != null ? simulatedPlayerInput.position : player.position();
        return BlockPos.containing(vector);
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player != null && mc.level != null) {
            BlockPos blockPos = this.planTarget();
            var var4 = blockPos == null ? null : this.buildTrap(this.planPlayer, blockPos);
            if (var4 != null && !var4.isEmpty() && this.placedCount != 0) {
                this.failCount = blockPos.equals(this.lastTargetPos) ? this.failCount + 1 : 1;
                this.lastTargetPos = blockPos;
                if (this.failCount > 8) {
                    this.cooldownTick = mc.player.tickCount + 20;
                    this.stop();
                } else {
                    this.markWebbing();
                    BlockTrapController.submit(var4, true, true, 100, this.rageMode.getValue(), this.matrixBypass.getValue());
                }
            } else {
                this.stop();
            }
        }
    }

    /**
     * Where webs should go this tick, or null when there is nobody to trap.
     */
    private BlockPos planTarget() {
        if (InventoryUtils.findItemIndex(Items.COBWEB, true) == -1 || mc.player.tickCount < this.cooldownTick) {
            return null;
        }
        this.planPlayer = this.findTarget();
        return this.planPlayer != null && !this.planPlayer.isEyeInFluid(FluidTags.WATER) ? this.predictFeet(this.planPlayer) : null;
    }

    private void markWebbing() {
        if (!this.webbing) {
            this.webbing = true;
            this.webbingFrom = mc.player.tickCount;
        }
    }

    private boolean canPlaceAt(BlockPos blockPos) {
        BlockPos blockPos2 = mc.player.blockPosition();
        return !blockPos.equals(blockPos2) && !blockPos.equals(blockPos2.above()) ? mc.player.getBoundingBox().intersects(new AABB(blockPos)) : true;
    }

    private boolean tryEnqueue(List<BlockTrapController.PlacementTask> items, BlockPos blockPos, Player player, boolean var4) {
        if (this.canPlaceAt(blockPos)) {
            return false;
        }
        if (!mc.level.getFluidState(blockPos).isEmpty()) {
            return false;
        }
        if (BlockPlacementTracker.isReserved(blockPos)) {
            return false;
        }
        if (var4 && ExplosionDamageUtils.canPlaceCrystal(blockPos, player.getBoundingBox())) {
            return false;
        }
        if (mc.level.getBlockState(blockPos).canBeReplaced()) {
            this.placedCount++;
        }
        items.add(new BlockTrapController.PlacementTask(blockPos, Items.COBWEB));
        return true;
    }

    /**
     * True while webs are still going down: the crystals go after them, a blast would clear them. The crystals can
     * ask before this tick's update has run, so a web that is only about to go down counts too.
     */
    public boolean crystalsWait() {
        if (!this.isEnabled()) {
            return false;
        }
        if (!this.webbing) {
            BlockPos blockPos = this.planTarget();
            var var2 = blockPos == null ? null : this.buildTrap(this.planPlayer, blockPos);
            if (var2 != null && !var2.isEmpty() && this.placedCount != 0) {
                this.markWebbing();
            }
        }
        return this.webbing && mc.player.tickCount - this.webbingFrom <= CRYSTAL_WAIT_TICKS;
    }

    private void stop() {
        this.webbing = false;
        BlockTrapController.cancel(100);
        this.failCount = 0;
        this.lastTargetPos = null;
    }

    private boolean canSee(Player player2) {
        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 eyePosition2 = player2.getEyePosition();
        double distance = eyePosition.distanceTo(eyePosition2);
        if (distance > 4.5) {
            return false;
        }
        Vector2f vector = PlayerUtils.rotationTo(eyePosition, eyePosition2);
        return RaycastUtils.isLookingAt(vector.x, vector.y, distance, player2, false);
    }

    private boolean isCobweb(BlockPos blockPos) {
        return mc.level.getBlockState(blockPos).is(Blocks.COBWEB);
    }

    public WebTrap() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.mode = new ModeSetting("Mode").addModes("Solo", "Multi").withDefault("Solo").withContainer(this);
        this.rageMode = new ToggleSetting("RageMode").withDefault(false).withContainer(this);
        this.matrixBypass = new ToggleSetting("Обход Matrix").withDefault(false).withContainer(this);
        this.cooldownTick = -1;
        this.setName("Web Trap");
    }

    @Override
    public void onDisable() {
        BlockTrapController.cancel(100);
        this.failCount = 0;
        this.cooldownTick = -1;
        this.lastTargetPos = null;
        super.onDisable();
    }
}
