package ess.ll.client.render;

import ess.ll.client.module.ClientModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Interface sounds, all synthesised by UiSynth. Toggled by ClickGUI → Звуки. */
public final class UiSounds {
    private static final SoundEvent OPEN=event("open"), CLOSE=event("close"), HOVER=event("hover"),
            TOGGLE_ON=event("toggle_on"), TOGGLE_OFF=event("toggle_off"), EXPAND=event("expand"), COLLAPSE=event("collapse"),
            SELECT=event("select"), SLIDER=event("slider"), BIND_LISTEN=event("bind_listen"), BIND_SET=event("bind_set");
    private static long lastHover, lastTick;
    private UiSounds() {}
    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("ess-best","ui."+name));
    }

    public static void open() { play(OPEN,1,.7f); }
    public static void close() { play(CLOSE,1,.55f); }
    /** Soft tick when the cursor enters a module card; rate-limited so sweeping the mouse is not noisy. */
    public static void hover() {
        long now=System.currentTimeMillis();
        if(now-lastHover<45) return;
        lastHover=now;
        play(HOVER,1,.35f);
    }
    public static void toggle(boolean on) { play(on ? TOGGLE_ON : TOGGLE_OFF,1,.6f); }
    public static void expand(boolean open) { play(open ? EXPAND : COLLAPSE,1,.5f); }
    public static void select() { play(SELECT,1,.5f); }
    /** Slider movement: pitch follows the value so dragging "plays" a scale; rate-limited per step. */
    public static void slider(float fraction) {
        long now=System.currentTimeMillis();
        if(now-lastTick<35) return;
        lastTick=now;
        play(SLIDER,.75f+fraction*.9f,.45f);
    }
    public static void bindListen() { play(BIND_LISTEN,1,.5f); }
    public static void bindSet() { play(BIND_SET,1,.55f); }

    private static void play(SoundEvent sound,float pitch,float volume) {
        if(!ClientModules.GUI_SOUNDS.get()) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,volume));
    }
}
