package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import ess.ll.client.event.HudRenderEvent;
import ess.ll.client.event.PostPlayerInputEvent;
import ess.ll.client.event.Render3DEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.features.combat.ProjectileTrajectoryTracker;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.render.MsdfFont;
import ess.ll.client.render.Theme;
import ess.ll.client.render.UiRenderer;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.ToggleSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Predictions: where thrown and shot projectiles will go. The path is a glowing ribbon with pulses running to
 * the landing point; the landing point is a glowing disk lying on the block face (red on an entity, the splash
 * radius for a splash potion); a plate over it names the projectile with its distance and flight time.
 * Everything is drawn by the prediction shader, additively, so it glows over the world.
 */
public class ProjectileHelper extends ClientModule {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath("ess-best", "pipeline/prediction")).withVertexShader(Identifier.fromNamespaceAndPath("ess-best", "core/prediction")).withFragmentShader(Identifier.fromNamespaceAndPath("ess-best", "core/prediction")).withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS).withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build());

    /**
     * The helper whose paths wait for the late draw this frame.
     */
    private static ProjectileHelper drawn;

    static {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.END_MAIN.register(context -> drawAfterTranslucent());
    }

    private static final int PATH = 0, LANDING = 1, HIT = 2, SPLASH = 3;
    private static final int HIT_COLOR = 0xff3c3c;

    /**
     * Radius of a splash potion's effect, and of the landing disk.
     */
    private static final float SPLASH_RADIUS = 4.0F, LANDING_RADIUS = 0.75F;
    private final MultiToggleSetting predict;
    private final MultiToggleSetting preview;
    private final ToggleSetting time;
    private final ToggleSetting plate;
    private final List<ProjectileTrajectoryTracker.TrajectoryPath> paths;
    private Vec3 lastMotion;
    private Vec3 motion;
    private final Vector3f point = new Vector3f();
    private Vec3 eye = Vec3.ZERO;

    /**
     * Path segments drawn at most; long flights are thinned out, so a path costs the same at any length.
     */
    private static final int MAX_SEGMENTS = 64;

    /**
     * Close to the camera the ribbon would fill the screen when looking along it, so it starts this far away.
     */
    private static final double NEAR = 2.0;

    public ProjectileHelper() {
        super(ModuleCategory.Render, ModuleSubCategory.World);
        this.predict = new MultiToggleSetting("Предиктить").addOption(NamedOption.of("Эндер жемчуг", true)).addOption(NamedOption.of("Снежок", false)).addOption(NamedOption.of("Заряд ветра", false)).addOption(NamedOption.of("Взрывное зелье", false)).addOption(NamedOption.of("Стрела", true)).addOption(NamedOption.of("Предмет", false)).addOption(NamedOption.of("Трезубец", false)).withContainer(this);
        this.preview = new MultiToggleSetting("Пред показ").addOption(NamedOption.of("Эндер жемчуг", true)).addOption(NamedOption.of("Снежок", false)).addOption(NamedOption.of("Заряд ветра", false)).addOption(NamedOption.of("Взрывное зелье", false)).addOption(NamedOption.of("Стрела", true)).addOption(NamedOption.of("Предмет", false)).addOption(NamedOption.of("Трезубец", false)).withContainer(this);
        this.time = new ToggleSetting("Считать время").withDefault(false).withContainer(this);
        this.plate = new ToggleSetting("Плашка приземления").withDefault(true).withContainer(this);
        this.paths = new ArrayList<>();
        this.lastMotion = Vec3.ZERO;
        this.motion = Vec3.ZERO;
        this.setName("Projectile Helper");
    }

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null)
            return;
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        this.paths.clear();
        this.paths.addAll(ProjectileTrajectoryTracker.INSTANCE.getHeldPaths(this, partial, this.lastMotion.lerp(this.motion, partial)));
        this.paths.addAll(ProjectileTrajectoryTracker.INSTANCE.getEntityPaths());
        this.eye = event.getCamera().position();
        drawn = this.paths.isEmpty() ? null : this;
    }

    /**
     * Fabric's stage after translucent terrain: the paths are drawn here, after water and ice, so those do not
     * cover them; depth still hides them behind solid blocks. The view matrix is the one captured for the frame
     * (ScreenProjectionMath), the projection is the level's own at this point.
     */
    private static void drawAfterTranslucent() {
        ProjectileHelper helper = drawn;
        drawn = null;
        if (helper == null || helper.paths.isEmpty())
            return;
        var builder = ess.ll.client.render.targetfx.internal.GpuResourceScope.getGpuResourceScope().getBufferBuilder(PrimitiveTopology.QUADS, DefaultVertexFormat.ENTITY);
        Matrix4f view = ess.ll.client.render.targetfx.internal.ScreenProjectionMath.viewMatrixCopy();
        int clock = (int) (System.currentTimeMillis() % 2000L * 1000L / 2000L);
        for (ProjectileTrajectoryTracker.TrajectoryPath path : helper.paths) {
            helper.ribbon(builder, view, helper.eye, path, clock);
            helper.landing(builder, view, helper.eye, path, clock);
        }
        var mesh = builder.build();
        if (mesh == null)
            return;
        try (mesh) {
            var state = mesh.drawState();
            int indices = state.indexCount();
            var sequence = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(state.primitiveTopology());
            var indexBuffer = sequence.getBuffer(indices);
            var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
            try (var vertices = device.createBuffer(() -> "ESS predictions", 32, mesh.vertexBuffer())) {
                var target = mc.gameRenderer.mainRenderTarget();
                var transforms = com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(), new org.joml.Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
                try (var pass = device.createCommandEncoder().createRenderPass(() -> "ESS predictions", target.getColorTextureView(), java.util.Optional.empty(), target.getDepthTextureView(), java.util.OptionalDouble.empty())) {
                    pass.setPipeline(com.mojang.blaze3d.systems.RenderSystem.getCompiledPipeline(PIPELINE));
                    com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setVertexBuffer(0, vertices.slice());
                    pass.setIndexBuffer(indexBuffer, sequence.type());
                    pass.drawIndexed(indices, 1, 0, 0, 0);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[ESS] Predictions draw failed: " + e);
        }
    }

    @Subscribe
    private void onPostPlayerInput(PostPlayerInputEvent event) {
        this.lastMotion = this.motion;
        this.motion = mc.player != null ? mc.player.getDeltaMovement() : Vec3.ZERO;
        ProjectileTrajectoryTracker.INSTANCE.update(this);
    }

    public boolean isPreviewSelected(String name) {
        return this.preview.isSelected(name);
    }

    public boolean isPredictSelected(String name) {
        return this.predict.isSelected(name);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.paths.clear();
        this.lastMotion = Vec3.ZERO;
        this.motion = Vec3.ZERO;
    }

    /**
     * The path as a ribbon turned to the camera at every point. It is wider further away so it keeps its size on
     * screen; UV0 carries the distance along it for the running pulses.
     */
    private void ribbon(VertexConsumer out, Matrix4f pose, Vec3 eye, ProjectileTrajectoryTracker.TrajectoryPath path, int clock) {
        List<Vec3> points = path.points;
        int count = points.size();
        if (count < 2)
            return;
        int color = (path.impactEntityId >= 0 ? HIT_COLOR : path.color) & 0xffffff;
        int step = Math.max(1, (count - 1 + MAX_SEGMENTS - 1) / MAX_SEGMENTS);
        float along = 0;
        Vec3 previous = points.get(0);
        double[] side = this.side(eye, previous, points.get(Math.min(step, count - 1)));
        for (int i = step; ; i += step) {
            if (i >= count - 1 + step)
                break;
            int index = Math.min(i, count - 1);
            Vec3 next = points.get(index);
            Vec3 after = index + step < count ? points.get(index + step) : next.add(next.subtract(previous));
            double[] nextSide = this.side(eye, next, after);
            float length = (float) previous.distanceTo(next);
            if (length < 1e-4F) {
                if (index == count - 1)
                    break;
                continue;
            }
            if (previous.distanceToSqr(eye) < NEAR * NEAR && next.distanceToSqr(eye) < NEAR * NEAR) {
                along += length;
                previous = next;
                side = nextSide;
                if (index == count - 1)
                    break;
                continue;
            }
            this.vertex(out, pose, eye, previous.x - side[0], previous.y - side[1], previous.z - side[2], color, 220, along, -1, PATH, clock);
            this.vertex(out, pose, eye, previous.x + side[0], previous.y + side[1], previous.z + side[2], color, 220, along, 1, PATH, clock);
            this.vertex(out, pose, eye, next.x + nextSide[0], next.y + nextSide[1], next.z + nextSide[2], color, 220, along + length, 1, PATH, clock);
            this.vertex(out, pose, eye, next.x - nextSide[0], next.y - nextSide[1], next.z - nextSide[2], color, 220, along + length, -1, PATH, clock);
            along += length;
            previous = next;
            side = nextSide;
            if (index == count - 1)
                break;
        }
    }

    /**
     * Half-width offset across the path at a point, perpendicular to both the path and the view.
     */
    private double[] side(Vec3 eye, Vec3 at, Vec3 towards) {
        double dx = towards.x - at.x, dy = towards.y - at.y, dz = towards.z - at.z;
        double vx = at.x - eye.x, vy = at.y - eye.y, vz = at.z - eye.z;
        double sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1e-6)
            return new double[] { 0, 0, 0 };
        double half = 0.07 + Math.sqrt(vx * vx + vy * vy + vz * vz) * 0.006;
        return new double[] { sx / length * half, sy / length * half, sz / length * half };
    }

    /**
     * The disk on the face that is hit; a splash potion lands with its effect radius on the ground instead.
     */
    private void landing(VertexConsumer out, Matrix4f pose, Vec3 eye, ProjectileTrajectoryTracker.TrajectoryPath path, int clock) {
        if (!path.hit || path.impactPosition == null)
            return;
        Vec3 at = path.impactPosition;
        int mode;
        float radius;
        Vec3 normal;
        int color = path.color & 0xffffff;
        if (path.local && path.impactEntityId < 0) {
            mode = SPLASH;
            radius = SPLASH_RADIUS;
            normal = new Vec3(0, 1, 0);
        } else if (path.impactEntityId >= 0) {
            mode = HIT;
            radius = LANDING_RADIUS;
            color = HIT_COLOR;
            normal = path.impactNormal != null ? path.impactNormal : eye.subtract(at);
        } else {
            mode = LANDING;
            radius = LANDING_RADIUS;
            normal = path.impactNormal != null ? path.impactNormal : new Vec3(0, 1, 0);
        }
        normal = normal.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : normal.normalize();
        Vec3 helper = Math.abs(normal.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = normal.cross(helper).normalize().scale(radius), v = normal.cross(u.normalize()).scale(radius);
        // Lifted off the face so it does not sink into the block.
        Vec3 centre = at.add(normal.scale(0.02));
        float[][] corners = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, 1 } };
        for (float[] c : corners) {
            this.vertex(out, pose, eye, centre.x + u.x * c[0] + v.x * c[1], centre.y + u.y * c[0] + v.y * c[1], centre.z + u.z * c[0] + v.z * c[1], color, 255, c[0], c[1], mode, clock);
        }
    }

    private void vertex(VertexConsumer out, Matrix4f pose, Vec3 eye, double x, double y, double z, int color, int strength, float u, float v, int mode, int clock) {
        pose.transformPosition((float) (x - eye.x), (float) (y - eye.y), (float) (z - eye.z), this.point);
        out.addVertex(this.point.x, this.point.y, this.point.z, strength << 24 | color, u, v, mode | clock << 16, 0, 0, 1, 0);
    }

    /**
     * The plate over each landing point: the projectile's item, its name and how far it lands, with the flight
     * time when "Считать время" is on, joined to the point by a short stem.
     */
    @Subscribe
    private void onHudRender(HudRenderEvent event) {
        if (!this.plate.getValue() || mc.level == null || mc.player == null)
            return;
        GuiGraphicsExtractor g = event.getGraphics();
        var font = MsdfFont.UI;
        for (ProjectileTrajectoryTracker.TrajectoryPath path : this.paths) {
            if (!path.hit || path.impactPosition == null)
                continue;
            Vector2f at = projectImpact(path.impactPosition);
            if (at == null)
                continue;
            boolean hit = path.impactEntityId >= 0;
            int accent = 0xff000000 | (hit ? HIT_COLOR : path.color & 0xffffff);
            String name = path.name != null ? path.name : "Снаряд";
            String info = String.format(Locale.ROOT, "%.1f м", mc.player.position().distanceTo(path.impactPosition));
            if (this.time.getValue() && path.flightTime > 0)
                info += String.format(Locale.ROOT, " · %.1f с", path.flightTime);
            if (hit)
                info += " · попадание";
            boolean icon = path.icon != null && !path.icon.isEmpty();
            float iconW = icon ? 14 : 0, textW = Math.max(font.width(name, 7.5F), font.width(info, 6.5F));
            float w = 8 + iconW + textW + 8, h = 22, x = at.x - w / 2, y = at.y - h - 10;
            UiRenderer.rounded(g, at.x - .5F, y + h, 1, 7, .5F, Theme.withAlpha(accent, .8F));
            UiRenderer.rounded(g, at.x - 2, at.y - 2, 4, 4, 2, accent);
            UiRenderer.rounded(g, x, y, w, h, 6, Theme.withAlpha(Theme.background(), .88F));
            UiRenderer.outline(g, x, y, w, h, 6, .75F, Theme.withAlpha(accent, .55F));
            UiRenderer.rounded(g, x + 3, y + 5, 2, h - 10, 1, accent);
            if (icon) {
                g.pose().pushMatrix();
                try {
                    g.pose().translate(x + 8, y + h / 2 - 6);
                    g.pose().scale(12 / 16F, 12 / 16F);
                    g.item(path.icon, 0, 0);
                } finally {
                    g.pose().popMatrix();
                }
            }
            float tx = x + 8 + iconW;
            font.mid(g, name, tx, y + 7.5F, 7.5F, Theme.text());
            font.mid(g, info, tx, y + 15.5F, 6.5F, hit ? accent : Theme.secondary());
        }
    }

    private static Vector2f projectImpact(Vec3 point) {
        var camera = mc.gameRenderer.mainCamera();
        Vec3 offset = point.subtract(camera.position());
        Vector3f view = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z).rotate(new org.joml.Quaternionf(camera.rotation()).conjugate());
        if (view.z >= -0.05f)
            return null;
        float h = mc.getWindow().getGuiScaledHeight(), w = mc.getWindow().getGuiScaledWidth();
        float f = (float) (1.0 / Math.tan(Math.toRadians(camera.getFov()) / 2.0));
        float x = w / 2 + view.x * f * h / (-2 * view.z);
        float y = h / 2 - view.y * f * h / (-2 * view.z);
        return Float.isFinite(x) && Float.isFinite(y) && x >= 0 && x <= w && y >= 0 && y <= h ? new Vector2f(x, y) : null;
    }
}
