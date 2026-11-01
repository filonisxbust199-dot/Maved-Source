package ess.ll.client.ui;

public class KeyBinding {
   private int key;
   private KeyBindingMode mode = KeyBindingMode.Toggle;
   private boolean enabled = true;

   public void setMode(KeyBindingMode keyBindingMode) {
      this.mode = keyBindingMode;
   }

   public KeyBinding() {
      this.key = -1;
   }

   public int getKey() {
      return this.key;
   }

   public KeyBinding setKey(int key) {
      this.key = key;

      return this;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public KeyBinding(int key) {
      this.key = key;
   }

   public KeyBindingMode getMode() {
      return this.mode;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
