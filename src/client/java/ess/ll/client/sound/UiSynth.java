package ess.ll.client.sound;

import com.mojang.blaze3d.audio.SoundBuffer;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procedurally synthesised interface sounds (no audio files). Each sound is rendered once into a
 * 16-bit mono PCM buffer and handed to the game's sound engine in place of an .ogg file.
 */
public final class UiSynth {
    public static final String PREFIX="sounds/synth/";
    private static final int RATE=44100;
    private static final AudioFormat FORMAT=new AudioFormat(RATE,16,1,true,false);
    private static final Map<String,float[]> CACHE=new ConcurrentHashMap<>();
    private UiSynth() {}

    /** Buffer for a resource path such as {@code sounds/synth/open.ogg}, or null when unknown. */
    public static SoundBuffer buffer(String path) {
        String name=path.substring(PREFIX.length()).replace(".ogg","");
        float[] samples=CACHE.computeIfAbsent(name,UiSynth::render);
        if(samples==null) return null;
        ByteBuffer data=ByteBuffer.allocateDirect(samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
        for(float s:samples) data.putShort((short)Math.round(Math.clamp(s,-1,1)*32767));
        data.flip();
        return new SoundBuffer(data,FORMAT);
    }
    public static boolean isSynth(String path) { return path.startsWith(PREFIX); }

    private static float[] render(String name) {
        return switch (name) {
            // Two-note rising chime with a soft airy swell underneath.
            case "open" -> mix(tone(659.25,.32,.004,.26,.55,.18), delay(tone(987.77,.30,.004,.24,.5,.15),.06), noiseSwell(.22,.05,.18));
            case "close" -> mix(tone(987.77,.22,.003,.18,.45,.12), delay(tone(659.25,.24,.003,.2,.45,.12),.05));
            // Tiny, airy tick for hovering over cards.
            case "hover" -> mix(click(.018,.35,5200), tone(3200,.03,.001,.02,.12,0));
            case "toggle_on" -> mix(sweep(620,1240,.09,.5), click(.012,.3,3800));
            case "toggle_off" -> mix(sweep(900,450,.09,.45), click(.012,.25,2600));
            case "expand" -> sweep(420,640,.06,.45);
            case "collapse" -> sweep(640,420,.06,.4);
            case "select" -> mix(click(.01,.4,4500), tone(1567.98,.09,.002,.07,.35,.2));
            case "slider" -> mix(click(.008,.35,6000), tone(2400,.025,.001,.015,.15,0));
            case "bind_listen" -> mix(tone(880,.07,.003,.05,.35,.1), delay(tone(880,.07,.003,.05,.35,.1),.09));
            case "bind_set" -> mix(tone(1046.5,.18,.003,.14,.4,.15), delay(tone(1318.5,.2,.003,.16,.4,.15),.045));
            default -> null;
        };
    }

    /** Sine with a touch of 2nd harmonic; attack and exponential decay in seconds. */
    private static float[] tone(double freq,double length,double attack,double decay,double gain,double harmonic) {
        float[] out=new float[(int)(length*RATE)];
        for(int i=0;i<out.length;i++) {
            double t=(double)i/RATE;
            double env=Math.min(1,t/attack)*Math.exp(-t/Math.max(decay,1e-4)*2.2);
            double wave=Math.sin(2*Math.PI*freq*t)+harmonic*Math.sin(4*Math.PI*freq*t);
            out[i]=(float)(wave*env*gain);
        }
        fadeOut(out);
        return out;
    }
    /** Exponential pitch glide, good for on/off blips. */
    private static float[] sweep(double from,double to,double length,double gain) {
        float[] out=new float[(int)(length*RATE)];
        double phase=0;
        for(int i=0;i<out.length;i++) {
            double k=(double)i/out.length;
            double freq=from*Math.pow(to/from,k);
            phase+=2*Math.PI*freq/RATE;
            double env=Math.min(1,k*40)*Math.pow(1-k,1.6);
            out[i]=(float)(Math.sin(phase)*env*gain);
        }
        return out;
    }
    /** Short band-limited noise burst: one-pole high-pass shaped by the cutoff, fast decay. */
    private static float[] click(double length,double gain,double cutoff) {
        Random random=new Random(7);
        float[] out=new float[(int)(length*RATE)];
        double alpha=Math.exp(-2*Math.PI*cutoff/RATE), prevIn=0, prevOut=0;
        for(int i=0;i<out.length;i++) {
            double in=random.nextDouble()*2-1;
            double high=alpha*(prevOut+in-prevIn);
            prevIn=in; prevOut=high;
            double env=Math.exp(-(double)i/out.length*6);
            out[i]=(float)(high*env*gain);
        }
        return out;
    }
    /** Low-passed noise that swells and fades, like a soft whoosh. */
    private static float[] noiseSwell(double length,double peakAt,double gain) {
        Random random=new Random(11);
        float[] out=new float[(int)(length*RATE)];
        double state=0;
        for(int i=0;i<out.length;i++) {
            double t=(double)i/RATE;
            state+=(random.nextDouble()*2-1-state)*.08;
            double env=t<peakAt ? t/peakAt : Math.exp(-(t-peakAt)/(length-peakAt)*4);
            out[i]=(float)(state*env*gain);
        }
        fadeOut(out);
        return out;
    }
    private static float[] delay(float[] in,double seconds) {
        int offset=(int)(seconds*RATE);
        float[] out=new float[in.length+offset];
        System.arraycopy(in,0,out,offset,in.length);
        return out;
    }
    private static float[] mix(float[]... parts) {
        int length=0;
        for(float[] p:parts) length=Math.max(length,p.length);
        float[] out=new float[length];
        for(float[] p:parts) for(int i=0;i<p.length;i++) out[i]+=p[i];
        return out;
    }
    /** Removes the click a non-zero final sample would cause. */
    private static void fadeOut(float[] s) {
        int n=Math.min(s.length,RATE/200);
        for(int i=0;i<n;i++) s[s.length-1-i]*=(float)i/n;
    }
}
