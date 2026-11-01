package ess.ll.client.wardrobe;

import ess.ll.client.wardrobe.WardrobeModel.Effect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Wardrobe particles (from Zenith): effects of worn items emitted from their bones at their rate (when moving,
 * idle, airborne, sneaking or with the full set), the full-set bonus around the player, all under a shared
 * per-second budget. Only in the world and within 48 blocks.
 */
public final class WardrobeEffects {
    private static final RandomSource RANDOM=RandomSource.create();
    private static final Matrix4f M=new Matrix4f();
    private static final Vector3f V=new Vector3f();
    private static float budget;
    private static long budgetAt;

    private WardrobeEffects() {}

    private static boolean enabled() {
        var mc=Minecraft.getInstance();
        return Wardrobe.PARTICLES.get() && mc.level!=null && mc.options.particles().get()!=ParticleStatus.MINIMAL;
    }
    /** Takes one particle from the budget (refilled at the set rate, holding at most half a second). */
    private static boolean spend() {
        long now=System.nanoTime();
        float rate=Wardrobe.PARTICLE_BUDGET.get();
        if(budgetAt==0) { budgetAt=now; budget=rate*.25f; }
        budget=Math.min(rate*.5f,budget+(now-budgetAt)/1e9f*rate);
        budgetAt=now;
        if(budget<1) return false;
        budget--;
        return true;
    }
    private static void spawn(ParticleOptions particle,double x,double y,double z,double vx,double vy,double vz) {
        var level=Minecraft.getInstance().level;
        if(level!=null && particle!=null && spend()) level.addParticle(particle,x,y,z,vx,vy,vz);
    }

    public static void emit(WardrobeModel model,WardrobeAnimator.Cache cache,AvatarRenderState state,PlayerModel player,float[] pose,float dt,
                            boolean moving,boolean airborne,boolean setBonus) {
        List<Effect> effects=model.allEffects;
        if(effects.isEmpty() || state.id==WardrobeLayer.PREVIEW_ID || !enabled() || state.distanceToCameraSq>48*48) return;
        if(cache.emitted.length!=effects.size()) cache.emitted=new float[effects.size()];
        for(int i=0;i<effects.size();i++) {
            Effect e=effects.get(i);
            boolean on=switch (e.when()) {
                case ALWAYS -> true;
                case MOVING -> moving;
                case IDLE -> !moving;
                case AIRBORNE -> airborne;
                case SNEAKING -> state.isCrouching;
                case SET_BONUS -> setBonus;
            };
            if(!on) { cache.emitted[i]=0; continue; }
            cache.emitted[i]+=e.rate()*dt;
            while(cache.emitted[i]>=1) {
                cache.emitted[i]--;
                // World space of the player model, as the renderer sets it up.
                M.identity().translate((float)state.x,(float)state.y,(float)state.z);
                M.rotateY((float)Math.toRadians(180-state.bodyRot));
                M.scale(state.scale);
                M.scale(-1,-1,1);
                M.translate(0,-1.501f,0);
                if(e.bone()>=0) {
                    int root=e.bone();
                    while(model.parent[root]>=0) root=model.parent[root];
                    if(model.anchor[root]>=0) { apply(player.root(),M); apply(WardrobeLayer.part(player,model.anchor[root]),M); }
                    WardrobeRenderer.boneMatrix(model,pose,e.bone(),M);
                }
                M.transformPosition(e.x()/16,e.y()/16,e.z()/16,V);
                double spread=e.spread();
                spawn(e.particle(),V.x+(RANDOM.nextDouble()-.5)*spread,V.y+(RANDOM.nextDouble()-.5)*spread,V.z+(RANDOM.nextDouble()-.5)*spread,
                        e.vx()+(RANDOM.nextDouble()-.5)*spread*.2,e.vy(),e.vz()+(RANDOM.nextDouble()-.5)*spread*.2);
            }
        }
    }
    /** The full-set bonus: the set's particle rising around the player, 5 a second. */
    public static void setBonus(Wardrobe.OutfitSet set,AvatarRenderState state,long now) {
        if(set==null || state.id==WardrobeLayer.PREVIEW_ID || !enabled() || state.distanceToCameraSq>48*48) return;
        ParticleOptions particle=WardrobeModel.particle(set.particle());
        if(particle==null) return;
        WardrobeAnimator.Cache cache=WardrobeAnimator.Cache.of(state.id,"set:"+set.id(),false);
        cache.setTimer+=5*cache.frame(now);
        while(cache.setTimer>=1) {
            cache.setTimer--;
            double angle=RANDOM.nextDouble()*Math.PI*2,radius=.55+RANDOM.nextDouble()*.15;
            spawn(particle,state.x+Math.cos(angle)*radius,state.y+.1+RANDOM.nextDouble()*1.7,state.z+Math.sin(angle)*radius,0,.015,0);
        }
    }
    private static void apply(ModelPart part,Matrix4f m) {
        m.translate(part.x/16,part.y/16,part.z/16);
        if(part.xRot!=0 || part.yRot!=0 || part.zRot!=0) m.rotateZYX(part.zRot,part.yRot,part.xRot);
        if(part.xScale!=1 || part.yScale!=1 || part.zScale!=1) m.scale(part.xScale,part.yScale,part.zScale);
    }
}
