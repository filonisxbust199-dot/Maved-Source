package ess.ll.client.render;

import ess.ll.client.ui.KeyboardKey;

public class ItemAnimationChecks {
    public static boolean isAnimationKeyPressed(int var0) {
        return var0 != -1 && var0 != 0 && var0 != 67 ? KeyboardKey.isKeyDown(var0) : false;
    }
}
