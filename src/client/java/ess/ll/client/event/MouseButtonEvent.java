package ess.ll.client.event;

import ess.ll.client.ui.InputAction;
public class MouseButtonEvent extends Event {
   public static final MouseButtonEvent INSTANCE = new MouseButtonEvent();
   private double mouseX;
   private double mouseY;
   private int button;
   private InputAction action;

   public void fire(double mouseX, double mouseY, int button, InputAction inputAction) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.button = button;
      this.action = inputAction;
      super.dispatch();
   }

   public void setMouseX(double mouseX) {
      this.mouseX = mouseX;
   }

   public int getButton() {
      return this.button;
   }

   public double getMouseX() {
      return this.mouseX;
   }

   public void setMouseY(double mouseY) {
      this.mouseY = mouseY;
   }

   public double getMouseY() {
      return this.mouseY;
   }

   public void setAction(InputAction inputAction) {
      this.action = inputAction;
   }

   public InputAction getAction() {
      return this.action;
   }

   public void setButton(int button) {
      this.button = button;
   }
}
