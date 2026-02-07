package indi.etern.musichud.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.ui.screen.MainFragment;
import indi.etern.musichud.interfaces.ClientRegister;
import indi.etern.musichud.interfaces.RegisterMark;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

@RegisterMark
public class Keybinds implements ClientRegister {
    public void register() {
        var mapping = new KeyMapping(
                MusicHud.MOD_ID + ".open_main",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(MusicHud.MOD_ID,MusicHud.MOD_ID))
        );
        KeyMappingRegistry.register(mapping);
        ClientTickEvent.CLIENT_POST.register(instance -> {
            while (mapping.consumeClick()) {
                Minecraft.getInstance().setScreen(MuiModApi.get().createScreen(new MainFragment()));
            }
        });
    }
}