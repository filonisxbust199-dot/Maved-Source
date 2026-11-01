package ess.ll.client.module.render;

import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.ViewModelOffsetEvent;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.module.combat.AttackAura;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

public class ViewModel extends ClientModule {
    private final NumberSetting<Float> rightX;
    private final NumberSetting<Float> rightY;
    private final NumberSetting<Float> rightZ;
    private final NumberSetting<Float> leftX;
    private final NumberSetting<Float> leftY;
    private final NumberSetting<Float> leftZ;
    private final ModeSetting activeHandMode;
    private boolean handsCustomized;

    @Subscribe
    private void onTick(TickEvent event) {
        this.handsCustomized = this.isHandHidden();
    }

    @Subscribe
    private void onViewModelOffset(ViewModelOffsetEvent event) {
        if (!this.handsCustomized) {
            event.setRightHandOffset(new Vec3(this.rightX.getValue().floatValue(), this.rightY.getValue().floatValue(), this.rightZ.getValue().floatValue()));
            event.setLeftHandOffset(new Vec3(this.leftX.getValue().floatValue(), this.leftY.getValue().floatValue(), this.leftZ.getValue().floatValue()));
            event.setCancelled(true);
        }
    }

    public boolean isHandHidden() {
        if (this.activeHandMode.is("Всегда")) {
            return false;
        } else if (!this.isEnabled()) {
            return true;
        } else if (!this.activeHandMode.is("При взмахе")) {
            return !this.activeHandMode.is("С аурой") ? false : AttackAura.currentTarget() == null;
        } else {
            return mc.player == null || !mc.player.isSwinging();
        }
    }

    public ViewModel() {
        super(ModuleCategory.Render, ModuleSubCategory.Interface);
        this.rightX = new NumberSetting<Float>("Правая X").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.rightY = new NumberSetting<Float>("Правая Y").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.rightZ = new NumberSetting<Float>("Правая Z").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.leftX = new NumberSetting<Float>("Левая X").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.leftY = new NumberSetting<Float>("Левая Y").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.leftZ = new NumberSetting<Float>("Левая Z").withMin((-3.0F)).withMax(3.0F).withStep(0.01F).withDefault(0.0F).withContainer(this);
        this.activeHandMode = new ModeSetting("Работать").addModes("С аурой", "При взмахе", "Всегда").withDefault("Всегда").withContainer(this);
        this.setName("View Model");
    }
}
