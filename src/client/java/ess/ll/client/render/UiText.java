package ess.ll.client.render;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Number and time strings for UI drawn every frame, without String.format: the HUD redraws coordinates, speed,
 * timers and the clock 200+ times a second, and formatting showed up in its frame cost. Results that only change
 * now and then (the clock, upper-cased labels) are kept.
 */
public final class UiText {
    private static final Map<String,String> UPPER=new HashMap<>();
    private static int clockMinute=-1;
    private static String clock="";

    private UiText() {}

    /** One decimal, like "%.1f" in the root locale. */
    public static String oneDecimal(double value) {
        long tenths=Math.round(value*10);
        StringBuilder out=new StringBuilder(8);
        if(tenths<0) { out.append('-'); tenths=-tenths; }
        return out.append(tenths/10).append('.').append(tenths%10).toString();
    }
    /** m:ss, or h:mm:ss from an hour. */
    public static String duration(long seconds) {
        StringBuilder out=new StringBuilder(8);
        if(seconds>=3600) { out.append(seconds/3600).append(':'); two(out,seconds/60%60); }
        else out.append(seconds/60);
        out.append(':');
        two(out,seconds%60);
        return out.toString();
    }
    /** HH:mm, rebuilt once a minute. */
    public static String clock() {
        LocalTime now=LocalTime.now();
        int minute=now.getHour()*60+now.getMinute();
        if(minute!=clockMinute) {
            clockMinute=minute;
            StringBuilder out=new StringBuilder(5);
            two(out,now.getHour()); out.append(':'); two(out,now.getMinute());
            clock=out.toString();
        }
        return clock;
    }
    /** Upper case in the root locale, kept per label. */
    public static String upper(String text) {
        String cached=UPPER.get(text);
        if(cached==null) {
            if(UPPER.size()>256) UPPER.clear();
            cached=text.toUpperCase(Locale.ROOT);
            UPPER.put(text,cached);
        }
        return cached;
    }
    private static void two(StringBuilder out,long value) {
        if(value<10) out.append('0');
        out.append(value);
    }
}
