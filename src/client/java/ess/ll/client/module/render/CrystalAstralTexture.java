package ess.ll.client.module.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** ESS's six astral crystal wire profiles, generated over the vanilla crystal UV mask. */
final class CrystalAstralTexture {
    private static final Identifier ID=Identifier.fromNamespaceAndPath("ess-best","dynamic/crystal_astral");
    private static final Identifier VANILLA=Identifier.fromNamespaceAndPath("minecraft","textures/entity/end_crystal/end_crystal.png");
    private static final int[][] FACE_UVS={
            {0,16,16,32},{16,0,32,16},{16,16,32,32},{32,0,48,16},
            {32,16,48,32},{48,16,64,32},{64,16,80,32},{80,0,96,16},
            {80,16,96,32},{96,0,112,16},{96,16,112,32},{112,16,128,32}};
    private static DynamicTexture texture;
    private static int[] alphaMask;
    private static boolean failed;
    private static int lastProfile=-1,lastWidth=-1;

    private CrystalAstralTexture() {}

    static Identifier identifier() { return texture!=null && !failed ? ID : null; }

    static void ensure() {
        if(failed) return;
        int profile=switch (CustomCrystal.ASTRAL_SHAPE.get()) {
            case "Контур" -> 1;
            case "Созвездие" -> 2;
            case "Грани" -> 3;
            case "Ядро" -> 4;
            default -> 0;
        };
        int width=Math.round(Math.clamp(CustomCrystal.ASTRAL_LINE_WIDTH.get(),.5f,2f)*20);
        if(texture!=null && profile==lastProfile && width==lastWidth) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.getTextureManager()==null) return;
        try {
            if(profile!=4 && alphaMask==null) {
                var resource=mc.getResourceManager().getResource(VANILLA);
                if(resource.isEmpty()) { failed=true; return; }
                try(InputStream in=resource.get().open(); NativeImage vanilla=NativeImage.read(in)) {
                    int[] mask=new int[128*64];
                    for(int y=0;y<64 && y<vanilla.getHeight();y++)
                        for(int x=0;x<128 && x<vanilla.getWidth();x++)
                            mask[y*128+x]=vanilla.getPixel(x,y)>>>24;
                    alphaMask=mask;
                }
            }
            if(texture==null) {
                texture=new DynamicTexture(()->ID.toString(),128,64,false);
                mc.getTextureManager().register(ID,texture);
            }
            NativeImage image=texture.getPixels();
            if(image==null) { failed=true; return; }
            for(int y=0;y<64;y++) for(int x=0;x<128;x++) {
                if(profile!=4 && alphaMask[y*128+x]==0) { image.setPixel(x,y,0); continue; }
                int alpha=wireAlpha(x,y,profile,width);
                image.setPixel(x,y,alpha==0 ? 0 : alpha<<24|0xffffff);
            }
            texture.upload();
            lastProfile=profile; lastWidth=width;
        } catch(Exception e) {
            failed=true;
            System.err.println("[ESS] Astral crystal texture failed: "+e);
        }
    }

    private static int wireAlpha(int x,int y,int profile,int widthKey) {
        float wire=distanceToWire(x,y,profile);
        if(wire==Float.MAX_VALUE) return 0;
        if(profile==4 && widthKey==40) return 255;
        float distance=wire/(widthKey/20f);
        if(distance<=.7f) return 255;
        if(distance<=1.65f) return 180;
        if(distance<=2.65f) return 72;
        return 0;
    }

    private static float distanceToWire(int x,int y,int profile) {
        float nearest=Float.MAX_VALUE;
        for(int[] face:FACE_UVS) {
            if(x<face[0] || x>=face[2] || y<face[1] || y>=face[3]) continue;
            float localX=x-face[0],localY=y-face[1];
            float maxX=face[2]-face[0]-1f,maxY=face[3]-face[1]-1f;
            float border=Math.min(Math.min(localX,maxX-localX),Math.min(localY,maxY-localY));
            float diagonalA=Math.abs(localX/maxX-localY/maxY)*11;
            float diagonalB=Math.abs(localX/maxX-(1-localY/maxY))*11;
            float wire=switch (profile) {
                case 1 -> border;
                case 2 -> {
                    float cornerX=Math.min(localX,maxX-localX),cornerY=Math.min(localY,maxY-localY);
                    float corner=(float)Math.sqrt(cornerX*cornerX+cornerY*cornerY);
                    float cx=localX-maxX*.5f,cy=localY-maxY*.5f;
                    yield Math.min(corner,(float)Math.sqrt(cx*cx+cy*cy));
                }
                case 3 -> Math.min(border,diagonalA);
                case 4 -> {
                    float diamond=Math.abs(Math.abs(localX-maxX*.5f)+Math.abs(localY-maxY*.5f)-maxX*.35f);
                    yield Math.min(border,diamond);
                }
                default -> Math.min(border,Math.min(diagonalA,diagonalB));
            };
            nearest=Math.min(nearest,wire);
        }
        return nearest;
    }
}
