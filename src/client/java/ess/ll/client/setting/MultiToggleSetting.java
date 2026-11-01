package ess.ll.client.setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiToggleSetting extends Setting<List<NamedOption<Boolean>>> {
   private int minSelected = 0;
   private final Map<String, NamedOption<Boolean>> optionsByName = new HashMap<>();

   @Override
   public void reset() {
      for (NamedOption namedOption : this.getValue()) {
         namedOption.reset();
      }
   }

   public int countEnabled() {
      int count = 0;

      for (NamedOption namedOption : this.getValue()) {
         if (Boolean.TRUE.equals(namedOption.getValue())) {
            count++;
         }
      }

      return count;
   }

   public Map<String, NamedOption<Boolean>> getOptionsByName() {
      return this.optionsByName;
   }

   public <S extends Setting<List<NamedOption<Boolean>>>> S withDefaultOptions(List<NamedOption<Boolean>> namedOptions) {
      if (this.getDefaultValue() == null && this.getValue() == null) {
         return super.withDefault(namedOptions);
      } else {
         throw new UnsupportedOperationException("Default value not supported for MultiSelecting (" + this.getClass().getName() + ")");
      }
   }

   public MultiToggleSetting addOption(NamedOption<Boolean> namedOption) {
      this.getValue().add(namedOption);
      this.optionsByName.put(namedOption.getName(), namedOption);
      return this;
   }

   public <S extends Setting<List<NamedOption<Boolean>>>> S initOptions(List<NamedOption<Boolean>> namedOptions) {
      if (this.getValue() != null) {
         throw new UnsupportedOperationException("Setting the value is not supported for not supported for MultiSelecting (" + this.getClass().getName() + ")");
      } else {
         return super.setValue(namedOptions);
      }
   }

   public boolean setOption(String text, boolean enabled) {
      NamedOption namedOption = this.optionsByName.get(text);
      if (namedOption == null) {
         return false;
      }

      namedOption.setValue(enabled);
      return true;
   }

   public MultiToggleSetting(String text) {
      super(text);
      this.initOptions(new ArrayList<>());
   }

   public boolean isSelected(String text) {
      NamedOption namedOption = this.optionsByName.get(text);
      return namedOption == null ? false : Boolean.TRUE.equals(namedOption.getValue());
   }

   public MultiToggleSetting withMinSelected(int min) {
      this.minSelected = Math.max(0, min);
      return this;
   }

   public int getMinSelected() {
      return this.minSelected;
   }

   public Optional<NamedOption<Boolean>> findOption(String text) {
      return Optional.ofNullable(this.optionsByName.get(text));
   }
}
