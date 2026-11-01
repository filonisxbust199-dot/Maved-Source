package ess.ll.client.render.targetfx.internal;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
public final class GpuUniformBufferHelper {
   private static final long lastUpdateMillis = 4L;

   private GpuUniformBufferHelper() {
   }

   public static GpuBufferSlice getGpuBufferSlice(ByteBuffer buffer, int value) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      return gpuDevice.createCommandEncoder().transientMemory().uploadStaging(buffer, lastUpdateMillis, 32, buffer.remaining(), value);
   }

   public static GpuBufferSlice getGpuBufferSlice2(ByteBuffer buffer2) {
      GpuDevice gpuDevice2 = RenderSystem.getDevice();
      long value2 = gpuDevice2.getDeviceInfo().limits().minUniformOffsetAlignment();
      return gpuDevice2.createCommandEncoder().transientMemory().uploadStaging(buffer2, value2, 128);
   }
}
