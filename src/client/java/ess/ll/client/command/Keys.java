package ess.ll.client.command;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.module.ClientModules;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Bind codes: keyboard keys use their InputConstants code, mouse buttons are MOUSE_BASE + button
 * (MOUSE1 = left ... MOUSE8). Toggle binds fire on press; hold binds follow the key state.
 */
public final class Keys {
    public static final int MOUSE_BASE=1000, MOUSE_BUTTONS=8;
    private static final Map<String,Integer> NAMES=new TreeMap<>();
    private static final Set<Integer> HELD=new HashSet<>();
    private static final boolean[] MOUSE=new boolean[MOUSE_BUTTONS];
    static {
        for(var field:InputConstants.class.getFields()) if(field.getName().startsWith("KEY_") && field.getType()==int.class && Modifier.isStatic(field.getModifiers())) {
            try { NAMES.put(field.getName().substring(4),field.getInt(null)); } catch(IllegalAccessException ignored) {}
        }
        NAMES.put("RSHIFT",InputConstants.KEY_RSHIFT); NAMES.put("LSHIFT",InputConstants.KEY_LSHIFT);
        for(int i=0;i<MOUSE_BUTTONS;i++) NAMES.put("MOUSE"+(i+1),MOUSE_BASE+i);
    }
    private Keys() {}
    public static int parse(String name) {
        String normalized=name.toUpperCase(Locale.ROOT);
        if(normalized.equals("NONE")) return -1;
        Integer value=NAMES.get(normalized);
        if(value==null) throw new IllegalArgumentException("Неизвестная клавиша: "+name+" (пример: R, F6, RSHIFT, MOUSE4, NONE)");
        return value;
    }
    public static boolean isValid(int key) { return NAMES.containsValue(key); }
    public static String name(int key) { return NAMES.entrySet().stream().filter(e->e.getValue()==key).map(Map.Entry::getKey).findFirst().orElse("NONE"); }
    /** Short label for UI pills: "Нет" when unbound. */
    public static String label(int key) { return key<0 ? "Нет" : name(key); }
    /** Fed from MouseHandler for every button, so side buttons can be bound. */
    public static void mouse(int button,boolean down) { if(button>=0 && button<MOUSE_BUTTONS) MOUSE[button]=down; }
    public static boolean isDown(int code) {
        if(code>=MOUSE_BASE) return code-MOUSE_BASE<MOUSE_BUTTONS && MOUSE[code-MOUSE_BASE];
        return code>=0 && InputConstants.isKeyDown(code);
    }
    public static void tick(Minecraft client) {
        Set<Integer> watched=new HashSet<>(),pressed=new HashSet<>();
        var macros=ClientData.macros();
        for(var module:ClientModules.all()) if(module.bind>=0) watched.add(module.bind);
        for(var macro:macros) watched.add(macro.key());
        for(int key:watched) {
            boolean down=isDown(key);
            if(down && !HELD.contains(key)) pressed.add(key);
            if(down) HELD.add(key); else HELD.remove(key);
        }
        HELD.retainAll(watched);
        boolean inGame=client.player!=null && client.level!=null && client.gui.screen()==null && client.isWindowActive();
        for(var module:ClientModules.all()) {
            if(module.bind<0) continue;
            if(module.hold) {
                // Hold binds release as soon as a screen opens or focus is lost.
                boolean want=inGame && HELD.contains(module.bind);
                if(module.enabled()!=want) module.setEnabled(want);
            } else if(inGame && pressed.contains(module.bind)) module.toggle();
        }
        if(!inGame) return;
        for(var macro:macros) if(pressed.contains(macro.key())) {
            String text=macro.text();
            if(text.startsWith(".")) DotCommands.execute(text);
            else if(text.startsWith("/")) client.player.connection.sendCommand(text.substring(1));
            else client.player.connection.sendChat(text);
        }
    }
}
