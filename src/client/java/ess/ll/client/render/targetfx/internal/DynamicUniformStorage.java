package ess.ll.client.render.targetfx.internal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** 26.3 replacement for the source's per-frame uniform allocator. */
public final class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> {
    public interface DynamicUniform { void write(ByteBuffer buffer); }
    private final int size;
    public DynamicUniformStorage(String name,int size,int capacity) { this.size=size; }
    public GpuBufferSlice writeUniform(T value) {
        ByteBuffer bytes=ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        value.write(bytes);
        bytes.position(0).limit(size);
        return RenderSystem.getDevice().createCommandEncoder().transientMemory()
            .uploadStaging(bytes,256,GpuBuffer.USAGE_UNIFORM);
    }
    public void endFrame() {}
}
