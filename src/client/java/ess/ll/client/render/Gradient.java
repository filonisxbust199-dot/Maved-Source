package ess.ll.client.render;

/** Immutable gradient in local GUI coordinates. Phase is captured once per frame. */
public record Gradient(int from, int to, Axis axis, float origin, float length, double phase, boolean flowing) {
    public enum Axis { HORIZONTAL, VERTICAL }
    public static Gradient solid(int color) { return new Gradient(color,color,Axis.HORIZONTAL,0,1,0,false); }
    public static Gradient linear(int from,int to,Axis axis,float origin,float length) {
        return new Gradient(from,to,axis,origin,Math.max(1,length),0,false);
    }
    public static Gradient flow(int from,int to,Axis axis,float origin,float length,double phase) {
        return new Gradient(from,to,axis,origin,Math.max(1,length),phase,true);
    }
    public int at(float x,float y) {
        if(from==to) return from;
        float position=((axis==Axis.HORIZONTAL ? x : y)-origin)/length;
        float t=flowing ? (float)(0.5-0.5*Math.cos((position-phase)*Math.PI*2)) : Math.clamp(position,0,1);
        int result=0;
        for(int shift=0;shift<=24;shift+=8) {
            int a=(from>>>shift)&255, b=(to>>>shift)&255;
            result |= Math.round(a+(b-a)*t)<<shift;
        }
        return result;
    }
}
