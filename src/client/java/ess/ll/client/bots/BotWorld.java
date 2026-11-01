package ess.ll.client.bots;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

/**
 * What a bot knows of the world: the blocks of the chunks the server sent it and where other players stand.
 * It is drawn as the bot's "screen" by a small CPU ray caster (one ray per pixel through the block grid), so
 * a background bot needs no second level renderer.
 */
final class BotWorld {
    private static final int SKY=0xff86b0ff, SKY_LOW=0xffc6dcff, VOID=0xff0b0d12, WATER=0xff3f76e4;
    private final Long2ObjectOpenHashMap<LevelChunkSection[]> chunks=new Long2ObjectOpenHashMap<>();
    /** Other players: entity id → {x, y, z}. */
    final Int2ObjectOpenHashMap<double[]> players=new Int2ObjectOpenHashMap<>();
    private final Map<BlockState,Look> looks=new IdentityHashMap<>();
    private PalettedContainerFactory factory;
    private int minY,sections;
    private boolean underground;

    synchronized void reset(DimensionType type,RegistryAccess registries) {
        chunks.clear(); players.clear();
        factory=PalettedContainerFactory.create(registries);
        minY=type.minY(); sections=type.height()>>4;
        underground=type.hasCeiling();
    }

    synchronized void chunk(int cx,int cz,ClientboundLevelChunkPacketData data) {
        if(factory==null) return;
        var buf=data.getReadBuffer();
        var list=new LevelChunkSection[sections];
        try {
            for(int i=0;i<sections;i++) { list[i]=new LevelChunkSection(factory); list[i].read(buf); }
        } catch(RuntimeException e) { return; }
        chunks.put(ChunkPos.pack(cx,cz),list);
    }
    synchronized void forget(long chunk) { chunks.remove(chunk); }

    synchronized void set(BlockPos pos,BlockState state) {
        var list=chunks.get(ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4));
        int i=(pos.getY()-minY)>>4;
        if(list!=null && i>=0 && i<list.length) list[i].setBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15,state,false);
    }
    /** A block the bot was sent, or null when its chunk is not loaded. */
    synchronized BlockState blockAt(net.minecraft.core.BlockPos pos) { return block(pos.getX(),pos.getY(),pos.getZ()); }
    synchronized void player(int id,double x,double y,double z) { players.put(id,new double[]{x,y,z}); }
    synchronized void playerIfKnown(int id,double x,double y,double z) { if(players.containsKey(id)) players.put(id,new double[]{x,y,z}); }
    synchronized void move(int id,net.minecraft.network.protocol.game.VecDelta delta) {
        double[] p=players.get(id);
        if(p==null) return;
        var codec=new net.minecraft.network.protocol.game.VecDeltaCodec();
        codec.setBase(new net.minecraft.world.phys.Vec3(p[0],p[1],p[2]));
        var to=delta.decode(codec).endPosition();
        p[0]=to.x; p[1]=to.y; p[2]=to.z;
    }
    synchronized void remove(int id) { players.remove(id); }
    synchronized boolean empty() { return chunks.isEmpty(); }

    private BlockState block(int x,int y,int z) {
        var list=chunks.get(ChunkPos.pack(x>>4,z>>4));
        int i=(y-minY)>>4;
        if(list==null || i<0 || i>=list.length) return null;
        return list[i].getBlockState(x&15,y&15,z&15);
    }
    /** How a block is drawn: passed through, water, solid or cut out (leaves, glass), with a texture per face. */
    private static final int PASS=0,LIQUID=1,SOLID=2,CUTOUT=3;
    private record Look(int kind,int[][] texels,int[] sizes,int color) {}

    /**
     * The block's look from its real model: for each side the sprite of its first quad facing that way, read
     * from the texture, with the block's tint (grass, leaves) applied. Blocks without a model use their map colour.
     */
    private Look look(BlockState state) {
        return looks.computeIfAbsent(state,s->{
            if(s.isAir()) return new Look(PASS,null,null,0);
            if(s.getFluidState().is(Fluids.WATER) || s.getFluidState().is(Fluids.FLOWING_WATER)) return new Look(LIQUID,null,null,WATER);
            if(s.getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty() && s.getFluidState().isEmpty()) return new Look(PASS,null,null,0);
            MapColor map=s.getMapColor(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
            int color=map==MapColor.NONE ? 0xff808080 : 0xff000000|map.col;
            int kind=s.isSolidRender() ? SOLID : CUTOUT;
            int[][] texels=new int[6][];
            int[] sizes=new int[12];
            try {
                var mc=net.minecraft.client.Minecraft.getInstance();
                var model=mc.getModelManager().getBlockStateModelSet().get(s);
                var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                model.collectParts(net.minecraft.util.RandomSource.create(42),parts);
                for(var dir:net.minecraft.core.Direction.values()) {
                    net.minecraft.client.resources.model.geometry.BakedQuad quad=null;
                    for(var part:parts) {
                        var quads=part.getQuads(dir);
                        if(!quads.isEmpty()) { quad=quads.getFirst(); break; }
                        for(var q:part.getQuads(null)) if(q.direction()==dir) { quad=q; break; }
                        if(quad!=null) break;
                    }
                    if(quad==null) continue;
                    var info=quad.materialInfo();
                    var contents=info.sprite().contents();
                    var image=((ess.ll.client.mixin.SpriteContentsAccessor)contents).ess$image();
                    if(image==null) continue;
                    int w=contents.width(),h=contents.height();
                    int tint=-1;
                    if(info.isTinted()) {
                        var source=mc.getBlockColors().getTintSource(s,info.tintIndex());
                        if(source!=null) tint=source.color(s);
                    }
                    int[] px=new int[w*h];
                    for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                        int c=image.getPixel(x,y);
                        px[y*w+x]=tint==-1 ? c : (c&0xff000000)|multiply(c,tint);
                    }
                    texels[dir.ordinal()]=px; sizes[dir.ordinal()*2]=w; sizes[dir.ordinal()*2+1]=h;
                }
            } catch(RuntimeException ignored) {}
            return new Look(kind,texels,sizes,color);
        });
    }
    private static int multiply(int c,int tint) {
        int r=((c>>16)&255)*((tint>>16)&255)/255,g=((c>>8)&255)*((tint>>8)&255)/255,b=(c&255)*(tint&255)/255;
        return r<<16|g<<8|b;
    }
    /** Texel of a face at (u, v) in [0, 1); the map colour when the face has no texture. */
    private static int texel(Look look,int dir,double u,double v) {
        int[] px=look.texels()==null ? null : look.texels()[dir];
        if(px==null) return look.color();
        int w=look.sizes()[dir*2],h=look.sizes()[dir*2+1];
        int x=Math.clamp((int)(u*w),0,w-1),y=Math.clamp((int)(v*h),0,h-1);
        return px[y*w+x];
    }

    /**
     * Draws the view from the eye into {@code pixels} (ARGB, w × h). Players are boxes; {@code highlight}
     * gives them their colour (the main account stands out).
     */
    synchronized void render(int[] pixels,int w,int h,double ex,double ey,double ez,float yaw,float pitch,java.util.function.IntUnaryOperator highlight) {
        double fov=Math.toRadians(70),tan=Math.tan(fov/2),aspect=(double)w/h;
        double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
        // Camera basis in Minecraft's frame: yaw 0 looks along +Z, positive pitch looks down.
        double fx=-Math.sin(yr)*Math.cos(pr),fy=-Math.sin(pr),fz=Math.cos(yr)*Math.cos(pr);
        double rx=-Math.cos(yr),rz=-Math.sin(yr);
        // Up is forward × right, turned to point skywards.
        double ux=fy*rz,uy=fz*rx-fx*rz,uz=-fy*rx;
        double ul=Math.sqrt(ux*ux+uy*uy+uz*uz); ux/=ul; uy/=ul; uz/=ul;
        if(uy<0) { ux=-ux; uy=-uy; uz=-uz; }
        int[] ids=players.keySet().toIntArray();
        double[][] boxes=new double[ids.length][];
        for(int i=0;i<ids.length;i++) boxes[i]=players.get(ids[i]);
        for(int py=0;py<h;py++) {
            double sy=(1-2*(py+.5)/h)*tan;
            for(int px=0;px<w;px++) {
                double sx=(2*(px+.5)/w-1)*tan*aspect;
                double dx=fx+rx*sx+ux*sy,dy=fy+uy*sy,dz=fz+rz*sx+uz*sy;
                double len=Math.sqrt(dx*dx+dy*dy+dz*dz); dx/=len; dy/=len; dz/=len;
                pixels[py*w+px]=trace(ex,ey,ez,dx,dy,dz,ids,boxes,highlight);
            }
        }
    }

    private int trace(double ox,double oy,double oz,double dx,double dy,double dz,int[] ids,double[][] boxes,java.util.function.IntUnaryOperator highlight) {
        double max=64;
        // Players first: the nearest box hit limits how far the block ray goes.
        double hitT=max; int hitColor=0;
        for(int i=0;i<ids.length;i++) {
            double[] b=boxes[i];
            double t=box(ox,oy,oz,dx,dy,dz,b[0]-.3,b[1],b[2]-.3,b[0]+.3,b[1]+1.8,b[2]+.3);
            if(t>0 && t<hitT) { hitT=t; hitColor=highlight.applyAsInt(ids[i]); }
        }
        int x=(int)Math.floor(ox),y=(int)Math.floor(oy),z=(int)Math.floor(oz);
        int sx=dx>0 ? 1 : -1,sy=dy>0 ? 1 : -1,sz=dz>0 ? 1 : -1;
        double tdx=Math.abs(1/dx),tdy=Math.abs(1/dy),tdz=Math.abs(1/dz);
        double tx=(dx>0 ? x+1-ox : ox-x)*tdx,ty=(dy>0 ? y+1-oy : oy-y)*tdy,tz=(dz>0 ? z+1-oz : oz-z)*tdz;
        double t=0; int face=1; int tint=0; double tintAt=0;
        while(t<hitT) {
            BlockState state=t>0 ? block(x,y,z) : null;
            if(state!=null) {
                Look look=look(state);
                if(look.kind()==LIQUID) { if(tint==0) { tint=WATER; tintAt=t; } }
                else if(look.kind()!=PASS) {
                    // Where the ray entered the block, as texture coordinates on that side.
                    double hx=ox+dx*t,hy=oy+dy*t,hz=oz+dz*t;
                    double fx=hx-Math.floor(hx),fy=hy-Math.floor(hy),fz=hz-Math.floor(hz);
                    int dir; double u,v;
                    if(face==0) { dir=sx>0 ? 4 : 5; u=sx>0 ? fz : 1-fz; v=1-fy; }
                    else if(face==1) { dir=sy>0 ? 0 : 1; u=fx; v=sy>0 ? 1-fz : fz; }
                    else { dir=sz>0 ? 2 : 3; u=sz>0 ? 1-fx : fx; v=1-fy; }
                    int c=texel(look,dir,u,v);
                    if(look.kind()==SOLID || (c>>>24)>=128) {
                        double shade=face==1 ? (sy>0 ? .55 : 1) : face==0 ? .8 : .66;
                        int col=fog(shadeColor(c|0xff000000,shade),t/max,dy);
                        return tint!=0 ? mix(col,tint,Math.min(.75,.35+(t-tintAt)*.08)) : col;
                    }
                }
            }
            if(tx<ty && tx<tz) { x+=sx; t=tx; tx+=tdx; face=0; }
            else if(ty<tz) { y+=sy; t=ty; ty+=tdy; face=1; }
            else { z+=sz; t=tz; tz+=tdz; face=2; }
        }
        int col=hitT<max ? fog(hitColor,hitT/max,dy) : sky(dy);
        return tint!=0 ? mix(col,tint,.55) : col;
    }

    private int sky(double dy) {
        if(underground) return VOID;
        return mix(SKY_LOW,SKY,Math.clamp(dy*2.2+.2,0,1));
    }
    private int fog(int color,double far,double dy) {
        double f=Math.clamp((far-.45)/.55,0,1);
        return mix(color,sky(dy),f*f);
    }
    private static double box(double ox,double oy,double oz,double dx,double dy,double dz,double x0,double y0,double z0,double x1,double y1,double z1) {
        double t0=-1e9,t1=1e9;
        double[] o={ox,oy,oz},d={dx,dy,dz},lo={x0,y0,z0},hi={x1,y1,z1};
        for(int a=0;a<3;a++) {
            if(Math.abs(d[a])<1e-9) { if(o[a]<lo[a] || o[a]>hi[a]) return -1; continue; }
            double ta=(lo[a]-o[a])/d[a],tb=(hi[a]-o[a])/d[a];
            t0=Math.max(t0,Math.min(ta,tb)); t1=Math.min(t1,Math.max(ta,tb));
        }
        return t1>=t0 && t1>0 ? Math.max(t0,0) : -1;
    }
    private static int shadeColor(int c,double s) {
        int r=(int)(((c>>16)&255)*s),g=(int)(((c>>8)&255)*s),b=(int)((c&255)*s);
        return 0xff000000|r<<16|g<<8|b;
    }
    private static int mix(int a,int b,double t) {
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t),g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t),bl=(int)((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|r<<16|g<<8|bl;
    }
}
