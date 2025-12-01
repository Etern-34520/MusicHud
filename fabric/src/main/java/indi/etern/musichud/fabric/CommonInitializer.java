package indi.etern.musichud.fabric;

import indi.etern.musichud.MusicHud;
import net.fabricmc.api.ModInitializer;

public final class CommonInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        MusicHud.init();
    }
}
