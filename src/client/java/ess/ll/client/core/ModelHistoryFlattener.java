package ess.ll.client.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ModelHistoryFlattener {
   public static float[] flatten(ArrayDeque<float[]> history, int steps, int width) {
      float[] result = new float[steps * width];
      if (history.isEmpty()) {
         return result;
      }

      var frames = new ArrayList<>(history);
      int count = frames.size();
      float[] oldest = frames.get(count - 1);
      int padding = steps - count;
      int offset = 0;

      for (int i = 0; i < steps; i++) {
         float[] frame;
         if (i < padding) {
            frame = oldest;
         } else {
            int index = i - padding;
            int frameIndex = count - 1 - index;
            frame = frames.get(frameIndex);
         }

         System.arraycopy(frame, 0, result, offset, width);
         offset += width;
      }

      return result;
   }

   private ModelHistoryFlattener() {
   }
}
