package ess.ll.client.setting;

import java.util.function.Supplier;

public class NamedOption<T> {
    private final String name;
    private final T defaultValue;
    private T value;
    private Supplier<Boolean> visibility = () -> true;

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public void setVisibility(Supplier<Boolean> supplier) {
        this.visibility = supplier;
    }

    public void setValue(T t) {
        this.value = (T) (Object) t;
    }

    public T getValue() {
        return this.value;
    }

    public boolean isVisible() {
        return this.visibility.get();
    }

    public NamedOption<T> withVisibility(Supplier<Boolean> supplier) {
        if (supplier != null) {
            this.visibility = supplier;
        }
        return this;
    }

    public static <R> NamedOption<R> of(String text, R r) {
        return (NamedOption<R>) (new NamedOption<>(text, r));
    }

    private NamedOption(String text, T t) {
        this.name = text;
        this.defaultValue = (T) (Object) t;
        this.value = (T) (Object) t;
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    public Supplier<Boolean> getVisibility() {
        return this.visibility;
    }

    public String getName() {
        return this.name;
    }
}
