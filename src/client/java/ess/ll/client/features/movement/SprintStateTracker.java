package ess.ll.client.features.movement;

import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.entity.Entity;
import ess.ll.client.mixin.LocalPlayerMixin_1170;

public final class SprintStateTracker {
    private int sprintStopWindowTicks;

    public void reset() {
        this.sprintStopWindowTicks = 0;
    }

    public void tick(LocalPlayer player) {
        if (this.sprintStopWindowTicks != 0) {
            this.sprintStopWindowTicks = wasSprinting(player) ? this.sprintStopWindowTicks - 1 : 0;
        }
    }

    public void onAttack(LocalPlayer player) {
        if (wasSprinting(player)) {
            this.sprintStopWindowTicks = 40;
        }
    }

    private static boolean containsSprintStop(List<DataValue<?>> items) {
        for (DataValue dataValue : items) {
            if (dataValue.id() == 0 && dataValue.value() instanceof Byte var3) {
                return (var3 & 8) == 0;
            }
        }
        return false;
    }

    public boolean consumeServerSprintStop(LocalPlayer player, List<DataValue<?>> items) {
        if (this.sprintStopWindowTicks != 0 && wasSprinting(player) && containsSprintStop(items)) {
            this.sprintStopWindowTicks = 0;
            return true;
        } else {
            return false;
        }
    }

    private static boolean wasSprinting(LocalPlayer player) {
        return ((LocalPlayerMixin_1170) (Object) player).wasSprinting();
    }
}
