package ess.ll.client.bots;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.mixin.GameRendererTargetAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * The real bot screen: when the bot stands in the world the player has loaded, the game renders that world a
 * second time from the bot's eyes (the same renderer, shaders and entities as the main view, the player
 * included). Like the vanilla panorama capture, the extra pass runs before the frame's own extraction; the main
 * target is swapped for the bot's own target for that pass, so the picture lands straight in the texture the
 * Bots tab draws.
 */
public final class BotCamera {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("ess-best","bots/camera");
    /** Captures per second while the tab shows a bot. */
    private static final long INTERVAL=1_000_000_000L/12;
    private static TextureTarget target;
    private static ViewTexture texture;
    private static Bot wanted,shown;
    private static long lastCapture;
    private static boolean capturing;

    private BotCamera() {}

    /** True while the extra pass runs, so hooks can leave out the player's own hands. */
    public static boolean capturing() { return capturing; }
    /** The entity the pass looks out of; its body is not drawn. */
    public static boolean isEyes(net.minecraft.world.entity.Entity entity) { return capturing && entity==eyes; }
    private static net.minecraft.world.entity.Entity eyes;

    /** Whether the bot can be seen through the real renderer: it has to be an entity of the loaded level. */
    static boolean canShow(Bot bot) {
        var mc=Minecraft.getInstance();
        return Bots.sameWorld(bot) && mc.level.getEntity(bot.entityId) instanceof Player;
    }
    /** The tab asks every frame it draws the bot; the capture follows on the next frame. */
    static boolean request(Bot bot) {
        wanted=bot;
        return shown==bot && texture!=null;
    }
    /** The part of the capture that keeps the panel's aspect; targets are stored bottom row first. */
    static float[] uv(float aspect) {
        if(target==null) return new float[]{0,1,1,0};
        float frame=(float)target.width/target.height;
        if(frame>aspect) { float cut=(1-aspect/frame)/2; return new float[]{cut,1-cut,1,0}; }
        float cut=(1-frame/aspect)/2;
        return new float[]{0,1,1-cut,cut};
    }

    /** GameRenderer.extract HEAD: renders the requested bot's view, then puts the camera back for the frame. */
    public static void beforeExtract(GameRenderer renderer,DeltaTracker delta) {
        Bot bot=wanted;
        wanted=null;
        if(capturing || bot==null) return;
        long now=System.nanoTime();
        if(now-lastCapture<INTERVAL && shown==bot) return;
        var mc=Minecraft.getInstance();
        if(!canShow(bot)) { if(shown==bot) shown=null; return; }
        eyes=mc.level.getEntity(bot.entityId);
        var camera=renderer.mainCamera();
        var previous=camera.entity();
        RenderTarget main=renderer.mainRenderTarget();
        prepare(main);
        lastCapture=now;
        capturing=true;
        renderer.setRenderBlockOutline(false);
        var access=(GameRendererTargetAccessor)renderer;
        try {
            camera.setEntity(eyes);
            renderer.update(delta);
            renderer.extract(delta,true);
            access.ess$setMainRenderTarget(target);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(),
                    new org.joml.Vector4f(0,0,0,1),target.getDepthTexture(),0);
            renderer.renderLevel();
            shown=bot;
        } catch(Exception e) {
            shown=null;
        } finally {
            access.ess$setMainRenderTarget(main);
            capturing=false;
            renderer.setRenderBlockOutline(true);
            camera.setEntity(previous);
            eyes=null;
            renderer.update(delta);
        }
    }

    /** The bot's target matches the main one in size, as the level renderer sizes its own targets by the window. */
    private static void prepare(RenderTarget main) {
        if(target!=null && target.width==main.width && target.height==main.height) return;
        if(target!=null) target.destroyBuffers();
        target=new TextureTarget("ESS bot camera",main.width,main.height,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
        if(texture==null) {
            texture=new ViewTexture();
            Minecraft.getInstance().getTextureManager().register(ID,texture);
        }
        texture.use(target);
    }

    /** Wraps the bot target's colour texture; the target owns it, so closing does not free it. */
    private static final class ViewTexture extends AbstractTexture {
        void use(RenderTarget source) {
            texture=source.getColorTexture();
            textureView=source.getColorTextureView();
            sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
        @Override public void close() {}
    }
}
