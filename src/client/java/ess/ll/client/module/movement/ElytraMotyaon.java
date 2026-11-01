package ess.ll.client.module.movement;

import ess.ll.client.event.EntityMoveEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.inventory.InventoryUtils;
import ess.ll.client.integration.EntityExtensionAccess;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.ElapsedTimer;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerVelocityTracker;
import ess.ll.client.util.RaycastUtils;
import net.minecraft.world.entity.LivingEntity;

public class ElytraMotyaon extends ClientModule {
    private final NumberSetting<Float> targetRange;
    private final ToggleSetting freezeOnTarget;
    private final ToggleSetting useFireworks;
    private final NumberSetting<Float> movementScale;
    private final ElapsedTimer fireworkTimer;
    private boolean slowing;

    private boolean isLookingAtTarget(LivingEntity entity) {
        return RaycastUtils.isLookingAt(mc.player.getYRot(), mc.player.getXRot(), ModuleManager.INSTANCE.aura().effectiveAttackRange(), entity);
    }

    public boolean isActive() {
        return this.slowing;
    }

    @Subscribe
    private void onEntityMove(EntityMoveEvent event) {
        if (mc.player != null && mc.level != null) {
            this.setSlowing(false);
            LivingEntity entity = AttackAura.currentTarget();
            float value = this.movementScale.getValue();
            boolean var4 = entity != null && mc.player.isFallFlying() && ((EntityExtensionAccess) mc.player).getDistanceEyePos(entity) < this.targetRange.getValue().floatValue() && !ModuleManager.INSTANCE.aura().chasingElytraTarget;
            if (this.freezeOnTarget.isEnabled()) {
                value = 0.0F;
                var4 = var4 && this.isLookingAtTarget(entity);
                PlayerVelocityTracker.INSTANCE.setFrozen(var4);
            }
            if (var4) {
                if (this.fireworkTimer.isOlderThan(1000L) && this.useFireworks.isEnabled()) {
                    InventoryUtils.useFirework();
                    this.fireworkTimer.reset();
                }
                this.setSlowing(true);
                event.setMovement(event.getMovement().scale(value));
            }
        }
    }

    public void setSlowing(boolean slowing2) {
        this.slowing = slowing2;
    }

    @Override
    public void toggle() {
        this.setSlowing(false);
        super.toggle();
    }

    public ElytraMotyaon() {
        super(ModuleCategory.Movement, ModuleSubCategory.Main);
        this.targetRange = new NumberSetting<Float>("Дистанция").withMin(1.0F).withMax(3.0F).withStep(0.1F).withDefault(1.5F).withContainer(this);
        this.freezeOnTarget = new ToggleSetting("Улучшить фриз").withDefault(false).withContainer(this);
        this.useFireworks = new ToggleSetting("Авто фейерверк").setValue(true).withVisibility(() -> !this.freezeOnTarget.isEnabled()).withContainer(this);
        this.movementScale = new NumberSetting<Float>("Сила замедления").withMin(0.0F).withMax(0.3F).withStep(0.01F).withDefault(0.0F).withVisibility(() -> !this.freezeOnTarget.isEnabled()).withContainer(this);
        this.fireworkTimer = new ElapsedTimer();
        this.slowing = false;
        this.setName("Elytra Motyaon");
    }

    @Override
    public void onDisable() {
        PlayerVelocityTracker.INSTANCE.setFrozen(false);
        super.onDisable();
    }
}
