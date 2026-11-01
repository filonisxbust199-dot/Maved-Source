package ess.ll.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import ess.ll.client.weather.FrostFoliage;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WorldTweaks snow: leaves and plants get snow caps painted into their sprites while it snows. */
@Mixin(SpriteContents.class)
public abstract class WeatherSpriteMixin {
    @Shadow @Final private NativeImage originalImage;

    @Inject(method="<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V",at=@At("RETURN"))
    private void ess$snowCap(Identifier id,FrameSize size,NativeImage image,Optional<?> animation,List<?> metadata,Optional<?> texture,CallbackInfo ci) {
        if(FrostFoliage.shouldApplyFrost(id)) FrostFoliage.applyFrost(id,originalImage);
    }
}
