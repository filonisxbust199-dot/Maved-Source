package ess.ll.client.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import ess.ll.client.module.render.ShaderESP;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ShaderESP: post chain uniforms are fixed when the chain loads, so the chain's EssEsp block is bound to the
 * buffer uploaded this frame with the module's current settings.
 */
@Mixin(PostPass.class)
public abstract class ShaderEspUniformMixin {
    @Redirect(method="lambda$addToFrame$1",at=@At(value="INVOKE",
        target="Lcom/mojang/renderpearl/api/commands/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;)V"))
    private void ess$liveEspUniforms(RenderPass pass,String name,GpuBuffer buffer) {
        var live=ShaderESP.uniforms();
        if("EssEsp".equals(name) && live!=null) pass.setUniform(name,live);
        else pass.setUniform(name,buffer);
    }
}
