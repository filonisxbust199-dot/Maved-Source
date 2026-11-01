package ess.ll.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface DispatcherEquipmentAccessor {
    @Accessor("equipmentAssets") EquipmentAssetManager ess$equipmentAssets();
    @Accessor("atlasManager") AtlasManager ess$atlasManager();
    @Accessor("palettedTextures") PalettedTextureManager ess$palettedTextures();
}
