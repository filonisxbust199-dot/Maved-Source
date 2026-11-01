package ess.ll.client.setting;

import java.util.function.Supplier;

public abstract class Setting<T> {
    private final String name;
    private String customName;
    private String description = "";
    private SettingsContainer container;
    private T value;
    private T defaultValue;
    private Supplier<Boolean> visibility = () -> true;

    public String getDisplayName() {
        return this.customName != null && !this.customName.isEmpty() ? this.customName : this.name;
    }

    public Setting(String text) {
        this.container = this.container;
        this.name = text;
    }

    public void reset() {
        if (this.defaultValue != null) {
            this.setValue(this.defaultValue);
        }
    }

    public String getCustomName() {
        return this.customName;
    }

    protected void setDefaultValue(T t) {
        this.defaultValue = (T) (Object) t;
    }

    public SettingsContainer getContainer() {
        return this.container;
    }

    public String getDescription() {
        return this.description;
    }

    public Supplier<Boolean> getVisibility() {
        return this.visibility;
    }

    public <S extends Setting<T>> S setValue(T t) {
        if (t == null) {
            return (S) this;
        }
        this.assignValue((T) (Object) t);
        return (S) this;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public <S extends Setting<T>> S withCustomName(String text) {
        this.customName = text;
        return (S) this;
    }

    protected void assignValue(T t) {
        this.value = (T) (Object) t;
    }

    public <S extends Setting<T>> S withDefault(T t) {
        if (t != null && this.defaultValue == null) {
            this.setDefaultValue((T) (Object) t);
            this.assignValue((T) (Object) t);
            return (S) this;
        } else {
            return (S) this;
        }
    }

    public <S extends Setting<T>> S withDescription(String text) {
        this.description = text;
        return (S) this;
    }

    public boolean isVisible() {
        return this.visibility.get();
    }

    public void setCustomName(String text) {
        this.customName = text;
    }

    public T getValue() {
        return this.value;
    }

    public <S extends Setting<T>> S withContainer(SettingsContainer settingsContainer) {
        if (settingsContainer != null && this.container == null) {
            this.container = settingsContainer;
            this.container.getSettings().add(this);
            return (S) this;
        } else {
            return (S) this;
        }
    }

    public String getName() {
        return this.name;
    }

    public <S extends Setting<T>> S withVisibility(Supplier<Boolean> supplier) {
        this.visibility = supplier;
        return (S) this;
    }
}
