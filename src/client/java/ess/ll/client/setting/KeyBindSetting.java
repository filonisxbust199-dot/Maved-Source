package ess.ll.client.setting;

import ess.ll.client.ui.KeyBinding;

public class KeyBindSetting extends Setting<KeyBinding> {
    private boolean flagged;

    public KeyBindSetting flag() {
        this.flagged = true;
        return this;
    }

    @Override
    public void reset() {
        if (this.getValue() != null) {
            this.getValue().setKey(-1);
        }
    }

    public KeyBindSetting(String text) {
        super(text);
    }

    public boolean isFlagged() {
        return this.flagged;
    }
}
