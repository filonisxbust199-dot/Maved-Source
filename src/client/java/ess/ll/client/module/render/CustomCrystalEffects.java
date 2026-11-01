package ess.ll.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GhostModelBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Matrix4f;

/** Astral crystal meshes and the seven removed-crystal animations from ESS. */
public final class CustomCrystalEffects {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final GhostModelBatch BATCH=new GhostModelBatch();
    private static final PoseStack POSE=new PoseStack();
    private static final EndCrystalRenderState STATE=new EndCrystalRenderState();
    private static final List<Ghost> GHOSTS=new ArrayList<>();
    private static EndCrystalModel model;
    private static EntityModelSet modelSource;
    private static Object trackedLevel;
    private static boolean capturing,registered;
    private static double effectTime,lastSample=Double.NaN;

    private record Options(String shape,String animation,float speed,float brightness,boolean throughWalls,
                           int explosion,float seconds) {}
    private record Ghost(double x,double y,double z,float age,int seed,float scale,int rgb,Identifier texture,
                         boolean emissive,Options options,float effectTime,float riseHeight,long born) {
        float progress(long now) { return Math.clamp((now-born)/1e9f/options.seconds,0,1); }
    }
    private record Burst(float x,float y,float z,float scale,float yaw,float tilt,float opacity,int part) {}
    private CustomCrystalEffects() {}

    public static void initialize() {
        if(registered) return;
        registered=true;
        LevelRenderEvents.END_MAIN.register(context->safeRender());
    }
    public static boolean capturing() { return capturing; }
    public static boolean replaces(EndCrystalRenderState state) {
        return CustomCrystal.astral() && CrystalAstralTexture.identifier()!=null &&
                MC.gameRenderer.mainCamera().position().distanceToSqr(state.x,state.y,state.z)<=9216;
    }
    public static void clear() {
        GHOSTS.clear(); trackedLevel=null; effectTime=0; lastSample=Double.NaN;
    }
    private static void checkLevel() {
        if(trackedLevel!=MC.level) { clear(); trackedLevel=MC.level; }
    }
    private static Options options() {
        int explosion=CustomCrystal.EXPLOSION_TYPE.options.indexOf(CustomCrystal.EXPLOSION_TYPE.get());
        return new Options(CustomCrystal.astral()?CustomCrystal.ASTRAL_SHAPE.get():"Классика",
                CustomCrystal.astral()?CustomCrystal.ASTRAL_ANIMATION.get():"Вращение",
                CustomCrystal.astral()?CustomCrystal.ASTRAL_SPEED.get():CustomCrystal.SHADER_SPEED.get(),
                CustomCrystal.astral()?CustomCrystal.ASTRAL_BRIGHTNESS.get():1,
                CustomCrystal.astral() && CustomCrystal.ASTRAL_THROUGH_WALLS.get(),
                Math.max(0,explosion),CustomCrystal.EXPLOSION_TIME.get());
    }
    public static void onRemove(EndCrystal crystal,Entity.RemovalReason reason) {
        if(!CustomCrystal.effectsActive() || !CustomCrystal.EXPLOSION.get() || MC.level==null || MC.player==null ||
                crystal.level()!=MC.level || MC.player.distanceToSqr(crystal)>2304 ||
                reason!=Entity.RemovalReason.KILLED && reason!=Entity.RemovalReason.DISCARDED) return;
        checkLevel();
        boolean emissive=CustomCrystal.astral();
        Identifier texture=emissive?CrystalAstralTexture.identifier():CrystalShaderTexture.ID;
        if(texture==null) return;
        if(GHOSTS.size()>=18) GHOSTS.removeFirst();
        float partial=MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        GHOSTS.add(new Ghost(crystal.getX(),crystal.getY(),crystal.getZ(),crystal.time+partial,
                crystal.getId(),CustomCrystal.SIZE.get(),emissive?CustomCrystal.ASTRAL_COLOR.get()&0xffffff:0xffffff,
                texture,emissive,options(),(float)effectTime,ThreadLocalRandom.current().nextFloat(.7f,1.2f),System.nanoTime()));
    }
    private static void safeRender() {
        if(!CustomCrystal.effectsActive()) { if(!GHOSTS.isEmpty()) clear(); return; }
        try { render(); }
        catch(RuntimeException e) {
            ClientModules.CUSTOM_CRYSTAL.setEnabled(false);
            System.err.println("[ESS] CustomCrystal render disabled: "+e);
            clear();
        }
    }
    private static void render() {
        if(MC.level==null || MC.player==null) { clear(); return; }
        checkLevel(); ensureModel();
        if(model==null) return;
        Options options=options();
        float partial=MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float time=sampleTime(partial,options.speed);
        Vec3 camera=MC.gameRenderer.mainCamera().position();
        Identifier astralTexture=CrystalAstralTexture.identifier();
        if(CustomCrystal.astral() && astralTexture!=null) {
            BATCH.begin(); int rendered=0;
            for(Entity entity:MC.level.entitiesForRendering()) {
                if(!(entity instanceof EndCrystal crystal) || crystal.isRemoved()) continue;
                Vec3 position=crystal.getPosition(partial);
                if(camera.distanceToSqr(position)>9216) continue;
                capture(position.x,position.y,position.z,crystal.time+partial,CustomCrystal.SIZE.get(),
                        CustomCrystal.ASTRAL_COLOR.get()&0xffffff,time,crystal.getId(),options,camera,
                        new Burst(0,0,0,1,0,0,1,-1));
                if(++rendered%48==0) BATCH.flush(astralTexture,options.brightness,options.throughWalls,true);
            }
            BATCH.flush(astralTexture,options.brightness,options.throughWalls,true);
        }
        if(!CustomCrystal.EXPLOSION.get()) { GHOSTS.clear(); return; }
        long now=System.nanoTime();
        GHOSTS.removeIf(ghost->ghost.progress(now)>=1);
        for(Ghost ghost:GHOSTS) renderGhost(ghost,now,camera);
    }
    private static void ensureModel() {
        EntityModelSet source=MC.getEntityModels();
        if(source!=null && (model==null || modelSource!=source)) {
            model=new EndCrystalModel(source.bakeLayer(ModelLayers.END_CRYSTAL)); modelSource=source;
        }
    }
    private static float sampleTime(float partial,float speed) {
        double sample=MC.level.getGameTime()+partial;
        if(!Double.isNaN(lastSample) && !MC.isPaused()) effectTime+=Math.clamp(sample-lastSample,0,5)*.05*speed;
        lastSample=sample;
        return (float)effectTime;
    }
    private static void renderGhost(Ghost ghost,long now,Vec3 camera) {
        float progress=ghost.progress(now),fade=1-smooth((progress-.06f)/.94f);
        float elapsed=(now-ghost.born)/1e9f;
        float time=ghost.effectTime+elapsed*ghost.options.speed;
        float age=ghost.age+elapsed*20;
        BATCH.begin();
        int copies=switch (ghost.options.explosion) { case 1,5 -> 3; case 6 -> 4; default -> 1; };
        for(int i=0;i<copies;i++) {
            Burst b=burst(ghost.options.explosion,progress,ghost.riseHeight,ghost.seed,i);
            if(b.opacity<=0) continue;
            if(!ghost.emissive) b=new Burst(b.x,b.y,b.z,b.scale,b.yaw,b.tilt,b.opacity*fade,b.part);
            capture(ghost.x+b.x*ghost.scale,ghost.y+b.y,ghost.z+b.z*ghost.scale,age,ghost.scale,
                    ghost.rgb,time,ghost.seed,ghost.options,camera,b);
        }
        BATCH.flush(ghost.texture,ghost.options.brightness*fade,ghost.options.throughWalls,ghost.emissive);
    }
    private static void capture(double x,double y,double z,float age,float scale,int rgb,float time,int seed,
                                Options options,Vec3 camera,Burst burst) {
        float phase=time+(seed&1023)*.31f;
        STATE.ageInTicks=age; STATE.showsBottom=false;
        capturing=true;
        try { model.setupAnim(STATE); } finally { capturing=false; }
        model.base.visible=false;
        model.outerGlass.visible=true; model.innerGlass.visible=true;
        model.outerGlass.skipDraw=options.shape.equals("Ядро") || burst.part>=0 && burst.part!=0;
        model.innerGlass.skipDraw=options.shape.equals("Ядро") || burst.part>=0 && burst.part!=1;
        model.cube.visible=!options.shape.equals("Контур") && (burst.part<0 || burst.part==2);
        applyShape(options.shape,phase);
        float motionScale=1,brightness=1,lift=0,yaw=0,tilt=0,innerYaw=0,innerTilt=0;
        switch (options.animation) {
            case "Пульсация" -> { float p=(float)Math.sin(phase*2.8f); motionScale=1+p*.14f; brightness=1+p*.18f; }
            case "Маятник" -> { lift=(float)Math.sin(phase*1.1f)*.12f; yaw=(float)Math.sin(phase*1.8f)*.4f; }
            case "Резонанс" -> { float p=(float)Math.sin(phase*4); motionScale=1+p*.05f; lift=(float)Math.sin(phase*1.5f)*.3f;
                innerYaw=-phase*.8f; innerTilt=(float)Math.sin(phase*2.1f)*.25f; brightness=1+p*.12f; }
            case "Гироскоп" -> { lift=phase*.45f; yaw=phase*.7f; innerYaw=-phase*.8f; innerTilt=phase*.55f; }
            case "Парение" -> { lift=(float)Math.sin(phase)*.18f; tilt=(float)Math.sin(phase*1.6f)*.18f; }
            case "Кувырок" -> { lift=phase*1.1f; yaw=phase*.9f; innerYaw=phase*.5f; innerTilt=-phase*.6f; }
        }
        model.outerGlass.y+=lift*8;
        model.outerGlass.rotateBy(new Quaternionf().rotationY(yaw).rotateZ(tilt));
        model.innerGlass.rotateBy(new Quaternionf().rotationY(innerYaw).rotateX(innerTilt));
        model.outerGlass.xScale*=motionScale; model.outerGlass.yScale*=motionScale; model.outerGlass.zScale*=motionScale;
        POSE.pushPose();
        POSE.translate(x-camera.x,y-camera.y,z-camera.z);
        POSE.scale(2*scale,2*scale,2*scale);
        POSE.translate(0,-.5f,0);
        float centerY=model.outerGlass.y/16f;
        POSE.translate(0,centerY,0);
        POSE.mulPose(new Matrix4f().rotation(new Quaternionf().rotationY(burst.yaw).rotateZ(burst.tilt)));
        POSE.scale(burst.scale,burst.scale,burst.scale);
        POSE.translate(0,-centerY,0);
        model.renderToBuffer(POSE,BATCH,0xF000F0,0,tint(rgb,brightness,burst.opacity));
        POSE.popPose();
    }
    private static void applyShape(String shape,float phase) {
        switch (shape) {
            case "Вихрь" -> { model.outerGlass.yScale*=1.25f; model.outerGlass.rotateBy(new Quaternionf().rotationY(phase*.65f).rotateZ(.22f));
                model.innerGlass.rotateBy(new Quaternionf().rotationY(-phase*1.3f)); model.innerGlass.yScale*=.8f; }
            case "Созвездие" -> { model.innerGlass.rotateBy(new Quaternionf().rotationY(-phase*.4f).rotateX(.3f)); model.cube.visible=false; }
            case "Контур" -> { model.cube.visible=false; model.outerGlass.y=24; }
            case "Грани" -> { model.outerGlass.yScale*=1.15f; model.innerGlass.rotateBy(new Quaternionf().rotationY(phase*.7f));
                model.cube.rotateBy(new Quaternionf().rotationZ(-phase*.45f)); }
            case "Ядро" -> { model.cube.xScale*=.95f; model.cube.yScale*=.95f; model.cube.zScale*=.95f;
                model.cube.rotateBy(new Quaternionf().rotationY(-phase*.6f)); }
        }
    }
    private static int tint(int rgb,float brightness,float opacity) {
        int r=Math.clamp(Math.round((rgb>>16&255)*brightness),0,255);
        int g=Math.clamp(Math.round((rgb>>8&255)*brightness),0,255);
        int b=Math.clamp(Math.round((rgb&255)*brightness),0,255);
        return Math.clamp(Math.round(opacity*255),0,255)<<24|r<<16|g<<8|b;
    }
    private static Burst burst(int type,float progress,float riseHeight,int seed,int copy) {
        float p=Math.clamp(progress,0,1),eased=1-(1-p)*(1-p)*(1-p);
        return switch (type) {
            case 1 -> { float angle=(seed&1023)*.31f+copy*2.0943952f,radius=eased*1.2f;
                yield new Burst((float)Math.cos(angle)*radius,eased*(.2f+copy*.22f)-p*p*.35f,
                        (float)Math.sin(angle)*radius,1-p*.45f,eased*(copy%2==0?2.4f:-2.4f),eased*.8f,1,copy); }
            case 2 -> new Burst(0,0,0,1+(float)Math.sin(p*Math.PI)*.65f-p*.15f,0,0,1,-1);
            case 3 -> new Burst(0,0,0,Math.max(.01f,1-smooth(p)),eased*.8f,0,1,-1);
            case 4 -> { float angle=p*(float)(Math.PI*2)+(seed&1023)*.31f,radius=(float)Math.sin(p*Math.PI)*.45f;
                yield new Burst((float)Math.cos(angle)*radius,eased*riseHeight,(float)Math.sin(angle)*radius,
                        1-p*.55f,p*(float)(Math.PI*2),(float)Math.sin(p*Math.PI)*.65f,1,-1); }
            case 5 -> new Burst(0,(copy-1)*eased*.85f,0,1-p*.25f,(copy-1)*eased*1.8f,0,1,copy);
            case 6 -> { float local=Math.clamp((p-copy*.07f)/(1-copy*.07f),0,1),rise=1-(1-local)*(1-local)*(1-local);
                yield new Burst(0,rise*riseHeight,0,1-local*.08f,0,0,copy==0?1:smooth(p/.2f)*.22f,-1); }
            default -> new Burst(0,riseHeight*eased,0,1-p*.08f,0,0,1,-1);
        };
    }
    private static float smooth(float v) { float p=Math.clamp(v,0,1); return p*p*(3-2*p); }
}
