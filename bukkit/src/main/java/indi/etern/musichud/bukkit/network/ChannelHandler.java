package indi.etern.musichud.bukkit.network;

import indi.etern.musichud.bukkit.MusicHudPlugin;
import indi.etern.musichud.bukkit.beans.MusicDetail;
import indi.etern.musichud.bukkit.beans.Version;
import io.netty.buffer.ByteBuf;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles plugin messaging channel registration and incoming messages.
 */
public class ChannelHandler implements PluginMessageListener {
    private final MusicHudPlugin plugin;
    private final Set<String> registeredChannels = new HashSet<>();
    
    public ChannelHandler(MusicHudPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register all plugin messaging channels.
     */
    public void register() {
        // Register C2S channels (incoming from client)
        registerIncoming(PacketType.CONNECT_REQUEST);
        registerIncoming(PacketType.CLIENT_PUSH_MUSIC_TO_QUEUE);
        registerIncoming(PacketType.VOTE_SKIP_CURRENT_MUSIC);
        registerIncoming(PacketType.ADD_PLAYLIST_TO_IDLE_PLAY_SOURCE);
        registerIncoming(PacketType.REMOVE_PLAYLIST_FROM_IDLE_PLAY_SOURCE);
        registerIncoming(PacketType.LOGOUT);
        registerIncoming(PacketType.ANONYMOUS_LOGIN_REQUEST);
        registerIncoming(PacketType.COOKIE_LOGIN_REQUEST);
        registerIncoming(PacketType.START_QR_LOGIN_REQUEST);
        registerIncoming(PacketType.CANCEL_QR_LOGIN_REQUEST);
        registerIncoming(PacketType.SEARCH_REQUEST);
        registerIncoming(PacketType.GET_USER_PLAYLIST_REQUEST);
        registerIncoming(PacketType.GET_PLAYLIST_DETAIL_REQUEST);
        registerIncoming(PacketType.CLIENT_REMOVE_MUSIC_FROM_QUEUE);
        
        // Register S2C channels (outgoing to client)
        registerOutgoing(PacketType.CONNECT_RESPONSE);
        registerOutgoing(PacketType.SWITCH_MUSIC);
        registerOutgoing(PacketType.SYNC_CURRENT_PLAYING);
        registerOutgoing(PacketType.REFRESH_MUSIC_QUEUE);
        registerOutgoing(PacketType.LOGIN_RESULT);
        registerOutgoing(PacketType.START_QR_LOGIN_RESPONSE);
        registerOutgoing(PacketType.SEARCH_RESPONSE);
        registerOutgoing(PacketType.GET_USER_PLAYLIST_RESPONSE);
        registerOutgoing(PacketType.GET_PLAYLIST_DETAIL_RESPONSE);
        
        plugin.getLogger().info("Registered " + registeredChannels.size() + " plugin messaging channels");
    }
    
    private void registerIncoming(PacketType type) {
        String channel = type.getFullChannelName();
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        registeredChannels.add(channel);
    }
    
    private void registerOutgoing(PacketType type) {
        String channel = type.getFullChannelName();
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        registeredChannels.add(channel);
    }
    
    /**
     * Unregister all plugin messaging channels.
     */
    public void unregister() {
        for (String channel : registeredChannels) {
            try {
                plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel);
            } catch (Exception ignored) {}
            try {
                plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            } catch (Exception ignored) {}
        }
        registeredChannels.clear();
    }
    
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        PacketType type = PacketType.fromChannelName(channel);
        if (type == null) {
            plugin.logDebug("Received unknown channel: %s", channel);
            return;
        }
        
        plugin.logDebug("Received packet %s from %s", type.name(), player.getName());
        
        // Handle in async thread
        plugin.getExecutor().execute(() -> handlePacket(type, player, message));
    }
    
    private void handlePacket(PacketType type, Player player, byte[] message) {
        try {
            plugin.logDebug("Packet %s from %s: %d bytes, data: %s", 
                    type.name(), player.getName(), message.length, bytesToHex(message));
            ByteBuf buf = PacketCodecs.fromByteArray(message);
            if (PacketCodecs.stripLengthPrefix(buf)) {
                plugin.logDebug("Stripped length prefix for %s from %s", type.name(), player.getName());
            }
            
            switch (type) {
                case CONNECT_REQUEST -> handleConnectRequest(player, buf);
                case CLIENT_PUSH_MUSIC_TO_QUEUE -> handlePushMusicToQueue(player, buf);
                case VOTE_SKIP_CURRENT_MUSIC -> handleVoteSkip(player, buf);
                case ADD_PLAYLIST_TO_IDLE_PLAY_SOURCE -> handleAddPlaylist(player, buf);
                case REMOVE_PLAYLIST_FROM_IDLE_PLAY_SOURCE -> handleRemovePlaylist(player, buf);
                case LOGOUT -> handleLogout(player);
                case ANONYMOUS_LOGIN_REQUEST -> handleAnonymousLogin(player);
                case COOKIE_LOGIN_REQUEST -> handleCookieLogin(player, buf);
                case CLIENT_REMOVE_MUSIC_FROM_QUEUE -> handleRemoveMusicFromQueue(player, buf);
                case START_QR_LOGIN_REQUEST -> handleStartQrLogin(player);
                case CANCEL_QR_LOGIN_REQUEST -> handleCancelQrLogin(player);
                case SEARCH_REQUEST -> handleSearchRequest(player, buf);
                case GET_USER_PLAYLIST_REQUEST -> handleGetUserPlaylistRequest(player);
                case GET_PLAYLIST_DETAIL_REQUEST -> handleGetPlaylistDetailRequest(player, buf);
                // TODO: Implement other handlers as needed
                default -> plugin.logDebug("Unhandled packet type: %s", type.name());
            }
            
            buf.release();
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling packet " + type.name() + " from " + player.getName() + ": " + e.getMessage());
            if (plugin.getPluginConfig().isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    // ==================== Packet Handlers ====================
    
    private void handleConnectRequest(Player player, ByteBuf buf) {
        // Debug: print buffer info
        plugin.logDebug("ConnectRequest buffer: readable=%d, readerIndex=%d", 
                buf.readableBytes(), buf.readerIndex());
        
        Version clientVersion = PacketCodecs.readVersion(buf);
        plugin.logDebug("Player %s connecting with version %s", player.getName(), clientVersion);
        
        boolean capable = Version.capableWith(clientVersion);
        
        // Send response
        sendConnectResponse(player, capable, Version.CURRENT);
        
        if (capable) {
            plugin.getLoginService().joinUnlogged(player);
            plugin.getMusicPlayerService().sendSyncPlayingStatusToPlayer(player);
        }
    }
    
    private void handlePushMusicToQueue(Player player, ByteBuf buf) {
        long musicId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s pushing music %d to queue", player.getName(), musicId);
        plugin.getMusicPlayerService().pushMusicToQueue(musicId, player);
    }
    
    private void handleVoteSkip(Player player, ByteBuf buf) {
        long musicId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s voting to skip music %d", player.getName(), musicId);
        plugin.getMusicPlayerService().voteSkipCurrent(musicId, player);
    }
    
    private void handleAddPlaylist(Player player, ByteBuf buf) {
        long playlistId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s adding playlist %d to idle sources", player.getName(), playlistId);
        plugin.getMusicPlayerService().addIdlePlaySource(playlistId, player);
    }
    
    private void handleRemovePlaylist(Player player, ByteBuf buf) {
        long playlistId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s removing playlist %d from idle sources", player.getName(), playlistId);
        plugin.getMusicPlayerService().removeIdlePlaySource(playlistId, player);
    }
    
    private void handleLogout(Player player) {
        plugin.logDebug("Player %s logging out", player.getName());
        plugin.getLoginService().logout(player);
    }
    
    private void handleAnonymousLogin(Player player) {
        plugin.logDebug("Player %s requesting anonymous login", player.getName());
        plugin.getLoginService().anonymousLogin(player);
    }
    
    private void handleCookieLogin(Player player, ByteBuf buf) {
        var loginCookieInfo = PacketCodecs.readLoginCookieInfo(buf);
        boolean tryRefresh = PacketCodecs.readBoolean(buf);
        plugin.logDebug("Player %s requesting cookie login (tryRefresh=%s)", player.getName(), tryRefresh);
        plugin.getLoginService().cookieLogin(player, loginCookieInfo, tryRefresh);
    }

    private void handleStartQrLogin(Player player) {
        plugin.logDebug("Player %s requesting QR login", player.getName());
        plugin.getLoginService().startQrLogin(player);
    }

    private void handleCancelQrLogin(Player player) {
        plugin.logDebug("Player %s canceling QR login", player.getName());
        plugin.getLoginService().cancelQrLogin(player);
    }
    
    private void handleRemoveMusicFromQueue(Player player, ByteBuf buf) {
        long musicId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s removing music %d from queue", player.getName(), musicId);
        plugin.getMusicPlayerService().removeMusicFromQueue(musicId, player);
    }

    private void handleSearchRequest(Player player, ByteBuf buf) {
        String query = PacketCodecs.readString(buf);
        plugin.logDebug("Player %s searching for '%s'", player.getName(), query);
        var result = plugin.getMusicPlayerService().search(query, player);
        sendSearchResponse(player, result);
    }

    private void handleGetUserPlaylistRequest(Player player) {
        plugin.logDebug("Player %s requesting user playlists", player.getName());
        var playlists = plugin.getMusicPlayerService().getUserPlaylists(player);
        sendGetUserPlaylistResponse(player, playlists);
    }

    private void handleGetPlaylistDetailRequest(Player player, ByteBuf buf) {
        long playlistId = PacketCodecs.readLong(buf);
        plugin.logDebug("Player %s requesting playlist detail %d", player.getName(), playlistId);
        var playlist = plugin.getMusicPlayerService().getPlaylistDetail(playlistId, player);
        if (playlist != null) {
            sendGetPlaylistDetailResponse(player, playlist);
        }
    }
    
    // ==================== Packet Senders ====================
    
    public void sendConnectResponse(Player player, boolean accepted, Version serverVersion) {
        ByteBuf buf = PacketCodecs.createBuffer();
        int startIdx = buf.writerIndex();
        PacketCodecs.writeBoolean(buf, accepted);
        int afterBool = buf.writerIndex();
        PacketCodecs.writeVersion(buf, serverVersion);
        int afterVersion = buf.writerIndex();
        plugin.logDebug("ConnectResponse: bool=%d bytes, version=%d bytes, total=%d bytes", 
                afterBool - startIdx, afterVersion - afterBool, afterVersion - startIdx);
        sendPacket(player, PacketType.CONNECT_RESPONSE, buf);
    }
    
    public void sendSwitchMusic(Player player, MusicDetail music, MusicDetail next, String message) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeMusicDetail(buf, music);
        PacketCodecs.writeMusicDetail(buf, next != null ? next : MusicDetail.NONE);
        PacketCodecs.writeString(buf, message != null ? message : "");
        sendPacket(player, PacketType.SWITCH_MUSIC, buf);
    }
    
    public void sendSyncCurrentPlaying(Player player, MusicDetail music, java.time.ZonedDateTime startTime) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeMusicDetail(buf, music);
        PacketCodecs.writeZonedDateTime(buf, startTime);
        sendPacket(player, PacketType.SYNC_CURRENT_PLAYING, buf);
    }
    
    public void sendRefreshMusicQueue(Player player, java.util.Queue<MusicDetail> queue) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeMusicDetailQueue(buf, queue);
        sendPacket(player, PacketType.REFRESH_MUSIC_QUEUE, buf);
    }
    
    public void sendLoginResult(Player player, boolean success, String message,
                                indi.etern.musichud.bukkit.services.LoginService.PlayerLoginInfo loginInfo) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeBoolean(buf, success);
        PacketCodecs.writeString(buf, message != null ? message : "");
        // Write LoginCookieInfo
        PacketCodecs.writeLoginCookieInfo(buf, loginInfo.getLoginCookieInfo());
        // Write Profile (order: nickname, avatarUrl, backgroundUrl, userId)
        var profile = loginInfo.getProfile();
        PacketCodecs.writeString(buf, profile.getNickname());
        PacketCodecs.writeString(buf, profile.getAvatarUrl());
        PacketCodecs.writeString(buf, profile.getBackgroundUrl());
        PacketCodecs.writeLong(buf, profile.getUserId());
        sendPacket(player, PacketType.LOGIN_RESULT, buf);
    }

    public void sendStartQrLoginResponse(Player player, String base64QrImg) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeString(buf, base64QrImg);
        sendPacket(player, PacketType.START_QR_LOGIN_RESPONSE, buf);
    }

    public void sendSearchResponse(Player player, java.util.List<MusicDetail> result) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writeMusicDetailList(buf, result);
        sendPacket(player, PacketType.SEARCH_RESPONSE, buf);
    }

    public void sendGetUserPlaylistResponse(Player player, java.util.List<indi.etern.musichud.bukkit.beans.Playlist> playlists) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writePlaylistList(buf, playlists);
        sendPacket(player, PacketType.GET_USER_PLAYLIST_RESPONSE, buf);
    }

    public void sendGetPlaylistDetailResponse(Player player, indi.etern.musichud.bukkit.beans.Playlist playlist) {
        ByteBuf buf = PacketCodecs.createBuffer();
        PacketCodecs.writePlaylist(buf, playlist);
        sendPacket(player, PacketType.GET_PLAYLIST_DETAIL_RESPONSE, buf);
    }
    
    private void sendPacket(Player player, PacketType type, ByteBuf buf) {
        byte[] payload = PacketCodecs.toByteArray(buf);
        buf.release();
        // Some clients expect a VarInt length prefix before the payload
        ByteBuf out = PacketCodecs.createBuffer();
        PacketCodecs.writeVarInt(out, payload.length);
        out.writeBytes(payload);
        byte[] data = PacketCodecs.toByteArray(out);
        out.release();

        // Debug: log the hex data being sent
        plugin.logDebug("Sending %s to %s: %d bytes, data: %s", 
                type.name(), player.getName(), data.length, bytesToHex(data));
        
        // Must run on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                player.sendPluginMessage(plugin, type.getFullChannelName(), data);
                plugin.logDebug("Sent packet %s to %s (%d bytes)", type.name(), player.getName(), data.length);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send packet " + type.name() + " to " + player.getName() + ": " + e.getMessage());
            }
        });
    }
    
    public void sendPacketToPlayers(Iterable<Player> players, PacketType type, ByteBuf buf) {
        byte[] payload = PacketCodecs.toByteArray(buf);
        buf.release();

        ByteBuf out = PacketCodecs.createBuffer();
        PacketCodecs.writeVarInt(out, payload.length);
        out.writeBytes(payload);
        byte[] data = PacketCodecs.toByteArray(out);
        out.release();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : players) {
                try {
                    player.sendPluginMessage(plugin, type.getFullChannelName(), data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send packet to " + player.getName() + ": " + e.getMessage());
                }
            }
        });
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 64); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > 64) {
            sb.append("...");
        }
        return sb.toString().trim();
    }
}
