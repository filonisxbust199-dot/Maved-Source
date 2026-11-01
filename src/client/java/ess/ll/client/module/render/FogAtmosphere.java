package ess.ll.client.module.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.Theme;
import ess.ll.client.render.WorldFx;
import ess.ll.client.setting.Settings.*;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/**
 * FogAtmosphere (from test-26.2): low volumetric fog with fast sun rays ("Optimized", rendered at half
 * resolution and composited), or a full-screen atmosphere with sky gradient and haze ("Powerful"). It replaces
 * the vanilla distance fog while on.
 */
public final class FogAtmosphere {
    private static final BooleanSupplier OPTIMIZED=()->is("Optimized"), POWERFUL=()->is("Powerful");
    public static final ModeSettings MODE=new ModeSettings("Режим рендера","Optimized","Powerful");
    public static final BooleanSetting FOG_ONLY=new BooleanSetting("Только туман",false);
    public static final ModeSettings STYLE=new ModeSettings("Стиль лёгкой атмосферы","Объёмный туман","Ночная дымка").visibleWhen(OPTIMIZED);
    public static final SliderSetting DENSITY=new SliderSetting("Густота лёгкого тумана",.05f,1.5f,.05f,1).visibleWhen(OPTIMIZED);
    public static final SliderSetting HEIGHT=new SliderSetting("Высота лёгкого тумана",2,64,1,14).visibleWhen(OPTIMIZED);
    public static final BooleanSetting RAYS=new BooleanSetting("Быстрые лучи",true).visibleWhen(()->is("Optimized") && !FOG_ONLY.get());
    public static final SliderSetting RAYS_BRIGHTNESS=new SliderSetting("Яркость быстрых лучей",.05f,2,.05f,1)
            .visibleWhen(()->is("Optimized") && !FOG_ONLY.get() && RAYS.get());
    public static final BooleanSetting RAYS_SHADOW=new BooleanSetting("Затенение быстрых лучей",true)
            .visibleWhen(()->is("Optimized") && !FOG_ONLY.get() && RAYS.get());
    public static final BooleanSetting MOON_HAZE=new BooleanSetting("Лунная дымка",true).visibleWhen(OPTIMIZED);
    public static final SliderSetting SOFTNESS=new SliderSetting("Мягкость лёгкого тумана",.1f,1.5f,.05f,.7f).visibleWhen(OPTIMIZED);
    public static final BooleanSetting CLIENT_COLORS=new BooleanSetting("Цвета клиента для лёгкого тумана",true).visibleWhen(OPTIMIZED);
    public static final ColorSetting COLD=new ColorSetting("Холодный оттенок лёгкого тумана",0xffbec4c6)
            .visibleWhen(()->is("Optimized") && !CLIENT_COLORS.get());
    public static final ColorSetting WARM=new ColorSetting("Тёплый оттенок лёгкого тумана",0xffffad7a)
            .visibleWhen(()->is("Optimized") && !CLIENT_COLORS.get());
    public static final ModeSettings PALETTE=new ModeSettings("Палитра","Рассвет","Сумерки","Цвет клиента").visibleWhen(POWERFUL);
    public static final SliderSetting SATURATION=new SliderSetting("Насыщенность атмосферы",.05f,.8f,.01f,.35f).visibleWhen(POWERFUL);
    public static final SliderSetting HAZE_TOP=new SliderSetting("Верхняя граница дымки",60,120,1,76).visibleWhen(POWERFUL);
    public static final SliderSetting SUN_GLOW=new SliderSetting("Сияние солнечных лучей",0,1,.01f,.75f)
            .visibleWhen(()->is("Powerful") && !FOG_ONLY.get());
    public static final SliderSetting POWER_SOFTNESS=new SliderSetting("Мягкость",0,1,.01f,.6f)
            .visibleWhen(()->is("Powerful") && !FOG_ONLY.get());
    public static final ColorSetting DAWN=new ColorSetting("Оттенок линии рассвета",0xffffad7a)
            .visibleWhen(()->is("Powerful") && PALETTE.is("Рассвет"));
    public static final Setting[] SETTINGS={MODE,FOG_ONLY,STYLE,DENSITY,HEIGHT,RAYS,RAYS_BRIGHTNESS,RAYS_SHADOW,MOON_HAZE,SOFTNESS,
            CLIENT_COLORS,COLD,WARM,PALETTE,SATURATION,HAZE_TOP,SUN_GLOW,POWER_SOFTNESS,DAWN};
    private static boolean is(String mode) { return MODE.is(mode); }

    /** Pipelines are built on first use, after the game's render system is up. */
    private static final class Pipelines {
        static final RenderPipeline LAYER=WorldFx.pipeline("fog_atmosphere",Optional.empty(),"FogAtmosphere","DepthSampler");
        static final RenderPipeline COMPOSITE=WorldFx.pipeline("fog_atmosphere_composite",Optional.of(BlendFunction.TRANSLUCENT),"FogAtmosphere","FogSampler");
        static final RenderPipeline LIGHT=WorldFx.pipeline("fog_atmosphere_light",Optional.of(BlendFunction.ADDITIVE),"FogAtmosphere","FogSampler");
        static final RenderPipeline POWERFUL=WorldFx.pipeline("fog_atmosphere_powerful",Optional.empty(),"PowerfulAtmosphere","SceneSampler","DepthSampler");
        static final RenderPipeline POWERFUL_FOG=WorldFx.pipeline("fog_atmosphere_powerful_fog_only",Optional.empty(),"PowerfulAtmosphere","SceneSampler","DepthSampler");
    }
    private static TextureTarget layer, scene;
    private static ClientLevel groundLevel, smoothLevel;
    private static float smoothGround=Float.NaN, smoothCameraY=Float.NaN, ground=Float.NaN;
    private static int groundX=Integer.MIN_VALUE, groundZ=Integer.MIN_VALUE;
    private FogAtmosphere() {}

    /** Frees the targets and smoothing state once the module is off. */
    public static void release() {
        groundLevel=smoothLevel=null;
        smoothGround=smoothCameraY=ground=Float.NaN;
        groundX=groundZ=Integer.MIN_VALUE;
        if(layer!=null) { layer.destroyBuffers(); layer=null; }
        if(scene!=null) { scene.destroyBuffers(); scene=null; }
    }
    /** The atmosphere draws its own fog, so the vanilla distance fog is pushed out of range (not under fluids). */
    public static void fog(Camera camera,FogData data) {
        if(!ClientModules.FOG_ATMOSPHERE.enabled() || data==null || camera.getFluidInCamera()!=FogType.NONE) return;
        data.environmentalStart=data.environmentalEnd=1e7f;
        data.renderDistanceStart=data.renderDistanceEnd=1e7f;
        data.skyEnd=data.cloudEnd=1e7f;
    }

    public static void render(Matrix4f projection,Matrix4f view,DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.gameRenderer.mainCamera().getFluidInCamera()!=FogType.NONE) return;
        RenderTarget main=mc.gameRenderer.mainRenderTarget();
        if(!WorldFx.usable(main)) return;
        if(is("Powerful")) renderPowerful(mc,main,projection,view,delta);
        else renderOptimized(mc,main,projection,view,delta);
    }

    private static void renderPowerful(Minecraft mc,RenderTarget main,Matrix4f projection,Matrix4f view,DeltaTracker delta) {
        int width=Math.max(1,main.width), height=Math.max(1,main.height);
        scene=WorldFx.target(scene,"FogAtmosphere powerful scene",width,height);
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Camera camera=mc.gameRenderer.mainCamera();
        Vec3 position=camera.position();
        float sunAngle=camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE,partial)*Mth.DEG_TO_RAD;
        float sunX=-Mth.sin(sunAngle);
        float elevation=PALETTE.is("Сумерки") ? -.045f : PALETTE.is("Цвет клиента") ? .13f : .11f;
        float side=sunX>=0 ? 1 : -1;
        float dirX=side*Mth.cos(elevation), dirY=Mth.sin(elevation);
        Vector4f sun=new Vector4f(dirX,dirY,0,0);
        new Matrix4f(view).transform(sun);
        float screenX=.5f, screenY=.5f, facing=Mth.clamp(-sun.z*4,0,1);
        if(-sun.z>1e-4f) {
            Vector4f clip=new Vector4f(sun.x*1000,sun.y*1000,sun.z*1000,1);
            new Matrix4f(projection).transform(clip);
            if(clip.w>1e-4f) { screenX=clip.x/clip.w*.5f+.5f; screenY=clip.y/clip.w*.5f+.5f; }
        }
        float[] palette=palette();
        float top=HAZE_TOP.get(), bottom=top-18;
        float time=((float)(mc.level.getGameTime()%100000L)+partial)*.05f;
        float distance=mc.options.getEffectiveRenderDistance()*16f;
        Matrix4f inverseProjection=new Matrix4f(projection).invert();
        Matrix4f inverseView=new Matrix4f(view).invert();
        inverseView.m30((float)position.x); inverseView.m31((float)position.y); inverseView.m32((float)position.z);
        WorldFx.copyColor(main,scene);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var data=WorldFx.upload(Std140Builder.onStack(stack,320)
                    .putMat4f(inverseProjection).putMat4f(inverseView)
                    .putVec4((float)position.x,(float)position.y,(float)position.z,0)
                    .putVec4(dirX,dirY,0,0)
                    .putVec4(screenX,screenY,facing,0)
                    .putVec4(SATURATION.get(),bottom,top,distance)
                    .putVec4(width,height,time,POWER_SOFTNESS.get())
                    .putVec4(SUN_GLOW.get(),0,0,0)
                    .putVec4(palette[0],palette[1],palette[2],1).putVec4(palette[3],palette[4],palette[5],1)
                    .putVec4(palette[6],palette[7],palette[8],1).putVec4(palette[9],palette[10],palette[11],1)
                    .putVec4(palette[12],palette[13],palette[14],1).putVec4(palette[15],palette[16],palette[17],1)
                    .get());
            WorldFx.draw("FogAtmosphere powerful",main,FOG_ONLY.get() ? Pipelines.POWERFUL_FOG : Pipelines.POWERFUL,"PowerfulAtmosphere",data,
                    "SceneSampler",scene.getColorTextureView(),FilterMode.LINEAR,"DepthSampler",main.getDepthTextureView(),FilterMode.NEAREST);
        }
    }

    private static void renderOptimized(Minecraft mc,RenderTarget main,Matrix4f projection,Matrix4f view,DeltaTracker delta) {
        int width=Math.max(1,main.width), height=Math.max(1,main.height);
        layer=WorldFx.target(layer,"FogAtmosphere layer",(width+1)/2,(height+1)/2);
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Camera camera=mc.gameRenderer.mainCamera();
        Vec3 position=camera.position();
        float groundY=groundHeight(mc,position);
        // The fog floor follows the ground smoothly while flying and resets after large jumps.
        if(mc.player.onGround()) { smoothLevel=mc.level; smoothGround=groundY; }
        else if(smoothLevel==mc.level && Float.isFinite(smoothGround) && Math.abs(groundY-smoothGround)<=96) smoothGround=Mth.lerp(.075f,smoothGround,groundY);
        else { smoothLevel=mc.level; smoothGround=groundY; }
        if(Float.isFinite(smoothCameraY) && Math.abs(position.y-smoothCameraY)<=96) smoothCameraY=Mth.lerp(.12f,smoothCameraY,(float)position.y);
        else smoothCameraY=(float)position.y;

        float sunAngle=camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE,partial)*Mth.DEG_TO_RAD;
        float sunX=-Mth.sin(sunAngle), sunY=Mth.cos(sunAngle);
        float day=smoothstep(-.08f,.2f,sunY);
        float moon=MOON_HAZE.get() ? smoothstep(-.08f,.2f,-sunY)*.75f : 0;
        boolean sunLight=day>=moon;
        float lightX=sunLight ? sunX : -sunX, lightY=sunLight ? sunY : -sunY, light=sunLight ? day : moon;
        boolean rays=!FOG_ONLY.get();
        Vector4f clip=new Vector4f(lightX,lightY,0,0);
        new Matrix4f(view).transform(clip);
        new Matrix4f(projection).transform(clip);
        float screenX=.5f, screenY=.5f, visible=0;
        if(clip.w>1e-4f) {
            screenX=clip.x/clip.w*.5f+.5f; screenY=clip.y/clip.w*.5f+.5f;
            float aspect=(float)width/height;
            float fromCenter=(float)Math.sqrt(Math.pow((screenX-.5f)*aspect,2)+Math.pow(screenY-.5f,2));
            visible=light*smoothstep(.015f,.12f,clip.w)*(1-smoothstep(.68f,1.55f,fromCenter));
        }
        float time=((float)(mc.level.getGameTime()%240000L)+partial)/20f;
        Vector4f cold=rgb(CLIENT_COLORS.get() ? Theme.accent() : COLD.get());
        Vector4f warm=rgb(CLIENT_COLORS.get() ? Theme.accentLight() : WARM.get());
        Matrix4f inverseProjection=new Matrix4f(projection).invert();
        Matrix4f inverseView=new Matrix4f(view).invert();
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var data=WorldFx.upload(Std140Builder.onStack(stack,256)
                    .putMat4f(inverseProjection).putMat4f(inverseView)
                    .putVec4(wrap(position.x),smoothCameraY,wrap(position.z),smoothGround)
                    .putVec4(lightX,lightY,0,light)
                    .putVec4(screenX,screenY,visible,!rays ? -1 : RAYS.get() && RAYS_SHADOW.get() ? 1 : 0)
                    .putVec4(day,moon,STYLE.is("Ночная дымка") ? 1 : 0,sunLight ? 1 : 0)
                    .putVec4(DENSITY.get(),HEIGHT.get(),SOFTNESS.get(),rays && RAYS.get() ? RAYS_BRIGHTNESS.get() : 0)
                    .putVec4(width,height,time,mc.options.getEffectiveRenderDistance()*16f)
                    .putVec4(cold.x,cold.y,cold.z,1).putVec4(warm.x,warm.y,warm.z,1)
                    .get());
            WorldFx.draw("FogAtmosphere layer",layer,Pipelines.LAYER,"FogAtmosphere",data,"DepthSampler",main.getDepthTextureView(),FilterMode.NEAREST);
            WorldFx.draw("FogAtmosphere composite",main,Pipelines.COMPOSITE,"FogAtmosphere",data,"FogSampler",layer.getColorTextureView(),FilterMode.LINEAR);
            if(rays && RAYS.get())
                WorldFx.draw("FogAtmosphere light",main,Pipelines.LIGHT,"FogAtmosphere",data,"FogSampler",layer.getColorTextureView(),FilterMode.LINEAR);
        }
    }

    /** Ground under the player: the player's Y on the ground, otherwise the highest surface nearby below the camera. */
    private static float groundHeight(Minecraft mc,Vec3 position) {
        int x=Mth.floor(mc.player.getX()), z=Mth.floor(mc.player.getZ());
        if(mc.player.onGround()) {
            groundLevel=mc.level; groundX=x; groundZ=z; ground=(float)mc.player.getY();
            return ground;
        }
        float floor=(float)mc.player.getY()-Math.max(8,HEIGHT.get()*.85f);
        if(groundLevel==mc.level && Math.abs(groundX-x)<=8 && Math.abs(groundZ-z)<=8 && Float.isFinite(ground)) return Math.max(ground,floor);
        groundLevel=mc.level; groundX=x; groundZ=z;
        float best=Float.NEGATIVE_INFINITY, limit=(float)position.y+4;
        for(int dx=-4;dx<=4;dx+=4) for(int dz=-4;dz<=4;dz+=4) {
            float h=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x+dx,z+dz);
            if(h<=limit) best=Math.max(best,h);
        }
        ground=Float.isFinite(best) ? best : floor;
        return Math.max(ground,floor);
    }

    /** Six sky / horizon colours for the Powerful shader. */
    private static float[] palette() {
        float[] c=new float[18];
        if(PALETTE.is("Сумерки")) {
            set(c,0,.135f,.125f,.3f); set(c,3,.89f,.46f,.55f); set(c,6,.38f,.35f,.56f);
            set(c,9,.8f,.52f,.62f); set(c,12,.47f,.44f,.64f); set(c,15,.92f,.56f,.72f);
            return c;
        }
        boolean client=PALETTE.is("Цвет клиента");
        int tint=client ? Theme.accent() : DAWN.get();
        float r=(tint>>16&255)/255f, g=(tint>>8&255)/255f, b=(tint&255)/255f;
        if(client) {
            oklab(c,0,.085f,.1f,.2f,r,g,b,.3f); oklab(c,3,r,g,b,1,.93f,.82f,.35f); oklab(c,6,.52f,.58f,.74f,r,g,b,.28f);
            oklab(c,9,r,g,b,.97f,.93f,.88f,.45f); oklab(c,12,.58f,.63f,.76f,r,g,b,.35f); oklab(c,15,r,g,b,1,.96f,.88f,.3f);
        } else {
            oklab(c,0,.16f,.19f,.38f,r,g,b,.14f);
            set(c,3,Mth.clamp(r*1.12f,0,1),Mth.clamp(g*.88f,0,1),Mth.clamp(b*.62f,0,1));
            set(c,6,.56f,.62f,.8f);
            oklab(c,9,r,g,b,.95f,.55f,.63f,.42f);
            set(c,12,.6f,.67f,.82f);
            set(c,15,Mth.clamp(r*1.08f,0,1),Mth.clamp(g*.94f,0,1),Mth.clamp(b*.72f,0,1));
        }
        return c;
    }
    private static void set(float[] c,int i,float r,float g,float b) { c[i]=r; c[i+1]=g; c[i+2]=b; }
    /** Mixes two sRGB colours in OKLab, so the blend keeps its lightness. */
    private static void oklab(float[] out,int i,float r1,float g1,float b1,float r2,float g2,float b2,float t) {
        float[] a=lms(r1,g1,b1), b=lms(r2,g2,b2);
        t=Mth.clamp(t,0,1);
        float l=cube(Mth.lerp(t,a[0],b[0])), m=cube(Mth.lerp(t,a[1],b[1])), s=cube(Mth.lerp(t,a[2],b[2]));
        out[i]=toSrgb(4.0767417f*l-3.3077116f*m+.23096994f*s);
        out[i+1]=toSrgb(-1.268438f*l+2.6097574f*m-.34131938f*s);
        out[i+2]=toSrgb(-.0041960864f*l-.7034186f*m+1.7076147f*s);
    }
    private static float[] lms(float r,float g,float b) {
        r=toLinear(r); g=toLinear(g); b=toLinear(b);
        return new float[]{(float)Math.cbrt(.41222146f*r+.53633255f*g+.051445995f*b),
                (float)Math.cbrt(.2119035f*r+.6806995f*g+.10739696f*b),(float)Math.cbrt(.08830246f*r+.28171885f*g+.6299787f*b)};
    }
    private static float cube(float v) { return v*v*v; }
    private static float toLinear(float v) { return v<=.04045f ? v/12.92f : (float)Math.pow((v+.055f)/1.055f,2.4); }
    private static float toSrgb(float v) { v=Mth.clamp(v,0,1); return v<=.0031308f ? v*12.92f : (float)(1.055*Math.pow(v,1/2.4)-.055); }
    private static float smoothstep(float from,float to,float v) { float t=Mth.clamp((v-from)/(to-from),0,1); return t*t*(3-2*t); }
    /** Camera X / Z wrapped to keep noise coordinates precise far from the origin. */
    private static float wrap(double v) { double m=v%8192; return (float)(m<0 ? m+8192 : m); }
    private static Vector4f rgb(int argb) { return new Vector4f((argb>>16&255)/255f,(argb>>8&255)/255f,(argb&255)/255f,(argb>>>24)/255f); }
}
