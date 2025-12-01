package indi.etern.musichud.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.ui.screen.MainFragment;
import indi.etern.musichud.interfaces.ClientRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@ForceLoad
public class Keybinds implements ClientRegister {
    public void register() {
        // 创建快捷键映射
        var mapping = new KeyMapping(
                MusicHud.MOD_ID + ".open_main",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category."+MusicHud.MOD_ID
        );

        // 注册快捷键
        KeyMappingRegistry.register(mapping);

        // 监听按键事件
        ClientTickEvent.CLIENT_POST.register(instance -> {
            while (mapping.consumeClick()) {
                Minecraft.getInstance().setScreen(MuiModApi.get().createScreen(new MainFragment()));
            }
        });
    }
}