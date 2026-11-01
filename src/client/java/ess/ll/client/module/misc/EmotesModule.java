package ess.ll.client.module.misc;

import com.mojang.blaze3d.platform.InputConstants;
import ess.ll.client.command.Keys;
import ess.ll.client.features.emote.Emotes;
import ess.ll.client.module.ClientModules;
import ess.ll.client.screen.EmoteWheelScreen;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Emotes (from Zenith): holding the wheel key opens the emote wheel (15 pages of 8 slots); releasing it over a
 * slot plays that emote. Empty slots open the picker, right click empties a slot. Auto F5 switches to the third
 * person view for a while so you see yourself dance. Emotes also play with .emote.
 */
public final class EmotesModule {
    public static final KeybindSetting WHEEL=new KeybindSetting("Колесо эмоций",InputConstants.KEY_B);
    public static final BooleanSetting AUTO_F5=new BooleanSetting("Авто F5",false);
    public static final SliderSetting AUTO_F5_TIME=new SliderSetting("Длительность F5 (с)",1,30,.5f,5);
    public static final Setting[] SETTINGS={WHEEL,AUTO_F5,AUTO_F5_TIME};
    static { AUTO_F5_TIME.visibleWhen(AUTO_F5::get); }

    private static boolean held;
    /** When the third person view switched on by Auto F5 goes back to first person; 0 when not switched. */
    private static long backToFirstAt;

    private EmotesModule() {}

    public static boolean enabled() { return ClientModules.EMOTES!=null && ClientModules.EMOTES.enabled(); }

    public static void tick(Minecraft mc) {
        Emotes.tick(mc);
        if(backToFirstAt!=0 && (!AUTO_F5.get() || System.currentTimeMillis()>=backToFirstAt)) {
            if(mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.FIRST_PERSON);
            backToFirstAt=0;
        }
        boolean down=WHEEL.get()>=0 && Keys.isDown(WHEEL.get());
        if(down && !held && enabled() && mc.player!=null && mc.gui.screen()==null && mc.isWindowActive())
            mc.gui.setScreen(new EmoteWheelScreen());
        held=down;
    }

    /** Plays an emote of the wheel, the picker or .emote. */
    public static boolean play(String id) {
        if(!Emotes.play(id)) return false;
        var mc=Minecraft.getInstance();
        if(AUTO_F5.get() && mc.options.getCameraType().isFirstPerson()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            backToFirstAt=System.currentTimeMillis()+Math.round(AUTO_F5_TIME.get()*1000);
        }
        return true;
    }
}
