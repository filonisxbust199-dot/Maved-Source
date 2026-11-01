package ess.ll.client.module.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** The animated 128x64 crystal skin generator from ESS, updated for the 26.3 texture API. */
final class CrystalShaderTexture {
    static final Identifier ID=Identifier.fromNamespaceAndPath("ess-best","dynamic/crystal_shader");
    private static final Identifier VANILLA=Identifier.fromNamespaceAndPath("minecraft","textures/entity/end_crystal/end_crystal.png");
    private static DynamicTexture texture;
    private static int[] mask;
    private static boolean maskFailed;
    private static long lastUpdate;
    private static float phase;

    private CrystalShaderTexture() {}

    private static void loadMask(Minecraft mc) {
        if(mask!=null || maskFailed) return;
        try {
            var resource=mc.getResourceManager().getResource(VANILLA);
            if(resource.isEmpty()) { maskFailed=true; return; }
            try(InputStream in=resource.get().open(); NativeImage image=NativeImage.read(in)) {
                int[] found=new int[128*64];
                for(int y=0;y<64 && y<image.getHeight();y++)
                    for(int x=0;x<128 && x<image.getWidth();x++)
                        found[y*128+x]=image.getPixel(x,y)>>>24;
                mask=found;
            }
        } catch(Exception e) { maskFailed=true; }
    }

    static void update() {
        long now=System.currentTimeMillis();
        if(texture!=null && now-lastUpdate<33) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.getTextureManager()==null) return;
        loadMask(mc);
        if(texture==null) {
            texture=new DynamicTexture(()->ID.toString(),128,64,false);
            mc.getTextureManager().register(ID,texture);
        }
        float dt=lastUpdate==0 ? 0 : Math.clamp((now-lastUpdate)/1000f,0,.1f);
        lastUpdate=now;
        phase+=dt*CustomCrystal.SHADER_SPEED.get();
        NativeImage image=texture.getPixels();
        if(image==null) return;
        int color=CustomCrystal.shaderRgb();
        float cr=(color>>16&255)/255f,cg=(color>>8&255)/255f,cb=(color&255)/255f;
        String mode=CustomCrystal.SHADER_STYLE.is("Заливка") ? "Flat" : CustomCrystal.SHADER_MODE.get();
        String animation=CustomCrystal.SHADER_ANIMATION.get();
        float opacity=CustomCrystal.SHADER_OPACITY.get()/100f;
        float flash=animation.equals("Вспышки") ? (float)Math.pow(Math.max(0,Math.sin(phase*5)),10) : 0;
        float rotation=animation.equals("Вращение") ? phase*.18f : 0;
        float rotationCos=(float)Math.cos(rotation),rotationSin=(float)Math.sin(rotation);
        float pulse=animation.equals("Пульсация") ? .72f+(.5f+.5f*(float)Math.sin(phase*2.2f))*.28f : 1;
        float scanOffset=animation.equals("Сканирование") ? fract(phase*.22f) : -1;

        for(int y=0;y<64;y++) for(int x=0;x<128;x++) {
            int a=mask==null ? 255 : mask[y*128+x];
            if(a==0) { image.setPixel(x,y,0); continue; }
            float u=x/128f,v=y/64f;
            if(animation.equals("Вращение")) {
                float px=u-.5f,py=v-.5f;
                u=.5f+px*rotationCos-py*rotationSin;
                v=.5f+px*rotationSin+py*rotationCos;
            }
            u=fract(u); v=fract(v);
            int r,g,b;
            int alpha=a;
            switch (mode) {
                case "Flat" -> {
                    alpha=Math.min(255,(int)(a*1.45f));
                    float shade=.9f+.1f*v;
                    r=clamp(cr*shade*255); g=clamp(cg*shade*255); b=clamp(cb*shade*255);
                }
                case "Туманность" -> {
                    int c=nebula(u,v,cr,cg,cb);
                    r=c>>16&255; g=c>>8&255; b=c&255;
                }
                case "Северное сияние" -> {
                    int c=aurora(u,v,cr,cg,cb);
                    r=c>>16&255; g=c>>8&255; b=c&255;
                }
                case "Электроразряд" -> {
                    float core=smoothstep(.18f,.9f,electric(u,v));
                    r=clamp((cr*.35f+core*.65f)*255);
                    g=clamp((cg*.45f+core*.55f)*255);
                    b=clamp((cb*.7f+core*.3f)*255);
                }
                case "Сетка" -> {
                    float line=grid(u,v);
                    r=clamp(cr*line*255); g=clamp(cg*line*255); b=clamp(cb*line*255);
                    alpha=clamp(a*(.18f+line*.82f));
                }
                case "Магма" -> {
                    float rock=fbm(u*9+phase*.1f,v*9-phase*.12f,4);
                    float crack=1-smoothstep(.025f,.12f,Math.abs(rock-.5f));
                    float heat=crack*(.65f+.35f*(float)Math.sin(phase*1.4f+v*12));
                    r=clamp((.07f+heat*.93f)*255);
                    g=clamp((.015f+heat*heat*.7f)*255);
                    b=clamp((cb*rock*.08f+heat*heat*heat*.12f)*255);
                }
                case "Голограмма" -> {
                    float stripe=1-smoothstep(.08f,.3f,Math.abs(fract(v*24-phase*.5f)-.5f));
                    float scan=1-smoothstep(.01f,.1f,Math.abs(v-fract(phase*.2f)));
                    r=clamp((cr*.25f+scan*.3f)*255);
                    g=clamp((.38f+cg*.35f+stripe*.25f)*255);
                    b=clamp((.65f+cb*.2f+scan*.15f)*255);
                    alpha=clamp(a*(.08f+stripe*.55f+scan*.35f));
                }
                case "Сингулярность" -> {
                    float sx=fract(u*4)-.5f,sy=fract(v*2)-.5f;
                    float radius=(float)Math.sqrt(sx*sx+sy*sy);
                    float angle=(float)Math.atan2(sy,sx);
                    float ring=1-smoothstep(.025f,.15f,Math.abs(radius-.25f));
                    float streams=.5f+.5f*(float)Math.sin(angle*6+radius*30-phase*2.4f);
                    float energy=ring*(.35f+streams*.65f);
                    r=clamp((cr*energy+energy*energy*.4f)*255);
                    g=clamp((cg*energy+energy*energy*.35f)*255);
                    b=clamp((cb*energy+energy*energy*.3f)*255);
                    alpha=clamp(a*(.05f+energy*.95f));
                }
                default -> { r=clamp(cr*255); g=clamp(cg*255); b=clamp(cb*255); }
            }
            if(animation.equals("Пульсация")) {
                r=clamp(r*pulse); g=clamp(g*pulse); b=clamp(b*pulse);
            } else if(scanOffset>=0) {
                float scan=1-smoothstep(.015f,.11f,Math.abs(v-scanOffset));
                float lift=scan*.8f;
                r=clamp(lerp(lift,r,255)); g=clamp(lerp(lift,g,255)); b=clamp(lerp(lift,b,255));
            }
            if(flash>0) {
                r=clamp(lerp(flash,r,255)); g=clamp(lerp(flash,g,255)); b=clamp(lerp(flash,b,255));
            }
            image.setPixel(x,y,clamp(alpha*opacity)<<24|r<<16|g<<8|b);
        }
        texture.upload();
    }

    private static int nebula(float u,float v,float cr,float cg,float cb) {
        float t=phase*.09f;
        float cloud=fbm(u*4.2f+t,v*4.2f-t*.6f,5);
        float wisp=fbm(u*9.5f-t*1.3f,v*9.5f+t*.4f,3);
        float dens=Math.clamp(cloud*1.95f-.55f,0,1);
        dens=dens*dens*(.65f+.35f*dens);
        float glow=Math.clamp(wisp-.6f,0,1)*1.7f;
        float r=.02f+cr*dens*.5f+glow*.3f;
        float g=.028f+cg*dens*.45f+glow*.34f;
        float b=.075f+cb*dens*.78f+glow*.42f;
        float star=stars(u,v);
        return clamp((r+star)*255)<<16|clamp((g+star)*255)<<8|clamp((b+star)*255);
    }

    private static float stars(float u,float v) {
        float gx=u*26,gy=v*26;
        int ix=(int)Math.floor(gx),iy=(int)Math.floor(gy);
        float h=hash2(ix,iy);
        if(h<.8f) return 0;
        float dx=gx-ix-hash2(ix+31,iy+17),dy=gy-iy-hash2(ix+71,iy+53);
        float core=Math.clamp(1-(float)Math.sqrt(dx*dx+dy*dy)*7,0,1);
        float tw=.55f+.45f*(float)Math.sin(phase*2.6f+h*40);
        return core*core*tw*.95f;
    }

    private static float fbm(float x,float y,int octaves) {
        float sum=0,amp=.5f,norm=0;
        for(int i=0;i<octaves;i++) { sum+=vnoise(x,y)*amp; norm+=amp; x*=2.03f; y*=2.03f; amp*=.5f; }
        return sum/norm;
    }

    private static float vnoise(float x,float y) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        float fx=x-ix,fy=y-iy;
        fx=fx*fx*(3-2*fx); fy=fy*fy*(3-2*fy);
        float a=hash2(ix,iy),b=hash2(ix+1,iy),c=hash2(ix,iy+1),d=hash2(ix+1,iy+1);
        return lerp(fy,lerp(fx,a,b),lerp(fx,c,d));
    }

    private static float hash2(int x,int y) {
        int n=x*374761393+y*668265263;
        n=(n^n>>13)*1274126177;
        return ((n^n>>16)&Integer.MAX_VALUE)/2.1474836E9f;
    }

    private static float grid(float u,float v) {
        float gx=Math.abs(fract(u*12+phase*.18f)-.5f);
        float gy=Math.abs(fract(v*6-phase*.12f)-.5f);
        return .08f+(1-smoothstep(.025f,.1f,Math.min(gx,gy)))*.92f;
    }

    private static float electric(float u,float v) {
        float bend=(fbm(u*5+phase*.4f,v*8,4)-.5f)*.28f;
        float bolt=Math.abs((float)Math.sin((u+bend)*34+Math.sin(v*18+phase)*2.5));
        float glow=1-smoothstep(.02f,.2f,bolt);
        float pulse=.55f+.45f*(float)Math.sin(v*11-phase*2);
        return Math.clamp(glow*pulse,0,1);
    }

    private static int aurora(float u,float v,float cr,float cg,float cb) {
        float flow=fbm(u*5+phase*.18f,v*3-phase*.08f,4);
        float curtain=(float)Math.sin((u*12+flow*3)*Math.PI+phase*.45f);
        float envelope=(float)Math.sin(Math.clamp(v*1.15f,0,1)*Math.PI);
        float glow=Math.clamp((curtain*.5f+.5f)*envelope*(.35f+flow),0,1);
        float cool=.35f+.65f*glow;
        return clamp((cr*glow+.08f*cool)*255)<<16|clamp((cg*glow+.48f*cool)*255)<<8|clamp((cb*glow+.72f*cool)*255);
    }

    private static float smoothstep(float a,float b,float x) {
        float t=Math.clamp((x-a)/(b-a),0,1);
        return t*t*(3-2*t);
    }
    private static float fract(float x) { return x-(float)Math.floor(x); }
    private static float lerp(float t,float a,float b) { return a+(b-a)*t; }
    private static int clamp(float v) { return Math.clamp((int)v,0,255); }
}
