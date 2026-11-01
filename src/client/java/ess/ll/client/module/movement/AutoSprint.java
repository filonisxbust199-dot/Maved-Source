package ess.ll.client.module.movement;

import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.CanSprintEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PlayerMovementActionEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.SprintStateEvent;
import ess.ll.client.event.SprintSyncEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.movement.SprintStateTracker;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.setting.ToggleSetting;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.effect.MobEffects;

public class AutoSprint extends ClientModule {
    private final ToggleSetting strictForwardCheck;
    private final ToggleSetting cancelWaterMovementAction;
    private final ToggleSetting trackWaterSprint;
    private final SprintStateTracker sprintStateTracker;
    private int movementPauseTicks;
    private int sprintPauseTicks;

    @Subscribe
    private void onCanSprint(CanSprintEvent event) {
        if (event.getOverride() == null) {
            if (mc.player != null) {
                mc.options.keySprint.setDown(true);
                boolean var2 = this.strictForwardCheck.isEnabled() ? this.hasForwardImpulse() : mc.player.input.keyPresses.forward();
                boolean sx = !mc.player.isPassenger() || mc.player.getVehicle().canSprint() && mc.player.getVehicle().isLocalInstanceAuthoritative();
                boolean sxx = canStartSprinting(mc.player.isUsingItem(), mc.player.hasEffect(MobEffects.BLINDNESS), sx, mc.player.isFallFlying(), mc.player.isMovingSlowly(), mc.player.isUnderWater(), mc.player.isInWater(), mc.player.horizontalCollision, mc.player.minorHorizontalCollision);
                event.setOverride(var2 && sxx && !mc.player.isSprinting());
            }
        }
    }

    @Subscribe
    private void onSprintSync(SprintSyncEvent event) {
        if (this.movementPauseTicks > 0) {
            this.movementPauseTicks--;
        }
        if (this.sprintPauseTicks > 0) {
            this.sprintPauseTicks--;
        }
        this.sprintStateTracker.tick(mc.player);
    }

    private boolean hasForwardImpulse() {
        return mc.player.isUnderWater() ? mc.player.input.hasForwardImpulse() : mc.player.input.getMoveVector().y >= 0.8F;
    }

    public boolean shouldTrackWaterSprint(LocalPlayer player) {
        return this.isEnabled() && this.trackWaterSprint.isEnabled() && player.isInWater();
    }

    public AutoSprint() {
        super(ModuleCategory.Movement, ModuleSubCategory.Main);
        this.strictForwardCheck = new ToggleSetting("Сброс 1.20 и ниже").withDefault(false).withContainer(this);
        this.cancelWaterMovementAction = new ToggleSetting("Сохранять спринт").withDefault(false).withVisibility(() -> !this.strictForwardCheck.isEnabled()).withContainer(this);
        this.trackWaterSprint = new ToggleSetting("Сброс для RW").withDefault(false).withContainer(this);
        this.sprintStateTracker = new SprintStateTracker();
        this.setName("Auto Sprint");
    }

    @Subscribe
    private void onAttackEntity(AttackEntityEvent event) {
        if (event.getPhase() == AttackEntityEvent.Phase.POST) {
            this.sprintStateTracker.onAttack(mc.player);
        }
    }

    @Subscribe
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.shouldStopAtWaterSurface()) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
                this.movementPauseTicks = 4;
            }
        }
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (this.sprintPauseTicks > 0) {
            event.setSprint(false);
        }
        if (this.movementPauseTicks > 0) {
            event.stopMoving();
            event.setSprint(false);
        }
    }

    public void handleEntityData(LocalPlayer player, List<DataValue<?>> items) {
        if (this.shouldTrackWaterSprint(player) && this.sprintStateTracker.consumeServerSprintStop(player, items)) {
            player.setSprinting(false);
            this.sprintPauseTicks = 1;
        }
    }

    @Override
    public void onDisable() {
        this.movementPauseTicks = 0;
        this.sprintPauseTicks = 0;
        this.sprintStateTracker.reset();
        super.onDisable();
    }

    @Subscribe
    private void onMovementAction(PlayerMovementActionEvent event) {
        if (this.cancelWaterMovementAction.isEnabled()) {
            if (!this.strictForwardCheck.isEnabled()) {
                if (mc.player != null) {
                    if (mc.player.isInWater()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean shouldStopAtWaterSurface() {
        return this.strictForwardCheck.isEnabled() && mc.player != null && mc.player.isInWater() && !mc.player.isUnderWater();
    }

    private static boolean canStartSprinting(boolean var0, boolean var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7, boolean var8) {
        return !var0 && !var1 && var2 && !var3 && (!var4 || var5) && (!var6 || var5) && !var7 && !var8;
    }

    @Subscribe
    private void onSprintState(SprintStateEvent event) {
        if (this.movementPauseTicks > 0 || this.sprintPauseTicks > 0 || this.shouldStopAtWaterSurface()) {
            event.setAllowed(false);
        }
    }
}
