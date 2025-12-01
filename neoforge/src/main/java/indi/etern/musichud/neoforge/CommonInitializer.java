package indi.etern.musichud.neoforge;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.neoforge.client.ClientInitializer;
import indi.etern.musichud.neoforge.config.ConfigEventHandler;
import indi.etern.musichud.neoforge.config.ConfigScreenFactory;
import indi.etern.musichud.server.config.ServerConfigDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(MusicHud.MOD_ID)
public final class CommonInitializer {
    public CommonInitializer() {
        MusicHud.init();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClientSetup(FMLClientSetupEvent event, IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfigDefinition.configure.getRight());
        container.registerExtensionPoint(IConfigScreenFactory.class,
                new ConfigScreenFactory());
        new ClientInitializer().onInitializeClient();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerSetup(FMLClientSetupEvent event, IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfigDefinition.configure.getRight());
        modEventBus.register(ConfigEventHandler.class);
    }
}
