package indi.etern.musichud.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.ui.hud.HudRendererManager;
import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.server.config.ServerConfigDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MusicHud.MOD_ID)
public final class CommonInitializer {

    @SuppressWarnings("unused")
    public CommonInitializer(IEventBus eventBus, ModContainer container) {
        eventBus.register(this);

        // 根据环境注册配置
        if (Platform.getEnvironment() == Env.CLIENT) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfigDefinition.configure.getRight());
            container.registerExtensionPoint(IConfigScreenFactory.class, new ConfigScreenFactory());
        } else {
            // 服务器环境
            container.registerConfig(ModConfig.Type.SERVER, ServerConfigDefinition.configure.getRight());
        }

        // 也可以在这里执行一些初始化
        MusicHud.init();
    }

    public static HudRendererManager hudRendererManager;

    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (hudRendererManager != null) {
            hudRendererManager.renderFrame(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    void onConfigEvent(final ModConfigEvent configEvent) {
        ModConfig config = configEvent.getConfig();
        if (config.getSpec() == ServerConfigDefinition.configure.getRight()) {
            ServerApiMeta.reload();
        } else if (config.getSpec() == ClientConfigDefinition.configure.getRight()) {
            hudRendererManager = HudRendererManager.getInstance();
            NeoForge.EVENT_BUS.addListener(CommonInitializer::onRenderGui);
        }
    }

    @SubscribeEvent
    void onConfigReload(final ModConfigEvent.Reloading configEvent) {
        onConfigEvent(configEvent);
    }
    @SubscribeEvent
    void onConfigLoaded(final ModConfigEvent.Loading configEvent) {
        onConfigEvent(configEvent);
    }
}