package indi.etern.musichud.neoforge.config;

import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.client.ui.screen.MainFragment;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

public class ConfigScreenFactory implements IConfigScreenFactory {
    @Override
    @NotNull
    public Screen createScreen(@NotNull ModContainer container, @NotNull Screen modListScreen) {
        var fragment = new MainFragment();
        fragment.setDefaultSelectedIndex(3); // Setting page
        return MuiModApi.get().createScreen(fragment, null, modListScreen);
    }
}
