package ess.ll.client.event;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class WeatherStateEvent extends Event {
    public static final WeatherStateEvent INSTANCE = new WeatherStateEvent();
    private WeatherStateEvent.Type type;

    public void overrideGradient(WeatherStateEvent.Type type, CallbackInfoReturnable<Float> cir) {
        if (this.isRemoved(type)) {
            cir.setReturnValue(0.0F);
        }
    }

    public WeatherStateEvent.Type getType() {
        return this.type;
    }

    public void overrideActive(WeatherStateEvent.Type type, CallbackInfoReturnable<Boolean> cir) {
        if (this.isRemoved(type)) {
            cir.setReturnValue(false);
        }
    }

    public void setType(WeatherStateEvent.Type type) {
        this.type = type;
    }

    public boolean isRemoved(WeatherStateEvent.Type type) {
        if (!this.hasListeners()) {
            return false;
        }
        this.type = type;
        super.dispatch();
        return this.consumeCancelled();
    }

    public enum Type {
        RAIN, THUNDER
    }
}
