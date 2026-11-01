package ess.ll.client.mixin;

import ess.ll.client.screen.MainMenuScreen;
import ess.ll.client.screen.PauseMenuScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every request for the vanilla title screen opens the New Year main menu instead. */
@Mixin(Gui.class)
public abstract class MainMenuMixin {
    @ModifyVariable(method="setScreen",at=@At("HEAD"),argsOnly=true)
    private Screen ess$mainMenu(Screen screen) {
        if(screen instanceof CreateWorldScreen create) {
            // The menu asked for a new world: it shows its own form over this screen's settings.
            var menu=MainMenuScreen.claimCreateWorld(create);
            if(menu!=null) return menu;
        }
        // Esc in game opens the ESS pause menu; F3+Esc (no menu) stays vanilla.
        if(screen instanceof PauseScreen pause && screen.getClass()==PauseScreen.class && pause.showsPauseMenu()) return new PauseMenuScreen();
        return screen!=null && screen.getClass()==TitleScreen.class ? new MainMenuScreen() : screen;
    }
}
