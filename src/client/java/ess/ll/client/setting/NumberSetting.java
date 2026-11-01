package ess.ll.client.setting;

public class NumberSetting<T extends Number> extends Setting<T> {
   private T min;
   private T max;
   private T step;
   private boolean flag = true;

   public NumberSetting<T> withStep(T t) {
      if (this.step == null) {
         this.step = t;
      }

      return this;
   }

   public NumberSetting<T> withMin(T t) {
      if (this.min == null) {
         this.min = (T)(Object)t;
      }

      return this;
   }

   public T getMin() {
      return this.min;
   }

   public void setFromFloat(float value) {
      if (this.getDefaultValue() instanceof Integer) {
         this.setValue((T)(Object)Math.round(value));
      } else if (this.getDefaultValue() instanceof Double) {
         this.setValue((T)(Object)((double)value));
      } else if (this.getDefaultValue() instanceof Float) {
         this.setValue((T)(Object)value);
      } else {
         if (!(this.getDefaultValue() instanceof Long)) {
            throw new UnsupportedOperationException("Тип " + this.getDefaultValue().getClass() + " не поддерживается.");
         }

         this.setValue((T)(Object)((long)value));
      }
   }

   public NumberSetting(String text) {
      super(text);
   }

   public NumberSetting<T> withMax(T t) {
      if (this.max == null) {
         this.max = (T)(Object)t;
      }

      return this;
   }

   public NumberSetting<T> withFlag(boolean flag) {
      this.flag = flag;
      return this;
   }

   public boolean isFlag() {
      return this.flag;
   }

   public T getStep() {
      return this.step;
   }

   public T getMax() {
      return this.max;
   }
}
