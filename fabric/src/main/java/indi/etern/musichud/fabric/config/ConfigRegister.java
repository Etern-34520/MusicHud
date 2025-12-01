package indi.etern.musichud.fabric.config;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.server.config.ServerConfigDefinition;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.neoforged.fml.config.ModConfig;

public class ConfigRegister implements DedicatedServerModInitializer, ClientModInitializer {
    @Override
    public void onInitializeServer() {
        ConfigRegistry.INSTANCE.register(MusicHud.MOD_ID, ModConfig.Type.SERVER, ServerConfigDefinition.configure.getRight());

        ModConfigEvents.loading(MusicHud.MOD_ID).register(modConfig -> {
            if (modConfig.getSpec() == ServerConfigDefinition.configure.getRight()) {
                ServerApiMeta.reload();
            }
        });
    }

    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register(MusicHud.MOD_ID, ModConfig.Type.CLIENT, ClientConfigDefinition.configure.getRight());
    }
}