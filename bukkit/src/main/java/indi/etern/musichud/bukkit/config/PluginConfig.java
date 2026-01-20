package indi.etern.musichud.bukkit.config;

import indi.etern.musichud.bukkit.MusicHudPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Plugin configuration wrapper.
 */
public class PluginConfig {
    private final MusicHudPlugin plugin;
    
    // API settings
    private String apiBaseUrl;
    private int apiTimeout;
    
    // Playback settings
    private int playbackInterval;
    private boolean idlePlaylistEnabled;
    
    // Vote skip settings
    private boolean voteSkipEnabled;
    private double voteSkipRatio;
    private int voteSkipMinVotes;
    
    // Debug settings
    private boolean debugEnabled;
    
    public PluginConfig(MusicHudPlugin plugin) {
        this.plugin = plugin;
        reload();
    }
    
    /**
     * Reload configuration from file.
     */
    public void reload() {
        FileConfiguration config = plugin.getConfig();
        
        // API settings
        apiBaseUrl = config.getString("api.base-url", "http://localhost:3000");
        apiTimeout = config.getInt("api.timeout", 10000);
        
        // Playback settings
        playbackInterval = config.getInt("playback.interval", 1000);
        idlePlaylistEnabled = config.getBoolean("playback.enable-idle-playlist", true);
        
        // Vote skip settings
        voteSkipEnabled = config.getBoolean("vote-skip.enabled", true);
        voteSkipRatio = config.getDouble("vote-skip.required-ratio", 0.5);
        voteSkipMinVotes = config.getInt("vote-skip.min-votes", 1);
        
        // Debug settings
        debugEnabled = config.getBoolean("debug.enabled", false);
        
        plugin.getLogger().info("Configuration loaded");
    }
    
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    public int getApiTimeout() {
        return apiTimeout;
    }
    
    public int getPlaybackInterval() {
        return playbackInterval;
    }
    
    public boolean isIdlePlaylistEnabled() {
        return idlePlaylistEnabled;
    }
    
    public boolean isVoteSkipEnabled() {
        return voteSkipEnabled;
    }
    
    public double getVoteSkipRatio() {
        return voteSkipRatio;
    }
    
    public int getVoteSkipMinVotes() {
        return voteSkipMinVotes;
    }
    
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}
