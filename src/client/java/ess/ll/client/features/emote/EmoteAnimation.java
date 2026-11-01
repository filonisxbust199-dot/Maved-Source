package ess.ll.client.features.emote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One Emotecraft emote (the "emote" json format of KosmX's playerAnimator, which Zenith plays them with), sampled
 * the way playerAnimator 2.0 does: an integer tick plus the partial tick, keyframes eased by the keyframe before,
 * the vanilla pose blended in before the first keyframe and back after endTick. In files older than version 3,
 * "torso" moves the whole player (here BODY); from version 3 it is the torso model part.
 */
public final class EmoteAnimation {
    public enum Part { HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, BODY }
    /** Positions are model units (BODY: blocks); rotations are radians. */
    public enum Axis { X, Y, Z, PITCH, YAW, ROLL }
    private static final String[] AXIS_KEYS={"x","y","z","pitch","yaw","roll"};
    /** Default x, y, z of each part, as playerAnimator's builder sets them. */
    private static final float[][] DEFAULTS={{0,0,0},{0,0,0},{-5,2,0},{5,2,0},{-1.9f,12,.1f},{1.9f,12,.1f},{0,0,0}};

    private record Key(int tick,float value,Ease ease) {}
    private static final class State {
        final float defaultValue;
        final List<Key> keys=new ArrayList<>();
        State(float defaultValue) { this.defaultValue=defaultValue; }
        /** Index of the last keyframe at or before the tick, -1 when none. */
        int findAtTick(int tick) {
            int i=-1;
            for(int k=0;k<keys.size();k++) if(keys.get(k).tick<=tick) i=k; else break;
            return i;
        }
        void add(Key key) { keys.add(findAtTick(key.tick)+1,key); }
    }
    /** Where the playback is: integer tick, partial tick, and whether a loop has wrapped once. */
    public record Time(int tick,float delta,boolean loopStarted) {}

    public final int beginTick,endTick,stopTick,returnTick;
    public final boolean loop;
    private final boolean easeBefore;
    private final Map<Part,State[]> parts=new EnumMap<>(Part.class);

    private EmoteAnimation(JsonObject root) {
        JsonObject emote=root.getAsJsonObject("emote");
        int version=root.has("version") ? root.get("version").getAsInt() : 1;
        beginTick=integer(emote,"beginTick",0);
        endTick=Math.max(1,integer(emote,"endTick",1));
        boolean looping=false;
        int back=0;
        if(emote.has("isLoop") && emote.has("returnTick")) {
            looping=bool(emote.get("isLoop"));
            back=integer(emote,"returnTick",0);
            if(looping && (back>endTick || back<0)) { looping=false; back=0; }
        }
        loop=looping;
        returnTick=back;
        stopTick=emote.has("stopTick") ? integer(emote,"stopTick",endTick) : endTick;
        easeBefore=emote.has("easeBeforeKeyframe") && bool(emote.get("easeBeforeKeyframe"));
        boolean degrees=!emote.has("degrees") || bool(emote.get("degrees"));
        for(JsonElement e:emote.getAsJsonArray("moves")) {
            JsonObject move=e.getAsJsonObject();
            int tick=integer(move,"tick",0);
            Ease ease=Ease.of(move.has("easing") ? move.get("easing").getAsString() : "linear");
            int turn=move.has("turn") ? move.get("turn").getAsInt() : 0;
            for(var entry:move.entrySet()) {
                Part part=part(entry.getKey(),version);
                if(part==null || !entry.getValue().isJsonObject()) continue;
                JsonObject node=entry.getValue().getAsJsonObject();
                State[] states=parts.computeIfAbsent(part,EmoteAnimation::states);
                for(int a=0;a<AXIS_KEYS.length;a++) {
                    if(!node.has(AXIS_KEYS[a])) continue;
                    float value=node.get(AXIS_KEYS[a]).getAsFloat();
                    boolean angle=a>=3;
                    if(degrees && angle) value*=(float)Math.PI/180;
                    states[a].add(new Key(tick,value,ease));
                    if(angle && turn!=0) states[a].add(new Key(tick,(float)(value+Math.PI*2*turn),ease));
                }
            }
        }
    }
    private static State[] states(Part part) {
        float[] d=DEFAULTS[part.ordinal()];
        return new State[]{new State(d[0]),new State(d[1]),new State(d[2]),new State(0),new State(0),new State(0)};
    }
    private static Part part(String key,int version) {
        return switch (key) {
            case "head" -> Part.HEAD;
            case "torso" -> version<3 ? Part.BODY : Part.TORSO;
            case "body" -> Part.BODY;
            case "rightArm" -> Part.RIGHT_ARM;
            case "leftArm" -> Part.LEFT_ARM;
            case "rightLeg" -> Part.RIGHT_LEG;
            case "leftLeg" -> Part.LEFT_LEG;
            default -> null;
        };
    }

    public static EmoteAnimation parse(JsonObject json) { return new EmoteAnimation(json); }

    /** The playback position after this many ticks (with the fraction); null once a one-shot emote stopped. */
    public Time time(float elapsed) {
        int ticks=(int)Math.floor(elapsed);
        float delta=elapsed-ticks;
        if(loop) {
            if(ticks<=endTick) return new Time(ticks,delta,false);
            return new Time(returnTick+(ticks-endTick-1)%(endTick-returnTick+1),delta,true);
        }
        return ticks>=stopTick ? null : new Time(ticks,delta,false);
    }

    public boolean animates(Part part) { return parts.containsKey(part); }

    /** The value of a part's axis at that time, given the value vanilla posed it with. */
    public float sample(Part part,Axis axis,Time time,float current) {
        State[] states=parts.get(part);
        State state=states==null ? null : states[axis.ordinal()];
        if(state==null || state.keys.isEmpty()) return current;
        boolean angle=axis.ordinal()>=3;
        if(angle) current=wrap(current);
        int pos=state.findAtTick(time.tick);
        Key before=before(state,pos,current,time);
        if(time.loopStarted && before.tick<returnTick) before=before(state,state.findAtTick(endTick),current,time);
        Key after=after(state,pos,current,time);
        if(loop && after.tick>endTick) after=after(state,state.findAtTick(returnTick-1),current,time);
        float value=interpolate(before,after,time);
        return angle ? wrap(value) : value;
    }
    private Key before(State s,int pos,float current,Time time) {
        if(pos==-1) {
            if(time.tick<beginTick) return new Key(0,current,Ease.LINEAR);
            return new Key(time.tick<endTick ? beginTick : endTick,s.defaultValue,Ease.LINEAR);
        }
        Key frame=s.keys.get(pos);
        if(!loop && time.tick>=endTick && pos==s.keys.size()-1 && frame.tick<endTick) return new Key(endTick,frame.value,frame.ease);
        return frame;
    }
    private Key after(State s,int pos,float current,Time time) {
        if(s.keys.size()>pos+1) return s.keys.get(pos+1);
        if(loop) return new Key(endTick+1,s.defaultValue,Ease.LINEAR);
        if(time.tick<endTick && !s.keys.isEmpty()) {
            Key last=s.keys.getLast();
            return new Key(endTick,last.value,last.ease);
        }
        if(time.tick>=endTick) return new Key(stopTick,current,Ease.LINEAR);
        return time.tick>=beginTick ? new Key(endTick,s.defaultValue,Ease.LINEAR) : new Key(beginTick,s.defaultValue,Ease.LINEAR);
    }
    private float interpolate(Key before,Key after,Time time) {
        int tb=before.tick,ta=after.tick;
        if(tb>=ta) {
            if(time.tick<tb) tb-=endTick-returnTick+1;
            else ta+=endTick-returnTick+1;
        }
        if(tb==ta) return before.value;
        float f=(time.tick+time.delta-tb)/(ta-tb);
        Ease ease=easeBefore ? after.ease : before.ease;
        return before.value+(after.value-before.value)*ease.apply(Math.clamp(f,0,1));
    }
    private static float wrap(float radians) {
        double b=(radians+Math.PI)%(Math.PI*2);
        if(b<0) b+=Math.PI*2;
        return (float)(b-Math.PI);
    }

    private static boolean bool(JsonElement e) {
        if(e==null || e.isJsonNull()) return false;
        if(e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        return e.getAsString().trim().equalsIgnoreCase("true");
    }
    private static int integer(JsonObject o,String key,int fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? Math.round(o.get(key).getAsFloat()) : fallback;
    }

    /** The easings of the format; names like EASEINOUTQUAD, InOutSine or inQuad, unknown ones are linear. */
    enum Ease {
        LINEAR, CONSTANT, INSINE, OUTSINE, INOUTSINE, INQUAD, OUTQUAD, INOUTQUAD, INCUBIC, OUTCUBIC, INOUTCUBIC,
        INQUART, OUTQUART, INOUTQUART, INQUINT, OUTQUINT, INOUTQUINT, INEXPO, OUTEXPO, INOUTEXPO,
        INCIRC, OUTCIRC, INOUTCIRC, INBACK, OUTBACK, INOUTBACK, INELASTIC, OUTELASTIC, INOUTELASTIC,
        INBOUNCE, OUTBOUNCE, INOUTBOUNCE;

        static Ease of(String name) {
            String n=name.toUpperCase(Locale.ROOT);
            if(n.length()>=4 && n.startsWith("EASE")) n=n.substring(4);
            try { return valueOf(n); } catch(IllegalArgumentException e) { return LINEAR; }
        }
        float apply(float t) {
            double x=t;
            return (float)switch (this) {
                case LINEAR -> x;
                case CONSTANT -> 0;
                case INSINE -> 1-Math.cos(x*Math.PI/2);
                case OUTSINE -> Math.sin(x*Math.PI/2);
                case INOUTSINE -> -(Math.cos(Math.PI*x)-1)/2;
                case INQUAD -> x*x;
                case OUTQUAD -> 1-(1-x)*(1-x);
                case INOUTQUAD -> x<.5 ? 2*x*x : 1-Math.pow(-2*x+2,2)/2;
                case INCUBIC -> x*x*x;
                case OUTCUBIC -> 1-Math.pow(1-x,3);
                case INOUTCUBIC -> x<.5 ? 4*x*x*x : 1-Math.pow(-2*x+2,3)/2;
                case INQUART -> Math.pow(x,4);
                case OUTQUART -> 1-Math.pow(1-x,4);
                case INOUTQUART -> x<.5 ? 8*Math.pow(x,4) : 1-Math.pow(-2*x+2,4)/2;
                case INQUINT -> Math.pow(x,5);
                case OUTQUINT -> 1-Math.pow(1-x,5);
                case INOUTQUINT -> x<.5 ? 16*Math.pow(x,5) : 1-Math.pow(-2*x+2,5)/2;
                case INEXPO -> x==0 ? 0 : Math.pow(2,10*x-10);
                case OUTEXPO -> x==1 ? 1 : 1-Math.pow(2,-10*x);
                case INOUTEXPO -> x==0 ? 0 : x==1 ? 1 : x<.5 ? Math.pow(2,20*x-10)/2 : (2-Math.pow(2,-20*x+10))/2;
                case INCIRC -> 1-Math.sqrt(1-x*x);
                case OUTCIRC -> Math.sqrt(1-Math.pow(x-1,2));
                case INOUTCIRC -> x<.5 ? (1-Math.sqrt(1-Math.pow(2*x,2)))/2 : (Math.sqrt(1-Math.pow(-2*x+2,2))+1)/2;
                case INBACK -> 2.70158*x*x*x-1.70158*x*x;
                case OUTBACK -> 1+2.70158*Math.pow(x-1,3)+1.70158*Math.pow(x-1,2);
                case INOUTBACK -> x<.5 ? Math.pow(2*x,2)*((2.5949095+1)*2*x-2.5949095)/2
                        : (Math.pow(2*x-2,2)*((2.5949095+1)*(x*2-2)+2.5949095)+2)/2;
                case INELASTIC -> x==0 ? 0 : x==1 ? 1 : -Math.pow(2,10*x-10)*Math.sin((x*10-10.75)*(2*Math.PI/3));
                case OUTELASTIC -> x==0 ? 0 : x==1 ? 1 : Math.pow(2,-10*x)*Math.sin((x*10-.75)*(2*Math.PI/3))+1;
                case INOUTELASTIC -> x==0 ? 0 : x==1 ? 1 : x<.5 ? -(Math.pow(2,20*x-10)*Math.sin((20*x-11.125)*(2*Math.PI/4.5)))/2
                        : Math.pow(2,-20*x+10)*Math.sin((20*x-11.125)*(2*Math.PI/4.5))/2+1;
                case INBOUNCE -> 1-bounce(1-x);
                case OUTBOUNCE -> bounce(x);
                case INOUTBOUNCE -> x<.5 ? (1-bounce(1-2*x))/2 : (1+bounce(2*x-1))/2;
            };
        }
        private static double bounce(double x) {
            double n=7.5625,d=2.75;
            if(x<1/d) return n*x*x;
            if(x<2/d) return n*(x-=1.5/d)*x+.75;
            if(x<2.5/d) return n*(x-=2.25/d)*x+.9375;
            return n*(x-=2.625/d)*x+.984375;
        }
    }
}
