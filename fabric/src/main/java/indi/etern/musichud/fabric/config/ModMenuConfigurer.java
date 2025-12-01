package indi.etern.musichud.fabric.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.client.ui.screen.MainFragment;

public class ModMenuConfigurer implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            var fragment = new MainFragment();
            fragment.setDefaultSelectedIndex(3);//Setting page
            return MuiModApi.get().createScreen(fragment, null, parent);
        };
    }
}
