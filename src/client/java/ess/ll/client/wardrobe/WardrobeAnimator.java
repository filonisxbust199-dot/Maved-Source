package ess.ll.client.wardrobe;

import ess.ll.client.wardrobe.WardrobeModel.Clip;
import ess.ll.client.wardrobe.WardrobeModel.Driver;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Wardrobe animation (from Zenith): which clip plays and how clips blend into each other, the physics drivers
 * (capes, cloth, springs, sway, pet legs, clock hands) and a per-wearer cache of that state. A pose is 9 floats per
 * bone: rotation xyz (radians), position xyz (pixels), scale xyz.
 */
public final class WardrobeAnimator {
    private WardrobeAnimator() {}

    /** How the wearer moves this frame; drives capes, cloth and springs. */
    public static final class Motion {
        /** Cape lean values of the player (forward lean, sideways lean, the second lean). */
        public float capeFlap,capeLean,capeLean2;
        public float leftLegPitch,rightLegPitch,walkSpeed,yawSpeed,dt;
        public boolean sneaking,airborne,moving;
    }

    /** Which clip is playing (by state name like idle / walk / run), blending 0.38 s between clips. */
    public static final class ClipPlayer {
        private final float[] scratch=new float[9];
        private String wanted="idle",current="idle",previous;
        private float wantedTime,time,previousTime,changedAt=-10,blend=.38f;
        private boolean oneShot,frozen;
        private float speed=1,targetSpeed=1,lastNow=Float.NaN;
        private float[] lastPose=new float[0],frozenPose=new float[0];

        public String current() { return current; }
        public boolean playingOneShot() { return oneShot; }
        public void speed(float speed) { targetSpeed=speed; }

        /** Asks for a state; clips missing from the model fall back (run → walk → idle, sit → ...). */
        public void want(WardrobeModel model,String state,float now) {
            String clip=resolve(model,state);
            if(clip!=null && !clip.equals(wanted)) {
                wanted=clip;
                wantedTime=0;
                if(!oneShot) switchTo(clip,now,0,.38f);
            }
        }
        /** Plays a clip once (like a bow pull) over the looping state; false when the model has no such clip. */
        public boolean playOnce(WardrobeModel model,String clip,float now) {
            Clip c=model.clip(clip);
            if(c==null) return false;
            oneShot=!c.loop;
            switchTo(clip,now,0,.26f);
            return true;
        }
        public void endOneShot(float now) { oneShot=false; switchTo(wanted,now,wantedTime,.38f); }
        public void update(WardrobeModel model,float now) {
            advance(now);
            if(oneShot) {
                Clip c=model.clip(current);
                if(c==null || time>=c.length) { oneShot=false; switchTo(wanted,now,wantedTime,.38f); }
            }
        }
        private void advance(float now) {
            if(Float.isNaN(lastNow)) { lastNow=now; return; }
            float dt=Math.max(0,Math.min(.25f,now-lastNow));
            lastNow=now;
            if(dt<=0) return;
            speed+=(targetSpeed-speed)*Math.min(1,dt*6);
            float rate=oneShot ? 1 : speed;
            time+=dt*rate;
            if(previous!=null) previousTime+=dt*rate;
            if(oneShot) wantedTime+=dt;
            else wantedTime=time;
        }
        private void switchTo(String clip,float now,float start,float duration) {
            if(clip.equals(current)) return;
            // A switch in the middle of a blend starts from the pose shown right now, so nothing jumps.
            float progress=(now-changedAt)/blend;
            if(progress<1 && lastPose.length>0) {
                if(frozenPose.length!=lastPose.length) frozenPose=new float[lastPose.length];
                System.arraycopy(lastPose,0,frozenPose,0,lastPose.length);
                frozen=true;
            } else frozen=false;
            previous=current; previousTime=time;
            current=clip; time=start; changedAt=now; blend=duration;
        }
        private static String resolve(WardrobeModel model,String state) {
            if(model.clip(state)!=null) return state;
            return switch (state) {
                case "run" -> model.clip("walk")!=null ? "walk" : resolve(model,"idle");
                case "perch" -> model.clip("sit")!=null ? "sit" : resolve(model,"idle");
                case "idle" -> model.clip("idle")!=null ? "idle" : null;
                default -> resolve(model,"idle");
            };
        }
        /** The clip pose of one bone at this moment, blended from the previous clip. */
        public void sample(WardrobeModel model,int bone,float now,float[] out) {
            advance(now);
            Clip clip=model.clip(current);
            if(clip==null) identity(out);
            else clip.sample(bone,clip.wrap(time),out);
            float progress=(now-changedAt)/blend;
            if(progress<1 && previous!=null) {
                float t=smootherStep(Math.max(0,progress));
                if(frozen && bone*9+9<=frozenPose.length) {
                    for(int k=0;k<9;k++) { float a=frozenPose[bone*9+k]; out[k]=a+(out[k]-a)*t; }
                } else {
                    Clip before=model.clip(previous);
                    if(before!=null) {
                        before.sample(bone,before.wrap(previousTime),scratch);
                        for(int k=0;k<9;k++) out[k]=scratch[k]+(out[k]-scratch[k])*t;
                    }
                }
            }
            remember(bone,out);
        }
        /** A clip at a fraction (0..1) of its length, for poses that follow something like a bow's pull. */
        public void sampleAt(WardrobeModel model,String name,int bone,float fraction,float[] out) {
            Clip clip=model.clip(name);
            if(clip==null) identity(out);
            else clip.sample(bone,Math.max(0,Math.min(1,fraction))*clip.length,out);
        }
        private void remember(int bone,float[] pose) {
            int need=(bone+1)*9;
            if(lastPose.length<need) {
                float[] grown=new float[Math.max(need,lastPose.length*2)];
                System.arraycopy(lastPose,0,grown,0,lastPose.length);
                for(int i=lastPose.length;i<grown.length;i+=9) grown[i+6]=grown[i+7]=grown[i+8]=1;
                lastPose=grown;
            }
            System.arraycopy(pose,0,lastPose,bone*9,9);
        }
        private static float smootherStep(float t) { return t*t*t*(t*(t*6-15)+10); }
    }

    /** Bone poses for one wearer and model: clip, rest rotation and the drivers on top. */
    public static final class Pose {
        private static final long START=System.nanoTime();
        private static int zoneOffset;
        private static long zoneCheckedAt;
        private static boolean zoneKnown;
        private final float[] scratch=new float[9];
        private float[] springAngle=new float[0],springVelocity=new float[0];
        public float[] values=new float[0];

        public float[] compute(WardrobeModel model,ClipPlayer clips,float now,Motion motion) {
            int n=model.bones();
            if(values.length!=n*9) { values=new float[n*9]; springAngle=new float[n*2]; springVelocity=new float[n*2]; }
            if(clips!=null) clips.update(model,now);
            for(int b=0;b<n;b++) {
                if(clips!=null) clips.sample(model,b,now,scratch);
                else identity(scratch);
                int i=b*9;
                values[i]=model.rotation[b*3]+scratch[0]*(float)(Math.PI/180);
                values[i+1]=model.rotation[b*3+1]+scratch[1]*(float)(Math.PI/180);
                values[i+2]=model.rotation[b*3+2]+scratch[2]*(float)(Math.PI/180);
                System.arraycopy(scratch,3,values,i+3,6);
                if(model.driver[b]==Driver.CLOCK) clock(model,b);
                else if(motion!=null) drive(model,b,motion,now);
            }
            return values;
        }
        /** Characters at rest: anchored root bones keep their rest rotation without clip motion. */
        public void restRoots(WardrobeModel model) {
            int n=Math.min(model.bones(),values.length/9);
            for(int b=0;b<n;b++) {
                if(model.parent[b]>=0 || model.anchor[b]<0) continue;
                int i=b*9;
                values[i]=model.rotation[b*3]; values[i+1]=model.rotation[b*3+1]; values[i+2]=model.rotation[b*3+2];
                values[i+3]=values[i+4]=values[i+5]=0;
                values[i+6]=values[i+7]=values[i+8]=1;
            }
        }
        /** Adds a clip at a fraction of its length on top, weighted (bow pull and similar). */
        public void overlay(WardrobeModel model,ClipPlayer clips,String clip,float fraction,float weight) {
            Clip c=model.clip(clip);
            if(c==null || weight<=0) return;
            for(int b=0;b<model.bones();b++) {
                if(!c.animates(b)) continue;
                clips.sampleAt(model,clip,b,fraction,scratch);
                int i=b*9;
                for(int k=0;k<3;k++) values[i+k]+=scratch[k]*(float)(Math.PI/180)*weight;
                for(int k=3;k<6;k++) values[i+k]+=scratch[k]*weight;
                for(int k=6;k<9;k++) values[i+k]*=1+(scratch[k]-1)*weight;
            }
        }
        private void drive(WardrobeModel model,int b,Motion m,float now) {
            Driver d=model.driver[b];
            if(d==Driver.NONE) return;
            float a0=model.driverArgs[b*4],a1=model.driverArgs[b*4+1],a2=model.driverArgs[b*4+2],a3=model.driverArgs[b*4+3];
            int i=b*9;
            float deg=(float)(Math.PI/180);
            switch (d) {
                case CAPE -> {
                    float k=a0==0 ? 1 : a0;
                    values[i]+=(6+m.capeLean/2+m.capeFlap)*deg*k;
                    values[i+2]+=m.capeLean2/2*deg*k;
                    values[i+1]-=m.capeLean2/2*deg*k*.5f;
                }
                case LEGS_FRONT -> values[i]+=Math.min(0,Math.min(m.leftLegPitch,m.rightLegPitch))*(a0==0 ? 1 : a0);
                case LEGS_BACK -> values[i]+=Math.max(0,Math.max(m.leftLegPitch,m.rightLegPitch))*(a0==0 ? 1 : a0);
                case SPRING -> spring(b,m,a0==0 ? 70 : a0,a1==0 ? 9 : a1,a2==0 ? 1 : a2,a3==0 ? 35 : a3);
                case SWAY -> {
                    float amplitude=a0==0 ? 6 : a0,frequency=a1==0 ? .5f : a1;
                    int axis=Math.max(0,Math.min(2,(int)a2));
                    values[i+axis]+=(float)Math.sin((now*frequency+a3)*Math.PI*2)*amplitude*deg;
                }
                case CLOTH -> {
                    float follow=a0==0 ? .35f : a0,flutter=a1==0 ? 5 : a1,frequency=a2==0 ? .9f : a2;
                    float energy=Math.min(1.5f,m.walkSpeed+(m.airborne ? .6f : 0));
                    float wave=(float)Math.sin((now*frequency*(1+energy*1.6f)-a3)*Math.PI*2);
                    float wave2=(float)Math.sin((now*frequency*2.3f*(1+energy)-a3*1.7f)*Math.PI*2);
                    values[i]+=((6+m.capeLean/2+m.capeFlap)*follow+wave*flutter*(.35f+energy))*deg;
                    values[i+2]+=(m.capeLean2/2*follow+wave2*flutter*.25f*(.2f+energy))*deg;
                }
                default -> {}
            }
        }
        /** A damped spring towards the lean and the turn speed, clamped to the limit (degrees). */
        private void spring(int b,Motion m,float stiffness,float damping,float gain,float limit) {
            float dt=Math.min(Math.max(m.dt,0),.05f);
            float targetPitch=clamp(m.capeLean*.35f*gain+(m.airborne ? -8*gain : 0),limit);
            float targetRoll=clamp(-m.yawSpeed*.04f*gain,limit);
            int s=b*2;
            if(dt>0) for(int k=0;k<2;k++) {
                float target=k==0 ? targetPitch : targetRoll;
                float force=stiffness*(target-springAngle[s+k])-damping*springVelocity[s+k];
                springVelocity[s+k]+=force*dt;
                springAngle[s+k]+=springVelocity[s+k]*dt;
                springAngle[s+k]=clamp(springAngle[s+k],limit*1.4f);
            }
            int i=b*9;
            values[i]+=springAngle[s]*(float)(Math.PI/180);
            values[i+2]+=springAngle[s+1]*(float)(Math.PI/180);
        }
        /** Clock hands: seconds (0), minutes (1), hours (2) of the local time; 3+ spin or swing by time. */
        private void clock(WardrobeModel model,int b) {
            int hand=(int)model.driverArgs[b*4];
            if(hand>=3) {
                double t=(System.nanoTime()-START)/1e9;
                float speed=model.driverArgs[b*4+1],phase=model.driverArgs[b*4+2];
                double angle=hand==3 ? t*speed+phase : Math.sin((t*model.driverArgs[b*4+3]+phase)*Math.PI*2)*speed;
                values[b*9]=(float)Math.toRadians(angle%360);
                return;
            }
            Instant now=Instant.now();
            long nanos=System.nanoTime();
            if(!zoneKnown || nanos-zoneCheckedAt>1_000_000_000L) {
                zoneOffset=ZoneId.systemDefault().getRules().getOffset(now).getTotalSeconds();
                zoneCheckedAt=nanos; zoneKnown=true;
            }
            long day=Math.floorMod(now.getEpochSecond()+zoneOffset,86400L);
            int hours=(int)(day/3600),minutes=(int)(day/60%60);
            double seconds=day%60+now.getNano()/1e9;
            double angle=switch (hand) {
                case 0 -> seconds*6;
                case 1 -> (minutes+seconds/60)*6;
                default -> (hours%12+minutes/60.0+seconds/3600)*30;
            };
            values[b*9]=(float)Math.toRadians(angle);
        }
        private static float clamp(float v,float limit) { return Math.max(-limit,Math.min(limit,v)); }
    }

    static void identity(float[] pose) {
        pose[0]=pose[1]=pose[2]=0; pose[3]=pose[4]=pose[5]=0; pose[6]=pose[7]=pose[8]=1;
    }

    /** Animation state per wearer (entity id) and item; entries unused for 10 s are dropped. */
    public static final class Cache {
        public final ClipPlayer clips=new ClipPlayer();
        public final Pose pose=new Pose();
        public final Motion motion=new Motion();
        public float lastBodyYaw=Float.NaN;
        /** Particle accumulators per effect, and of the set bonus. */
        public float[] emitted=new float[0];
        public float setTimer;
        private long seenAt,lastFrame;

        /** Seconds since this entry was last drawn, at most 0.25. */
        public float frame(long now) {
            float dt=lastFrame==0 ? 0 : (now-lastFrame)/1e9f;
            lastFrame=now;
            return Math.max(0,Math.min(dt,.25f));
        }

        private static final Map<Long,Cache> ENTRIES=new HashMap<>();
        private static long prunedAt;
        public static Cache of(int entityId,String key,boolean preview) {
            long now=System.nanoTime();
            if(now-prunedAt>=5_000_000_000L) {
                prunedAt=now;
                ENTRIES.values().removeIf(e->now-e.seenAt>10_000_000_000L);
            }
            long id=(long)entityId<<32^key.hashCode()&0xffffffffL^(preview ? 0x5a5a5a5a00000000L : 0);
            Cache c=ENTRIES.computeIfAbsent(id,k->new Cache());
            c.seenAt=now;
            return c;
        }
        public static void clear() { ENTRIES.clear(); }
    }
}
