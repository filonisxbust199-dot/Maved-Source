package ess.ll.client.module;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.ToggleStateEvent;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.render.AnimationUtils;
import ess.ll.client.setting.Setting;
import ess.ll.client.ui.KeyBinding;
import java.util.ArrayList;
import java.util.List;

public class ClientModule implements ToggleableModule, MinecraftAccess {
   private final List<Setting<?>> settings = new ArrayList<>();
   private boolean toggleable = true;
   private boolean bindable = true;
   private String name = this.getClass().getSimpleName();
   private String description;
   private boolean enabled;
   private KeyBinding binding;
   private ModuleSubCategory subCategory;
   private final ModuleCategory category;
   private AnimationUtils animation;
   private static final String DEFAULT_DESCRIPTION = "У этого модуля отстутсвуют подсказки.";
   private static final long ANIMATION_DURATION_MS = 150L;

   @Override
   public void toggle() {
      this.applyEnabled(!this.enabled);
      ToggleStateEvent.INSTANCE
         .fire(this, this.enabled ? ToggleStateEvent.State.ENABLE : ToggleStateEvent.State.DISABLE, ToggleStateEvent.Kind.MODULE);
   }

   @Override
   public void setBinding(KeyBinding keyBinding) {
      this.binding = keyBinding;
   }

   protected boolean canEqual(Object object) {
      return object instanceof ClientModule;
   }

   public void setSubCategory(ModuleSubCategory moduleSubCategory) {
      this.subCategory = moduleSubCategory;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String toString() {
      return "Module(settings="
         + this.getSettings()
         + ", toggleble="
         + this.isToggleable()
         + ", bindable="
         + this.isBindable()
         + ", name="
         + this.getName()
         + ", desc="
         + this.getDescription()
         + ", state="
         + this.isEnabled()
         + ", binding="
         + this.getBinding()
         + ", subCategory="
         + this.getSubCategory()
         + ", category="
         + this.getCategory()
         + ", anim="
         + this.getAnimation()
         + ")";
   }

   @Override
   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   public ClientModule(ModuleCategory moduleCategory, ModuleSubCategory moduleSubCategory) {
      this.description = DEFAULT_DESCRIPTION;
      this.animation = new AnimationUtils(AnimationUtils.DirectionalCurve.LINEAR, ANIMATION_DURATION_MS);
      this.category = moduleCategory;
      this.subCategory = moduleSubCategory;
      this.binding = new KeyBinding();
   }

   public String getDescription() {
      return this.description;
   }

   @Override
   public AnimationUtils getAnimation() {
      return this.animation;
   }

   @Override
   public boolean isToggleable() {
      return this.toggleable;
   }

   @Override
   public void onEnable() {
      EventBus.register(this);
      this.animation.update(1.0);
   }

   public void setToggleable(boolean toggleable) {
      this.toggleable = toggleable;
   }

   @Override
   public KeyBinding getBinding() {
      return this.binding;
   }

   public void setDescription(String text) {
      this.description = text;
   }

   @Override
   public List<Setting<?>> settings() {
      return this.settings;
   }

   @Override
   public void onDisable() {
      EventBus.unregister(this);
      this.animation.update(0.0);
   }

   public void setBindable(boolean bindable) {
      this.bindable = bindable;
   }

   public void setAnimation(AnimationUtils animationUtils) {
      this.animation = animationUtils;
   }

   public void setName(String text) {
      this.name = text;
   }

   @Override
   public boolean isBindable() {
      return this.bindable;
   }

   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Override
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Override
   public final void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         try {
            this.applyEnabled(enabled);
         } catch (Exception e) {
         }
      }
   }

   @Override
   public boolean equals(Object object) {
      if (object == this) {
         return true;
      } else if (!(object instanceof ClientModule other)) {
         return false;
      } else {
         if (!other.canEqual(this)) {
            return false;
         }

         if (this.isToggleable() != other.isToggleable()) {
            return false;
         }

         if (this.isBindable() != other.isBindable()) {
            return false;
         }

         if (this.isEnabled() != other.isEnabled()) {
            return false;
         }

         var settings = this.getSettings();
         var otherSettings = other.getSettings();
         if (settings == null ? otherSettings == null : settings.equals(otherSettings)) {
            String name = this.getName();
            String otherName = other.getName();
            if (name == null ? otherName == null : name.equals(otherName)) {
               String description = this.getDescription();
               String otherDescription = other.getDescription();
               if (description == null ? otherDescription == null : description.equals(otherDescription)) {
                  KeyBinding binding = this.getBinding();
                  KeyBinding otherBinding = other.getBinding();
                  if (binding == null ? otherBinding == null : binding.equals(otherBinding)) {
                     ModuleSubCategory subCategory = this.getSubCategory();
                     ModuleSubCategory otherSubCategory = other.getSubCategory();
                     if (subCategory == null ? otherSubCategory == null : subCategory.equals(otherSubCategory)) {
                        ModuleCategory category = this.getCategory();
                        ModuleCategory otherCategory = other.getCategory();
                        if (category == null ? otherCategory == null : category.equals(otherCategory)) {
                           AnimationUtils animation = this.getAnimation();
                           AnimationUtils otherAnimation = other.getAnimation();
                           return animation == null ? otherAnimation == null : animation.equals(otherAnimation);
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public ModuleSubCategory getSubCategory() {
      return this.subCategory;
   }

   @Override
   public int hashCode() {
      byte prime = 59;
      int result = 1;
      result = result * 59 + (this.isToggleable() ? 79 : 97);
      result = result * 59 + (this.isBindable() ? 79 : 97);
      result = result * 59 + (this.isEnabled() ? 79 : 97);
      var settings = this.getSettings();
      result = result * 59 + (settings == null ? 43 : settings.hashCode());
      String name = this.getName();
      result = result * 59 + (name == null ? 43 : name.hashCode());
      String description = this.getDescription();
      result = result * 59 + (description == null ? 43 : description.hashCode());
      KeyBinding binding = this.getBinding();
      result = result * 59 + (binding == null ? 43 : binding.hashCode());
      ModuleSubCategory subCategory = this.getSubCategory();
      result = result * 59 + (subCategory == null ? 43 : subCategory.hashCode());
      ModuleCategory category = this.getCategory();
      result = result * 59 + (category == null ? 43 : category.hashCode());
      AnimationUtils animation = this.getAnimation();
      return result * 59 + (animation == null ? 43 : animation.hashCode());
   }

   private void applyEnabled(boolean enabled) {
      this.enabled = enabled;
      if (enabled) {
         this.onEnable();
      } else {
         this.onDisable();
      }
   }
}
