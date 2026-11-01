package ess.ll.client.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import java.util.*;

/** Completion ranges refer to the original chat text, including the dot and quoted arguments. */
public final class DotSuggestions {
    private DotSuggestions() {}
    public static Suggestions complete(String text,int cursor) {
        String prefix=text.substring(0,Math.clamp(cursor,1,text.length()));
        int start=1; boolean quoted=false,escaped=false;
        for(int i=1;i<prefix.length();i++) {
            char c=prefix.charAt(i);
            if(escaped) { escaped=false; continue; }
            if(c=='\\') { escaped=true; continue; }
            if(c=='"') quoted=!quoted;
            if(Character.isWhitespace(c) && !quoted) start=i+1;
        }
        List<String> args=CommandLine.parse(prefix.substring(1,start));
        String token=prefix.substring(start), match=token.startsWith("\"")?token.substring(1):token;
        List<String> values=new ArrayList<>();
        if(args.isEmpty()) values.addAll(List.of("help","modules","toggle","bind","config","friend","macro","set","blockesp","way","emote","search","sort","tg","fakeplayer","tikva"));
        else {
            String cmd=args.getFirst().toLowerCase(Locale.ROOT);
            int index=args.size();
            if(Set.of("toggle","t","bind","set").contains(cmd) && index==1)
                ClientModules.all().forEach(m->values.add(m.name));
            else if(cmd.equals("bind") && index==2) values.addAll(List.of("NONE","RSHIFT","LSHIFT","LALT","RALT","SPACE","MOUSE3","MOUSE4","MOUSE5","A","B","C","F","G","H","R","V","X","Z"));
            else if(cmd.equals("bind") && index==3) values.addAll(List.of("toggle","hold"));
            else if(Set.of("config","cfg").contains(cmd)) {
                if(index==1) values.addAll(List.of("save","load","delete","list"));
                else if(index==2) try { values.addAll(ClientData.profiles()); } catch(java.io.IOException ignored) {}
            } else if(Set.of("friend","friends").contains(cmd)) {
                if(index==1) values.addAll(List.of("add","remove","list","clear"));
                else if(index==2) {
                    if(Set.of("remove","del").contains(args.get(1))) values.addAll(ClientData.friends());
                    else if(args.get(1).equals("add") && Minecraft.getInstance().getConnection()!=null)
                        Minecraft.getInstance().getConnection().getOnlinePlayers().forEach(p->values.add(p.getProfile().name()));
                }
            } else if(cmd.equals("blockesp")) {
                if(index==1)values.addAll(List.of("add","remove","clear","list","on","off"));
                else if(index==2 && args.get(1).equalsIgnoreCase("add")) {
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().forEach(id->{
                        values.add(id.toString());
                        if(id.getNamespace().equals("minecraft"))values.add(id.getPath());
                    });
                } else if(index==2)values.addAll(ess.ll.client.module.render.BlockESP.INSTANCE.entries().keySet());
                else if(index==3 && args.get(1).equalsIgnoreCase("add"))values.addAll(List.of("#FFFFFF","white","black","red","green","blue","cyan","yellow","orange","pink","purple"));
            } else if(Set.of("way","waypoint","wp").contains(cmd)) {
                if(index==1) values.addAll(List.of("add","remove","go","list","clear","off"));
                else if(index==2 && Set.of("remove","del","go","set").contains(args.get(1))) ess.ll.client.features.Waypoints.here().forEach(p->values.add(p.name()));
                else if(Set.of(2,3).contains(index) && args.get(1).equalsIgnoreCase("add")) values.add("~");
            } else if(Set.of("fakeplayer","fake","fp").contains(cmd)) {
                if(index==1) values.addAll(List.of("add","del","list"));
                else if(index==2 && Set.of("del","remove","rm").contains(args.get(1))) { values.add("all"); values.addAll(ess.ll.client.features.FakePlayers.names()); }
                else if(index==2 && Set.of("add","spawn").contains(args.get(1))) values.add("FakePlayer");
            } else if(Set.of("tikva","rwtikva").contains(cmd)) {
                if(index==1) values.addAll(List.of("home","start","stop"));
            } else if(Set.of("sort","sorter").contains(cmd)) {
                if(index==1) values.addAll(List.of("source","add","learn","remove","list","clear","start","stop"));
            } else if(Set.of("tg","telegram").contains(cmd)) {
                if(index==1) values.addAll(List.of("menu","token","start","stop","code","unlink"));
            } else if(Set.of("emote","e").contains(cmd)) {
                if(index==1) { values.addAll(List.of("stop","list")); ess.ll.client.features.emote.Emotes.all().forEach(e->values.add(e.id())); }
            } else if(cmd.equals("macro")) {
                if(index==1) values.addAll(List.of("add","remove","list","clear"));
                else if(index==2 && Set.of("remove","del").contains(args.get(1))) ClientData.macros().forEach(m->values.add(m.name()));
            } else if(cmd.equals("set") && index>=2) {
                try {
                    var module=ClientModules.find(args.get(1));
                    if(index==2) module.settings.forEach(s->values.add(s.name()));
                    else if(index==3) for(var setting:module.settings) if(setting.name().equalsIgnoreCase(args.get(2))) {
                        if(setting instanceof BooleanSetting) values.addAll(List.of("on","off"));
                        else if(setting instanceof ModeSettings mode) values.addAll(mode.options);
                        else if(setting instanceof MultiBooleanSettings multi) { values.add("none"); values.addAll(multi.options); }
                        else if(setting instanceof SliderSetting slider) values.add(Float.toString(slider.get()));
                    }
                } catch(IllegalArgumentException ignored) {}
            }
        }
        SuggestionsBuilder builder=new SuggestionsBuilder(prefix,start);
        String needle=match.toLowerCase(Locale.ROOT);
        for(String value:new TreeSet<>(values)) if(value.toLowerCase(Locale.ROOT).startsWith(needle)) {
            boolean quote=token.startsWith("\"") || value.chars().anyMatch(Character::isWhitespace);
            builder.suggest(quote ? "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"" : value);
        }
        return builder.build();
    }
}
