package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ess.ll.client.mixin.DispatcherEquipmentAccessor;
import ess.ll.client.render.GhostModelBatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

/** Keeps the actual equipment layers (including dyes and 26.3 trim palettes) with each ChronoTrail frame. */
final class ChronoArmor {
    record Surface(Identifier texture,int dye,UvMapping uv) {}
    record Piece(EquipmentSlot slot,List<Surface> surfaces,boolean foil) {}
    private static final Minecraft MC=Minecraft.getInstance();
    private static final PoseStack POSE=new PoseStack();
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static ArmorModelSet<PlayerModel> wide,slim;
    private ChronoArmor() {}

    static List<Piece> capture(AvatarRenderState state) {
        if(state.isSpectator || state.skin==null) return List.of();
        var access=(DispatcherEquipmentAccessor)MC.getEntityRenderDispatcher();
        var assets=access.ess$equipmentAssets();
        ArrayList<Piece> pieces=new ArrayList<>(4);
        captureSlot(pieces,access,assets,state,EquipmentSlot.CHEST,state.chestEquipment);
        captureSlot(pieces,access,assets,state,EquipmentSlot.LEGS,state.legsEquipment);
        captureSlot(pieces,access,assets,state,EquipmentSlot.FEET,state.feetEquipment);
        captureSlot(pieces,access,assets,state,EquipmentSlot.HEAD,state.headEquipment);
        return List.copyOf(pieces);
    }
    private static void captureSlot(List<Piece> result,DispatcherEquipmentAccessor access,
                                    net.minecraft.client.resources.model.EquipmentAssetManager assets,
                                    AvatarRenderState state,EquipmentSlot slot,ItemStack stack) {
        Equippable equippable=stack.get(DataComponents.EQUIPPABLE);
        if(equippable==null || equippable.slot()!=slot || equippable.assetId().isEmpty()) return;
        var asset=equippable.assetId().orElseThrow();
        LayerType type=slot==EquipmentSlot.LEGS?LayerType.HUMANOID_LEGGINGS:LayerType.HUMANOID;
        EquipmentClientInfo info=assets.get(asset);
        ArrayList<Surface> surfaces=new ArrayList<>(3);
        int dyed=DyedItemColor.getOrDefault(stack,0);
        for(EquipmentClientInfo.Layer layer:info.getLayers(type)) {
            int undyed=layer.dyeable().flatMap(dye->dye.colorWhenUndyed()).map(ARGB::opaque).orElse(0);
            int color=layer.dyeable().isPresent()?(dyed!=0?dyed:undyed):-1;
            if(color==0) continue;
            Identifier texture=layer.usePlayerTexture()?state.skin.body().texturePath():layer.getTextureLocation(type);
            surfaces.add(new Surface(texture,color,null));
        }
        ArmorTrim trim=stack.get(DataComponents.TRIM);
        if(trim!=null && !surfaces.isEmpty()) {
            Identifier pattern=trim.pattern().value().assetId();
            Identifier palette=trim.material().value().paletteId();
            for(EquipmentClientInfo.TrimOverride override:info.trimOverrides()) if(override.predicate().matches(trim)) {
                pattern=override.textureId().orElse(pattern);
                palette=override.paletteId().orElse(null);
                break;
            }
            Identifier base=pattern.withPath(path->type.trimAssetPrefix()+"/"+path);
            if(palette!=null) {
                var handle=access.ess$palettedTextures().getOrPrepare(base,palette);
                surfaces.add(new Surface(handle.textureLocation(),-1,handle));
            } else surfaces.add(new Surface(base.withPath(path->"textures/"+path+".png"),-1,null));
        }
        if(!surfaces.isEmpty()) result.add(new Piece(slot,List.copyOf(surfaces),stack.hasFoil()));
    }
    static void render(ChronoFrame frame,Vec3 camera,int tint,float glow) {
        if(frame.armor.isEmpty()) return;
        ArmorModelSet<PlayerModel> models=models(frame.slimArms);
        for(Piece piece:frame.armor) {
            PlayerModel model=models.get(piece.slot);
            ModelPart[] parts=ChronoFrame.parts(model);
            for(int i=0;i<parts.length;i++) { parts[i].loadPose(frame.parts[i]); parts[i].visible=frame.visible[i]; }
            for(Surface surface:piece.surfaces) {
                if(surface.texture==null) continue;
                VertexConsumer consumer=surface.uv==null?BATCH:surface.uv.wrap(BATCH);
                POSE.setIdentity(); POSE.pushPose();
                frame.applyTransform(POSE,frame.x-camera.x,frame.y-camera.y,frame.z-camera.z,1);
                BATCH.begin();
                int color=multiply(tint,surface.dye);
                for(int i=0;i<6;i++) if(parts[i].visible) parts[i].render(POSE,consumer,0xF000F0,0,color);
                POSE.popPose();
                BATCH.flush(surface.texture,glow*(piece.foil?1.15f:1),false,false);
            }
        }
    }
    /** Draw equipment into a separate depth mask for the Chams through-armor switch. */
    static void renderMask(ChronoFrame frame,Vec3 camera,RenderTarget target) {
        if(frame.armor.isEmpty()) return;
        ArmorModelSet<PlayerModel> models=models(frame.slimArms);
        for(Piece piece:frame.armor) {
            PlayerModel model=models.get(piece.slot);
            ModelPart[] parts=ChronoFrame.parts(model);
            for(int i=0;i<parts.length;i++) { parts[i].loadPose(frame.parts[i]); parts[i].visible=frame.visible[i]; }
            for(Surface surface:piece.surfaces) {
                if(surface.texture==null) continue;
                VertexConsumer consumer=surface.uv==null?BATCH:surface.uv.wrap(BATCH);
                POSE.setIdentity(); POSE.pushPose();
                frame.applyTransform(POSE,frame.x-camera.x,frame.y-camera.y,frame.z-camera.z,1);
                BATCH.begin();
                for(int i=0;i<6;i++) if(parts[i].visible) parts[i].render(POSE,consumer,0xF000F0,0,-1);
                POSE.popPose();
                BATCH.flushMaskTo(surface.texture,target);
            }
        }
    }
    private static ArmorModelSet<PlayerModel> models(boolean slimArms) {
        if(slimArms) {
            if(slim==null) slim=ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR,MC.getEntityModels(),part->new PlayerModel(part,true));
            return slim;
        }
        if(wide==null) wide=ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR,MC.getEntityModels(),part->new PlayerModel(part,false));
        return wide;
    }
    private static int multiply(int tint,int dye) {
        int r=(tint>>16&255)*(dye>>16&255)/255;
        int g=(tint>>8&255)*(dye>>8&255)/255;
        int b=(tint&255)*(dye&255)/255;
        return tint&0xff000000|r<<16|g<<8|b;
    }
}
