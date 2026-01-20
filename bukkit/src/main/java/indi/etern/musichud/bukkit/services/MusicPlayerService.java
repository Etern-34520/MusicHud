package indi.etern.musichud.bukkit.services;

import indi.etern.musichud.bukkit.MusicHudPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import indi.etern.musichud.bukkit.beans.*;
import indi.etern.musichud.bukkit.beans.user.Profile;
import indi.etern.musichud.bukkit.http.ApiClient;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing music playback on the server.
 */
public class MusicPlayerService {
    private final MusicHudPlugin plugin;
    private final ArrayDeque<MusicDetail> musicQueue = new ArrayDeque<>();
    private final Map<Player, Set<Playlist>> idlePlaySources = new ConcurrentHashMap<>();
    private final CurrentVoteInfo currentVoteInfo = new CurrentVoteInfo();
    private final Random random = new Random();
    
    private volatile MusicDetail currentMusicDetail = MusicDetail.NONE;
    private volatile ZonedDateTime nowPlayingStartTime = ZonedDateTime.now();
    private volatile boolean running = false;
    private Thread pusherThread;
    
    public MusicPlayerService(MusicHudPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start the music pusher thread.
     */
    public synchronized void start() {
        if (running) return;
        running = true;
        
        pusherThread = new Thread(this::musicPusherLoop, "MusicHud-MusicPusher");
        pusherThread.setDaemon(true);
        pusherThread.start();
        
        plugin.getLogger().info("Music player service started");
    }
    
    /**
     * Stop the music pusher thread.
     */
    public synchronized void shutdown() {
        running = false;
        if (pusherThread != null) {
            pusherThread.interrupt();
            pusherThread = null;
        }
        plugin.getLogger().info("Music player service stopped");
    }
    
    private void musicPusherLoop() {
        MusicDetail preloadMusicDetail = MusicDetail.NONE;
        String message = "";
        
        while (running) {
            try {
                MusicDetail switchedToPlay = null;
                MusicDetail nextMusicDetail = MusicDetail.NONE;
                
                Set<Player> connectedPlayers = plugin.getLoginService().getConnectedPlayers();
                if (connectedPlayers.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }
                
                synchronized (musicQueue) {
                    if (musicQueue.isEmpty()) {
                        Optional<MusicDetail> optionalMusic = getRandomMusicFromIdleSources();
                        if (optionalMusic.isEmpty()) {
                            Thread.sleep(1000);
                            continue;
                        }
                        
                        MusicDetail musicDetail = optionalMusic.get();
                        if (preloadMusicDetail == null || preloadMusicDetail.equals(MusicDetail.NONE)) {
                            preloadMusicDetail = musicDetail;
                            Optional<MusicDetail> nextOptional = getRandomMusicFromIdleSources();
                            if (nextOptional.isPresent()) {
                                switchedToPlay = preloadMusicDetail;
                                nextMusicDetail = nextOptional.get();
                                preloadMusicDetail = nextMusicDetail;
                            } else {
                                switchedToPlay = musicDetail;
                                preloadMusicDetail = MusicDetail.NONE;
                            }
                        } else {
                            switchedToPlay = preloadMusicDetail;
                            preloadMusicDetail = musicDetail;
                        }
                    } else {
                        switchedToPlay = musicQueue.poll();
                        sendRefreshQueueToAll();
                    }
                    
                    if (!musicQueue.isEmpty()) {
                        nextMusicDetail = musicQueue.peek();
                    } else if (preloadMusicDetail != null && !preloadMusicDetail.equals(MusicDetail.NONE)) {
                        nextMusicDetail = preloadMusicDetail;
                    }
                }
                
                if (switchedToPlay == null || switchedToPlay.equals(MusicDetail.NONE)) {
                    Thread.sleep(1000);
                    continue;
                }
                
                // Send switch music to all connected players
                sendSwitchMusicToAll(switchedToPlay, nextMusicDetail, message);
                message = "";
                
                currentVoteInfo.resetTo(switchedToPlay);
                currentMusicDetail = switchedToPlay;
                nowPlayingStartTime = ZonedDateTime.now();
                
                plugin.getLogger().info("Now playing: " + switchedToPlay.getName() + " (ID: " + switchedToPlay.getId() + ")");
                
                // Wait for song duration
                int duration = switchedToPlay.getDurationMillis();
                int interval = plugin.getPluginConfig().getPlaybackInterval();
                
                try {
                    Thread.sleep(duration + interval);
                } catch (InterruptedException e) {
                    // Song was skipped
                    message = "投票切歌通过";
                    plugin.logDebug("Current song was skipped");
                }
                
            } catch (InterruptedException e) {
                // Thread interrupted, check if still running
                if (!running) break;
            } catch (Exception e) {
                plugin.getLogger().warning("Error in music pusher: " + e.getMessage());
                if (plugin.getPluginConfig().isDebugEnabled()) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    if (!running) break;
                }
            }
        }
    }
    
    private Optional<MusicDetail> getRandomMusicFromIdleSources() {
        if (idlePlaySources.isEmpty()) {
            return Optional.empty();
        }
        
        List<Map.Entry<Player, Set<Playlist>>> entries = new ArrayList<>(idlePlaySources.entrySet());
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        
        Map.Entry<Player, Set<Playlist>> randomEntry = entries.get(random.nextInt(entries.size()));
        Player sourcePlayer = randomEntry.getKey();
        Set<Playlist> playlists = randomEntry.getValue();
        
        List<MusicDetail> allTracks = playlists.stream()
                .flatMap(playlist -> playlist.getTracks().stream())
                .toList();
        
        if (allTracks.isEmpty()) {
            return Optional.empty();
        }
        
        MusicDetail randomTrack = allTracks.get(random.nextInt(allTracks.size()));
        
        // Set pusher info
        LoginService.PlayerLoginInfo loginInfo = plugin.getLoginService().getLoginInfo(sourcePlayer);
        if (loginInfo != null) {
                PusherInfo pusherInfo = new PusherInfo(
                    loginInfo.getProfile().getUserId(),
                    sourcePlayer.getUniqueId(),
                    sourcePlayer.getName()
                );
            randomTrack.setPusherInfo(pusherInfo);
        }
        
        return Optional.of(randomTrack);
    }
    
    /**
     * Push a music to the queue.
     */
    public void pushMusicToQueue(long musicId, Player player) {
        MusicDetail music = fetchMusicDetail(musicId, player);
        if (music == null) {
            plugin.getLogger().warning("Failed to fetch music detail for ID " + musicId);
            return;
        }
        
        LoginService.PlayerLoginInfo loginInfo = plugin.getLoginService().getLoginInfo(player);
        if (loginInfo != null) {
                music.setPusherInfo(new PusherInfo(
                    loginInfo.getProfile().getUserId(),
                    player.getUniqueId(),
                    player.getName()
                ));
        }
        
        synchronized (musicQueue) {
            musicQueue.add(music);
        }
        
        sendRefreshQueueToAll();
        
        // Start music service if not running
        if (!running && plugin.getPluginConfig().isIdlePlaylistEnabled()) {
            start();
        }
        
        plugin.getLogger().info(player.getName() + " added music " + musicId + " to queue");
    }
    
    /**
     * Remove a music from the queue.
     */
    public void removeMusicFromQueue(long musicId, Player player) {
        synchronized (musicQueue) {
            musicQueue.removeIf(m -> m.getId() == musicId);
        }
        sendRefreshQueueToAll();
        plugin.logDebug("Player %s removed music %d from queue", player.getName(), musicId);
    }
    
    /**
     * Vote to skip the current music.
     */
    public void voteSkipCurrent(long musicId, Player player) {
        if (currentMusicDetail == null || currentMusicDetail.getId() != musicId) {
            return;
        }
        
        if (currentVoteInfo.addVote(player)) {
            int totalPlayers = plugin.getLoginService().getConnectedPlayers().size();
            double requiredRatio = plugin.getPluginConfig().getVoteSkipRatio();
            int minVotes = plugin.getPluginConfig().getVoteSkipMinVotes();
            
            int required = Math.max(minVotes, (int) Math.ceil(totalPlayers * requiredRatio));
            int current = currentVoteInfo.getVoteCount();
            
            if (current >= required) {
                // Skip current song
                if (pusherThread != null) {
                    pusherThread.interrupt();
                }
                plugin.getLogger().info("Vote skip passed (" + current + "/" + required + ")");
            } else {
                plugin.logDebug("Vote skip: %d/%d", current, required);
            }
        }
    }
    
    /**
     * Add a playlist to idle play sources.
     */
    public void addIdlePlaySource(long playlistId, Player player) {
        Playlist playlist = getPlaylistDetail(playlistId, player);
        if (playlist == null) {
            plugin.getLogger().warning("Failed to fetch playlist detail for ID " + playlistId);
            return;
        }
        
        idlePlaySources.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(playlist);
        
        // Start music service if not running
        if (!running && plugin.getPluginConfig().isIdlePlaylistEnabled()) {
            start();
        }
        
        plugin.logDebug("Player %s added playlist %d to idle sources", player.getName(), playlistId);
    }
    
    /**
     * Remove a playlist from idle play sources.
     */
    public void removeIdlePlaySource(long playlistId, Player player) {
        Set<Playlist> playlists = idlePlaySources.get(player);
        if (playlists != null) {
            playlists.removeIf(p -> p.getId() == playlistId);
            if (playlists.isEmpty()) {
                idlePlaySources.remove(player);
            }
        }
        plugin.logDebug("Player %s removed playlist %d from idle sources", player.getName(), playlistId);
    }

    /**
     * Handle player disconnect.
     */
    public void onPlayerDisconnect(Player player) {
        idlePlaySources.remove(player);
        currentVoteInfo.removeVote(player);
    }

    public List<MusicDetail> search(String query, Player player) {
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            ObjectNode body = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
            body.put("keywords", query);
            body.put("limit", 30);
            body.put("offset", 0);
            body.put("type", 1);
            JsonNode response = ApiClient.post(baseUrl, "/cloudsearch", null, body, null, timeout);
            JsonNode songs = response.path("result").path("songs");
            if (!songs.isArray()) {
                return List.of();
            }
            List<MusicDetail> result = new ArrayList<>();
            for (JsonNode song : songs) {
                MusicDetail detail = parseMusicDetail(song);
                if (detail != null) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("Search failed: " + e.getMessage());
            return List.of();
        }
    }

    public List<Playlist> getUserPlaylists(Player player) {
        LoginService.PlayerLoginInfo loginInfo = plugin.getLoginService().getLoginInfo(player);
        if (loginInfo == null) {
            return List.of();
        }
        long uid = loginInfo.getProfile().getUserId();
        if (uid <= 0) {
            return List.of();
        }
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            String cookie = loginInfo.getLoginCookieInfo().getRawCookie();
            JsonNode response = ApiClient.get(baseUrl, "/user/playlist", Map.of("uid", String.valueOf(uid)), cookie, timeout);
            JsonNode list = response.path("playlist");
            if (!list.isArray()) {
                return List.of();
            }
            List<Playlist> playlists = new ArrayList<>();
            for (JsonNode node : list) {
                Playlist playlist = new Playlist();
                playlist.setId(node.path("id").asLong(0L));
                playlist.setName(node.path("name").asText(""));
                playlist.setCoverImgId(node.path("coverImgId").asLong(0L));
                playlist.setCoverImgIdStr(node.path("coverImgId_str").asText(""));
                playlist.setCoverImgUrl(node.path("coverImgUrl").asText(""));
                playlist.setCreator(parseProfile(node.path("creator")));
                playlists.add(playlist);
            }
            return playlists;
        } catch (Exception e) {
            plugin.getLogger().warning("Get user playlists failed: " + e.getMessage());
            return List.of();
        }
    }

    public Playlist getPlaylistDetail(long playlistId, Player player) {
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            LoginService.PlayerLoginInfo loginInfo = plugin.getLoginService().getLoginInfo(player);
            String cookie = loginInfo != null ? loginInfo.getLoginCookieInfo().getRawCookie() : null;
            JsonNode response = ApiClient.get(baseUrl, "/playlist/detail/all", Map.of("id", String.valueOf(playlistId)), cookie, timeout);
            JsonNode playlistNode = response.path("playlist");
            if (playlistNode.isMissingNode() || playlistNode.isNull()) {
                return null;
            }
            Playlist playlist = new Playlist();
            playlist.setId(playlistNode.path("id").asLong(playlistId));
            playlist.setName(playlistNode.path("name").asText(""));
            playlist.setCoverImgId(playlistNode.path("coverImgId").asLong(0L));
            playlist.setCoverImgIdStr(playlistNode.path("coverImgId_str").asText(""));
            playlist.setCoverImgUrl(playlistNode.path("coverImgUrl").asText(""));
            playlist.setCreator(parseProfile(playlistNode.path("creator")));
            JsonNode tracks = playlistNode.path("tracks");
            if (tracks.isArray()) {
                List<MusicDetail> details = new ArrayList<>();
                for (JsonNode track : tracks) {
                    MusicDetail detail = parseMusicDetail(track);
                    if (detail != null) {
                        details.add(detail);
                    }
                }
                playlist.setTracks(details);
            }
            return playlist;
        } catch (Exception e) {
            plugin.getLogger().warning("Get playlist detail failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Send sync playing status to a player.
     */
    public void sendSyncPlayingStatusToPlayer(Player player) {
        if (currentMusicDetail != null && !currentMusicDetail.equals(MusicDetail.NONE)) {
            plugin.getChannelHandler().sendSyncCurrentPlaying(player, currentMusicDetail, nowPlayingStartTime);
            
            synchronized (musicQueue) {
                if (!musicQueue.isEmpty()) {
                    plugin.getChannelHandler().sendRefreshMusicQueue(player, musicQueue);
                }
            }
        }
    }
    
    private void sendSwitchMusicToAll(MusicDetail music, MusicDetail next, String message) {
        Set<Player> players = plugin.getLoginService().getConnectedPlayers();
        for (Player player : players) {
            plugin.getChannelHandler().sendSwitchMusic(player, music, next, message);
        }
    }
    
    private void sendRefreshQueueToAll() {
        Set<Player> players = plugin.getLoginService().getConnectedPlayers();
        synchronized (musicQueue) {
            for (Player player : players) {
                plugin.getChannelHandler().sendRefreshMusicQueue(player, musicQueue);
            }
        }
    }
    
    public MusicDetail getCurrentMusicDetail() {
        return currentMusicDetail;
    }
    
    public ZonedDateTime getNowPlayingStartTime() {
        return nowPlayingStartTime;
    }
    
    public Queue<MusicDetail> getMusicQueue() {
        return musicQueue;
    }

    private MusicDetail fetchMusicDetail(long musicId, Player player) {
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            JsonNode response = ApiClient.get(baseUrl, "/song/detail", Map.of("ids", String.valueOf(musicId)), null, timeout);
            JsonNode songs = response.path("songs");
            if (!songs.isArray() || songs.isEmpty()) {
                return null;
            }
            MusicDetail detail = parseMusicDetail(songs.get(0));
            if (detail == null) {
                return null;
            }
            MusicResourceInfo resourceInfo = fetchResourceInfo(musicId, player);
            if (resourceInfo != null) {
                detail.setMusicResourceInfo(resourceInfo);
            }
            return detail;
        } catch (Exception e) {
            plugin.getLogger().warning("Fetch music detail failed: " + e.getMessage());
            return null;
        }
    }

    private MusicResourceInfo fetchResourceInfo(long musicId, Player player) {
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            LoginService.PlayerLoginInfo loginInfo = plugin.getLoginService().getLoginInfo(player);
            String cookie = loginInfo != null ? loginInfo.getLoginCookieInfo().getRawCookie() : null;
            Map<String, String> query = new HashMap<>();
            query.put("id", String.valueOf(musicId));
            query.put("level", "lossless");
            query.put("unblock", "true");
            JsonNode response = ApiClient.get(baseUrl, "/song/url/v1", query, cookie, timeout);
            JsonNode data = response.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            JsonNode item = data.get(0);
            MusicResourceInfo info = new MusicResourceInfo();
            info.setId(item.path("id").asLong(musicId));
            info.setUrl(item.path("url").asText(""));
            info.setBitrate(item.path("br").asInt(0));
            info.setSize(item.path("size").asLong(0));
            info.setType(FormatType.fromString(item.path("type").asText("")));
            info.setMd5(item.path("md5").asText(""));
            info.setFee(Fee.fromCode(item.path("fee").asInt(-1)));
            info.setTime(item.path("time").asInt(0));
            LyricInfo lyricInfo = fetchLyricInfo(musicId, cookie);
            info.setLyricInfo(lyricInfo != null ? lyricInfo : LyricInfo.NONE);
            return info;
        } catch (Exception e) {
            plugin.getLogger().warning("Fetch resource info failed: " + e.getMessage());
            return null;
        }
    }

    private LyricInfo fetchLyricInfo(long musicId, String cookie) {
        try {
            String baseUrl = plugin.getPluginConfig().getApiBaseUrl();
            int timeout = plugin.getPluginConfig().getApiTimeout();
            JsonNode response = ApiClient.get(baseUrl, "/lyric/new", Map.of("id", String.valueOf(musicId)), cookie, timeout);
            LyricInfo info = new LyricInfo();
            info.setCode(response.path("code").asInt(0));
            JsonNode lrc = response.path("lrc");
            JsonNode tlyric = response.path("tlyric");
            Lyric main = new Lyric(lrc.path("version").asInt(0), lrc.path("lyric").asText(""));
            Lyric trans = new Lyric(tlyric.path("version").asInt(0), tlyric.path("lyric").asText(""));
            info.setLrc(main);
            info.setTlyric(trans);
            return info;
        } catch (Exception e) {
            plugin.getLogger().warning("Fetch lyric failed: " + e.getMessage());
            return LyricInfo.NONE;
        }
    }

    private Profile parseProfile(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Profile.ANONYMOUS;
        }
        String nickname = node.path("nickname").asText("");
        String avatarUrl = node.path("avatarUrl").asText("");
        String backgroundUrl = node.path("backgroundUrl").asText("");
        long userId = node.path("userId").asLong(0L);
        return new Profile(nickname, avatarUrl, backgroundUrl, userId);
    }

    private MusicDetail parseMusicDetail(JsonNode song) {
        if (song == null || song.isMissingNode()) {
            return null;
        }
        MusicDetail detail = new MusicDetail();
        detail.setName(song.path("name").asText(""));
        detail.setId(song.path("id").asLong(0L));
        List<Artist> artists = new ArrayList<>();
        JsonNode ar = song.path("ar");
        if (ar.isArray()) {
            for (JsonNode artistNode : ar) {
                Artist artist = new Artist(artistNode.path("id").asLong(0L), artistNode.path("name").asText(""));
                artists.add(artist);
            }
        }
        detail.setArtists(artists);
        List<String> alias = new ArrayList<>();
        JsonNode alia = song.path("alia");
        if (alia.isArray()) {
            for (JsonNode a : alia) {
                alias.add(a.asText(""));
            }
        }
        detail.setAlias(alias);
        JsonNode al = song.path("al");
        AlbumInfo album = new AlbumInfo(
                al.path("id").asLong(0L),
                al.path("name").asText(""),
                al.path("picUrl").asText(""),
                al.path("pic").asLong(0L)
        );
        detail.setAlbum(album);
        detail.setDurationMillis(song.path("dt").asInt(0));
        List<String> translations = new ArrayList<>();
        JsonNode tns = song.path("tns");
        if (tns.isArray()) {
            for (JsonNode t : tns) {
                translations.add(t.asText(""));
            }
        }
        detail.setTranslations(translations);
        detail.setPusherInfo(PusherInfo.EMPTY);
        detail.setMusicResourceInfo(MusicResourceInfo.NONE);
        return detail;
    }
    
    /**
     * Tracks vote information for current song.
     */
    private static class CurrentVoteInfo {
        private MusicDetail music = MusicDetail.NONE;
        private final Set<Player> voters = ConcurrentHashMap.newKeySet();
        
        public synchronized void resetTo(MusicDetail music) {
            this.music = music;
            this.voters.clear();
        }
        
        public synchronized boolean addVote(Player player) {
            if (voters.contains(player)) {
                return false;
            }
            voters.add(player);
            return true;
        }
        
        public synchronized void removeVote(Player player) {
            voters.remove(player);
        }
        
        public synchronized int getVoteCount() {
            return voters.size();
        }
    }
}
