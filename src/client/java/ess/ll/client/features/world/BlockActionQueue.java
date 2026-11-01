package ess.ll.client.features.world;

import ess.ll.client.event.EventBus;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import ess.ll.client.module.combat.CrystalAura;
import org.joml.Vector2f;

public final class BlockActionQueue implements MinecraftAccess {
    private static BlockActionQueue instance;
    private final Object[] owners = new Object[2];
    private final int[] lastRequestTicks = new int[] { -100, -100 };
    private final int[] priorities = new int[] { 0, 0 };
    private int rotationTick = -1;
    private int rotationPriority;
    private int lastTick = -1;
    private Object preferredOwner;

    private void releaseOwner(Object var1) {
        int var2 = this.findOwner(var1);
        if (var2 != -1) {
            this.owners[var2] = null;
            this.lastRequestTicks[var2] = -100;
            this.priorities[var2] = 0;
        }
        if (this.preferredOwner == var1) {
            this.preferredOwner = null;
        }
    }

    private Object otherOwner(int var1) {
        return var1 == -1 ? null : this.owners[1 - var1];
    }

    private int reserveSlot(Object var1) {
        int var2 = this.findOwner(var1);
        if (var2 != -1) {
            return var2;
        }
        for (int var3 = 0; var3 < 2; var3++) {
            if (this.owners[var3] == null) {
                this.owners[var3] = var1;
                return var3;
            }
        }
        return -1;
    }

    public static boolean requestRotation(Object var0, Vector2f vector) {
        return instance != null && instance.tryRotate(var0, vector, 62);
    }

    private boolean tryRotate(Object var1, Vector2f vector, int rotationPriority2) {
        if (mc.player == null) {
            return false;
        }
        int tickCount2 = mc.player.tickCount;
        if (tickCount2 < this.lastTick) {
            this.reset();
        }
        this.lastTick = tickCount2;
        int var5 = this.reserveSlot(var1);
        if (var5 != -1) {
            this.lastRequestTicks[var5] = tickCount2;
            this.priorities[var5] = rotationPriority2;
        }
        if (this.rotationTick == tickCount2 && rotationPriority2 <= this.rotationPriority) {
            return false;
        }
        if (RotationManager.isBusy(rotationPriority2)) {
            return false;
        }
        Object var6 = this.otherOwner(var5);
        if (var6 != null && this.isOtherOwnerActive(var5, tickCount2) && this.preferredOwner != null && this.preferredOwner != var1 && rotationPriority2 <= this.priorities[1 - var5]) {
            return false;
        }
        this.rotationTick = tickCount2;
        this.rotationPriority = rotationPriority2;
        this.preferredOwner = var6;
        Vector2f aim = new Vector2f(vector.x + BlockPlacementManager.yawJitter(vector.x), vector.y + BlockPlacementManager.pitchJitter());
        if (!(var1 instanceof CrystalAura crystal && crystal.requestAuraRotation(aim, rotationPriority2))) {
            RotationManager.rotate(new Rotation(aim.x, aim.y), 180.0F, rotationPriority2, false);
        }
        RotationManager.moveCorrectionRequested = true;
        return true;
    }

    public BlockActionQueue() {
        EventBus.register(this);
        instance = this;
    }

    public static void release(Object var0) {
        if (instance != null) {
            instance.releaseOwner(var0);
        }
    }

    private int findOwner(Object var1) {
        for (int var2 = 0; var2 < 2; var2++) {
            if (this.owners[var2] == var1) {
                return var2;
            }
        }
        return -1;
    }

    private boolean isOtherOwnerActive(int var1, int var2) {
        int var3 = var2 - this.lastRequestTicks[1 - var1];
        return var3 >= 0 && var3 <= 1;
    }

    public static boolean requestRotation(Object var0, Vector2f vector, int var2) {
        return instance != null && instance.tryRotate(var0, vector, var2);
    }

    private void reset() {
        this.lastRequestTicks[0] = -100;
        this.lastRequestTicks[1] = -100;
        this.priorities[0] = 0;
        this.priorities[1] = 0;
        this.rotationTick = -1;
        this.rotationPriority = 0;
        this.preferredOwner = null;
    }
}
