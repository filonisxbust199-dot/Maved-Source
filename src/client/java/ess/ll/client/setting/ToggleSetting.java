package ess.ll.client.setting;

import ess.ll.client.event.ToggleStateEvent;
import ess.ll.client.render.AnimationUtils;
import ess.ll.client.ui.KeyBinding;

public class ToggleSetting extends Setting<Boolean> {
    public AnimationUtils animation;
    public KeyBinding binding;
    private static final long ANIMATION_DURATION_MS = 150L;

    public void toggle() {
        if (this.getValue() != null) {
            this.setValue(!this.getValue());
            if (this.getValue()) {
                ToggleStateEvent.INSTANCE.fire(this, ToggleStateEvent.State.ENABLE, ToggleStateEvent.Kind.CHECKBOX);
            } else {
                ToggleStateEvent.INSTANCE.fire(this, ToggleStateEvent.State.DISABLE, ToggleStateEvent.Kind.CHECKBOX);
            }
        }
    }

    public KeyBinding getBinding() {
        return this.binding;
    }

    public AnimationUtils getAnimation() {
        return this.animation;
    }

    public boolean isEnabled() {
        return this.getValue() == null ? false : this.getValue();
    }

    public ToggleSetting(String text) {
        super(text);
        this.animation = new AnimationUtils(AnimationUtils.DirectionalCurve.LINEAR, ANIMATION_DURATION_MS);
        this.binding = new KeyBinding(-1);
    }
}
