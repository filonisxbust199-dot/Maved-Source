package ess.ll.client.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable local-space meshes. Animation changes only the transform, never the cache key. */
final class UiGeometry {
    private static final int SEGMENTS=16, POINTS=4*(SEGMENTS+1), SHADOW_RINGS=8;
    /** Corner profiles: the L4 squircle used across the UI, and a plain circular arc. */
    private static final float[] COS=new float[SEGMENTS+1], SIN=new float[SEGMENTS+1], ARC_COS=new float[SEGMENTS+1], ARC_SIN=new float[SEGMENTS+1];
    private static final Map<Key,float[]> CACHE=new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key,float[]> entry) { return size()>128; }
    };
    static {
        for(int i=0;i<=SEGMENTS;i++) {
            COS[i]=(float)Math.sqrt(Math.max(0,Math.cos(i*Math.PI/(2*SEGMENTS))));
            SIN[i]=(float)Math.sqrt(Math.max(0,Math.sin(i*Math.PI/(2*SEGMENTS))));
            ARC_COS[i]=(float)Math.cos(i*Math.PI/(2*SEGMENTS));
            ARC_SIN[i]=(float)Math.sin(i*Math.PI/(2*SEGMENTS));
        }
    }
    private record Key(float w,float h,float radius,float feather,boolean shadow,boolean circular) {}
    static synchronized float[] mesh(float w,float h,float radius,float feather,boolean shadow,boolean circular) {
        return CACHE.computeIfAbsent(new Key(w,h,radius,feather,shadow,circular),UiGeometry::build);
    }
    private static float[] build(Key k) {
        if(k.radius==0 && !k.shadow) return new float[]{0,0,1, 0,k.h,1, k.w,k.h,1, k.w,0,1};
        int rings=k.shadow ? SHADOW_RINGS : 1;
        float[] vertices=new float[POINTS*4*(rings+1)*3];
        float[] inner=contour(k,0);
        int cursor=0;
        for(int i=0;i<POINTS;i++) {
            int a=i*2,b=((i+1)%POINTS)*2;
            cursor=put(vertices,cursor,k.w/2,k.h/2,1);
            cursor=put(vertices,cursor,inner[b],inner[b+1],1);
            cursor=put(vertices,cursor,inner[a],inner[a+1],1);
            cursor=put(vertices,cursor,k.w/2,k.h/2,1);
        }
        float innerAlpha=1;
        for(int ring=1;ring<=rings;ring++) {
            float t=(float)ring/rings;
            float[] outer=contour(k,k.feather*t);
            float outerAlpha=ring==rings ? 0 : (float)Math.exp(-4.5*t*t);
            for(int i=0;i<POINTS;i++) {
                int a=i*2,b=((i+1)%POINTS)*2;
                cursor=put(vertices,cursor,inner[b],inner[b+1],innerAlpha);
                cursor=put(vertices,cursor,outer[b],outer[b+1],outerAlpha);
                cursor=put(vertices,cursor,outer[a],outer[a+1],outerAlpha);
                cursor=put(vertices,cursor,inner[a],inner[a+1],innerAlpha);
            }
            inner=outer;
            innerAlpha=outerAlpha;
        }
        return vertices;
    }
    private record RingKey(float w,float h,float radius,float width,float feather,boolean circular) {}
    private static final Map<RingKey,float[]> RINGS=new LinkedHashMap<>(64,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<RingKey,float[]> entry) { return size()>64; }
    };
    /** A stroke of the given width just inside the rounded rectangle's edge, feathered on both sides. */
    static synchronized float[] ring(float w,float h,float radius,float width,float feather,boolean circular) {
        return RINGS.computeIfAbsent(new RingKey(w,h,radius,width,feather,circular),k->{
            float t=Math.min(k.width,Math.min(k.w,k.h)/2),f=k.feather;
            float[][] bands={
                    shape(-f,-f,k.w+2*f,k.h+2*f,k.radius+f,k.circular),shape(0,0,k.w,k.h,k.radius,k.circular),
                    shape(t,t,k.w-2*t,k.h-2*t,Math.max(0,k.radius-t),k.circular),
                    shape(t+f,t+f,k.w-2*t-2*f,k.h-2*t-2*f,Math.max(0,k.radius-t-f),k.circular)};
            float[] alphas={0,1,1,0};
            float[] v=new float[POINTS*4*3*3];
            int cursor=0;
            for(int band=0;band<3;band++) {
                float[] a=bands[band],b=bands[band+1];
                for(int i=0;i<POINTS;i++) {
                    int p=i*2,q=((i+1)%POINTS)*2;
                    cursor=put(v,cursor,b[q],b[q+1],alphas[band+1]);
                    cursor=put(v,cursor,a[q],a[q+1],alphas[band]);
                    cursor=put(v,cursor,a[p],a[p+1],alphas[band]);
                    cursor=put(v,cursor,b[p],b[p+1],alphas[band+1]);
                }
            }
            return v;
        });
    }
    /** Contour of a rounded rectangle at (x, y) with the same corner profile and point order as contour(). */
    private static float[] shape(float x,float y,float w,float h,float r,boolean circular) {
        w=Math.max(0,w); h=Math.max(0,h); r=Math.clamp(r,0,Math.min(w,h)/2);
        float[] points=new float[POINTS*2];
        for(int i=0;i<POINTS;i++) {
            int corner=i/(SEGMENTS+1),step=i%(SEGMENTS+1);
            float c=circular ? ARC_COS[step] : COS[step],s=circular ? ARC_SIN[step] : SIN[step],px,py;
            switch (corner) {
                case 0 -> { px=w-r+r*c; py=h-r+r*s; }
                case 1 -> { px=r-r*s; py=h-r+r*c; }
                case 2 -> { px=r-r*c; py=r-r*s; }
                default -> { px=w-r+r*s; py=r-r*c; }
            }
            points[i*2]=x+px; points[i*2+1]=y+py;
        }
        return points;
    }
    private static int put(float[] v,int i,float x,float y,float alpha) {
        v[i++]=x; v[i++]=y; v[i++]=alpha; return i;
    }
    private static float[] contour(Key k,float expand) {
        float[] points=new float[POINTS*2];
        float r=k.radius, rr=r+expand;
        for(int i=0;i<POINTS;i++) {
            int corner=i/(SEGMENTS+1), step=i%(SEGMENTS+1);
            float c=k.circular ? ARC_COS[step] : COS[step],s=k.circular ? ARC_SIN[step] : SIN[step],x,y;
            switch (corner) {
                case 0 -> { x=k.w-r+rr*c; y=k.h-r+rr*s; }
                case 1 -> { x=r-rr*s; y=k.h-r+rr*c; }
                case 2 -> { x=r-rr*c; y=r-rr*s; }
                default -> { x=k.w-r+rr*s; y=r-rr*c; }
            }
            points[i*2]=x; points[i*2+1]=y;
        }
        return points;
    }
}
