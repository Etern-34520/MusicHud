package indi.etern.musichud.neoforge.config;

import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.server.config.ServerConfigDefinition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

public class ConfigEventHandler {
    @SubscribeEvent
    static void onConfigLoad(final ModConfigEvent configEvent) {
        ModConfig config = configEvent.getConfig();
        if (config.getSpec() == ServerConfigDefinition.configure.getRight()) {
            ServerApiMeta.reload();
        }
    }

    @SubscribeEvent
    static void onConfigReload(final ModConfigEvent.Reloading configEvent) {
        // 处理配置重新加载
        onConfigLoad(configEvent);
    }
}
