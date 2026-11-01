package ess.ll.client.render;

import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ess.ll.client.render.targetfx.internal.GpuResourceScope;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

public final class WorldVertexCollector {
    private static final Map<RenderType, WorldVertexCollector.RecordedVertices> BUFFER_POOL = new IdentityHashMap<>();
    private static Map<RenderType, WorldVertexCollector.RecordedVertices> previousBuffers;
    private final SubmitNodeCollector collector;
    private final Map<RenderType, WorldVertexCollector.RecordedVertices> buffers = new LinkedHashMap<>();
    private RenderType lastRenderType;
    private WorldVertexCollector.RecordedVertices lastBuffer;
    private boolean submitted;
    private int submittedBatchCount;

    public void submit() {
        if (!this.submitted && !this.buffers.isEmpty()) {
            this.submitted = true;
            for (var var2 : this.buffers.entrySet()) {
                WorldVertexCollector.RecordedVertices value = var2.getValue();
                if (!value.isEmpty()) {
                    RenderType key = var2.getKey();
                    this.collector.order(0).submitCustomGeometry(new PoseStack(), key, (pose, consumer) -> value.replay(consumer, key == EffectRenderPipelines.LINES));
                    this.submittedBatchCount++;
                }
            }
        }
    }

    public int getSubmittedBatchCount() {
        return this.submittedBatchCount;
    }

    public VertexConsumer getBuffer(RenderType lastRenderType2) {
        if (lastRenderType2 == this.lastRenderType) {
            return this.lastBuffer;
        }
        WorldVertexCollector.RecordedVertices recordedVertices = this.buffers.computeIfAbsent(lastRenderType2, var0 -> {
            WorldVertexCollector.RecordedVertices recordedVertices2 = BUFFER_POOL.remove(var0);
            if (recordedVertices2 != null) {
                recordedVertices2.reset();
                return recordedVertices2;
            } else {
                return new WorldVertexCollector.RecordedVertices();
            }
        });
        this.lastRenderType = lastRenderType2;
        this.lastBuffer = recordedVertices;
        return recordedVertices;
    }

    /**
     * A collector that is not submitted with the frame: drawNow puts what it holds straight onto the main target at
     * the moment it is called. It keeps its own buffers apart from the per-frame pool.
     */
    public static WorldVertexCollector late() {
        return new WorldVertexCollector();
    }

    private WorldVertexCollector() {
        this.collector = null;
    }

    /**
     * Draws and empties a late collector, each render type with its own pipeline, textures and transform.
     */
    public void drawNow() {
        if (this.buffers.isEmpty())
            return;
        try {
            var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            var device = RenderSystem.getDevice();
            for (var entry : this.buffers.entrySet()) {
                WorldVertexCollector.RecordedVertices data = entry.getValue();
                if (data.isEmpty())
                    continue;
                RenderType type = entry.getKey();
                var builder = GpuResourceScope.getGpuResourceScope().getBufferBuilder(type.primitiveTopology(), type.format());
                data.replay(builder, type == EffectRenderPipelines.LINES);
                MeshData mesh = builder.build();
                if (mesh == null)
                    continue;
                try (mesh) {
                    var state = mesh.drawState();
                    var prepared = type.prepare();
                    var sequence = RenderSystem.getSequentialBuffer(state.primitiveTopology());
                    var indexBuffer = sequence.getBuffer(state.indexCount());
                    try (var vertices = device.createBuffer(() -> "ESS late geometry", 32, mesh.vertexBuffer());
                        var pass = device.createCommandEncoder().createRenderPass(() -> "ESS late geometry", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(RenderSystem.getCompiledPipeline(prepared.pipeline()));
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", prepared.dynamicTransforms());
                        for (var texture : prepared.textures()) pass.setUniform(texture.name(), texture.textureView(), texture.sampler());
                        pass.setVertexBuffer(0, vertices.slice());
                        pass.setIndexBuffer(indexBuffer, sequence.type());
                        pass.drawIndexed(state.indexCount(), 1, 0, 0, 0);
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[ESS] Late geometry draw failed: " + e);
        } finally {
            this.buffers.clear();
            this.lastRenderType = null;
            this.lastBuffer = null;
        }
    }

    public WorldVertexCollector(SubmitNodeCollector collector2) {
        this.collector = collector2;
        var var2 = previousBuffers;
        if (var2 != null && var2 != this.buffers) {
            BUFFER_POOL.putAll(var2);
        }
        previousBuffers = this.buffers;
    }

    private static final class RecordedVertices implements BaritoneVertexConsumerAccess, VertexConsumer {
        public VertexConsumer setUv3(float x, float y) {
            this.appendOpcode((byte) 13);
            this.appendFloat(x);
            this.appendFloat(y);
            return this;
        }

        private byte[] opcodes = new byte[64];
        private int opcodeCount;
        private float[] floatValues = new float[128];
        private int floatCount;
        private int[] intValues = new int[64];
        private int intCount;

        @Override
        public void lineVertexF(Matrix4f matrix, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
            float var13 = matrix.m00() * var2 + matrix.m10() * var3 + matrix.m20() * var4 + matrix.m30();
            float sx = matrix.m01() * var2 + matrix.m11() * var3 + matrix.m21() * var4 + matrix.m31();
            float sxx = matrix.m02() * var2 + matrix.m12() * var3 + matrix.m22() * var4 + matrix.m32();
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 12;
            this.ensureFloats(11);
            this.floatValues[this.floatCount++] = var13;
            this.floatValues[this.floatCount++] = sx;
            this.floatValues[this.floatCount++] = sxx;
            this.floatValues[this.floatCount++] = var5;
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
            this.floatValues[this.floatCount++] = var8;
            this.floatValues[this.floatCount++] = var9;
            this.floatValues[this.floatCount++] = var10;
            this.floatValues[this.floatCount++] = var11;
            this.floatValues[this.floatCount++] = var12;
        }

        @Override
        public void coloredVertex(Matrix4f matrix, float var2, float var3, float var4, int var5) {
            float var6 = matrix.m00() * var2 + matrix.m10() * var3 + matrix.m20() * var4 + matrix.m30();
            float var7 = matrix.m01() * var2 + matrix.m11() * var3 + matrix.m21() * var4 + matrix.m31();
            float var8 = matrix.m02() * var2 + matrix.m12() * var3 + matrix.m22() * var4 + matrix.m32();
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 10;
            this.ensureFloats(3);
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
            this.floatValues[this.floatCount++] = var8;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var5;
        }

        @Override
        public void lineVertex(Matrix4f matrix, float var2, float var3, float var4, int var5, float var6, float var7, float var8, float var9) {
            float var10 = matrix.m00() * var2 + matrix.m10() * var3 + matrix.m20() * var4 + matrix.m30();
            float var11 = matrix.m01() * var2 + matrix.m11() * var3 + matrix.m21() * var4 + matrix.m31();
            float var12 = matrix.m02() * var2 + matrix.m12() * var3 + matrix.m22() * var4 + matrix.m32();
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 9;
            this.ensureFloats(7);
            this.floatValues[this.floatCount++] = var10;
            this.floatValues[this.floatCount++] = var11;
            this.floatValues[this.floatCount++] = var12;
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
            this.floatValues[this.floatCount++] = var8;
            this.floatValues[this.floatCount++] = var9;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var5;
        }

        @Override
        public VertexConsumer setUv(float var1, float var2) {
            this.appendOpcode((byte) 3);
            this.appendFloat(var1);
            this.appendFloat(var2);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float var1) {
            this.appendOpcode((byte) 7);
            this.appendFloat(var1);
            return this;
        }

        private void ensureFloats(int var1) {
            int var2 = this.floatCount + var1;
            if (var2 > this.floatValues.length) {
                int length2 = this.floatValues.length;
                while (length2 < var2) {
                    length2 <<= 1;
                }
                this.floatValues = Arrays.copyOf(this.floatValues, length2);
            }
        }

        @Override
        public void coloredVertexPre(float var1, float var2, float var3, int var4) {
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 10;
            this.ensureFloats(3);
            this.floatValues[this.floatCount++] = var1;
            this.floatValues[this.floatCount++] = var2;
            this.floatValues[this.floatCount++] = var3;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var4;
        }

        @Override
        public VertexConsumer setUv2(int var1, int var2) {
            this.appendOpcode((byte) 5);
            this.appendInt(var1);
            this.appendInt(var2);
            return this;
        }

        @Override
        public VertexConsumer setColor(int var1) {
            this.appendOpcode((byte) 2);
            this.appendInt(var1);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float var1, float var2, float var3) {
            this.appendOpcode((byte) 6);
            this.appendFloat(var1);
            this.appendFloat(var2);
            this.appendFloat(var3);
            return this;
        }

        @Override
        public void texturedVertexPre(float var1, float var2, float var3, float var4, float var5, int var6) {
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 8;
            this.ensureFloats(5);
            this.floatValues[this.floatCount++] = var1;
            this.floatValues[this.floatCount++] = var2;
            this.floatValues[this.floatCount++] = var3;
            this.floatValues[this.floatCount++] = var4;
            this.floatValues[this.floatCount++] = var5;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var6;
        }

        @Override
        public void lineVertexPre(float var1, float var2, float var3, int var4, float var5, float var6, float var7, float var8) {
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 9;
            this.ensureFloats(7);
            this.floatValues[this.floatCount++] = var1;
            this.floatValues[this.floatCount++] = var2;
            this.floatValues[this.floatCount++] = var3;
            this.floatValues[this.floatCount++] = var5;
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
            this.floatValues[this.floatCount++] = var8;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var4;
        }

        @Override
        public VertexConsumer setUv1(int var1, int var2) {
            this.appendOpcode((byte) 4);
            this.appendInt(var1);
            this.appendInt(var2);
            return this;
        }

        @Override
        public void coloredVertexF(Matrix4f matrix, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
            float var9 = matrix.m00() * var2 + matrix.m10() * var3 + matrix.m20() * var4 + matrix.m30();
            float var10 = matrix.m01() * var2 + matrix.m11() * var3 + matrix.m21() * var4 + matrix.m31();
            float var11 = matrix.m02() * var2 + matrix.m12() * var3 + matrix.m22() * var4 + matrix.m32();
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 11;
            this.ensureFloats(7);
            this.floatValues[this.floatCount++] = var9;
            this.floatValues[this.floatCount++] = var10;
            this.floatValues[this.floatCount++] = var11;
            this.floatValues[this.floatCount++] = var5;
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
            this.floatValues[this.floatCount++] = var8;
        }

        private void reset() {
            this.opcodeCount = 0;
            this.floatCount = 0;
            this.intCount = 0;
        }

        @Override
        public VertexConsumer addVertex(float var1, float var2, float var3) {
            this.appendOpcode((byte) 0);
            this.appendFloat(var1);
            this.appendFloat(var2);
            this.appendFloat(var3);
            return this;
        }

        private void appendInt(int var1) {
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var1;
        }

        @Override
        public void coloredVertexFPre(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 11;
            this.ensureFloats(7);
            this.floatValues[this.floatCount++] = var1;
            this.floatValues[this.floatCount++] = var2;
            this.floatValues[this.floatCount++] = var3;
            this.floatValues[this.floatCount++] = var4;
            this.floatValues[this.floatCount++] = var5;
            this.floatValues[this.floatCount++] = var6;
            this.floatValues[this.floatCount++] = var7;
        }

        private boolean isEmpty() {
            return this.opcodeCount == 0;
        }

        private void appendOpcode(byte var1) {
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = var1;
        }

        private void appendFloat(float var1) {
            if (this.floatCount == this.floatValues.length) {
                this.floatValues = Arrays.copyOf(this.floatValues, this.floatCount << 1);
            }
            this.floatValues[this.floatCount++] = var1;
        }

        @Override
        public void texturedVertex(Matrix4f matrix, float var2, float var3, float var4, float var5, float var6, int var7) {
            float var8 = matrix.m00() * var2 + matrix.m10() * var3 + matrix.m20() * var4 + matrix.m30();
            float var9 = matrix.m01() * var2 + matrix.m11() * var3 + matrix.m21() * var4 + matrix.m31();
            float var10 = matrix.m02() * var2 + matrix.m12() * var3 + matrix.m22() * var4 + matrix.m32();
            if (this.opcodeCount == this.opcodes.length) {
                this.opcodes = Arrays.copyOf(this.opcodes, this.opcodeCount << 1);
            }
            this.opcodes[this.opcodeCount++] = 8;
            this.ensureFloats(5);
            this.floatValues[this.floatCount++] = var8;
            this.floatValues[this.floatCount++] = var9;
            this.floatValues[this.floatCount++] = var10;
            this.floatValues[this.floatCount++] = var5;
            this.floatValues[this.floatCount++] = var6;
            if (this.intCount == this.intValues.length) {
                this.intValues = Arrays.copyOf(this.intValues, this.intCount << 1);
            }
            this.intValues[this.intCount++] = var7;
        }

        private void replay(VertexConsumer vertexConsumer, boolean lines) {
            byte[] opcodes2 = this.opcodes;
            float[] floatValues2 = this.floatValues;
            int[] intValues2 = this.intValues;
            int var5 = 0;
            int var6 = 0;
            int var7 = 0;
            for (int opcodeCount2 = this.opcodeCount; var7 < opcodeCount2; var7++) {
                switch (opcodes2[var7]) {
                    case 0:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]);
                        break;
                    case 1:
                        vertexConsumer.setColor(intValues2[var6++], intValues2[var6++], intValues2[var6++], intValues2[var6++]);
                        break;
                    case 2:
                        vertexConsumer.setColor(intValues2[var6++]);
                        break;
                    case 3:
                        vertexConsumer.setUv(floatValues2[var5++], floatValues2[var5++]);
                        break;
                    case 4:
                        vertexConsumer.setUv1(intValues2[var6++], intValues2[var6++]);
                        break;
                    case 5:
                        vertexConsumer.setUv2(intValues2[var6++], intValues2[var6++]);
                        break;
                    case 6:
                        vertexConsumer.setNormal(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]);
                        break;
                    case 7:
                        vertexConsumer.setLineWidth(floatValues2[var5++]);
                        break;
                    case 8:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setUv(floatValues2[var5++], floatValues2[var5++]).setColor(intValues2[var6++]);
                        break;
                    case 9:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setColor(intValues2[var6++]).setNormal(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setLineWidth(floatValues2[var5++]);
                        break;
                    case 10:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setColor(intValues2[var6++]);
                        if (lines)
                            vertexConsumer.setNormal(0.0F, 1.0F, 0.0F).setLineWidth(1.0F);
                        break;
                    case 11:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setColor(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]);
                        break;
                    case 12:
                        vertexConsumer.addVertex(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setColor(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setNormal(floatValues2[var5++], floatValues2[var5++], floatValues2[var5++]).setLineWidth(floatValues2[var5++]);
                        break;
                    case 13:
                        vertexConsumer.setUv3(floatValues2[var5++], floatValues2[var5++]);
                        break;
                }
            }
        }

        @Override
        public VertexConsumer setColor(int var1, int var2, int var3, int var4) {
            this.appendOpcode((byte) 1);
            this.appendInt(var1);
            this.appendInt(var2);
            this.appendInt(var3);
            this.appendInt(var4);
            return this;
        }
    }
}
