package ess.ll.client.module;

import ess.ll.client.render.AnimationUtils;
import ess.ll.client.setting.Setting;
import ess.ll.client.setting.SettingsContainer;
import ess.ll.client.ui.KeyBinding;
import java.util.List;

public interface ToggleableModule extends SettingsContainer {
   void onEnable();

   boolean isEnabled();

   default void enable() {
      this.setEnabled(true);
   }

   ModuleCategory getCategory();

   AnimationUtils getAnimation();

   default void setState(boolean enabled) {
      this.setEnabled(enabled);
   }

   default void disable() {
      this.setEnabled(false);
   }

   ModuleSubCategory getSubCategory();

   List<Setting<?>> settings();

   boolean isBindable();

   String getName();

   void setEnabled(boolean enabled);

   boolean isToggleable();

   void toggle();

   default boolean isActive() {
      return this.isEnabled();
   }

   void onDisable();

   KeyBinding getBinding();

   void setBinding(KeyBinding keyBinding);
}
