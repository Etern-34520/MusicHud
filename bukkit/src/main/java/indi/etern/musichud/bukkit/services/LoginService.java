package indi.etern.musichud.bukkit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import indi.etern.musichud.bukkit.MusicHudPlugin;
import indi.etern.musichud.bukkit.beans.login.LoginCookieInfo;
import indi.etern.musichud.bukkit.beans.login.LoginType;
import indi.etern.musichud.bukkit.beans.user.Profile;
import indi.etern.musichud.bukkit.http.ApiClient;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing player login states.
 */
public class LoginService implements Listener {
    private final MusicHudPlugin plugin;
    private final Map<Player, Runnable> qrPollingMap = new ConcurrentHashMap<>();
    
    // Players who have connected but not logged in
    private final Set<Player> unloggedPlayers = ConcurrentHashMap.newKeySet();
    
    // Players who are fully logged in
    private final Map<Player, PlayerLoginInfo> loggedInPlayers = new ConcurrentHashMap<>();
    
    public LoginService(MusicHudPlugin plugin) {
        this.plugin = plugin;
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Mark a player as connected but not logged in.
     */
    public void joinUnlogged(Player player) {
        unloggedPlayers.add(player);
        plugin.logDebug("Player %s joined as unlogged", player.getName());
    }
    
    /**
     * Handle anonymous login request.
     */
    public void anonymousLogin(Player player) {
        plugin.getExecutor().execute(() -> {
            try {
                String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
                int timeout = plugin.getPluginConfig().getApiTimeout();
                JsonNode response = ApiClient.post(baseUrl, "/register/anonimous", null, null, null, timeout);
                String cookie = response.path("cookie").asText("");
                if (cookie.isEmpty()) {
                    plugin.getChannelHandler().sendLoginResult(player, false, "匿名登录失败", PlayerLoginInfo.unlogged());
                    return;
                }
                LoginCookieInfo loginCookieInfo = new LoginCookieInfo(LoginType.ANONYMOUS, cookie, ZonedDateTime.now());
                Profile profile = loadProfile(baseUrl, timeout, cookie);
                PlayerLoginInfo loginInfo = new PlayerLoginInfo(loginCookieInfo, profile, true);
                loggedInPlayers.put(player, loginInfo);
                unloggedPlayers.remove(player);
                plugin.getChannelHandler().sendLoginResult(player, true, "匿名登录成功", loginInfo);
                plugin.getLogger().info(player.getName() + " logged in anonymously");
            } catch (Exception e) {
                plugin.getChannelHandler().sendLoginResult(player, false, "匿名登录失败", PlayerLoginInfo.unlogged());
                plugin.getLogger().warning("Anonymous login failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handle cookie login request.
     */
    public void cookieLogin(Player player, LoginCookieInfo loginCookieInfo, boolean tryRefresh) {
        plugin.getExecutor().execute(() -> {
            try {
                String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
                int timeout = plugin.getPluginConfig().getApiTimeout();
                String cookie = loginCookieInfo.getRawCookie();
                if (tryRefresh) {
                    JsonNode refresh = ApiClient.post(baseUrl, "/login/refresh", null, null, cookie, timeout);
                    String refreshed = refresh.path("cookie").asText("");
                    if (!refreshed.isEmpty()) {
                        cookie = refreshed;
                    }
                }
                LoginCookieInfo effective = new LoginCookieInfo(loginCookieInfo.getType(), cookie, ZonedDateTime.now());
                Profile profile = loadProfile(baseUrl, timeout, cookie);
                PlayerLoginInfo info = new PlayerLoginInfo(effective, profile, effective.getType() == LoginType.ANONYMOUS);
                loggedInPlayers.put(player, info);
                unloggedPlayers.remove(player);
                plugin.getChannelHandler().sendLoginResult(player, true, "登录成功", info);
                plugin.getLogger().info(player.getName() + " logged in with cookie");
            } catch (Exception e) {
                plugin.getChannelHandler().sendLoginResult(player, false, "登录失败", PlayerLoginInfo.unlogged());
                plugin.getLogger().warning("Cookie login failed: " + e.getMessage());
            }
        });
    }

    /**
     * Handle QR login request.
     *
     * Currently not implemented for Bukkit. Send a failure result so the client UI can reset.
     */
    public void startQrLogin(Player player) {
        plugin.getExecutor().execute(() -> {
            try {
                String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
                int timeout = plugin.getPluginConfig().getApiTimeout();
                JsonNode keyResp = ApiClient.get(baseUrl, "/login/qr/key", Map.of("timestamp", String.valueOf(System.currentTimeMillis())), null, timeout);
                String key = keyResp.path("data").path("unikey").asText("");
                if (key.isEmpty()) {
                    plugin.getChannelHandler().sendLoginResult(player, false, "获取二维码失败", PlayerLoginInfo.unlogged());
                    return;
                }
                ObjectNode body = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
                body.put("key", key);
                body.put("qrimg", true);
                JsonNode qrResp = ApiClient.post(baseUrl, "/login/qr/create", Map.of("timestamp", String.valueOf(System.currentTimeMillis())), body, null, timeout);
                String qrImg = qrResp.path("data").path("qrimg").asText("");
                if (!qrImg.isEmpty()) {
                    plugin.getChannelHandler().sendStartQrLoginResponse(player, qrImg);
                }
                startQrPolling(player, key);
            } catch (Exception e) {
                plugin.getChannelHandler().sendLoginResult(player, false, "获取二维码失败", PlayerLoginInfo.unlogged());
                plugin.getLogger().warning("QR login start failed: " + e.getMessage());
            }
        });
    }

    /**
     * Handle QR login cancellation.
     */
    public void cancelQrLogin(Player player) {
        plugin.logDebug("QR login canceled for %s", player.getName());
        qrPollingMap.remove(player);
    }

    private void startQrPolling(Player player, String key) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
                    int timeout = plugin.getPluginConfig().getApiTimeout();
                    while (qrPollingMap.get(player) == this) {
                        Thread.sleep(5000);
                        ObjectNode body = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
                        body.put("key", key);
                        JsonNode check = ApiClient.post(baseUrl, "/login/qr/check", Map.of("timestamp", String.valueOf(System.currentTimeMillis())), body, null, timeout);
                        int code = check.path("code").asInt(0);
                        if (code == 803) {
                            String cookie = check.path("cookie").asText("");
                            if (!cookie.isEmpty()) {
                                completeLogin(player, cookie, LoginType.QR_CODE);
                            } else {
                                plugin.getChannelHandler().sendLoginResult(player, false, "二维码登录失败", PlayerLoginInfo.unlogged());
                            }
                            break;
                        } else if (code == 800) {
                            plugin.getChannelHandler().sendLoginResult(player, false, "二维码已过期", PlayerLoginInfo.unlogged());
                            break;
                        }
                    }
                } catch (InterruptedException ignored) {
                } catch (Exception e) {
                    plugin.getChannelHandler().sendLoginResult(player, false, "二维码登录失败", PlayerLoginInfo.unlogged());
                    plugin.getLogger().warning("QR polling failed: " + e.getMessage());
                } finally {
                    qrPollingMap.remove(player);
                }
            }
        };
        qrPollingMap.put(player, task);
        plugin.getExecutor().execute(task);
    }

    private void completeLogin(Player player, String cookie, LoginType type) {
        String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
        int timeout = plugin.getPluginConfig().getApiTimeout();
        Profile profile = loadProfile(baseUrl, timeout, cookie);
        LoginCookieInfo loginCookieInfo = new LoginCookieInfo(type, cookie, ZonedDateTime.now());
        PlayerLoginInfo info = new PlayerLoginInfo(loginCookieInfo, profile, type == LoginType.ANONYMOUS);
        loggedInPlayers.put(player, info);
        unloggedPlayers.remove(player);
        plugin.getChannelHandler().sendLoginResult(player, true, "", info);
    }

    private Profile loadProfile(String baseUrl, int timeout, String cookie) {
        try {
            JsonNode account = ApiClient.get(baseUrl, "/user/account", null, cookie, timeout);
            JsonNode profileNode = account.path("profile");
            if (!profileNode.isMissingNode() && !profileNode.isNull()) {
                String nickname = profileNode.path("nickname").asText("anonymous");
                String avatarUrl = profileNode.path("avatarUrl").asText("");
                String backgroundUrl = profileNode.path("backgroundUrl").asText("");
                long userId = profileNode.path("userId").asLong(0L);
                return new Profile(nickname, avatarUrl, backgroundUrl, userId);
            }
            boolean anonymous = account.path("account").path("anonimousUser").asBoolean(false);
            return anonymous ? Profile.ANONYMOUS : new Profile("anonymous", "", "", 0L);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load profile: " + e.getMessage());
            return Profile.ANONYMOUS;
        }
    }
    
    /**
     * Handle player logout.
     */
    public void logout(Player player) {
        loggedInPlayers.remove(player);
        unloggedPlayers.remove(player);
        
        plugin.getMusicPlayerService().onPlayerDisconnect(player);
        
        plugin.logDebug("Player %s logged out", player.getName());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        logout(player);
    }
    
    /**
     * Get login info for a player.
     */
    public PlayerLoginInfo getLoginInfo(Player player) {
        return loggedInPlayers.get(player);
    }
    
    /**
     * Check if a player is connected (logged in or unlogged).
     */
    public boolean isConnected(Player player) {
        return unloggedPlayers.contains(player) || loggedInPlayers.containsKey(player);
    }
    
    /**
     * Check if a player is logged in.
     */
    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.containsKey(player);
    }
    
    /**
     * Get all connected players (both logged in and unlogged).
     */
    public Set<Player> getConnectedPlayers() {
        Set<Player> all = ConcurrentHashMap.newKeySet();
        all.addAll(unloggedPlayers);
        all.addAll(loggedInPlayers.keySet());
        return all;
    }
    
    /**
     * Get all logged in players.
     */
    public Set<Player> getLoggedInPlayers() {
        return loggedInPlayers.keySet();
    }
    
    /**
     * Player login information.
     */
    public static class PlayerLoginInfo {
        private final LoginCookieInfo loginCookieInfo;
        private final Profile profile;
        private final boolean anonymous;
        public PlayerLoginInfo(LoginCookieInfo loginCookieInfo, Profile profile, boolean anonymous) {
            this.loginCookieInfo = loginCookieInfo;
            this.profile = profile;
            this.anonymous = anonymous;
        }

        public static PlayerLoginInfo unlogged() {
            return new PlayerLoginInfo(new LoginCookieInfo(LoginType.UNLOGGED, "", ZonedDateTime.now()), Profile.ANONYMOUS, true);
        }

        public LoginCookieInfo getLoginCookieInfo() {
            return loginCookieInfo;
        }

        public Profile getProfile() {
            return profile;
        }

        public boolean isAnonymous() {
            return anonymous;
        }
    }
}
