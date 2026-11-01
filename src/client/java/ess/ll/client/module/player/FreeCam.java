package ess.ll.client.module.player;

import ess.ll.client.core.FreeLookController;
import ess.ll.client.event.CameraPositionEvent;
import ess.ll.client.event.FrameUpdateEvent;
import ess.ll.client.event.HudRenderEvent;
import ess.ll.client.event.PlayerInputEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.WorldLoadEvent;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ModuleManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class FreeCam extends ClientModule {
    private final ToggleSetting showCoordinates;
    private final NumberSetting<Float> horizontalSpeed;
    private final NumberSetting<Float> verticalSpeed;
    public final ToggleSetting freezePackets;
    public Vec3 cameraPosition;
    private final Vector2f savedRotation;
    private CameraType savedCameraType;
    private Vec3 savedVelocity = Vec3.ZERO;

    @Subscribe
    private void freezePackets(ess.ll.client.network.PreMovementPacketEvent event) {
        if (this.freezePackets.isEnabled())
            event.cancel();
    }

    @Subscribe
    private void freezeMovement(ess.ll.client.event.EntityMoveEvent event) {
        if (this.freezePackets.isEnabled())
            event.setMovement(Vec3.ZERO);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null && mc.level != null) {
            this.savedRotation.x = mc.player.getYRot();
            this.savedRotation.y = mc.player.getXRot();
            this.savedCameraType = mc.options.getCameraType();
            this.savedVelocity = mc.player.getDeltaMovement();
            this.cameraPosition = mc.gameRenderer.mainCamera().position();
            FreeLookController.setYaw(this.savedRotation.x);
            FreeLookController.setPitch(this.savedRotation.y);
        }
    }

    @Subscribe
    private void onFrameUpdate(FrameUpdateEvent event) {
        this.updateCameraPosition(event.getDeltaTracker().getRealtimeDeltaTicks());
    }

    public FreeCam() {
        super(ModuleCategory.Player, ModuleSubCategory.Main);
        this.showCoordinates = new ToggleSetting("Отображать позицию").withDefault(false).withContainer(this);
        this.horizontalSpeed = new NumberSetting<Float>("Скорость по xz").withMin(0.1F).withMax(1.5F).withStep(0.05F).withDefault(0.5F).withContainer(this);
        this.verticalSpeed = new NumberSetting<Float>("Скорость по y").withMin(0.1F).withMax(1.5F).withStep(0.05F).withDefault(0.5F).withContainer(this);
        this.freezePackets = new ToggleSetting("Замораживать пакеты").withDefault(true).withContainer(this);
        this.cameraPosition = null;
        this.savedRotation = new Vector2f(0.0F, 0.0F);
        this.setName("Free Cam");
    }

    private boolean isElytraHelperActive() {
        ElytraHelper elytraHelper2 = ModuleManager.INSTANCE.elytraHelper();
        return elytraHelper2.isAutoTakeoffActive();
    }

    private void updateCameraPosition(float deltaTicks) {
        if (mc.player != null && mc.level != null) {
            CameraType cameraType = mc.options.getCameraType();
            Vec3 renderedCameraPosition = mc.getEntityRenderDispatcher().camera.position();
            if (this.cameraPosition == null) {
                this.cameraPosition = renderedCameraPosition;
            }
            if (cameraType == CameraType.THIRD_PERSON_FRONT) {
                FreeLookController.setYaw(mc.getEntityRenderDispatcher().camera.yRot());
                FreeLookController.setPitch(mc.getEntityRenderDispatcher().camera.xRot());
            }
            if (cameraType != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            double horizontalDistance = this.horizontalSpeed.getValue() * deltaTicks;
            double verticalDistance = this.verticalSpeed.getValue() * deltaTicks;
            float yaw = FreeLookController.getYaw();
            double forward = 0.0;
            double strafe = 0.0;
            if (mc.options.keyUp.isDown()) {
                forward++;
            }
            if (mc.options.keyDown.isDown()) {
                forward--;
            }
            if (mc.options.keyLeft.isDown()) {
                strafe++;
            }
            if (mc.options.keyRight.isDown()) {
                strafe--;
            }
            double offsetX = 0.0;
            double offsetZ = 0.0;
            if (forward != 0.0 || strafe != 0.0) {
                if (forward != 0.0) {
                    yaw = diagonalYaw(yaw, forward, strafe);
                    strafe = 0.0;
                    if (forward > 0.0) {
                        forward = 1.0;
                    } else {
                        forward = -1.0;
                    }
                }
                offsetX = forward * horizontalDistance * Mth.cos((float) Math.toRadians(yaw + 90.0F)) + strafe * horizontalDistance * Mth.sin((float) Math.toRadians(yaw + 90.0F));
                offsetZ = forward * horizontalDistance * Mth.sin((float) Math.toRadians(yaw + 90.0F)) - strafe * horizontalDistance * Mth.cos((float) Math.toRadians(yaw + 90.0F));
            }
            this.cameraPosition = this.cameraPosition.add(offsetX, 0.0, offsetZ);
            if (mc.options.keyJump.isDown()) {
                this.cameraPosition = this.cameraPosition.add(0.0, verticalDistance, 0.0);
            }
            if (mc.options.keyShift.isDown()) {
                this.cameraPosition = this.cameraPosition.add(0.0, -verticalDistance, 0.0);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null && mc.level != null) {
            mc.player.setYRot(this.savedRotation.x);
            mc.player.setXRot(this.savedRotation.y);
            if (this.savedCameraType != null) {
                mc.options.setCameraType(this.savedCameraType);
            }
            this.cameraPosition = null;
            mc.player.fallDistance = 0.0;
            if (this.freezePackets.isEnabled())
                mc.player.setDeltaMovement(this.savedVelocity);
        }
    }

    @Subscribe
    private void onCameraPosition(CameraPositionEvent event) {
        if (mc.player != null && mc.level != null) {
            if (this.cameraPosition != null) {
                event.setPosition(this.cameraPosition);
            }
        }
    }

    @Subscribe
    private void onHudRender(HudRenderEvent event) {
        if (this.showCoordinates.isEnabled() && mc.player != null) {
            float fontSize = 5.5F;
            float lineHeight = 7.5F;
            Vec3 playerPosition = mc.player.position();
            Vec3 cameraPosition = mc.getEntityRenderDispatcher().camera.position();
            GuiGraphicsExtractor graphics = event.getGraphics();
            float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
            float centerY = mc.getWindow().getGuiScaledHeight() / 2.0F;
            String playerCoordinates = "Позиция игрока: x" + (int) playerPosition.x() + " y" + (int) playerPosition.y() + " z" + (int) playerPosition.z();
            String cameraCoordinates = "Позиция камеры: x" + (int) cameraPosition.x() + " y" + (int) cameraPosition.y() + " z" + (int) cameraPosition.z();
            MsdfFont.UI.draw(graphics, playerCoordinates, centerX - MsdfFont.UI.width(playerCoordinates, fontSize) / 2, centerY + lineHeight, fontSize, -1);
            MsdfFont.UI.draw(graphics, cameraCoordinates, centerX - MsdfFont.UI.width(cameraCoordinates, fontSize) / 2, centerY + lineHeight * 2.0F, fontSize, -1);
        }
    }

    @Subscribe(4)
    private void onPlayerInput(PlayerInputEvent event) {
        if (this.isElytraHelperActive()) {
            event.stopMoving();
        } else {
            event.reset();
        }
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        this.setEnabled(false);
    }

    private static float diagonalYaw(float yaw, double forward, double strafe) {
        if (strafe > 0.0) {
            return yaw + (forward > 0.0 ? -45.0F : 45.0F);
        } else {
            return strafe < 0.0 ? yaw + (forward > 0.0 ? 45.0F : -45.0F) : yaw;
        }
    }
}
