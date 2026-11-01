package ess.ll.client.mixin;

import ess.ll.client.module.render.ShaderESP;
import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ShaderESP: the entity outline post chain is replaced by ess-best:esp_glow while the module is on. */
@Mixin(LevelRenderer.class)
public abstract class ShaderEspChainMixin {
    @Redirect(method="render",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
    private PostChain ess$shaderEspChain(ShaderManager shaders,Identifier id,Set<Identifier> targets) {
        if(ShaderESP.active()) {
            PostChain chain=shaders.getPostChain(ShaderESP.CHAIN,targets);
            if(chain!=null) { ShaderESP.upload(); return chain; }
        }
        return shaders.getPostChain(id,targets);
    }
}
