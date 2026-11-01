package ess.ll.client.module.render;
import ess.ll.client.setting.Settings.*;
import java.util.List;

/** Expensive render examples; opt-in, state is saved like any other module. */
public final class RenderTestModule {
    public static final BooleanSetting ANIMATION=new BooleanSetting("Анимация",true);
    public static final SliderSetting SPEED=new SliderSetting("Скорость",.2f,2,.1f,1);
    public static final ModeSettings PALETTE=new ModeSettings("Палитра","Blue","Ice","Mono");
    public static final MultiBooleanSettings EXAMPLES=new MultiBooleanSettings("Примеры","Градиент","Тени","Классика");
    public static final List<Setting> SETTINGS=List.of(ANIMATION,SPEED,PALETTE,EXAMPLES);
    private static boolean enabled;
    private RenderTestModule() {}
    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }
}
