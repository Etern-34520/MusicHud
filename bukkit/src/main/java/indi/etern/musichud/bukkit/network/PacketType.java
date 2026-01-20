package indi.etern.musichud.bukkit.network;

import indi.etern.musichud.bukkit.MusicHudPlugin;

/**
 * Represents different packet types for MusicHud protocol.
 */
public enum PacketType {
    // C2S (Client to Server) - Push Messages
    ADD_PLAYLIST_TO_IDLE_PLAY_SOURCE("add_playlist_to_idle_play_source_message"),
    CLIENT_PUSH_MUSIC_TO_QUEUE("client_push_music_to_queue_message"),
    CLIENT_REMOVE_MUSIC_FROM_QUEUE("client_remove_music_from_queue_message"),
    LOGOUT("logout_message"),
    REMOVE_PLAYLIST_FROM_IDLE_PLAY_SOURCE("remove_playlist_from_idle_play_source_message"),
    VOTE_SKIP_CURRENT_MUSIC("vote_skip_current_music_message"),
    
    // C2S (Client to Server) - Request/Response
    CONNECT_REQUEST("connect_request"),
    ANONYMOUS_LOGIN_REQUEST("anonymous_login_request"),
    CANCEL_QR_LOGIN_REQUEST("cancel_qr_login_request"),
    COOKIE_LOGIN_REQUEST("cookie_login_request"),
    GET_PLAYLIST_DETAIL_REQUEST("get_playlist_detail_request"),
    GET_USER_PLAYLIST_REQUEST("get_user_playlist_request"),
    SEARCH_REQUEST("search_request"),
    START_QR_LOGIN_REQUEST("start_qr_login_request"),
    
    // S2C (Server to Client) - Push Messages
    LOGIN_RESULT("login_result_message"),
    REFRESH_MUSIC_QUEUE("refresh_music_queue_message"),
    SWITCH_MUSIC("switch_music_message"),
    SYNC_CURRENT_PLAYING("sync_current_playing_message"),
    
    // S2C (Server to Client) - Response
    CONNECT_RESPONSE("connect_response"),
    GET_PLAYLIST_DETAIL_RESPONSE("get_playlist_detail_response"),
    GET_USER_PLAYLIST_RESPONSE("get_user_playlist_response"),
    SEARCH_RESPONSE("search_response"),
    START_QR_LOGIN_RESPONSE("start_qr_login_response");
    
    private final String channelName;
    
    PacketType(String channelName) {
        this.channelName = channelName;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public String getFullChannelName() {
        return MusicHudPlugin.MOD_ID + ":" + channelName;
    }
    
    public static PacketType fromChannelName(String fullChannelName) {
        String prefix = MusicHudPlugin.MOD_ID + ":";
        if (!fullChannelName.startsWith(prefix)) {
            return null;
        }
        String name = fullChannelName.substring(prefix.length());
        for (PacketType type : values()) {
            if (type.channelName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
