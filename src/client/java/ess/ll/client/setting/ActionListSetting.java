package ess.ll.client.setting;

import java.util.ArrayList;
import java.util.List;

public class ActionListSetting extends Setting<List<NamedOption<Runnable>>> {
    public ActionListSetting(String text) {
        super(text);
        this.setValue(new ArrayList<>());
    }

    public ActionListSetting addAction(String text, Runnable task) {
        this.getValue().add(NamedOption.of(text, task));
        return this;
    }
}
