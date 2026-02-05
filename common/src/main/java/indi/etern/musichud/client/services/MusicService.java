package indi.etern.musichud.client.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.UIManager;
import icyllis.modernui.widget.Toast;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.login.LoginType;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.MusicResourceInfo;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.config.ProfileConfigData;
import indi.etern.musichud.client.music.NowPlayingInfo;
import indi.etern.musichud.client.music.StreamAudioPlayer;
import indi.etern.musichud.client.ui.hud.HudRendererManager;
import indi.etern.musichud.client.ui.utils.image.ImageUtils;
import indi.etern.musichud.interfaces.ClientRegister;
import indi.etern.musichud.interfaces.RegisterMark;
import indi.etern.musichud.network.pushMessages.c2s.AddPlaylistToIdlePlaySourceMessage;
import indi.etern.musichud.network.pushMessages.c2s.ClientPushMusicToQueueMessage;
import indi.etern.musichud.network.pushMessages.c2s.ClientRemoveMusicFromQueueMessage;
import indi.etern.musichud.network.pushMessages.c2s.RemovePlaylistFromIdlePlaySourceMessage;
import indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailRequest;
import indi.etern.musichud.network.requestResponseCycle.GetPlaylistDetailResponse;
import indi.etern.musichud.throwable.ApiException;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MusicService {
    private static final Logger logger = MusicHud.getLogger(MusicService.class);
    private static final Cache<Long, Playlist> playlistCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(20)
            .build();
    private static volatile MusicService instance;
    private static final ProfileConfigData profileConfigData = ProfileConfigData.getInstance();
    @Getter
    private final Set<Playlist> idlePlaylists = new HashSet<>();
    @Getter
    private final List<Consumer<Playlist>> idlePlaylistAddListeners = new ArrayList<>();
    @Getter
    private final List<Consumer<Playlist>> idlePlaylistRemoveListeners = new ArrayList<>();
    @Getter
    private final List<Consumer<Playlist>> idlePlaylistChangeListeners = new ArrayList<>();
    @Getter
    private final Queue<MusicDetail> musicQueue = new ArrayDeque<>();
    @Getter
    private final List<Consumer<Queue<MusicDetail>>> musicQueueRefreshListeners = new ArrayList<>();
    @Getter
    private final List<Consumer<MusicDetail>> musicQueuePushListeners = new ArrayList<>();
    @Getter
    private final List<BiConsumer<Integer, MusicDetail>> musicQueueRemoveListeners = new ArrayList<>();
    @Getter
    private boolean idlePlaySourceLoaded = false;

    public static MusicService getInstance() {
        if (instance == null) {
            synchronized (MusicService.class) {
                if (instance == null) {
                    instance = new MusicService();
                }
            }
        }
        return instance;
    }

    private void loadIdlePlaylistsFromConfig() {
        if (!idlePlaySourceLoaded) {
            idlePlaySourceLoaded = true;
            if (!profileConfigData.getIdlePlaySourcePlaylistIds().isEmpty()) {
                MusicHud.EXECUTOR.execute(() -> {
                    for (Long id : profileConfigData.getIdlePlaySourcePlaylistIds()) {
                        try {
                            NetworkManager.sendToServer(new AddPlaylistToIdlePlaySourceMessage(id));
                            getInstance().loadPlaylistDetail(id).thenAcceptAsync(playlist1 -> {
                                getInstance().addToIdlePlaySource(playlist1);
                            }, MusicHud.EXECUTOR);
                        } catch (Exception e) {
                            logger.error("Failed to load idle play source playlist with id:{}", id, e);
                        }
                    }
                });
            }
        }
    }

    public CompletableFuture<Playlist> loadPlaylistDetail(long id) {
        Playlist cachedPlaylist = playlistCache.getIfPresent(id);
        if (cachedPlaylist != null) {
            return CompletableFuture.completedFuture(cachedPlaylist);
        }
        CompletableFuture<Playlist> completableFuture = new CompletableFuture<>();
        MusicHud.EXECUTOR.execute(() -> {
            NetworkManager.sendToServer(new GetPlaylistDetailRequest(id));
            Thread pendingThread = Thread.currentThread();
            GetPlaylistDetailResponse.setReceiver(id, value -> {
                Playlist playlist = value.playlist();
                playlistCache.put(id, playlist);
                completableFuture.complete(playlist);
                pendingThread.interrupt();
            });
            try {
                Thread.sleep(Duration.of(5, ChronoUnit.SECONDS));
                completableFuture.completeExceptionally(new ApiException());
            } catch (InterruptedException ignored) {
            }
        });
        return completableFuture;
    }

    public void addToIdlePlaySource(Playlist playlist) {
        idlePlaylists.add(playlist);
        idlePlaylistAddListeners.forEach(l -> l.accept(playlist));
        idlePlaylistChangeListeners.forEach(l -> l.accept(playlist));
        profileConfigData.getIdlePlaySourcePlaylistIds().add(playlist.getId());
        profileConfigData.saveToConfig();
        NetworkManager.sendToServer(new AddPlaylistToIdlePlaySourceMessage(playlist.getId()));
    }

    public void removeFromIdlePlaySource(Playlist playlist) {
        idlePlaylists.remove(playlist);
        idlePlaylistRemoveListeners.forEach(l -> l.accept(playlist));
        idlePlaylistChangeListeners.forEach(l -> l.accept(playlist));
        profileConfigData.getIdlePlaySourcePlaylistIds().remove(playlist.getId());
        profileConfigData.saveToConfig();
        NetworkManager.sendToServer(new RemovePlaylistFromIdlePlaySourceMessage(playlist.getId()));
    }

    public synchronized void refreshQueue(Queue<MusicDetail> queue) {
        Iterator<MusicDetail> originalIterator = musicQueue.iterator();
        Iterator<MusicDetail> newIterator = queue.iterator();
        int index = 0;
        while (originalIterator.hasNext() || newIterator.hasNext()) {
            if (originalIterator.hasNext() && newIterator.hasNext()) {
                MusicDetail original = originalIterator.next();
                MusicDetail news = newIterator.next();
                if (original.getId() != news.getId()) {
                    AtomicInteger atomicInt = new AtomicInteger(0);
                    int finalIndex = index;
                    musicQueue.removeIf((musicDetail) -> {
                        int i = atomicInt.getAndIncrement();
                        boolean remove = i == finalIndex && musicDetail.equals(original);
                        if (remove) {
                            musicQueueRemoveListeners.forEach(l -> {
                                l.accept(i, musicDetail);
                            });
                        }
                        return remove;
                    });
                }
            } else if (newIterator.hasNext()) {
                MusicDetail addedMusicDetail = newIterator.next();
                musicQueue.add(addedMusicDetail);
                musicQueuePushListeners.forEach(l -> {
                    l.accept(addedMusicDetail);
                });
            } else {
                AtomicInteger atomicInt = new AtomicInteger(0);
                int finalIndex = index;
                musicQueue.removeIf((removedMusicDetail) -> {
                    int i = atomicInt.getAndIncrement();
                    boolean remove = i >= finalIndex;
                    if (remove) {
                        musicQueueRemoveListeners.forEach(l -> {
                            l.accept(i, removedMusicDetail);
                        });
                    }
                    return remove;
                });
                break;
            }
            index++;
        }
        musicQueueRefreshListeners.forEach(l -> {
            l.accept(queue);
        });
    }

    public void sendPushMusicToQueue(MusicDetail musicDetail) {
        NetworkManager.sendToServer(new ClientPushMusicToQueueMessage(musicDetail.getId()));
    }

    public void sendRemoveMusicFromQueue(int index, MusicDetail musicDetail) {
        NetworkManager.sendToServer(new ClientRemoveMusicFromQueueMessage(index, musicDetail.getId()));
    }

    public synchronized void switchMusic(MusicDetail musicDetail, MusicResourceInfo resourceInfo, ZonedDateTime serverStartTime, String message) {
        if (ClientConfigDefinition.enable.get()) {
            if (!message.isEmpty()) {
                MuiModApi.postToUiThread(() -> {
                    //noinspection UnstableApiUsage
                    Context context = UIManager.getInstance().getDecorView().getContext();
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                });
            }
            if (!musicDetail.equals(MusicDetail.NONE)) {
                loadResource(musicDetail);
                StreamAudioPlayer streamAudioPlayer = StreamAudioPlayer.getInstance();
                streamAudioPlayer.playAsyncFromUrl(resourceInfo.getUrl(), resourceInfo.getType(), serverStartTime).thenAccept(zonedDateTime -> {
                    NowPlayingInfo.getInstance().switchMusic(musicDetail, resourceInfo, zonedDateTime);
                }).exceptionally(e -> {
                    return null;//TODO display error in hud
                });
            } else {
                NowPlayingInfo.getInstance().switchMusic(MusicDetail.NONE, MusicResourceInfo.NONE, null);
                StreamAudioPlayer streamAudioPlayer = StreamAudioPlayer.getInstance();
                streamAudioPlayer.stop();
            }
        }
    }

    public void loadResource(MusicDetail musicDetail) {
        if (ClientConfigDefinition.enable.get()) {
            ImageUtils.downloadAsync(musicDetail.getAlbum().getThumbnailPicUrl(200));
        }
    }

    @RegisterMark
    public static class RegisterImpl implements ClientRegister {
        @Override
        public void register() {
            LoginService.getInstance().getLoginCompleteListeners().add((loginCookieInfo) -> {
                if (loginCookieInfo.type() != LoginType.ANONYMOUS) {
                    MusicService.getInstance().loadIdlePlaylistsFromConfig();
                }
            });
            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
                reset();
            });
        }

        public static void reset() {
            if (instance != null) {
                instance.switchMusic(MusicDetail.NONE, MusicResourceInfo.NONE, null, "");
                instance.idlePlaySourceLoaded = false;
                instance.musicQueue.clear();
                instance.idlePlaylistAddListeners.clear();
                instance.idlePlaylistRemoveListeners.clear();
                instance.idlePlaylistChangeListeners.clear();
                instance.musicQueueRefreshListeners.clear();
                instance.musicQueuePushListeners.clear();
                instance.musicQueueRemoveListeners.clear();
            }
            if (HudRendererManager.isLoaded()) {
                HudRendererManager.getInstance().reset();
            }
            NowPlayingInfo.getInstance().switchMusic(MusicDetail.NONE, MusicResourceInfo.NONE, null);
        }
    }
}