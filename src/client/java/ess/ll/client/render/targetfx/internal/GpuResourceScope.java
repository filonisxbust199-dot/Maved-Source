package ess.ll.client.render.targetfx.internal;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;
public final class GpuResourceScope implements AutoCloseable {
   private final ByteBufferBuilder byteBufferBuilder;

   public void updateState() {
      this.byteBufferBuilder.clear();
   }

   @Override
   public void close() {
      this.byteBufferBuilder.close();
   }

   public static GpuResourceScope getGpuResourceScope() {
      return GpuResourceScope.YCcnTUTZivpF9col.gpuResourceScope;
   }

   public GpuResourceScope(int value) {
      this.byteBufferBuilder = new ByteBufferBuilder(value);
   }

   public BufferBuilder getBufferBuilder(PrimitiveTopology primitiveTopology, VertexFormat vertexFormat) {
      return new BufferBuilder(this.byteBufferBuilder, primitiveTopology, vertexFormat);
   }

   private static final class YCcnTUTZivpF9col {
      private static final GpuResourceScope gpuResourceScope = new GpuResourceScope(GpuResourceScope.YCcnTUTZivpF9col.stateCounter);
      private static final int stateCounter = 786432;
   }
}
