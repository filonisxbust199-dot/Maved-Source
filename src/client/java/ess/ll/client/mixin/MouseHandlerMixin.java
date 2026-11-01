package ess.ll.client.mixin;

import ess.ll.client.command.Keys;
import ess.ll.client.module.render.ZoomModule;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    /** Tracks every mouse button (side buttons included) for module binds. */
    @Inject(method="onButton",at=@At("HEAD"))
    private void ess$trackButton(long window,MouseButtonInfo info,int action,CallbackInfo ci) {
        Keys.mouse(info.button(),action!=0);
        ess.ll.client.module.misc.CreamyKeys.onMouse(info.button(),action);
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.player!=null && mc.gui.screen()==null)
            ess.ll.client.event.MouseButtonEvent.INSTANCE.fire(0,0,info.button(),action==0
                ? ess.ll.client.ui.InputAction.RELEASE : ess.ll.client.ui.InputAction.PRESS);
    }
    /** Keeps aim speed constant on screen while zoomed. */
    @Inject(method="turnPlayer",at=@At("HEAD"))
    private void ess$zoomSensitivity(double movementTime,CallbackInfo ci) {
        float factor=ZoomModule.factor();
        if(factor>1.001f) { accumulatedDX/=factor; accumulatedDY/=factor; }
    }
}
