package ess.ll.client.integration;

public interface PlayerAnimationAccess {
   void hMI5_0$resetOffHandSwing(boolean interact);

   float hMI5_0$getOffHandSwingProgress(float partialTick);

   boolean hMI5_0$getOHandEvent();

   boolean hMI5_0$getOInteract();

   boolean hMI5_0$getMHandEvent();

   boolean hMI5_0$getBlockBreak();

   boolean hMI5_0$getMInteract();

   int hMI5_0$getSwingCount();

   void hMI5_0$resetMainHandSwing(boolean interact);

   float hMI5_0$getMainHandSwingProgress(float partialTick);
}
