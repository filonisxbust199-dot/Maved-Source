package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * StreamerMode: hides what gives you away on stream. Every text drawn by the game and by the client goes through
 * here: your nick becomes "maved", the grief number on the scoreboard ("ГРИФ #12") becomes "@maveddlc" and the clan
 * ("Клан: XXXX") becomes "Клан: mavedownerr". Replaced text keeps the colour of the text it replaces.
 */
public final class StreamerMode {
    public static final MultiBooleanSettings HIDE=new MultiBooleanSettings("Скрывать","Ник","Гриф","Клан").none();
    public static final Setting[] SETTINGS={HIDE};
    static { HIDE.toggle(0); HIDE.toggle(1); HIDE.toggle(2); }

    private static final String NICK="maved",GRIEF="@maveddlc",CLAN="mavedownerr";
    private static final Pattern GRIEF_PATTERN=Pattern.compile("(?iu)гриф\\s*#\\s*\\d+");
    private static final Pattern CLAN_PATTERN=Pattern.compile("(?iu)(клан\\s*:\\s*)(?:§.)*([^\\s§]+)");
    private StreamerMode() {}

    public static boolean enabled() { return ClientModules.STREAMER_MODE!=null && ClientModules.STREAMER_MODE.enabled(); }

    private static String nick() {
        var user=Minecraft.getInstance().getUser();
        String name=user==null ? null : user.getName();
        return name==null || name.length()<3 || name.equals(NICK) ? null : name;
    }
    /** Cheap test before any regex: most text has nothing to hide. */
    private static boolean candidate(String text,String nick) {
        if(text.isEmpty()) return false;
        if(nick!=null && HIDE.selected("Ник") && text.contains(nick)) return true;
        String lower=text.toLowerCase(Locale.ROOT);
        return HIDE.selected("Гриф") && lower.contains("гриф") || HIDE.selected("Клан") && lower.contains("клан");
    }

    /** One replacement: [start, end) of the text becomes value. */
    private record Cut(int start,int end,String value) {}
    private static List<Cut> cuts(String text,String nick) {
        List<Cut> out=new ArrayList<>();
        if(nick!=null && HIDE.selected("Ник")) for(int i=text.indexOf(nick);i>=0;i=text.indexOf(nick,i+nick.length())) out.add(new Cut(i,i+nick.length(),NICK));
        if(HIDE.selected("Гриф")) for(Matcher m=GRIEF_PATTERN.matcher(text);m.find();) out.add(new Cut(m.start(),m.end(),GRIEF));
        if(HIDE.selected("Клан")) for(Matcher m=CLAN_PATTERN.matcher(text);m.find();) out.add(new Cut(m.start(2),m.end(2),CLAN));
        // Overlapping matches: the earlier one wins.
        out.sort((a,b)->Integer.compare(a.start,b.start));
        List<Cut> kept=new ArrayList<>();
        int end=-1;
        for(Cut c:out) if(c.start>=end) { kept.add(c); end=c.end; }
        return kept;
    }

    public static String apply(String text) {
        if(text==null || !enabled()) return text;
        String nick=nick();
        if(!candidate(text,nick)) return text;
        List<Cut> cuts=cuts(text,nick);
        if(cuts.isEmpty()) return text;
        StringBuilder out=new StringBuilder(text.length());
        int at=0;
        for(Cut c:cuts) { out.append(text,at,c.start).append(c.value); at=c.end; }
        return out.append(text,at,text.length()).toString();
    }

    /** Styled text (chat, scoreboard, tab, name tags): the text is matched as a whole, across its coloured pieces. */
    public static FormattedCharSequence apply(FormattedCharSequence sequence) {
        if(sequence==null || !enabled()) return sequence;
        StringBuilder text=new StringBuilder();
        List<Style> styles=new ArrayList<>();
        sequence.accept((index,style,code)->{
            text.appendCodePoint(code);
            for(int k=Character.charCount(code);k>0;k--) styles.add(style);
            return true;
        });
        String plain=text.toString(),nick=nick();
        if(!candidate(plain,nick)) return sequence;
        List<Cut> cuts=cuts(plain,nick);
        if(cuts.isEmpty()) return sequence;
        StringBuilder out=new StringBuilder(plain.length());
        List<Style> outStyles=new ArrayList<>(styles.size());
        int at=0;
        for(Cut c:cuts) {
            out.append(plain,at,c.start); outStyles.addAll(styles.subList(at,c.start));
            Style style=styles.get(c.start);
            out.append(c.value); for(int k=0;k<c.value.length();k++) outStyles.add(style);
            at=c.end;
        }
        out.append(plain,at,plain.length()); outStyles.addAll(styles.subList(at,plain.length()));
        String result=out.toString();
        return sink->{
            for(int i=0,n=0;i<result.length();n++) {
                int code=result.codePointAt(i);
                if(!sink.accept(n,outStyles.get(i),code)) return false;
                i+=Character.charCount(code);
            }
            return true;
        };
    }
}
