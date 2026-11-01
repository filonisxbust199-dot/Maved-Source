package ess.ll.client.event;

import ess.ll.client.ui.InputAction;
public class KeyInputEvent extends Event {
   public static final KeyInputEvent INSTANCE = new KeyInputEvent();
   private int key;
   private InputAction action;

   public InputAction getAction() {
      return this.action;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public void fire(int key, InputAction inputAction) {
      this.key = key;
      this.action = inputAction;
      super.dispatch();
   }

   public int getKey() {
      return this.key;
   }

   public void setAction(InputAction inputAction) {
      this.action = inputAction;
   }
}
