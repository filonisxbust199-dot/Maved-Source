package ess.ll.client.command;

import java.util.*;

/** Small quoted-argument parser shared by chat commands and macro commands. */
public final class CommandLine {
    private CommandLine() {}
    public static List<String> parse(String input) {
        List<String> args=new ArrayList<>(); StringBuilder token=new StringBuilder();
        boolean quote=false,started=false;
        for(int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if(c=='\\' && i+1<input.length() && (input.charAt(i+1)=='"' || input.charAt(i+1)=='\\')) { token.append(input.charAt(++i)); started=true; }
            else if(c=='"') { quote=!quote; started=true; }
            else if(Character.isWhitespace(c) && !quote) {
                if(started) { args.add(token.toString()); token.setLength(0); started=false; }
            } else { token.append(c); started=true; }
        }
        if(quote) throw new IllegalArgumentException("Закройте кавычки");
        if(started) args.add(token.toString());
        return args;
    }
}
