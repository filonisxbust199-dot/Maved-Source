package ess.ll.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudRenderEvent extends Event {
    public static final HudRenderEvent INSTANCE = new HudRenderEvent();
    private PoseStack poseStack;
    private GuiGraphicsExtractor graphics;
    private DeltaTracker deltaTracker;

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public void fire(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
        this.poseStack = new PoseStack();
        this.graphics = guiGraphicsExtractor;
        this.deltaTracker = deltaTracker;
        guiGraphicsExtractor.pose().pushMatrix();
        try {
            super.dispatch();
        } finally {
            guiGraphicsExtractor.pose().popMatrix();
        }
    }

    public void setGraphics(GuiGraphicsExtractor guiGraphicsExtractor) {
        this.graphics = guiGraphicsExtractor;
    }

    public GuiGraphicsExtractor getGraphics() {
        return this.graphics;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public void setDeltaTracker(DeltaTracker deltaTracker) {
        this.deltaTracker = deltaTracker;
    }

    public void setPoseStack(PoseStack poseStack) {
        this.poseStack = poseStack;
    }
}
