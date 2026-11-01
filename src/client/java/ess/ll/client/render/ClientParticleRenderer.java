package ess.ll.client.render;

import ess.ll.client.event.EventBus;
import ess.ll.client.event.FrameDeltaEvent;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.WorldLoadEvent;
import ess.ll.client.integration.BaritoneVertexConsumerAccess;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.render.Particles;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.util.ModuleManager;
import ess.ll.client.util.PlayerUtils;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ClientParticleRenderer implements MinecraftAccess {
    private static final Quaternionf BILLBOARD_ROTATION = Axis.ZP.rotationDegrees(90.0F);
    private final Quaternionf cameraRotation = new Quaternionf();
    private final Vector3f rotatedRight = new Vector3f().set(1.0F, 0.0F, 0.0F);
    private final Vector3f rotatedUp = new Vector3f().set(0.0F, 1.0F, 0.0F);
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();
    private final Vector3f cameraForward = new Vector3f();
    private final Vector3f transformedRight = new Vector3f();
    private final Vector3f transformedUp = new Vector3f();
    private final Vector3f transformedRotatedRight = new Vector3f();
    private final Vector3f transformedRotatedUp = new Vector3f();
    private final Quaternionf particleRotation = new Quaternionf();
    private RenderType renderType;
    private int textureMask = -1;

    @Subscribe
    private void onFrameDelta(FrameDeltaEvent event) {
        this.updateAtlas(ModuleManager.INSTANCE.particles());
        if (!ClientParticleManager.INSTANCE.isEmpty()) {
            ClientParticleManager.INSTANCE.updateVisibility();
        }
    }

    private static void emitQuad(BaritoneVertexConsumerAccess baritoneVertexConsumerAccess, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, int var12) {
        baritoneVertexConsumerAccess.texturedVertexPre(var1 - var4 - var7, var2 - var5 - var8, var3 - var6 - var9, var10, 0.0F, var12);
        baritoneVertexConsumerAccess.texturedVertexPre(var1 + var4 - var7, var2 + var5 - var8, var3 + var6 - var9, var10, 1.0F, var12);
        baritoneVertexConsumerAccess.texturedVertexPre(var1 + var4 + var7, var2 + var5 + var8, var3 + var6 + var9, var11, 1.0F, var12);
        baritoneVertexConsumerAccess.texturedVertexPre(var1 - var4 + var7, var2 - var5 + var8, var3 - var6 + var9, var11, 0.0F, var12);
    }

    private static int packAdditiveColor(float var0, float var1, float var2) {
        float var3 = Math.max(var0, Math.max(var1, var2)) / 2.0F;
        if (var3 <= 0.0F) {
            return 0;
        }
        int var4 = Math.min(255, Math.max(1, Math.round(var3)));
        float var5 = 255.0F / (var4 * 2.0F);
        return var4 << 24 | clampChannel(var0 * var5) << 16 | clampChannel(var1 * var5) << 8 | clampChannel(var2 * var5);
    }

    private static int clampChannel(float var0) {
        return Math.min(255, Math.max(0, Math.round(var0)));
    }

    @Subscribe
    private void onTick(TickEvent event) {
        ClientParticleManager.INSTANCE.tick();
    }

    private static int combineAdditiveColors(int var0, int var1) {
        float var2 = ColorUtils.alpha(var0) * 0.003921569F;
        float var3 = ColorUtils.alpha(var1) * 0.003921569F;
        return packAdditiveColor(ColorUtils.getRed(var0) * var2 + ColorUtils.getRed(var1) * var3, ColorUtils.getGreen(var0) * var2 + ColorUtils.getGreen(var1) * var3, ColorUtils.getBlue(var0) * var2 + ColorUtils.getBlue(var1) * var3);
    }

    private void updateAtlas(Particles particles) {
        int var2 = selectedTextureMask(particles);
        if (var2 == 0) {
            this.renderType = null;
        } else {
            if (var2 != this.textureMask) {
                ParticleAtlas.INSTANCE.select(var2);
                if (ParticleAtlas.INSTANCE.getRenderType() == null) {
                    return;
                }
                this.textureMask = var2;
            }
            this.renderType = ParticleAtlas.INSTANCE.getRenderType();
        }
    }

    public static int premultiplyColor(int var0) {
        float var1 = ColorUtils.alpha(var0) * 0.003921569F;
        return packAdditiveColor(ColorUtils.getRed(var0) * var1, ColorUtils.getGreen(var0) * var1, ColorUtils.getBlue(var0) * var1);
    }

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        Particles particles2 = ModuleManager.INSTANCE.particles();
        if (!ClientParticleManager.INSTANCE.isEmpty()) {
            if (this.renderType == null) {
                this.updateAtlas(particles2);
            }
            RenderType renderType2 = this.renderType;
            int slotCount = ParticleAtlas.INSTANCE.getSlotCount();
            if (renderType2 != null && slotCount != 0) {
                if (event.getVertexCollector().getBuffer(renderType2) instanceof BaritoneVertexConsumerAccess var6) {
                    float var7 = PlayerUtils.tickDelta();
                    boolean enabled = particles2.getGlow().isEnabled();
                    Vec3 position2 = event.getCamera().position();
                    this.cameraRotation.set(mc.getEntityRenderDispatcher().camera.rotation()).mul(BILLBOARD_ROTATION);
                    this.cameraRight.set(1.0F, 0.0F, 0.0F);
                    this.cameraUp.set(0.0F, 1.0F, 0.0F);
                    this.cameraRotation.transform(this.cameraRight);
                    this.cameraRotation.transform(this.cameraUp);
                    this.cameraRight.cross(this.cameraUp, this.cameraForward).normalize();
                    Matrix4f matrix = event.getPoseStack().last().pose();
                    matrix.transformDirection(this.transformedRight.set(this.cameraRight));
                    matrix.transformDirection(this.transformedUp.set(this.cameraUp));
                    for (ClientParticleManager.RenderEntry renderEntry : ClientParticleManager.INSTANCE.getParticles()) {
                        if (renderEntry.isVisible()) {
                            ClientParticle particle = renderEntry.getParticle();
                            if (particle.isAlive()) {
                                float var14 = renderEntry.interpolateOpacity(var7);
                                if (!(var14 <= 0.001F)) {
                                    float var15 = particle.interpolateScale(var7);
                                    if (!(var15 <= 0.001F)) {
                                        float var16 = particle.getAngularVelocity() != 0.0F ? particle.interpolateRotation(var7) : 0.0F;
                                        if (var16 != 0.0F) {
                                            this.particleRotation.setAngleAxis(var16, this.cameraForward.x, this.cameraForward.y, this.cameraForward.z);
                                            this.particleRotation.transform(this.rotatedRight.set(this.cameraRight));
                                            this.particleRotation.transform(this.rotatedUp.set(this.cameraUp));
                                            matrix.transformDirection(this.transformedRotatedRight.set(this.rotatedRight));
                                            matrix.transformDirection(this.transformedRotatedUp.set(this.rotatedUp));
                                        } else {
                                            this.rotatedRight.set(this.cameraRight);
                                            this.rotatedUp.set(this.cameraUp);
                                            this.transformedRotatedRight.set(this.transformedRight);
                                            this.transformedRotatedUp.set(this.transformedUp);
                                        }
                                        int var17 = (int) (renderEntry.getTextureOffset() * slotCount);
                                        if (var17 >= slotCount) {
                                            var17 = slotCount - 1;
                                        }
                                        this.renderParticle(var6, matrix, position2, renderEntry, var14, var15, enabled, particle.interpolateX(var7), particle.interpolateY(var7), particle.interpolateZ(var7), ParticleAtlas.INSTANCE.getMinU(var17), ParticleAtlas.INSTANCE.getMaxU(var17));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderParticle(BaritoneVertexConsumerAccess baritoneVertexConsumerAccess, Matrix4f matrix, Vec3 vector, ClientParticleManager.RenderEntry renderEntry, float var5, float var6, boolean var7, double var8, double var10, double var12, float var14, float var15) {
        float var16 = renderEntry.getSize() * var6;
        float var17 = var16 * 0.5F;
        var5 *= 0.75F;
        int var18 = ColorUtils.multiplyAlpha(renderEntry.getColor(), var5);
        float var19 = (float) (var8 - vector.x) + this.rotatedUp.x * var17;
        float var20 = (float) (var10 - vector.y) + this.rotatedUp.y * var17;
        float var21 = (float) (var12 - vector.z) + this.rotatedUp.z * var17;
        float var22 = matrix.m00();
        float var23 = matrix.m10();
        float var24 = matrix.m20();
        float var25 = matrix.m30();
        float var26 = matrix.m01();
        float var27 = matrix.m11();
        float var28 = matrix.m21();
        float var29 = matrix.m31();
        float var30 = matrix.m02();
        float var31 = matrix.m12();
        float var32 = matrix.m22();
        float var33 = matrix.m32();
        float var34 = var22 * var19 + var23 * var20 + var24 * var21 + var25;
        float var35 = var26 * var19 + var27 * var20 + var28 * var21 + var29;
        float var36 = var30 * var19 + var31 * var20 + var32 * var21 + var33;
        float var37 = this.transformedRotatedRight.x * var16;
        float var38 = this.transformedRotatedRight.y * var16;
        float var39 = this.transformedRotatedRight.z * var16;
        float var40 = this.transformedRotatedUp.x * var16;
        float var41 = this.transformedRotatedUp.y * var16;
        float var42 = this.transformedRotatedUp.z * var16;
        int var43 = ColorUtils.withAlpha(var18, (int) (128.0F * var5));
        emitQuad(baritoneVertexConsumerAccess, var34, var35, var36, var37, var38, var39, var40, var41, var42, var14, var15, combineAdditiveColors(var18, var43));
        if (var7) {
            emitQuad(baritoneVertexConsumerAccess, var34, var35, var36, var37 * 2.0F, var38 * 2.0F, var39 * 2.0F, var40 * 2.0F, var41 * 2.0F, var42 * 2.0F, var14, var15, premultiplyColor(ColorUtils.withAlpha(var43, (int) (64.0F * var5))));
            emitQuad(baritoneVertexConsumerAccess, var34, var35, var36, var37 * 4.0F, var38 * 4.0F, var39 * 4.0F, var40 * 4.0F, var41 * 4.0F, var42 * 4.0F, var14, var15, premultiplyColor(ColorUtils.withAlpha(var43, (int) (32.0F * var5))));
        }
    }

    private static int selectedTextureMask(Particles particles) {
        var value = particles.getTextureOptions().getValue();
        int var2 = 0;
        for (int var3 = 0; var3 < value.size() && var3 < 11; var3++) {
            if (Boolean.TRUE.equals(((NamedOption) value.get(var3)).getValue())) {
                var2 |= 1 << var3;
            }
        }
        return var2;
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        ClientParticleManager.INSTANCE.clear();
    }

    public void registerListeners() {
        EventBus.register(this);
    }
}
