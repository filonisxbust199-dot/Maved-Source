package ess.ll.client.module.render;

import ess.ll.client.event.Subscribe;
import ess.ll.client.event.SwingDurationEvent;
import ess.ll.client.event.SwingTransformEvent;
import ess.ll.client.event.TickEvent;
import ess.ll.client.integration.ItemInHandRendererHooks;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.render.ParticleRenderer;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

public class SwingAnimations extends ClientModule {
    private static final Set<String> MOD_STYLES = Set.of("Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5");
    private final ModeSetting swingStyle;
    private final NumberSetting<Float> itemScale;
    private final NumberSetting<Integer> tiltStrength;
    private final NumberSetting<Integer> attackSpeed;
    private final ToggleSetting auraOnly;
    private final NumberSetting<Float> alternateItemScale;
    private final NumberSetting<Integer> alternateTiltStrength;
    private final NumberSetting<Integer> swingSmoothness;
    private final ToggleSetting overrideSwingSpeed;
    private final ModeSetting speedMode;
    private final NumberSetting<Integer> swingSpeed;
    private final ModeSetting activeHandMode;
    private final ToggleSetting customization;
    private final NumberSetting<Integer> rotationX;
    private final NumberSetting<Integer> rotationY;
    private final NumberSetting<Integer> rotationZ;
    private boolean transformApplied;
    private float swingProgress;

    public float currentItemScale() {
        return this.isModStyle() ? this.itemScale.getValue() : this.alternateItemScale.getValue();
    }

    private static void applyMod1(PoseStack poseStack, float var1, float var2) {
        poseStack.translate(0.1, 0.0, -0.2);
        poseStack.translate(0.5, -0.4, -0.82);
        poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees((-60.0F)));
        poseStack.rotate(Axis.XP.rotationDegrees((-80.0F) - var1 * var2));
    }

    public NumberSetting<Integer> getSwingSmoothness() {
        return this.swingSmoothness;
    }

    @Subscribe
    private void onSwingDuration(SwingDurationEvent event) {
        if (event.getCallback() != null) {
            if (event.getEntity() instanceof LocalPlayer var2) {
                if (this.isModStyle()) {
                    if (this.isEnabled()) {
                        event.getCallback().setReturnValue(this.modifiedSwingDuration(var2));
                    }
                } else if (!this.transformApplied) {
                    int var5 = this.vanillaAttackDelay(var2);
                    if (!this.overrideSwingSpeed.isEnabled()) {
                        event.getCallback().setReturnValue(var5);
                    } else {
                        int var4 = speedToTicks(this.swingSpeed.getValue());
                        if (this.speedMode.is("Статичный")) {
                            event.getCallback().setReturnValue(var4);
                        } else {
                            event.getCallback().setReturnValue(Math.min(var4, var5));
                        }
                    }
                }
            }
        }
    }

    private static void applyBaseTransform(PoseStack poseStack) {
        poseStack.translate(0.1, 0.0, -0.2);
        poseStack.translate(0.5, -0.4, -0.82);
        poseStack.rotate(Axis.XP.rotationDegrees((-120.0F)));
    }

    public NumberSetting<Integer> getSwingSpeed() {
        return this.swingSpeed;
    }

    public NumberSetting<Float> getItemScale() {
        return this.itemScale;
    }

    public NumberSetting<Integer> getAlternateTiltStrength() {
        return this.alternateTiltStrength;
    }

    private static void applyMod5(PoseStack poseStack, float var1, float var2) {
        float var3 = var1 / 10.0F / 10.0F * 40.0F;
        poseStack.translate(0.3, 0.0, -0.3);
        poseStack.translate(0.6, -0.6, -0.9);
        poseStack.rotate(Axis.YP.rotationDegrees(45.0F));
        poseStack.rotate(Axis.YP.rotationDegrees(var2 * -(var3 / 2.0F)));
        poseStack.rotate(Axis.ZP.rotationDegrees(var2 * -(var3 / 2.0F)));
        poseStack.rotate(Axis.XP.rotationDegrees(var2 * -var3));
        poseStack.translate((-0.5), 0.2F, 0.0);
        poseStack.rotate(Axis.YP.rotationDegrees(30.0F));
        poseStack.rotate(Axis.XP.rotationDegrees((-80.0F)));
        poseStack.rotate(Axis.YP.rotationDegrees(60.0F));
    }

    private float smoothProgress(float var1) {
        int var2 = Mth.clamp(2 + this.swingSmoothness.getValue(), 3, 6);
        if (var2 == 3) {
            return var1;
        }
        float var3 = (var2 - 3.0F) / 3.0F;
        float var4 = 0.5F - Mth.cos(var1 * (float) Math.PI) * 0.5F;
        return Mth.clamp(Mth.lerp(var3, var1, var4), 0.0F, 1.0F);
    }

    private static int roundDelayTicks(float var0) {
        if (Float.isFinite(var0) && !(var0 <= 0.0F)) {
            int var1 = (int) var0;
            int var2 = var0 < var1 ? var1 - 1 : var1;
            return Math.max(1, var2);
        } else {
            return 20;
        }
    }

    public NumberSetting<Integer> getRotationX() {
        return this.rotationX;
    }

    public ModeSetting getSpeedMode() {
        return this.speedMode;
    }

    private static void applyMod2(PoseStack poseStack, float var1, float var2, float var3) {
        poseStack.translate(0.56F, -0.52F, -0.72F);
        poseStack.rotate(Axis.YP.rotationDegrees(45.0F + Mth.sin(var3 * var3 * (float) Math.PI) / 4.0F * -20.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees(Mth.sin(Mth.sqrt(var3) * (float) Math.PI) * -20.0F));
        poseStack.rotate(Axis.XP.rotationDegrees(var2 * -var1));
        poseStack.rotate(Axis.YP.rotationDegrees((-45.0F)));
    }

    @Subscribe
    private void onTick(TickEvent event) {
        this.transformApplied = this.overridesSwing();
        this.syncHookState();
    }

    private boolean isModStyle() {
        return MOD_STYLES.contains(this.swingStyle.getValue());
    }

    public void setSwingProgress(float var1) {
        this.swingProgress = var1;
    }

    public ToggleSetting getOverrideSwingSpeed() {
        return this.overrideSwingSpeed;
    }

    private static void applyMod3(PoseStack poseStack, float var1, float var2) {
        applyBaseTransform(poseStack);
        poseStack.rotate(Axis.YP.rotationDegrees(-var2 * var1 + 30.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
    }

    private int modifiedSwingDuration(LocalPlayer player) {
        int value = this.attackSpeed.getValue();
        if (MobEffectUtil.hasDigSpeed(player)) {
            return value - (1 + MobEffectUtil.getDigSpeedAmplification(player));
        }
        MobEffectInstance effect = player.getEffect(MobEffects.MINING_FATIGUE);
        return effect == null ? value : value + (1 + effect.getAmplifier()) * 2;
    }

    public boolean shouldUseVanillaSwing() {
        return !this.isEnabled() || this.transformApplied;
    }

    private static void applyMod4(PoseStack poseStack, float var1, float var2) {
        applyBaseTransform(poseStack);
        poseStack.rotate(Axis.YP.rotationDegrees(-var2 * var1));
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
    }

    public SwingAnimations() {
        super(ModuleCategory.Render, ModuleSubCategory.Interface);
        this.swingStyle = new ModeSetting("Свинги").addModes("Обычный", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "Мод 8", "Мод 9", "Мод 10", "Мод 11", "Мод 12", "Мод 13", "Мод 14", "Мод 15", "Мод 16", "Мод 17", "HoldMyItems").withDefault("Обычный").withContainer(this);
        this.itemScale = new NumberSetting<Float>("Размер предмета").withMin(0.5F).withMax(1.5F).withStep(0.1F).withDefault(1.0F).withCustomName("nurItemScale").withVisibility(this::isModStyle).withContainer(this);
        this.tiltStrength = new NumberSetting<Integer>("Сила наклона").withMin(1).withMax(10).withStep(1).withDefault(3).withCustomName("nurTilt").withVisibility(this::isModStyle).withContainer(this);
        this.attackSpeed = new NumberSetting<Integer>("Скорость удара").withMin(3).withMax(10).withStep(1).withDefault(6).withCustomName("nurSwingSpeed").withVisibility(this::isModStyle).withContainer(this);
        this.auraOnly = new ToggleSetting("Только с аурой").withDefault(false).withCustomName("nurOnlyWithAura").withVisibility(this::isModStyle).withContainer(this);
        this.alternateItemScale = new NumberSetting<Float>("Размер предмета").withMin(0.5F).withMax(1.5F).withStep(0.01F).withDefault(1.0F).withVisibility(() -> !this.swingStyle.is("HoldMyItems") && !this.isModStyle()).withContainer(this);
        this.alternateTiltStrength = new NumberSetting<Integer>("Сила наклона").withMin(1).withMax(10).withStep(1).withDefault(6).withVisibility(this::usesCustomSwing).withContainer(this);
        this.swingSmoothness = new NumberSetting<Integer>("Плавность взмаха").withMin(1).withMax(4).withStep(1).withDefault(1).withVisibility(this::usesCustomSwing).withContainer(this);
        this.overrideSwingSpeed = new ToggleSetting("Менять скорость свинга").withDefault(false).withVisibility(() -> !this.isModStyle()).withContainer(this);
        this.speedMode = new ModeSetting("Режим скорости").addModes("Динамический", "Статичный").withDefault("Динамический").withVisibility(() -> this.overrideSwingSpeed.isEnabled() && !this.isModStyle()).withContainer(this);
        this.swingSpeed = new NumberSetting<Integer>("Скорость").withMin(1).withMax(20).withStep(1).withDefault(10).withVisibility(() -> this.overrideSwingSpeed.isEnabled() && !this.isModStyle()).withContainer(this);
        this.activeHandMode = new ModeSetting("Работать").addModes("С аурой", "При взмахе", "Всегда").withDefault("Всегда").withVisibility(this::usesCustomSwing).withContainer(this);
        this.customization = new ToggleSetting("Кастомизация").withDefault(false).withVisibility(this::usesCustomSwing).withContainer(this);
        this.rotationX = new NumberSetting<Integer>("Разворот по X").withMin(-180).withMax(180).withStep(1).withDefault(0).withVisibility(() -> this.customization.getValue() && this.usesCustomSwing()).withContainer(this);
        this.rotationY = new NumberSetting<Integer>("Разворот по Y").withMin(-180).withMax(180).withStep(1).withDefault(0).withVisibility(() -> this.customization.getValue() && this.usesCustomSwing()).withContainer(this);
        this.rotationZ = new NumberSetting<Integer>("Разворот по Z").withMin(-180).withMax(180).withStep(1).withDefault(0).withVisibility(() -> this.customization.getValue() && this.usesCustomSwing()).withContainer(this);
        this.transformApplied = true;
        this.setName("Swing Animations");
        ItemInHandRendererHooks.particleLayer = ParticleRenderer::renderAnimations;
    }

    private int vanillaAttackDelay(LocalPlayer player) {
        return roundDelayTicks(player.getCurrentItemAttackStrengthDelay());
    }

    private void applyHoldMyItems(PoseStack poseStack, float var2, float var3, float var4) {
        poseStack.translate(0.0F, 0.2F, 0.0F);
        poseStack.rotate(Axis.YP.rotationDegrees(60.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees((-60.0F)));
        poseStack.rotate(Axis.YP.rotationDegrees(var2 * var3 * -5.0F * var4));
    }

    public ModeSetting getActiveHandMode() {
        return this.activeHandMode;
    }

    private void applyTilt(PoseStack poseStack, float var2) {
        poseStack.translate((-0.56F), 0.52F, 0.72F);
        float var3 = this.tiltStrength.getValue().intValue() * 10.0F;
        float var4 = Mth.sin(var2 * (float) Math.PI);
        switch ((String) this.swingStyle.getValue()) {
            case "Мод 1":
                applyMod1(poseStack, var3, var4);
                break;
            case "Мод 2":
                applyMod2(poseStack, var3, var4, var2);
                break;
            case "Мод 3":
                applyMod3(poseStack, var3, var4);
                break;
            case "Мод 4":
                applyMod4(poseStack, var3, var4);
                break;
            case "Мод 5":
                applyMod5(poseStack, var3, var4);
                break;
            default:
                return;
        }
        float value = this.itemScale.getValue();
        poseStack.scale(value, value, value);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ItemInHandRendererHooks.enabled = false;
    }

    private void syncHookState() {
        ItemInHandRendererHooks.enabled = this.isEnabled() && this.swingStyle.is("HoldMyItems");
    }

    public void renderWithSwing(PoseStack poseStack, float var2, Runnable runnable) {
        if (this.transformApplied) {
            runnable.run();
        } else if (this.isModStyle()) {
            this.applyTilt(poseStack, Mth.clamp(var2, 0.0F, 1.0F));
        } else {
            float var4 = this.smoothProgress(Mth.clamp(var2, 0.0F, 1.0F));
            float var5 = (float) Math.sin(var4 * Math.PI);
            float var6 = (float) Math.sin(Mth.sqrt(var4) * Math.PI);
            float var7 = (float) Math.sin(var4 * var4 * Math.PI);
            float var8 = var5 * 0.5F;
            float var9 = this.alternateTiltStrength.getValue().intValue() / 6.0F;
            float value = this.alternateItemScale.getValue();
            if (!this.swingStyle.is("Мод 15")) {
                poseStack.scale(value, value, value);
            }
            this.applyCustomRotation(poseStack);
            switch ((String) this.swingStyle.getValue()) {
                case "Мод 6":
                    poseStack.translate(0.0F, var6 * -0.52F * var9 + 0.1F, 0.0F);
                    poseStack.rotate(Axis.YP.rotationDegrees((-20.0F)));
                    break;
                case "Мод 7":
                    poseStack.translate(0.0F, 0.1F, -0.1F);
                    poseStack.rotate(Axis.XP.rotationDegrees((-50.0F)));
                    poseStack.rotate(Axis.ZP.rotationDegrees(75.0F + var5 * 10.0F * this.alternateTiltStrength.getValue().intValue()));
                    poseStack.rotate(Axis.YP.rotationDegrees(50.0F + var5 * 10.0F * this.alternateTiltStrength.getValue().intValue()));
                    break;
                case "Мод 8":
                    this.applyHoldMyItems(poseStack, var6, var7, var9);
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * var7 * -120.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees((-60.0F)));
                    break;
                case "Мод 9":
                    this.applyHoldMyItems(poseStack, var6, var7, var9);
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * var7 * 120.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees((-60.0F)));
                    break;
                case "Мод 10":
                    poseStack.translate(0.0F, 0.2F, 0.0F);
                    poseStack.rotate(Axis.YP.rotationDegrees(76.0F));
                    poseStack.rotate(Axis.YP.rotationDegrees(var6 * -5.0F * var9));
                    poseStack.rotate(Axis.XN.rotationDegrees(var6 * -100.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * -155.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees((-100.0F)));
                    break;
                case "Мод 11":
                    poseStack.translate(0.0F, 0.1F, 0.0F);
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * -80.0F * var9));
                    poseStack.rotate(Axis.ZP.rotationDegrees(var6 * 45.0F * var9));
                    poseStack.rotate(Axis.YP.rotationDegrees(var6 * 15.0F * var9));
                    poseStack.translate(0.0F, -0.1F, 0.0F);
                    break;
                case "Мод 12":
                    poseStack.translate(0.0F, 0.2F, 0.0F);
                    poseStack.translate(-var8 * var8 * var7 * var9, 0.0F, 0.0F);
                    poseStack.rotate(Axis.YP.rotationDegrees(61.0F));
                    poseStack.rotate(Axis.ZP.rotationDegrees(var6 * var9));
                    poseStack.rotate(Axis.YP.rotationDegrees(var6 * var7 * -5.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * var7 * -30.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees((-60.0F)));
                    poseStack.rotate(Axis.XP.rotationDegrees(var8 * -60.0F * var9));
                    break;
                case "Мод 13":
                    poseStack.translate(0.0F, 0.2F, 0.0F);
                    poseStack.rotate(Axis.YP.rotationDegrees(30.0F));
                    poseStack.rotate(Axis.YP.rotationDegrees(var6 * 75.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * -45.0F * var9));
                    poseStack.rotate(Axis.YP.rotationDegrees(30.0F));
                    poseStack.rotate(Axis.XP.rotationDegrees((-80.0F)));
                    poseStack.rotate(Axis.YP.rotationDegrees(35.0F));
                    break;
                case "Мод 14":
                    poseStack.translate(0.0F, 0.1F, 0.0F);
                    this.applyHoldMyItems(poseStack, var6, var7, var9);
                    poseStack.rotate(Axis.YP.rotationDegrees(var6 * var7 * -20.0F * var9));
                    poseStack.rotate(Axis.ZP.rotationDegrees(var6 * var7 * -20.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees((-75.0F)));
                    break;
                case "Мод 15":
                    float var13 = this.alternateTiltStrength.getValue().intValue() * 10.0F;
                    float var14 = -40.0F - 40.0F * var5 * var13 / 50.0F;
                    float var15 = this.swingProgress;
                    poseStack.translate(0.8F, 0.3F, -0.8F);
                    poseStack.translate(0.56F, -1.0F, -0.82F);
                    poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
                    poseStack.rotate(Axis.ZN.rotationDegrees(60.0F));
                    poseStack.rotate(new Quaternionf().fromAxisAngleDeg(1.0F, var15, 0.0F, var14));
                    poseStack.rotate(Axis.XP.rotationDegrees((-40.0F)));
                    poseStack.translate((-0.5F), 0.08F, 0.0F);
                    value *= 3.0F;
                    poseStack.scale(value, value, value);
                    break;
                case "Мод 16":
                    poseStack.rotate(Axis.YP.rotationDegrees(45.0F + var5 * -5.0F * var9));
                    poseStack.rotate(Axis.ZP.rotationDegrees(var6 * -8.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * -15.0F * var9));
                    poseStack.rotate(Axis.YP.rotationDegrees((-45.0F)));
                    poseStack.translate(0.0F, var6 * -0.18F * var9, 0.0F);
                    break;
                case "Мод 17":
                    poseStack.rotate(Axis.YP.rotationDegrees(45.0F + var5 * 12.0F * var9));
                    poseStack.rotate(Axis.ZP.rotationDegrees(var6 * -8.0F * var9));
                    poseStack.rotate(Axis.XP.rotationDegrees(var6 * -25.0F * var9));
                    poseStack.rotate(Axis.YP.rotationDegrees((-45.0F)));
                    poseStack.translate(0.0F, 0.0F, var6 * -0.15F * var9);
                    break;
                default:
                    runnable.run();
            }
        }
    }

    public boolean overridesSwing() {
        if (this.isVanillaStyle()) {
            return false;
        } else if (this.isModStyle()) {
            return !this.isEnabled() ? true : this.auraOnly.isEnabled() && AttackAura.currentTarget() == null;
        } else if (this.activeHandMode.is("Всегда")) {
            return false;
        } else if (!this.isEnabled()) {
            return true;
        } else if (!this.activeHandMode.is("При взмахе")) {
            return !this.activeHandMode.is("С аурой") ? false : AttackAura.currentTarget() == null;
        } else {
            return mc.player == null || !mc.player.isSwinging();
        }
    }

    @Subscribe
    private void onSwingTransform(SwingTransformEvent event) {
        if (event.getKind() != null) {
            switch (event.getKind()) {
                case TRANSLATE:
                    if (this.shouldUseVanillaSwing() || this.isVanillaStyle()) {
                        return;
                    }
                    event.cancel();
                    break;
                case SWING_OFFSET:
                    if (!this.isEnabled() || this.transformApplied) {
                        return;
                    }
                    PoseStack poseStack = event.getPoseStack();
                    if (poseStack == null) {
                        return;
                    }
                    if (this.isVanillaStyle()) {
                        float value = this.alternateItemScale.getValue();
                        poseStack.scale(value, value, value);
                        return;
                    }
                    HumanoidArm arm = event.getArm();
                    if (arm == null) {
                        return;
                    }
                    HumanoidArm humanoidArm = mc.player != null ? mc.player.getMainArm() : HumanoidArm.RIGHT;
                    if (arm != humanoidArm) {
                        return;
                    }
                    event.cancel();
                    if (arm == HumanoidArm.LEFT) {
                        poseStack.scale((-1.0F), 1.0F, 1.0F);
                    }
                    this.renderWithSwing(poseStack, event.getSwingProgress(), () -> {
                    });
                    if (arm == HumanoidArm.LEFT) {
                        poseStack.scale((-1.0F), 1.0F, 1.0F);
                    }
                    break;
                case EQUIP_OFFSET:
                    this.setSwingProgress(event.getEquipProgress());
                    if (this.shouldUseVanillaSwing() || this.isVanillaStyle()) {
                        return;
                    }
                    PoseStack poseStack2 = event.getPoseStack();
                    if (poseStack2 == null) {
                        return;
                    }
                    event.cancel();
                    poseStack2.translate(event.getSide() * 0.56F, -0.52F, -0.72F);
            }
        }
    }

    public NumberSetting<Float> getAlternateItemScale() {
        return this.alternateItemScale;
    }

    private boolean isVanillaStyle() {
        return this.swingStyle.is("Обычный");
    }

    public ToggleSetting getCustomization() {
        return this.customization;
    }

    public NumberSetting<Integer> getRotationY() {
        return this.rotationY;
    }

    public NumberSetting<Integer> getTiltStrength() {
        return this.tiltStrength;
    }

    public ToggleSetting getAuraOnly() {
        return this.auraOnly;
    }

    public NumberSetting<Integer> getRotationZ() {
        return this.rotationZ;
    }

    public boolean isTransformApplied() {
        return this.transformApplied;
    }

    public NumberSetting<Integer> getAttackSpeed() {
        return this.attackSpeed;
    }

    public float getSwingProgress() {
        return this.swingProgress;
    }

    private boolean usesCustomSwing() {
        return this.hasCustomSwing() && !this.isModStyle();
    }

    private static int speedToTicks(int var0) {
        return Math.max(1, Math.round(21.0F - var0));
    }

    private void applyCustomRotation(PoseStack poseStack) {
        if (this.customization.isEnabled()) {
            float var2 = this.rotationX.getValue().intValue();
            float var3 = this.rotationY.getValue().intValue();
            float var4 = this.rotationZ.getValue().intValue();
            poseStack.rotate(Axis.XP.rotationDegrees(var2));
            poseStack.rotate(Axis.YP.rotationDegrees(var3));
            poseStack.rotate(Axis.ZP.rotationDegrees(var4));
        }
    }

    public ModeSetting getSwingStyle() {
        return this.swingStyle;
    }

    private boolean hasCustomSwing() {
        return !this.isVanillaStyle() && !this.swingStyle.is("HoldMyItems");
    }
}
