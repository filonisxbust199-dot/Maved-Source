package ess.ll.client.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ess.ll.client.wardrobe.Wardrobe.Item;
import ess.ll.client.wardrobe.Wardrobe.Slot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal and anchor skins in the world (from Zenith's CrystalAnimation): placed end crystals are drawn as the worn
 * crystal skin, and respawn anchors within 6 chunks as the anchor skin, its charge bones lit by the anchor's charges
 * (the vanilla anchor block is left out of the chunk meshes meanwhile).
 */
public final class WardrobeWorld {
    /** Read by chunk meshing threads: anchors are skipped while an anchor skin is worn. */
    private static volatile boolean hideAnchors;
    private static final List<BlockPos> ANCHORS=new ArrayList<>();
    private static int scanTimer;

    private WardrobeWorld() {}

    private static Item worn(Slot slot) {
        if(ess.ll.client.module.ClientModules.WARDROBE==null || !ess.ll.client.module.ClientModules.WARDROBE.enabled()) return null;
        return Wardrobe.worn(slot);
    }
    public static boolean hidesBlock(BlockState state) { return hideAnchors && state.is(Blocks.RESPAWN_ANCHOR); }

    /** Draws a placed crystal as the skin; false leaves it to vanilla. */
    public static boolean crystal(EndCrystalRenderState state,PoseStack pose,SubmitNodeCollector collector) {
        Item item=worn(Slot.CRYSTAL);
        WardrobeModel model=item==null ? null : Wardrobe.model(item);
        if(model==null) return false;
        float time=state.ageInTicks/20;
        int id=(int)(Double.doubleToLongBits(state.x*31+state.z)^Double.doubleToLongBits(state.y));
        var cache=WardrobeAnimator.Cache.of(id,item.id(),false);
        cache.clips.want(model,"idle",time);
        float[] bones=cache.pose.compute(model,cache.clips,time,null);
        pose.pushPose();
        try {
            pose.translate(0,.5f,0);
            pose.scale(.5f,.5f,.5f);
            pose.translate(0,.775f,0);
            pose.rotate(Axis.XP.rotationDegrees(180));
            pose.scale(2,2,2);
            WardrobeRenderer.draw(model,bones,WardrobeModel.FREE,pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,true,item.rarity().glows,-1,false);
        } finally { pose.popPose(); }
        return true;
    }

    /** Client tick: follows the anchor skin being put on or off, and rescans the anchors around you. */
    public static void tick(Minecraft mc) {
        boolean on=mc.level!=null && mc.player!=null && worn(Slot.ANCHOR)!=null && Wardrobe.model(worn(Slot.ANCHOR))!=null;
        if(on!=hideAnchors) {
            hideAnchors=on;
            // The sections with anchors are meshed again, without them or with them back.
            if(on) scan(mc);
            if(mc.level!=null) for(BlockPos p:ANCHORS) mc.level.setSectionDirtyWithNeighbors(p.getX()>>4,p.getY()>>4,p.getZ()>>4);
            scanTimer=5;
        }
        if(!on) { ANCHORS.clear(); return; }
        if(--scanTimer>0) return;
        scanTimer=5;
        scan(mc);
    }
    private static void scan(Minecraft mc) {
        ANCHORS.clear();
        if(mc.level==null || mc.player==null) return;
        int cx=mc.player.chunkPosition().x(),cz=mc.player.chunkPosition().z();
        for(int x=cx-6;x<=cx+6;x++) for(int z=cz-6;z<=cz+6;z++) {
            LevelChunk chunk=mc.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
            if(chunk==null) continue;
            LevelChunkSection[] sections=chunk.getSections();
            for(int s=0;s<sections.length;s++) {
                LevelChunkSection section=sections[s];
                if(section==null || section.hasOnlyAir() || !section.maybeHas(b->b.is(Blocks.RESPAWN_ANCHOR))) continue;
                int baseY=chunk.getSectionYFromSectionIndex(s)<<4;
                for(int y=0;y<16;y++) for(int zz=0;zz<16;zz++) for(int xx=0;xx<16;xx++)
                    if(section.getBlockState(xx,y,zz).is(Blocks.RESPAWN_ANCHOR)) ANCHORS.add(new BlockPos((x<<4)+xx,baseY+y,(z<<4)+zz));
            }
        }
    }
    /** Draws the anchor skin on every anchor within 96 blocks; charge bones show only for the charges it has. */
    public static void anchors(PoseStack stack,SubmitNodeCollector collector) {
        var mc=Minecraft.getInstance();
        Item item=worn(Slot.ANCHOR);
        WardrobeModel model=item==null ? null : Wardrobe.model(item);
        if(!hideAnchors || model==null || ANCHORS.isEmpty() || mc.level==null) return;
        Vec3 camera=mc.gameRenderer.mainCamera().position();
        float time=(mc.level.getGameTime()+mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))/20;
        List<Integer> charges=new ArrayList<>();
        for(int b=0;b<model.bones();b++) if(model.boneNames[b].startsWith("charge")) charges.add(b);
        for(BlockPos pos:ANCHORS) {
            double dx=pos.getX()+.5-camera.x,dy=pos.getY()+.5-camera.y,dz=pos.getZ()+.5-camera.z;
            if(dx*dx+dy*dy+dz*dz>96*96) continue;
            BlockState state=mc.level.getBlockState(pos);
            if(!state.is(Blocks.RESPAWN_ANCHOR)) continue;
            var cache=WardrobeAnimator.Cache.of(pos.hashCode(),item.id(),false);
            cache.clips.want(model,"idle",time);
            float[] bones=cache.pose.compute(model,cache.clips,time,null);
            int charge=state.getValue(RespawnAnchorBlock.CHARGE);
            for(int i=charge;i<charges.size();i++) { int b=charges.get(i)*9; bones[b+6]=bones[b+7]=bones[b+8]=0; }
            int light=LightCoordsUtil.getLightCoords(mc.level,pos.above());
            stack.pushPose();
            try {
                stack.translate(dx,dy,dz);
                stack.rotate(Axis.XP.rotationDegrees(180));
                WardrobeRenderer.draw(model,bones,WardrobeModel.FREE,stack,collector,light,OverlayTexture.NO_OVERLAY,true,item.rarity().glows,-1,false);
            } finally { stack.popPose(); }
        }
    }
}
