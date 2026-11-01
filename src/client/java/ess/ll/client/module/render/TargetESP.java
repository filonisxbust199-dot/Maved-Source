package ess.ll.client.module.render;

import ess.ll.client.event.EntityOutlineColorEvent;
import ess.ll.client.event.FrameDeltaEvent;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.RenderTargetEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.render.AnimationUtils;
import ess.ll.client.render.ClientParticleRenderer;
import ess.ll.client.render.ClientTextures;
import ess.ll.client.render.ColorUtils;
import ess.ll.client.render.EffectRenderPipelines;
import ess.ll.client.render.EspColorUtils;
import ess.ll.client.render.SmoothAnimation;
import ess.ll.client.render.TargetScanRenderer;
import ess.ll.client.render.WorldVertexCollector;
import ess.ll.client.render.targetfx.event.DeltaTrackerPoseStackEvent;
import ess.ll.client.render.targetfx.TargetEspRenderer;
import ess.ll.client.setting.ColorSetting;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.ui.GuiHitTestUtils;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.render.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class TargetESP extends ClientModule {
    private static final long START_NANOS = System.nanoTime();
    private final ModeSetting style;
    private final ToggleSetting glow;
    private final ToggleSetting throughWalls;
    private final ToggleSetting legacyStyle;
    private final ToggleSetting grid;
    private final NumberSetting<Integer> gridCoverage;
    private final ModeSetting lightningType;
    private final ModeSetting crystalType;
    private final ToggleSetting lightningThroughWalls;
    private final ToggleSetting crystalThroughWalls;
    private final ToggleSetting targetLighting;
    private final ToggleSetting targetHitRed;
    private final ToggleSetting autoThemeColor;
    private final ColorSetting targetColor;
    private final ColorSetting targetGradient;
    private final ToggleSetting targetDistortion;
    private final NumberSetting<Float> distortionStrength;
    private LivingEntity target;
    private boolean glowPassPending;
    private LivingEntity lastTarget;
    private float targetBlend;
    private double lastRenderTime;
    private final AnimationUtils fadeAnimation;
    private long lastTargetTime;
    private final long createdTime;
    private static final int HURT_COLOR_DARK = ColorUtils.packRgbUnchecked(205, 50, 50);
    private static final int HURT_COLOR_LIGHT = ColorUtils.packRgbUnchecked(255, 140, 140);
    private static final double[] COS_5_DEGREES = new double[73];
    private static final double[] SIN_5_DEGREES = new double[73];
    private static final double[] COS_4_DEGREES = new double[46];
    private static final double[] SIN_4_DEGREES = new double[46];
    private final List<TargetESP.Ghost> ghosts;
    private double rotationAngle;
    private float orbitPhase;

    AnimationUtils pulseAnimation;

    private static final double TWO_PI = Math.PI * 2;
    private static final double TEN_PI = Math.PI * 10;

    private void renderGhostTrail(Render3DEvent event) {
        this.renderGhosts(event, this.ghosts);
    }

    private void renderGhosts(Render3DEvent event, List<TargetESP.Ghost> ghosts) {
        PoseStack poseStack = event.getPoseStack();
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.ghost(this.throughWalls.isEnabled())) instanceof BaritoneVertexConsumerAccess sx) {
            Camera camera = event.getCamera();
            Vec3 position2 = camera.position();
            float var7 = 0.4F;
            poseStack.pushPose();
            long var8 = System.currentTimeMillis();
            for (TargetESP.Ghost sxxx : ghosts) {
                SmoothAnimation smoothAnimation = sxxx.getFade();
                smoothAnimation.update();
                long sxxxxx = sxxx.getCreatedTime();
                float sxxxxxx = (float) (var8 - sxxxxx);
                float sxxxxxxx;
                if (sxxxxxx < 400.0F) {
                    sxxxxxxx = 1.0F - sxxxxxx / 400.0F;
                } else if (sxxxxxx < 800.0F) {
                    sxxxxxxx = sxxxxxx / 400.0F - 1.0F;
                } else {
                    sxxxxxxx = 1.0F;
                }
                sxxxxxxx = Math.max(0.0F, Math.min(1.0F, sxxxxxxx));
                smoothAnimation.animateTo(sxxxxx - var8 > -200L ? 1.0 : 0.0, 0.05F);
                int sxxxxxxxx = ClientParticleRenderer.premultiplyColor(ColorUtils.withAlpha(sxxx.getColor(), Math.round(ColorUtils.getAlpha(sxxx.getColor()) * smoothAnimation.getFloatValue())));
                poseStack.pushPose();
                poseStack.translate(sxxx.getX() - position2.x(), sxxx.getY() - position2.y(), sxxx.getZ() - position2.z());
                poseStack.mulPose(new Matrix4f().rotate(camera.rotation()));
                Matrix4f matrix = poseStack.last().pose();
                float sxxxxxxxxxx = var7 * sxxxxxxx;
                this.emitQuad(sx, matrix, sxxxxxxxxxx, sxxxxxxxx);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    public void renderLightning(Render3DEvent event) {
        boolean enabled = this.glow.isEnabled();
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.ghost(this.throughWalls.isEnabled())) instanceof BaritoneVertexConsumerAccess var4) {
            PoseStack poseStack = event.getPoseStack();
            float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 position2 = event.getCamera().position();
            Vec3 vector = this.target.getPosition(gameTimeDeltaPartialTick).subtract(position2);
            Quaternionf quaternionf = event.getCamera().rotation();
            long var10 = System.currentTimeMillis();
            this.orbitPhase = this.orbitPhase + 4.0F * (float) (var10 - this.lastTargetTime) / 550.0F;
            this.lastTargetTime = var10;
            float var12 = GuiHitTestUtils.clamp01((this.target.hurtTime - (this.target.hurtTime != 0 ? gameTimeDeltaPartialTick : 0.0F)) / 10.0F);
            float floatValue = this.fadeAnimation.getFloatValue();
            int var14 = ColorUtils.reverseLerpClamped(ColorUtils.packRgbUnchecked(200, 100, 100), ColorUtils.Theme.accentAtOffset(0), var12);
            var14 = ColorUtils.multiplyAlpha(var14, floatValue);
            int var15 = ClientParticleRenderer.premultiplyColor(var14);
            int var16 = ColorUtils.withAlpha(var14, Math.round(ColorUtils.getAlpha(var14) * 0.25F));
            int var17 = ClientParticleRenderer.premultiplyColor(var16);
            int var18 = ClientParticleRenderer.premultiplyColor(ColorUtils.withAlpha(var16, Math.round(ColorUtils.getAlpha(var16) * 0.125F)));
            float var19 = 0.8F;
            for (byte var20 = 0; var20 < 9; var20 = (byte) (var20 + 3)) {
                float var21 = var20 * var20;
                for (byte var22 = 0; var22 < 12; var22++) {
                    float var23 = (0.0875F + 0.0125F * var22) * floatValue;
                    float var24 = this.orbitPhase + var22 * 0.1F;
                    poseStack.pushPose();
                    poseStack.translate(vector.x + 0.8F * Mth.sin(var24 + var21), vector.y + 0.5 + 0.3F * Mth.sin(this.orbitPhase + var22 * 0.2F) + 0.2F * var20, vector.z + 0.8F * Mth.cos(var24 - var21));
                    poseStack.mulPose(new Matrix4f().rotate(quaternionf));
                    Matrix4f matrix = poseStack.last().pose();
                    this.emitRing(var4, matrix, var23, var15, var17, var18, enabled);
                    poseStack.popPose();
                }
            }
        }
    }

    private Color targetEffectColor() {
        if (this.targetHitRed.isEnabled() && this.target != null && this.target.hurtTime > 0)
            return new Color(255, 70, 70);
        if (this.autoThemeColor.isEnabled())
            return new Color(Theme.accent(), true);
        return this.targetColor.getValue();
    }

    private Color targetEffectGradient() {
        if (this.autoThemeColor.isEnabled())
            return new Color(Theme.accentLight(), true);
        return this.targetGradient.getValue();
    }

    /**
     * Runs the recovered Aura renderer after world rendering, while its depth texture is available.
     */
    public void renderOriginalTargetEffect(DeltaTrackerPoseStackEvent event) {
        String mode = this.style.getValue();
        if (!"Молнии".equals(mode) && !"Кристалики".equals(mode))
            return;
        LivingEntity target = AttackAura.target;
        if (target instanceof LocalPlayer)
            target = null;
        renderDimasikEffect(event, target, mode);
    }

    private void renderDimasikEffect(DeltaTrackerPoseStackEvent event, LivingEntity target, String mode) {
        TargetEspRenderer.updateState7(this.targetDistortion.isEnabled(), this.distortionStrength.getValue());
        TargetEspRenderer.updateState8(false, true);
        TargetEspRenderer.updateState5(true, 0.2F, 14);
        TargetEspRenderer.updateState6(1.2F, "Тип 1", 3, this.targetHitRed.isEnabled(), "Тип 1", this.crystalType.getValue(), this.crystalThroughWalls.isEnabled(), this.lightningType.getValue(), this.lightningThroughWalls.isEnabled(), this.targetLighting.isEnabled(), "Тип 1");
        TargetEspRenderer.renderTargetEsp(event, target, mode, this.targetEffectColor().getRGB(), this.targetEffectGradient().getRGB());
    }

    public TargetESP() {
        super(ModuleCategory.Render, ModuleSubCategory.World);
        this.style = new ModeSetting("Тип").addModes("Призраки 1", "Призраки 2", "Призраки 3", "Квадрат", "Круг", "Сканирование", "Молнии", "Кристалики").withDefault("Призраки 1").withContainer(this);
        this.lightningType = new ModeSetting("Тип молний").addModes("Тип 1", "Тип 2", "Тип 3").withDefault("Тип 1").withVisibility(() -> this.style.is("Молнии")).withContainer(this);
        this.crystalType = new ModeSetting("Тип кристалликов").addModes("Тип 1", "Тип 2", "Тип 3", "Тип 4").withDefault("Тип 1").withVisibility(() -> this.style.is("Кристалики")).withContainer(this);
        this.lightningThroughWalls = new ToggleSetting("Просвечивать молнии").withDefault(false).withVisibility(() -> this.style.is("Молнии")).withContainer(this);
        this.crystalThroughWalls = new ToggleSetting("Просвечивать кристаллы").withDefault(true).withVisibility(() -> this.style.is("Кристалики")).withContainer(this);
        this.targetLighting = new ToggleSetting("Освещение").withDefault(true).withVisibility(() -> this.style.is("Молнии") || this.style.is("Кристалики")).withContainer(this);
        this.targetHitRed = new ToggleSetting("Красный при ударе").withDefault(true).withVisibility(() -> this.style.is("Молнии") || this.style.is("Кристалики")).withContainer(this);
        this.autoThemeColor = new ToggleSetting("Авто цвет под тему").withDefault(true).withVisibility(() -> this.style.is("Молнии") || this.style.is("Кристалики")).withContainer(this);
        this.targetColor = new ColorSetting("Цвет эффекта").withDefault(new Color(75, 200, 255)).withVisibility(() -> !this.autoThemeColor.isEnabled() && (this.style.is("Молнии") || this.style.is("Кристалики"))).withContainer(this);
        this.targetGradient = new ColorSetting("Цвет градиента").withDefault(new Color(210, 115, 255)).withVisibility(() -> !this.autoThemeColor.isEnabled() && this.style.is("Кристалики")).withContainer(this);
        this.targetDistortion = new ToggleSetting("Искажение").withDefault(true).withVisibility(() -> this.style.is("Молнии") || this.style.is("Кристалики")).withContainer(this);
        this.distortionStrength = new NumberSetting<Float>("Сила искажения").withMin(3.0F).withMax(30.0F).withStep(1.0F).withDefault(12.0F).withVisibility(() -> this.targetDistortion.isEnabled() && (this.style.is("Молнии") || this.style.is("Кристалики"))).withContainer(this);
        this.glow = new ToggleSetting("Свечение").withDefault(false).withVisibility(() -> this.style.is("Квадрат") || this.style.is("Призраки 1") || this.style.is("Призраки 2")).withContainer(this);
        this.throughWalls = new ToggleSetting("Просвечивать").withDefault(true).withVisibility(() -> this.style.is("Призраки 1") || this.style.is("Призраки 2") || this.style.is("Призраки 3")).withContainer(this);
        this.legacyStyle = new ToggleSetting("Старый").withDefault(false).withVisibility(() -> this.style.is("Круг")).withContainer(this);
        this.grid = new ToggleSetting("Сетка").withDefault(false).withVisibility(() -> this.style.is("Сканирование")).withContainer(this);
        this.gridCoverage = new NumberSetting<Integer>("Охват сетки").withMin(1).withMax(10).withStep(1).withDefault(5).withVisibility(() -> this.style.is("Сканирование") && this.grid.isEnabled()).withContainer(this);
        this.fadeAnimation = new AnimationUtils(AnimationUtils.DirectionalCurve.LINEAR, 300L);
        this.lastTargetTime = System.currentTimeMillis();
        this.createdTime = System.currentTimeMillis();
        this.ghosts = new ArrayList<>();
        this.rotationAngle = 0.0;
        this.pulseAnimation = new AnimationUtils(AnimationUtils.DirectionalCurve.LINEAR, 1500L);
        this.setName("Target ESP");
    }

    @Subscribe(4)
    private void onEntityOutlineColor(EntityOutlineColorEvent event) {
        if (this.style.is("Сканирование") && this.target != null && event.getEntity() == this.target && this.isFadeVisible()) {
            event.setColor(EspColorUtils.ensureOverlayAlphaBit(event.getColor()));
            this.glowPassPending = true;
        }
    }

    private static double cosPulse() {
        return Math.cos(System.currentTimeMillis() / 200.0) / 2.0;
    }

    private void renderCrystals(Render3DEvent event) {
        PoseStack poseStack = event.getPoseStack();
        float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.ghost(this.throughWalls.isEnabled())) instanceof BaritoneVertexConsumerAccess var5) {
            poseStack.pushPose();
            float var6 = 0.2F;
            float var7 = 15.0F;
            float var8 = 0.55F;
            float var9 = 0.2F;
            byte var10 = 9;
            float var11 = 10.0F;
            float var12 = 0.45F;
            Camera camera = event.getCamera();
            Vec3 position2 = camera.position();
            Vec3 vector = this.target.getPosition(gameTimeDeltaPartialTick).add(0.0, this.target.getBbHeight() / 2.0F, 0.0).subtract(position2);
            poseStack.translate(vector.x, vector.y, vector.z);
            Quaternionf quaternionf = camera.rotation();
            long var17 = System.currentTimeMillis();
            double var19 = (var17 - this.createdTime) / 4.0;
            poseStack.scale(1.5F, 1.5F, 1.5F);
            for (byte var21 = 0; var21 < 4; var21++) {
                for (byte var22 = 0; var22 < 9; var22++) {
                    poseStack.pushPose();
                    float var23 = var22 / 8.0F;
                    float var24 = 0.55F * (1.0F - var23) + 0.2F * var23;
                    double var25 = var19 - var21 * 125.0;
                    double var27 = var6 * (var25 - var22 * 10.0F) / 15.0;
                    double var29 = 0.0;
                    double var31 = -Math.sin(var27) * var12;
                    double var33 = -Math.cos(var27) * var12;
                    switch (var21) {
                        case 0:
                            var29 += var22 * 0.045F;
                            poseStack.translate(var33, var29 + 0.35F, -var31);
                            break;
                        case 1:
                        case 3:
                            var29 -= var22 * 0.045F;
                            poseStack.translate(var33, var29 - 0.35F, -var31);
                            break;
                        case 2:
                            var29 += var22 * 0.045F;
                            poseStack.translate(var33, var29, -var31);
                    }
                    float var35 = var24 * 0.5F;
                    int var36 = ColorUtils.reverseLerpClamped(ColorUtils.Theme.accentAtOffset(var22 * 25), -1, 0.85F);
                    var36 = ColorUtils.withAlpha(var36, Math.round(ColorUtils.getAlpha(var36) * this.fadeAnimation.getFloatValue()));
                    poseStack.mulPose(new Matrix4f().rotate(quaternionf));
                    Matrix4f matrix = poseStack.last().pose();
                    int var38 = ClientParticleRenderer.premultiplyColor(var36);
                    int var39 = 0;
                    int var40 = 0;
                    boolean enabled = this.glow.isEnabled();
                    if (enabled) {
                        int var42 = ColorUtils.withAlpha(var36, Math.round(ColorUtils.getAlpha(var36) * 0.25F));
                        var39 = ClientParticleRenderer.premultiplyColor(var42);
                        var40 = ClientParticleRenderer.premultiplyColor(ColorUtils.withAlpha(var42, Math.round(ColorUtils.getAlpha(var42) * 0.125F)));
                    }
                    this.emitRing(var5, matrix, var35, var38, var39, var40, enabled);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    private float updateTargetBlend(float var1, double var2) {
        if (this.lastTarget != this.target) {
            this.lastTarget = this.target;
            this.targetBlend = var1;
            this.lastRenderTime = var2;
            return var1;
        }
        double var4 = var2 - this.lastRenderTime;
        this.lastRenderTime = var2;
        if (var4 > 0.0) {
            this.targetBlend = this.targetBlend + (var1 - this.targetBlend) * (float) (-Math.expm1(-var4 / 0.12));
        }
        return this.targetBlend;
    }

    private void emitQuad(BaritoneVertexConsumerAccess baritoneVertexConsumerAccess, Matrix4f matrix, float var3, int var4) {
        baritoneVertexConsumerAccess.texturedVertex(matrix, -var3, -var3, 0.0F, 1.0F, 1.0F, var4);
        baritoneVertexConsumerAccess.texturedVertex(matrix, var3, -var3, 0.0F, 0.0F, 1.0F, var4);
        baritoneVertexConsumerAccess.texturedVertex(matrix, var3, var3, 0.0F, 0.0F, 0.0F, var4);
        baritoneVertexConsumerAccess.texturedVertex(matrix, -var3, var3, 0.0F, 1.0F, 0.0F, var4);
    }

    private static double sinPulse(float var0) {
        double var1 = Math.sin(System.currentTimeMillis() / 200.0);
        return (var1 + 1.0) / 2.0 * var0;
    }

    private void renderCircle(Render3DEvent event) {
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 position2 = camera.position();
        Vec3 position3 = this.target.getPosition(gameTimeDeltaPartialTick);
        double var7 = position3.x - position2.x();
        double var9 = position3.z - position2.z();
        double var11 = position3.y - position2.y();
        float bbWidth = this.target.getBbWidth();
        float var14 = (float) (var11 + sinPulse(this.target.getBbHeight()));
        float var15 = (float) (var14 - cosPulse());
        float floatValue = this.fadeAnimation.getFloatValue();
        Matrix4f matrix = poseStack.last().pose();
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.legacyCircleFill()) instanceof BaritoneVertexConsumerAccess var19) {
            for (int var20 = 0; var20 <= 45; var20++) {
                int var21 = ColorUtils.Theme.accentAtOffset(var20 * 24);
                float var22 = ColorUtils.redFloat(var21);
                float var23 = ColorUtils.greenFloat(var21);
                float var24 = ColorUtils.blueFloat(var21);
                float var25 = (float) (var7 + bbWidth * COS_4_DEGREES[var20]);
                float var26 = (float) (var9 + bbWidth * SIN_4_DEGREES[var20]);
                var19.coloredVertexF(matrix, var25, var15, var26, var22, var23, var24, 0.0F);
                var19.coloredVertexF(matrix, var25, var14, var26, var22, var23, var24, floatValue * 0.7058824F);
            }
            if (event.getVertexCollector().getBuffer(EffectRenderPipelines.legacyCircleOutline()) instanceof BaritoneVertexConsumerAccess var32) {
                for (int var33 = 0; var33 < 45; var33++) {
                    int var34 = ColorUtils.Theme.accentAtOffset(var33 * 24);
                    float var35 = ColorUtils.redFloat(var34);
                    float var36 = ColorUtils.greenFloat(var34);
                    float var37 = ColorUtils.blueFloat(var34);
                    float var27 = (float) (var7 + bbWidth * COS_4_DEGREES[var33]);
                    float var28 = (float) (var9 + bbWidth * SIN_4_DEGREES[var33]);
                    float var29 = (float) (var7 + bbWidth * COS_4_DEGREES[var33 + 1]);
                    float var30 = (float) (var9 + bbWidth * SIN_4_DEGREES[var33 + 1]);
                    emitRibbon(var32, matrix, var27, var28, var29, var30, var14, var35, var36, var37, floatValue, 1.5F);
                }
            }
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        long var2 = System.currentTimeMillis();
        this.ghosts.removeIf(var2x -> var2x.getCreatedTime() - var2 < -100L && var2x.getFade().getFloatValue() < 0.01F);
    }

    /**
     * With Chams on, the target effect is drawn after the chams pass (Chams.drawLate), so the chams of the target
     * do not paint over it.
     */
    @Subscribe
    private void onRender3D(Render3DEvent event) {
        WorldVertexCollector late = Chams.lateCollector();
        if (late == null) {
            this.renderTarget(event);
            return;
        }
        WorldVertexCollector normal = event.swapCollector(late);
        try {
            this.renderTarget(event);
        } finally {
            event.swapCollector(normal);
        }
    }

    private void renderTarget(Render3DEvent event) {
        TargetScanRenderer.cancel();
        if (!ModuleManager.INSTANCE.aura().isEnabled() || AttackAura.target == null) {
            this.target = null;
            this.lastTarget = null;
            this.ghosts.clear();
            this.fadeAnimation.update(0.0);
            TargetEspRenderer.clearTargetEffects();
            return;
        }
        if (AttackAura.target != null) {
            this.target = AttackAura.target;
        }
        if (this.target != null) {
            boolean var2 = ModuleManager.INSTANCE.aura().isEnabled() && AttackAura.target != null;
            this.fadeAnimation.update(var2 ? 1.0 : 0.0);
            if (this.style.is("Призраки 3")) {
                this.renderGhostTrail(event);
            } else if (!this.isFadeVisible()) {
                this.orbitPhase = 0.0F;
            } else if (!(this.target instanceof LocalPlayer)) {
                switch ((String) this.style.getValue()) {
                    case "Молнии":
                        break;
                    case "Кристалики":
                        break;
                    case "Призраки 1":
                        this.renderCrystals(event);
                        break;
                    case "Призраки 2":
                        this.renderLightning(event);
                        break;
                    case "Квадрат":
                        this.renderSquare(event);
                        break;
                    case "Сканирование":
                        this.renderScan(event);
                        break;
                    case "Круг":
                        if (this.legacyStyle.isEnabled()) {
                            this.renderCircle(event);
                        } else {
                            this.renderGhosts3d(event);
                        }
                }
            }
        }
    }

    private void emitRing(BaritoneVertexConsumerAccess baritoneVertexConsumerAccess, Matrix4f matrix, float var3, int var4, int var5, int var6, boolean var7) {
        this.emitQuad(baritoneVertexConsumerAccess, matrix, var3, var4);
        if (var7) {
            float var8 = var3 * 1.5F;
            this.emitQuad(baritoneVertexConsumerAccess, matrix, var8, var5);
            this.emitQuad(baritoneVertexConsumerAccess, matrix, var8 * 1.5F, var6);
        }
    }

    public void renderSquare(Render3DEvent event) {
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.targetSquare(ClientTextures.INSTANCE.squareTexture())) instanceof BaritoneVertexConsumerAccess var3) {
            PoseStack poseStack = event.getPoseStack();
            if (event.getFrustum().isVisible(this.target.getBoundingBox())) {
                float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                float var6 = (float) (System.currentTimeMillis() % 5500L);
                float var7 = var6 / 5500.0F * 2.0F - 1.0F;
                float var9 = 2750.0F;
                float var10 = Math.abs((var6 - var9) / var9);
                float var8 = 1500.0F * var7 + -1500.0F * var7 * var10;
                var8 = Math.max((-1500.0F), Math.min(1500.0F, var8));
                this.pulseAnimation.update(var6 % 3000.0F > 1500.0F ? 1.0 : 0.0);
                float var11 = (float) (3.5 * this.fadeAnimation.getValue() - 7.0);
                poseStack.pushPose();
                Camera camera = event.getCamera();
                Vec3 position2 = camera.position();
                Vec3 position3 = this.target.getPosition(gameTimeDeltaPartialTick);
                float var15 = (float) (position3.x - position2.x());
                float var16 = (float) (position3.y - position2.y() + this.target.getBbHeight() / 2.0F);
                float var17 = (float) (position3.z - position2.z());
                poseStack.translate(var15, var16, var17);
                poseStack.mulPose(new Matrix4f().rotate(camera.rotation()));
                poseStack.scale((-0.175F), -0.175F, -0.175F);
                poseStack.mulPose(new Matrix4f().rotate(Axis.ZP.rotationDegrees(var8)));
                float floatValue = this.fadeAnimation.getFloatValue();
                int var19 = ColorUtils.Theme.primaryAccent();
                int var20 = ColorUtils.Theme.secondaryAccent();
                int var21 = ColorUtils.Theme.secondaryAccent();
                int var22 = ColorUtils.Theme.primaryAccent();
                var19 = ColorUtils.multiplyAlpha(var19, floatValue);
                var20 = ColorUtils.multiplyAlpha(var20, floatValue);
                var21 = ColorUtils.multiplyAlpha(var21, floatValue);
                var22 = ColorUtils.multiplyAlpha(var22, floatValue);
                Matrix4f matrix = poseStack.last().pose();
                var3.texturedVertex(matrix, -var11, -var11, 0.0F, 0.0F, 0.0F, var19);
                var3.texturedVertex(matrix, -var11, var11, 0.0F, 0.0F, 1.0F, var20);
                var3.texturedVertex(matrix, var11, var11, 0.0F, 1.0F, 1.0F, var21);
                var3.texturedVertex(matrix, var11, -var11, 0.0F, 1.0F, 0.0F, var22);
                poseStack.popPose();
            }
        }
    }

    @Subscribe
    private void onRenderTarget(RenderTargetEvent event) {
        if (this.glowPassPending) {
            this.glowPassPending = false;
            event.setCancelled(true);
        }
    }

    private boolean isFadeVisible() {
        return this.fadeAnimation.getFloatValue() > 0.01F;
    }

    static {
        for (int var0 = 0; var0 <= 72; var0++) {
            double var1 = Math.toRadians(var0 * 5);
            COS_5_DEGREES[var0] = Math.cos(var1);
            SIN_5_DEGREES[var0] = Math.sin(var1);
        }
        for (int var3 = 0; var3 <= 45; var3++) {
            double var4 = Math.toRadians(var3 * 8);
            COS_4_DEGREES[var3] = Math.cos(var4);
            SIN_4_DEGREES[var3] = Math.sin(var4);
        }
    }

    private void renderScan(Render3DEvent event) {
        float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 position2 = event.getCamera().position();
        Vec3 position3 = this.target.getPosition(gameTimeDeltaPartialTick);
        double var5 = (System.nanoTime() - START_NANOS) / 1.0E9;
        float var7 = this.updateTargetBlend(this.target.getBbHeight(), var5);
        double var8 = var5 % 4.0 / 4.0 * TWO_PI;
        float var10 = (float) Math.cos(var8);
        float var11 = (float) (position3.y - position2.y());
        float var12 = (float) (var11 + var7 * 0.5 + Math.sin(var8) * (var7 * 0.5F + 0.1F));
        float var13 = this.grid.isEnabled() ? (var7 + 2.4F) * (0.1F + (this.gridCoverage.getValue() - 1) * 0.011111111F) : 0.0F;
        TargetScanRenderer.queue(event.getProjection(), (float) (position3.x - position2.x()), var11, (float) (position3.z - position2.z()), this.target.getBbWidth() * 0.5F, var7, 1.2F, var12, var10 > 0.0F ? 1.0F : -1.0F, Math.abs(var10), (float) (var5 % TEN_PI), ColorUtils.Theme.primaryAccent(), ColorUtils.Theme.secondaryAccent(), this.fadeAnimation.getFloatValue(), var13);
    }

    private static void emitRibbon(BaritoneVertexConsumerAccess baritoneVertexConsumerAccess, Matrix4f matrix, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11) {
        float var12 = var4 - var2;
        float var13 = var5 - var3;
        float var14 = (float) Math.hypot(var12, var13);
        float var15 = var14 != 0.0F ? var12 / var14 : 0.0F;
        float var16 = var14 != 0.0F ? var13 / var14 : 1.0F;
        baritoneVertexConsumerAccess.lineVertexF(matrix, var2, var6, var3, var7, var8, var9, var10, var15, 0.0F, var16, var11);
        baritoneVertexConsumerAccess.lineVertexF(matrix, var4, var6, var5, var7, var8, var9, var10, var15, 0.0F, var16, var11);
    }

    private void renderGhosts3d(Render3DEvent event) {
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        float gameTimeDeltaPartialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 position2 = camera.position();
        Vec3 position3 = this.target.getPosition(gameTimeDeltaPartialTick);
        double var7 = position3.x - position2.x();
        double var9 = position3.y - position2.y();
        double var11 = position3.z - position2.z();
        float var13 = this.fadeAnimation.getFloatValue() * 0.65F;
        float bbHeight = this.target.getBbHeight();
        double var15 = 1550.0;
        double var17 = System.currentTimeMillis() % 1550.0 / 1550.0;
        double var19 = var17 * Math.PI * 2.0;
        float var21 = GuiHitTestUtils.clamp01((this.target.hurtTime - (this.target.hurtTime != 0 ? gameTimeDeltaPartialTick : 0.0F)) / 10.0F);
        boolean var22 = var21 > 0.0F;
        double var23 = 0.5 + 0.5 * Math.cos(var19);
        double var25 = 1.15;
        double var27 = bbHeight * 0.5 * 1.15 * 0.5;
        double var29 = Math.sin(var19) * var27;
        float var31 = this.target.getBbWidth() * 0.25F;
        float var32 = this.target.getBbWidth() + var31 - var31 * var13;
        Matrix4f matrix = poseStack.last().pose();
        float var34 = (float) (var9 + bbHeight * var23);
        float var35 = 0.005F;
        byte var36 = 2;
        if (event.getVertexCollector().getBuffer(EffectRenderPipelines.circleOutline()) instanceof BaritoneVertexConsumerAccess var38) {
            for (int var39 = 0; var39 < var36; var39++) {
                float var40 = var34 + var35 / var36 * var39;
                float var41 = 1.0F - (float) var39 / var36 * 0.3F;
                float var42 = var13 * var41;
                int var43 = var39 * 10;
                for (int var44 = 0; var44 < 360; var44 += 5) {
                    int var45 = var44 / 5;
                    int var46 = ColorUtils.reverseLerpClamped(ColorUtils.Theme.accentAtOffset(var44 * 5 + var43), -1, 0.75F);
                    if (var22) {
                        var46 = ColorUtils.reverseLerpClamped(ColorUtils.animatedTwoColorGradient(4, var44 * 5 + var43, HURT_COLOR_DARK, HURT_COLOR_LIGHT), var46, var21);
                    }
                    float var47 = ColorUtils.redFloat(var46);
                    float var48 = ColorUtils.greenFloat(var46);
                    float var49 = ColorUtils.blueFloat(var46);
                    float var50 = (float) (var7 + COS_5_DEGREES[var45] * var32);
                    float var51 = (float) (var11 + SIN_5_DEGREES[var45] * var32);
                    float var52 = (float) (var7 + COS_5_DEGREES[var45 + 1] * var32);
                    float var53 = (float) (var11 + SIN_5_DEGREES[var45 + 1] * var32);
                    emitRibbon(var38, matrix, var50, var51, var52, var53, var40, var47, var48, var49, var42, 2.0F);
                }
            }
            if (event.getVertexCollector().getBuffer(EffectRenderPipelines.circleFill()) instanceof BaritoneVertexConsumerAccess var55) {
                for (int var56 = 0; var56 <= 360; var56 += 5) {
                    int var57 = var56 / 5;
                    int var58 = ColorUtils.reverseLerpClamped(ColorUtils.Theme.accentAtOffset(var56 * 20), -1, 0.65F);
                    if (var22) {
                        var58 = ColorUtils.reverseLerpClamped(ColorUtils.animatedTwoColorGradient(4, var56 * 20, HURT_COLOR_DARK, HURT_COLOR_LIGHT), var58, var21);
                    }
                    float var59 = ColorUtils.redFloat(var58);
                    float var60 = ColorUtils.greenFloat(var58);
                    float var61 = ColorUtils.blueFloat(var58);
                    float var62 = (float) (var7 + COS_5_DEGREES[var57] * var32);
                    float var63 = (float) (var11 + SIN_5_DEGREES[var57] * var32);
                    var55.coloredVertexF(matrix, var62, var34, var63, var59, var60, var61, var13);
                    var55.coloredVertexF(matrix, var62, (float) (var34 + var29), var63, var59, var60, var61, 0.0F);
                }
            }
        }
    }

    @Subscribe
    private void onFrameDelta(FrameDeltaEvent event) {
        if (this.target != null) {
            if (this.style.is("Призраки 3")) {
                this.rotationAngle = this.rotationAngle + 4.0;
                double var2 = this.rotationAngle % 360.0;
                float var4 = 0.6F + this.target.getBbWidth() * 0.5F;
                float var5 = this.target.getBbHeight() / 2.0F;
                long var6 = System.currentTimeMillis();
                float gameTimeDeltaPartialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                Vec3 position = this.target.getPosition(gameTimeDeltaPartialTick);
                if (AttackAura.target != null) {
                    for (int var10 = 0; var10 < 4; var10++) {
                        float var11 = (var10 - 1) * 0.4F;
                        double var12 = var2 + var11 * 360.0;
                        double var14 = Math.toRadians(-var12);
                        double var16 = Math.sin(var14 + 2.0) * var11;
                        double var18 = Math.cos(var14) * var4;
                        double var20 = Math.sin(var14) * var4;
                        double var22 = var5 + var16;
                        int var24 = ColorUtils.Theme.accentAtOffset((int) var2 + 90 * var10);
                        this.ghosts.add(new TargetESP.Ghost(position.x() + var18, position.y() + var22, position.z() + var20, var6, var24));
                    }
                }
            }
        }
    }

    private static class Ghost {
        private final double x;
        private final double y;
        private final double z;
        private final long createdTime;
        private final SmoothAnimation fade;
        private final int color;

        public double getY() {
            return this.y;
        }

        public double getZ() {
            return this.z;
        }

        public long getCreatedTime() {
            return this.createdTime;
        }

        public int getColor() {
            return this.color;
        }

        public double getX() {
            return this.x;
        }

        public SmoothAnimation getFade() {
            return this.fade;
        }

        public Ghost(double var1, double var3, double var5, long var7, int var9) {
            this.x = var1;
            this.y = var3;
            this.z = var5;
            this.createdTime = var7;
            this.color = var9;
            this.fade = new SmoothAnimation();
            this.fade.animateTo(1.0, 0.1F);
        }
    }
}
