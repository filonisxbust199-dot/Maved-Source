package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** CreamyKeys, ported from dimasik: mechanical switch sounds for key and mouse presses (assets/dimasik/sounds/keyboards). */
public final class CreamyKeys {
    private static final String[] SWITCHES={"Blue ABS","Blue ABS 2","Black ABS","Black PBT","Brown ABS","Brown PBT","Red ABS"};
    public static final ModeSettings KEYBOARD=new ModeSettings("Клавиатура",SWITCHES);
    public static final ModeSettings MOUSE=new ModeSettings("Мышь",SWITCHES);
    public static final BooleanSetting ALL_KEYS=new BooleanSetting("Все клавиши",true);
    public static final BooleanSetting MOUSE_SOUNDS=new BooleanSetting("Звуки мыши",true);
    public static final SliderSetting VOLUME=new SliderSetting("Громкость",0,1,.05f,1);
    public static final SliderSetting BOOST=new SliderSetting("Усиление",1,8,1,1);
    public static final Setting[] SETTINGS={KEYBOARD,MOUSE,ALL_KEYS,MOUSE_SOUNDS,VOLUME,BOOST};
    /** GLFW_PRESS; repeats and releases stay silent. */
    private static final int PRESS=1;
    static {
        KEYBOARD.select("Black PBT");
        MOUSE.select("Blue ABS 2");
    }
    private CreamyKeys() {}

    public static boolean enabled() { return ClientModules.CREAMY_KEYS!=null && ClientModules.CREAMY_KEYS.enabled(); }

    public static void onKey(int key,int action) {
        if(action==PRESS && key!=-1 && enabled() && ALL_KEYS.get()) play(switchName(KEYBOARD));
    }
    public static void onMouse(int button,int action) {
        if(action==PRESS && (button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) && enabled() && MOUSE_SOUNDS.get()) play(switchName(MOUSE));
    }

    private static String switchName(ModeSettings setting) {
        return "cherrymx_"+setting.get().toLowerCase().replace(' ','_');
    }

    private static void play(String name) {
        var sounds=Minecraft.getInstance().getSoundManager();
        if(sounds==null) return;
        Identifier id=Identifier.fromNamespaceAndPath("dimasik",String.format("keyboards.%s.key_%02d",name,ThreadLocalRandom.current().nextInt(1,41)));
        float volume=Math.clamp(VOLUME.get(),0,1);
        for(int i=0,n=Math.max(1,Math.round(BOOST.get()));i<n;i++)
            sounds.play(new SimpleSoundInstance(id,SoundSource.MASTER,volume,1,RandomSource.create(),false,0,Attenuation.NONE,0,0,0,true));
    }
}
