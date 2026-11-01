package ess.ll.client.module.combat;

import ess.ll.client.module.ClientModules;
import ess.ll.client.event.*;
import ess.ll.client.core.FreeLookController;
import ess.ll.client.core.TickTaskScheduler;
import ess.ll.client.features.combat.AuraUpdateEvent;
import ess.ll.client.features.inventory.InventoryActionManager;
import ess.ll.client.features.world.*;
import ess.ll.client.render.WorldVertexCollector;
import ess.ll.client.render.ClientParticleManager;
import ess.ll.client.render.TargetScanRenderer;
import ess.ll.client.module.render.TargetESP;
import ess.ll.client.mixin.CombatGameRendererAccessor;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.render.targetfx.event.DeltaTrackerPoseStackEvent;
import ess.ll.client.render.targetfx.internal.ScreenProjectionMath;
import ess.ll.client.render.targetfx.TargetEspRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import ess.ll.client.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import java.util.concurrent.TimeUnit;

/**
 * Bridges the source event bus to the Minecraft 26.3 client lifecycle.
 */
public final class CombatRuntime {
    private static boolean initialized;
    private static long lastFrame = System.nanoTime(), auraElapsed, frameElapsed;
    private static boolean worldPresent;
    private static final java.util.Set<Integer> heldSourceKeys = new java.util.HashSet<>();
    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    private CombatRuntime() {
    }

    public static boolean bypassPackets() {
        return BYPASS.get();
    }

    public static boolean sendWithoutEvents(net.minecraft.network.protocol.Packet<?> packet) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null)
            return false;
        boolean previous = BYPASS.get();
        BYPASS.set(true);
        try {
            connection.send(packet);
            return true;
        } finally {
            BYPASS.set(previous);
        }
    }

    public static void initialize() {
        if (initialized)
            return;
        initialized = true;
        new FreeLookController();
        new RotationManager();
        new BlockActionQueue();
        new RotationChangeTracker();
        BlockBreakManager.INSTANCE.register();
        new BlockTrapController();
        BlockPlacementManager.INSTANCE.register();
        new RaycastManager();
        new TickTaskScheduler();
        PlayerVelocityTracker.INSTANCE.init();
        ServerLatencyTracker.INSTANCE.init();
        ServerTickRateTracker.INSTANCE.init();
        InventoryActionManager.INSTANCE.register();
        DelayedInventoryActionManager.INSTANCE.init();
        ClientParticleManager.INSTANCE.render();
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || ess.ll.client.features.Waypoints.active() == null && !ClientModules.all().stream().anyMatch(m -> (m.category.equals("Combat") || m.name.equals("Particles") || m.name.equals("Predictions") || m.name.equals("TargetESP") || m.name.equals("EntityESP") || m.name.equals("BlockESP") || m.name.equals("JumpCircles") || m.name.equals("Trails") || m.name.equals("HitBubbles") || m.name.equals("BlockOverlay") || m.name.equals("CrystalPosESP")) && m.enabled()))
                return;
            var collector = new WorldVertexCollector(ctx.submitNodeCollector());
            var camera = ctx.gameRenderer().mainCamera();
            var cameraState = ctx.levelState().cameraRenderState;
            PoseStack cameraEffects = new PoseStack();
            var effects = (CombatGameRendererAccessor) mc.gameRenderer;
            if (mc.player.hurtTime > 0)
                effects.ess$bobHurt(cameraState, cameraEffects);
            if (mc.options.bobView().get())
                effects.ess$bobView(cameraState, cameraEffects);
            Matrix4f guiView = new Matrix4f(cameraEffects.last().pose()).mul(cameraState.viewRotationMatrix);
            ScreenProjectionMath.captureCameraEffects(cameraEffects.last().pose());
            ScreenProjectionMath.captureViewProjection(guiView, cameraState.projectionMatrix, camera.position(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            TargetScanRenderer.setProjectionMatrix(cameraState.projectionMatrix);
            Render3DEvent.INSTANCE.fire(collector, mc.getDeltaTracker(), ctx.poseStack(), cameraState.viewRotationMatrix, cameraState.cullFrustum, camera);
        });
    }

    public static void renderTargetEffects() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        TargetESP esp = ModuleManager.INSTANCE.targetESP();
        if (esp == null || !esp.isEnabled())
            return;
        if (!ModuleManager.INSTANCE.aura().isEnabled() || ess.ll.client.module.combat.AttackAura.target == null) {
            TargetEspRenderer.clearTargetEffects();
            return;
        }
        var tracker = mc.getDeltaTracker();
        Matrix4f projection = ScreenProjectionMath.projectionMatrixCopy();
        Matrix4f view = ScreenProjectionMath.viewMatrixCopy();
        PoseStack pose = new PoseStack();
        pose.mulPose(view);
        var event = new DeltaTrackerPoseStackEvent(tracker, pose, null, mc.gameRenderer.mainCamera().position(), tracker.getGameTimeDeltaPartialTick(false), projection, view);
        TargetEspRenderer.getStateActive8();
        esp.renderOriginalTargetEffect(event);
        TargetEspRenderer.renderQueuedTargetEsp();
    }

    public static void frame() {
        if (!initialized)
            return;
        var mc = Minecraft.getInstance();
        if (mc == null)
            return;
        boolean world = mc.player != null && mc.level != null;
        if (world && !worldPresent)
            WorldLoadEvent.INSTANCE.dispatch();
        worldPresent = world;
        if (!world)
            return;
        long now = System.nanoTime(), delta = Math.clamp(now - lastFrame, 0, TimeUnit.MILLISECONDS.toNanos(100));
        lastFrame = now;
        FrameUpdateEvent.INSTANCE.fire(mc.getDeltaTracker());
        frameElapsed += delta;
        int steps = 0;
        while (frameElapsed >= 16_666_666L && steps++ < 6) {
            frameElapsed -= 16_666_666L;
            FrameDeltaEvent.INSTANCE.fire(1);
        }
        auraElapsed += delta;
        steps = 0;
        while (auraElapsed >= 4_166_666L && steps++ < 24) {
            auraElapsed -= 4_166_666L;
            AuraUpdateEvent.INSTANCE.dispatch();
        }
    }

    public static void tick() {
        if (!initialized)
            return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            heldSourceKeys.clear();
            return;
        }
        java.util.Set<Integer> watched = new java.util.HashSet<>();
        for (var module : new ess.ll.client.module.ClientModule[] { ModuleManager.INSTANCE.clickPearl(), ModuleManager.INSTANCE.elytraHelper() }) if (module.isEnabled())
            for (var setting : module.getSettings()) if (setting instanceof ess.ll.client.setting.KeyBindSetting bind) {
                int key = bind.getValue().getKey();
                if (key >= 0)
                    watched.add(key);
            }
        for (int key : watched) {
            // Mouse presses arrive directly from onButton, including clicks shorter than one tick.
            if (key >= ess.ll.client.command.Keys.MOUSE_BASE)
                continue;
            boolean down = mc.gui.screen() == null && ess.ll.client.command.Keys.isDown(key);
            if (down && heldSourceKeys.add(key)) {
                if (key >= ess.ll.client.command.Keys.MOUSE_BASE)
                    ess.ll.client.event.MouseButtonEvent.INSTANCE.fire(0, 0, key - ess.ll.client.command.Keys.MOUSE_BASE, ess.ll.client.ui.InputAction.PRESS);
                else
                    ess.ll.client.event.KeyInputEvent.INSTANCE.fire(key, ess.ll.client.ui.InputAction.PRESS);
            }
            if (!down && heldSourceKeys.remove(key)) {
                if (key >= ess.ll.client.command.Keys.MOUSE_BASE)
                    ess.ll.client.event.MouseButtonEvent.INSTANCE.fire(0, 0, key - ess.ll.client.command.Keys.MOUSE_BASE, ess.ll.client.ui.InputAction.RELEASE);
                else
                    ess.ll.client.event.KeyInputEvent.INSTANCE.fire(key, ess.ll.client.ui.InputAction.RELEASE);
            }
        }
        heldSourceKeys.retainAll(watched);
        TickEvent.INSTANCE.dispatch();
        PostTickEvent.INSTANCE.dispatch();
    }

    public static void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        if (initialized && Minecraft.getInstance().player != null)
            HudRenderEvent.INSTANCE.fire(graphics, delta);
    }
}
