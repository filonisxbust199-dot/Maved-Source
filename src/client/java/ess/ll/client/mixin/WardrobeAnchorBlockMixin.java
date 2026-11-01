package ess.ll.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ess.ll.client.wardrobe.WardrobeWorld;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wardrobe: respawn anchors leave the chunk meshes while an anchor skin draws them instead. */
@Mixin(SectionCompiler.class)
public abstract class WardrobeAnchorBlockMixin {
    @WrapOperation(method="compile",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void ess$hideAnchor(ModelBlockRenderer renderer,BlockQuadOutput output,float x,float y,float z,BlockAndTintGetter level,BlockPos pos,
                                BlockState state,BlockStateModel model,long seed,Operation<Void> original) {
        if(!WardrobeWorld.hidesBlock(state)) original.call(renderer,output,x,y,z,level,pos,state,model,seed);
    }
}
