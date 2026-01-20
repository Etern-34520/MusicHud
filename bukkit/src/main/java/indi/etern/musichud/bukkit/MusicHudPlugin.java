package indi.etern.musichud.bukkit;

import indi.etern.musichud.bukkit.commands.MusicHudCommand;
import indi.etern.musichud.bukkit.config.PluginConfig;
import indi.etern.musichud.bukkit.network.ChannelHandler;
import indi.etern.musichud.bukkit.services.LoginService;
import indi.etern.musichud.bukkit.services.MusicPlayerService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class MusicHudPlugin extends JavaPlugin {
    public static final String MOD_ID = "music_hud";
    private static MusicHudPlugin instance;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    private PluginConfig pluginConfig;
    private ChannelHandler channelHandler;
    private MusicPlayerService musicPlayerService;
    private LoginService loginService;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize configuration
        pluginConfig = new PluginConfig(this);
        
        // Initialize services
        loginService = new LoginService(this);
        musicPlayerService = new MusicPlayerService(this);
        
        // Register plugin messaging channels
        channelHandler = new ChannelHandler(this);
        channelHandler.register();
        
        // Register commands
        MusicHudCommand command = new MusicHudCommand(this);
        getCommand("musichud").setExecutor(command);
        getCommand("musichud").setTabCompleter(command);
        
        getLogger().info("MusicHud plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // Stop music playback
        if (musicPlayerService != null) {
            musicPlayerService.shutdown();
        }
        
        // Unregister channels
        if (channelHandler != null) {
            channelHandler.unregister();
        }
        
        // Shutdown executor
        executor.shutdown();
        
        getLogger().info("MusicHud plugin disabled!");
    }
    
    public static MusicHudPlugin getInstance() {
        return instance;
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }
    
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
    
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }
    
    public MusicPlayerService getMusicPlayerService() {
        return musicPlayerService;
    }
    
    public LoginService getLoginService() {
        return loginService;
    }
    
    public void logDebug(String message) {
        if (pluginConfig.isDebugEnabled()) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    public void logDebug(String message, Object... args) {
        if (pluginConfig.isDebugEnabled()) {
            getLogger().log(Level.INFO, "[DEBUG] " + String.format(message, args));
        }
    }
}
