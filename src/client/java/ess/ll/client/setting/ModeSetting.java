package ess.ll.client.setting;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModeSetting extends Setting<String> {
   private final List<String> modes = new CopyOnWriteArrayList<>();

   public ModeSetting removeMode(String text) {
      if (text != null && this.modes.remove(text)) {
         if (text.equals(this.getValue())) {
            this.assignValue(this.modes.isEmpty() ? null : this.modes.get(0));
         }

         return this;
      } else {
         return this;
      }
   }

   public ModeSetting addModes(String... modes) {
      if (modes == null) {
         return this;
      }

      Arrays.stream(modes).forEach(this::addMode);
      return this;
   }

   public ModeSetting(String text) {
      super(text);
   }

   public List<String> getModes() {
      return this.modes;
   }

   public boolean is(String text) {
      return this.getValue() == null ? false : this.getValue().equals(text);
   }

   public ModeSetting addMode(String text) {
      if (text != null && !text.isBlank() && !this.modes.contains(text)) {
         this.modes.add(text);
         if (this.getValue() == null) {
            this.assignValue(text);
         }

         return this;
      } else {
         return this;
      }
   }

   public <S extends Setting<String>> S select(String text) {
      if (text == null) {
         return (S)this;
      }

      this.modes.stream().filter(mode -> mode.equals(text)).findFirst().ifPresent(mode -> this.assignValue(mode));
      return (S)this;
   }
}
