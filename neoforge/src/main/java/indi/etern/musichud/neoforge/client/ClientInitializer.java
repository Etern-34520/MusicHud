package indi.etern.musichud.neoforge.client;

import indi.etern.musichud.client.ui.hud.HudRendererManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientInitializer {
    private static HudRendererManager hudRendererManager;

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent.Loading configEvent) {
        hudRendererManager = HudRendererManager.getInstance();
        NeoForge.EVENT_BUS.addListener(ClientInitializer::onRenderGui);
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Pre event) {
        hudRendererManager.renderFrame(event.getGuiGraphics(), event.getPartialTick());
    }

    public void onInitializeClient() {
        NeoForge.EVENT_BUS.addListener(ClientInitializer::onConfigLoad);
    }
}
