package ess.ll.client.module.combat;

import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.PlayerMovementActionEvent;
import ess.ll.client.event.PlayerUpdateEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.TimerSpeedEvent;
import ess.ll.client.features.world.WorldCollisionUtils;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PacketEvent;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
// 26.3 replaces the standalone swing packet with punch; entity attacks still use ServerboundAttackPacket.
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import ess.ll.client.mixin.LocalPlayerMixin_1170;

public class Criticals extends ClientModule {
    private final ToggleSetting legacyGrim;
    private final MultiToggleSetting activationConditions;
    private final ToggleSetting accelerateSlowFallingAttacks;
    private final List<Packet<?>> heldPackets;
    private boolean holdingPackets;
    private boolean wasHoldingPackets;
    private int stage;
    private double offsetDelta;
    private int lastTick;
    private long releaseTime;
    private final List<Packet<?>> incomingPackets;
    private final List<Packet<?>> pendingIncomingPackets;
    private final Object packetLock;
    private double savedX;
    private double savedY;
    private double savedZ;
    private int attackTargetId;
    private boolean attackQueued;
    private volatile boolean flushingPackets;

    @Subscribe
    private void onPlayerMovementAction(PlayerMovementActionEvent event) {
        if (event.getAction() == PlayerMovementActionEvent.Action.VELOCITY) {
            synchronized (this.packetLock) {
                if (this.incomingPackets.isEmpty()) {
                    return;
                }
            }
            this.attackQueued = true;
            event.setCancelled(true);
        }
    }

    private void flushBufferedPackets() {
        this.flushPackets(this.pendingIncomingPackets);
        this.flushPackets(this.incomingPackets);
        this.restoreHorizontalVelocity();
    }

    private void bufferAttackPacket(Packet<?> packet, PacketEvent event) {
        if (!this.holdingPackets && this.heldPackets.isEmpty() && !this.flushingPackets) {
            if (this.canBufferFallingAttack()) {
                if (!((LocalPlayerMixin_1170) (Object) mc.player).wasSprinting()) {
                    synchronized (this.packetLock) {
                        if (packet instanceof ServerboundPunchPacket && this.incomingPackets.isEmpty()) {
                            return;
                        }
                        this.incomingPackets.add(packet);
                    }
                    event.cancel();
                }
            }
        }
    }

    private ServerboundMovePlayerPacket offsetMovementPacket(ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
        double x = mc.player.getX();
        double var4 = mc.player.getY() - this.offsetDelta;
        double z = mc.player.getZ();
        boolean onGround = serverboundMovePlayerPacket.isOnGround();
        boolean var9 = serverboundMovePlayerPacket.horizontalCollision();
        return serverboundMovePlayerPacket.hasRotation() ? new PosRot(x, var4, z, serverboundMovePlayerPacket.getYRot(0.0F), serverboundMovePlayerPacket.getXRot(0.0F), onGround, var9) : new Pos(x, var4, z, onGround, var9);
    }

    private boolean canBufferFallingAttack() {
        return this.canPrepareSlowFallingCritical() && this.isAirborneWithoutFlightOrFluid() ? this.isDescending() && !ModuleManager.INSTANCE.aura().hasNearbyCollision() : false;
    }

    private static boolean isNegligibleMovement(double var0, double var2, double var4) {
        return var0 * var0 + var2 * var2 + var4 * var4 < 9.0E-4;
    }

    private void flushWebPackets() {
        this.holdingPackets = false;
        if (!this.heldPackets.isEmpty()) {
            for (Packet packet : this.heldPackets) {
                PlayerUtils.sendSilently(packet);
            }
            this.heldPackets.clear();
        }
    }

    @Subscribe(0)
    private void onPlayerInput(PlayerInputEvent event) {
        if (!this.isLegacyModeActive()) {
            boolean var2 = event.getForward() != 0.0F || event.getStrafe() != 0.0F;
            if (!var2) {
                if (this.stage == 2) {
                    event.setJump(true);
                    this.stage--;
                } else if (this.stage == 1) {
                    event.setSneak(true);
                    this.stage--;
                }
            } else {
                this.stage = 0;
            }
        }
    }

    @Override
    public void onEnable() {
        this.resetState();
        super.onEnable();
    }

    @Subscribe(3)
    private void onPacket(PacketEvent event) {
        if (!this.isLegacyModeActive() && event.isSend() && !event.isCancelled()) {
            if (mc.player != null) {
                Packet packet = event.getPacket();
                if (packet instanceof ServerboundPongPacket) {
                    this.bufferInteractionPacket(packet, event);
                } else if (packet instanceof ServerboundMovePlayerPacket var3) {
                    this.trackSentPosition(var3);
                } else if (!(packet instanceof ServerboundAttackPacket) && !(packet instanceof ServerboundPunchPacket)) {
                    if (packet instanceof ServerboundClientTickEndPacket) {
                        this.flushAfterPacket(event);
                        this.flushingPackets = false;
                    }
                } else {
                    this.bufferAttackPacket(packet, event);
                }
            }
        }
    }

    private void sendFallOffset() {
        this.lastTick = TickEvent.getTicks();
        this.offsetDelta = this.offsetDelta + 1.0E-9;
        PlayerUtils.sendSilently(new Pos(mc.player.getX(), mc.player.getY() - this.offsetDelta, mc.player.getZ(), mc.player.onGround(), mc.player.horizontalCollision));
    }

    @Subscribe
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isLegacyModeActive()) {
            this.resetState();
        } else {
            if (!this.canBufferFallingAttack()) {
                this.flushBufferedPackets();
            }
        }
    }

    private void trackSentPosition(ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
        if (serverboundMovePlayerPacket.hasPosition()) {
            double x = serverboundMovePlayerPacket.getX(Double.NaN);
            double y = serverboundMovePlayerPacket.getY(Double.NaN);
            double z = serverboundMovePlayerPacket.getZ(Double.NaN);
            if (!Double.isNaN(this.savedY) && isNegligibleMovement(x - this.savedX, y - this.savedY, z - this.savedZ)) {
                this.attackTargetId = TickEvent.getTicks();
            }
            this.savedX = x;
            this.savedY = y;
            this.savedZ = z;
        }
    }

    public boolean isSlowFallingConditionEnabled() {
        return this.isEnabled() && !this.legacyGrim.getValue() && this.activationConditions.isSelected("С плавным падением");
    }

    private static boolean isSlowFallingVelocity(double var0) {
        return var0 <= -0.085F && var0 >= -0.15F;
    }

    private void bufferInteractionPacket(Packet<?> packet, PacketEvent event) {
        if (!this.flushingPackets) {
            synchronized (this.packetLock) {
                boolean var4 = !this.pendingIncomingPackets.isEmpty();
                if (!var4 && !this.canBufferFallingAttack()) {
                    return;
                }
                this.pendingIncomingPackets.add(packet);
            }
            event.cancel();
        }
    }

    @Subscribe
    private void handleKnockback(PacketEvent event) {
        if (!this.isLegacyModeActive() && event.isReceive() && mc.player != null) {
            if (this.appliesKnockbackToPlayer(event.getPacket())) {
                this.flushingPackets = true;
                this.flushBufferedPackets();
            }
        }
    }

    private boolean appliesKnockbackToPlayer(Packet<?> packet) {
        return packet instanceof ClientboundSetEntityMotionPacket var3 ? var3.id() == mc.player.getId() : packet instanceof ClientboundExplodePacket var2 && var2.playerKnockback().isPresent();
    }

    private boolean hasMovementInput() {
        return mc.options.keyJump.isDown() || mc.options.keyShift.isDown() || mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    private boolean isDescending() {
        return mc.player.getDeltaMovement().y <= -0.003;
    }

    private void adjustSlowFallingTimer(TimerSpeedEvent event) {
        if (this.isSlowFallingConditionEnabled() && this.accelerateSlowFallingAttacks.isEnabled()) {
            if (!mc.player.onGround()) {
                if (!(ModuleManager.INSTANCE.aura().targetDistance > 2.5)) {
                    double var2 = mc.player.getDeltaMovement().y();
                    if (isSlowFallingVelocity(var2)) {
                        event.setSpeed(0.6F);
                        event.cancel();
                    }
                }
            }
        }
    }

    @Subscribe
    private void onTimerSpeed(TimerSpeedEvent event) {
        if (!this.isLegacyModeActive()) {
            if (mc.player != null) {
                if (!mc.player.isFallFlying() && !mc.player.isInWater() && !mc.player.isInLava()) {
                    if (AttackAura.currentTarget() != null) {
                        this.adjustSlowFallingTimer(event);
                    }
                }
            }
        }
    }

    private void resetState() {
        this.flushWebPackets();
        this.wasHoldingPackets = false;
        this.stage = 0;
        this.offsetDelta = 0.0;
        this.lastTick = -1;
        this.releaseTime = 0L;
        this.attackTargetId = Integer.MIN_VALUE;
        this.flushBufferedPackets();
    }

    public boolean isWebCriticalWindowActive() {
        return this.isEnabled() && !this.legacyGrim.getValue() && System.currentTimeMillis() < this.releaseTime;
    }

    private void flushPackets(List<Packet<?>> items) {
        Packet[] packet;
        synchronized (this.packetLock) {
            if (items.isEmpty()) {
                return;
            }
            packet = items.toArray(new Packet[0]);
            items.clear();
        }
        for (Packet packet2 : packet) {
            PlayerUtils.sendSilently(packet2);
        }
    }

    private boolean hasRecentNegligibleMovement() {
        return this.attackTargetId != Integer.MIN_VALUE && TickEvent.getTicks() - this.attackTargetId <= 2;
    }

    @Subscribe
    public void onAttackEntity(AttackEntityEvent event) {
        if (mc.player != null) {
            if (event.getEntity() instanceof LivingEntity) {
                if (!this.performLegacyCritical()) {
                    if (!mc.player.onGround() && !mc.player.isFallFlying() && !mc.player.isInWater() && !mc.player.isInLava()) {
                        if (event.getPhase() == AttackEntityEvent.Phase.PRE) {
                            double var2 = 1.0E-9;
                            double x = mc.player.getX();
                            double y = mc.player.getY();
                            double z = mc.player.getZ();
                            if (this.activationConditions.isSelected("В паутине") && this.prepareWebCritical(var2, x, y, z)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void flushAfterPacket(PacketEvent event) {
        synchronized (this.packetLock) {
            if (this.incomingPackets.isEmpty() && this.pendingIncomingPackets.isEmpty()) {
                return;
            }
        }
        event.cancel();
        PlayerUtils.sendSilently(event.getPacket());
        this.flushBufferedPackets();
    }

    private boolean hasHorizontalInput() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    private boolean isAirborneWithoutFlightOrFluid() {
        return !mc.player.onGround() && !mc.player.isFallFlying() && !mc.player.isInWater() && !mc.player.isInLava();
    }

    @Override
    public void onDisable() {
        this.resetState();
        super.onDisable();
    }

    private void restoreHorizontalVelocity() {
        if (this.attackQueued) {
            this.attackQueued = false;
            if (mc.player != null)
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
        }
    }

    public boolean canPrepareSlowFallingCritical() {
        if (!this.isSlowFallingCriticalActive() || mc.player.connection == null) {
            return false;
        } else if (AttackAura.currentTarget() == null) {
            return false;
        } else {
            return !this.hasHorizontalInput() && !this.hasRecentNegligibleMovement() ? false : !this.isSuspendedInWeb();
        }
    }

    public Criticals() {
        super(ModuleCategory.Combat, ModuleSubCategory.Utility);
        this.legacyGrim = new ToggleSetting("Старый GrimAC").withDefault(false).withContainer(this);
        this.activationConditions = new MultiToggleSetting("Работать если").addOption(NamedOption.of("В паутине", true)).addOption(NamedOption.of("С плавным падением", true)).withVisibility(() -> !this.legacyGrim.isEnabled()).withContainer(this);
        this.accelerateSlowFallingAttacks = new ToggleSetting("Ускорение ударов при плавном падении").withDefault(false).withVisibility(() -> this.activationConditions.isSelected("С плавным падением")).withVisibility(() -> !this.legacyGrim.isEnabled()).withContainer(this);
        this.heldPackets = new ArrayList<>();
        this.holdingPackets = false;
        this.wasHoldingPackets = false;
        this.stage = 0;
        this.offsetDelta = 0.0;
        this.lastTick = -1;
        this.releaseTime = 0L;
        this.incomingPackets = new ArrayList<>();
        this.pendingIncomingPackets = new ArrayList<>();
        this.packetLock = new Object();
        this.savedX = Double.NaN;
        this.savedY = Double.NaN;
        this.savedZ = Double.NaN;
        this.attackTargetId = Integer.MIN_VALUE;
        this.attackQueued = false;
        this.flushingPackets = false;
        this.setName("Criticals");
    }

    public boolean canForceCritical() {
        if (!this.isEnabled() || this.legacyGrim.getValue() || mc.player == null) {
            return false;
        } else {
            return this.activationConditions.isSelected("В паутине") && this.isAirborneWithoutFlightOrFluid() && this.isSuspendedInWeb() ? true : this.canBufferFallingAttack();
        }
    }

    private boolean isSuspendedInWeb() {
        if (mc.player != null && mc.level != null) {
            boolean var1 = WorldCollisionUtils.containsBlock(mc.player.getBoundingBox(), Blocks.COBWEB);
            return var1 && !WorldCollisionUtils.hasGroundBelowWeb(var1, 0.1);
        } else {
            return false;
        }
    }

    public boolean isSlowFallingCriticalActive() {
        return this.isEnabled() && !this.legacyGrim.getValue() && this.activationConditions.isSelected("С плавным падением") && mc.player != null && mc.player.hasEffect(MobEffects.SLOW_FALLING);
    }

    private boolean performLegacyCritical() {
        if (!this.isLegacyModeActive()) {
            return false;
        }
        if (!mc.player.isInWater() && !mc.player.isFallFlying() && !mc.player.onGround()) {
            PlayerUtils.sendSilently(new PosRot(mc.player.getX(), mc.player.getY() - 1.0E-9, mc.player.getZ(), mc.player.getXRot(), mc.player.getYRot(), false, mc.player.horizontalCollision));
            if (!mc.player.isUsingItem()) {
                PlayerUtils.sendSilently(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, mc.player.getXRot(), mc.player.getYRot()));
                mc.gameMode.releaseUsingItem(mc.player);
            }
            return true;
        } else {
            return true;
        }
    }

    public boolean isLegacyModeActive() {
        return this.isEnabled() && this.legacyGrim.getValue();
    }

    @Subscribe
    private void handleOutgoingPacket(PacketEvent event) {
        if (!this.isLegacyModeActive() && event.isSend()) {
            Packet packet = event.getPacket();
            if (!(packet instanceof ServerboundAttackPacket) && !(packet instanceof ServerboundPunchPacket)) {
                if (packet instanceof ServerboundMovePlayerPacket var3) {
                    if (this.wasHoldingPackets) {
                        this.wasHoldingPackets = false;
                        event.cancel();
                    } else if (!this.holdingPackets) {
                        this.offsetDelta = 0.0;
                    } else {
                        this.lastTick = TickEvent.getTicks();
                        this.offsetDelta = this.offsetDelta + 1.0E-9;
                        event.cancel();
                        PlayerUtils.sendSilently(this.offsetMovementPacket(var3));
                        this.flushWebPackets();
                    }
                } else {
                    if (packet instanceof ServerboundClientTickEndPacket) {
                        if (this.heldPackets.isEmpty()) {
                            return;
                        }
                        if (this.lastTick != TickEvent.getTicks()) {
                            this.sendFallOffset();
                        }
                        this.flushWebPackets();
                    }
                }
            } else if (this.holdingPackets) {
                this.heldPackets.add(packet);
                event.cancel();
            }
        }
    }

    private boolean prepareWebCritical(double var1, double var3, double var5, double var7) {
        if (mc.player == null || mc.player.connection == null) {
            return false;
        }
        if (!this.isSuspendedInWeb()) {
            return false;
        }
        this.releaseTime = System.currentTimeMillis() + 150L;
        if (this.hasMovementInput()) {
            this.holdingPackets = true;
        } else {
            mc.player.connection.send(new Pos(var3, var5 - var1, var7, false, mc.player.horizontalCollision));
            this.wasHoldingPackets = true;
            this.stage = 2;
        }
        return true;
    }
}
