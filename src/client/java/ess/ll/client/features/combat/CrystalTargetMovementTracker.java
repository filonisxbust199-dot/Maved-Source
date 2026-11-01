package ess.ll.client.features.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CrystalTargetMovementTracker {
    private final Vec3[] positions = new Vec3[10];
    private Entity target;
    private int writeIndex;
    private int sampleCount;

    public void record(Entity target2) {
        if (target2 != this.target) {
            this.target = target2;
            this.writeIndex = 0;
            this.sampleCount = 0;
        }
        this.positions[this.writeIndex] = target2.position();
        this.writeIndex = (this.writeIndex + 1) % 10;
        if (this.sampleCount < 10) {
            this.sampleCount++;
        }
    }

    public boolean tracks(Entity entity) {
        return entity == this.target;
    }

    public float averageSpeed() {
        if (this.sampleCount < 2) {
            return 0.0F;
        }
        Vec3 vector = this.positions[(this.writeIndex - 1 + 10) % 10];
        Vec3 vector2 = this.positions[(this.writeIndex - this.sampleCount + 10) % 10];
        return (float) (vector.distanceTo(vector2) / (this.sampleCount - 1));
    }

    public void reset() {
        this.target = null;
        this.writeIndex = 0;
        this.sampleCount = 0;
    }
}
