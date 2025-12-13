package indi.etern.musichud.server.api;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.MusicResourceInfo;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.beans.music.PusherInfo;
import indi.etern.musichud.interfaces.RegisterMark;
import indi.etern.musichud.interfaces.ServerRegister;
import indi.etern.musichud.network.pushMessages.s2c.RefreshMusicQueueMessage;
import indi.etern.musichud.network.pushMessages.s2c.SwitchMusicMessage;
import indi.etern.musichud.network.pushMessages.s2c.SyncCurrentPlayingMessage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MusicPlayerServerService {
    private static volatile MusicPlayerServerService instance;
    private final MusicApiService musicApiService = MusicApiService.getInstance();
    @Getter
    ArrayDeque<MusicDetail> musicQueue = new ArrayDeque<>();
    Map<ServerPlayer, Set<Playlist>> idlePlaySources = new ConcurrentHashMap<>();
    boolean continuable;
    volatile Runnable musicDataPusher;
    private Logger logger = MusicHud.getLogger(MusicPlayerServerService.class);
    private int musicIntervalMillis = 1000;
    @Getter
    private volatile MusicDetail currentMusicDetail = MusicDetail.NONE;
    @Getter
    private volatile ZonedDateTime nowPlayingStartTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());

    public MusicPlayerServerService() {
        updateContinuable(!LoginApiService.getInstance().getLoginStateChangeListeners().isEmpty());
    }

    public static MusicPlayerServerService getInstance() {
        if (instance == null) {
            synchronized (MusicPlayerServerService.class) {
                if (instance == null) {
                    instance = new MusicPlayerServerService();
                }
            }
        }
        return instance;
    }

    private void updateContinuable(boolean continuable) {
        this.continuable = continuable;
        if (continuable && musicDataPusher == null) {
            startMusicPusher();
        }
    }

    private boolean haveSentMusic = false;
    private void startMusicPusher() {
        synchronized (MusicPlayerServerService.class) {
            haveSentMusic = false;
            if (musicDataPusher == null) {
                AtomicReference<Runnable> runnableReference = new AtomicReference<>();
                runnableReference.set(new Runnable() {
                    private MusicDetail preloadMusicDetail = MusicDetail.NONE;

                    @Override
                    public void run() {
                        Thread.currentThread().setName("Music Data Pusher Thread");
                        while (MusicPlayerServerService.this.continuable && musicDataPusher == runnableReference.get()) {
                            MusicDetail switchedToPlay = null;
                            MusicDetail nextMusicDetail;
                            try {
                                if (musicQueue.isEmpty()) {
                                    Optional<MusicDetail> optionalMusicDetail = getRandomMusicFromIdleSources();
                                    if (optionalMusicDetail.isEmpty()) {
                                        break;
                                    } else {
                                        MusicDetail musicDetail = optionalMusicDetail.get();
                                        if (preloadMusicDetail == null || preloadMusicDetail.equals(MusicDetail.NONE)) {
                                            preloadMusicDetail = musicDetail;
                                            Optional<MusicDetail> optionalMusicDetail1 = getRandomMusicFromIdleSources();
                                            if (optionalMusicDetail1.isPresent()) {
                                                switchedToPlay = preloadMusicDetail;
                                                nextMusicDetail = optionalMusicDetail1.get();
                                                preloadMusicDetail = nextMusicDetail;
                                            } else {
                                                switchedToPlay = musicDetail;
                                                preloadMusicDetail = MusicDetail.NONE;
                                            }
                                        } else {
                                            switchedToPlay = preloadMusicDetail;
                                            preloadMusicDetail = musicDetail;
                                        }
                                    }
                                } else {
                                    switchedToPlay = musicQueue.remove();
                                    NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                                            new RefreshMusicQueueMessage(musicQueue));
                                }

                                if (!musicQueue.isEmpty()) {
                                    nextMusicDetail = musicQueue.getFirst();
                                } else {
                                    nextMusicDetail = preloadMusicDetail != null ? preloadMusicDetail : MusicDetail.NONE;
                                }

                                loadResourceInfoIfUnloaded(switchedToPlay);
//                                loadResourceInfoIfUnloaded(nextMusicDetail);
//
                                NetworkManager.sendToPlayers(
                                        LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                                        new SwitchMusicMessage(switchedToPlay, nextMusicDetail)
                                );
                                haveSentMusic = true;
                                currentMusicDetail = switchedToPlay;
                                nowPlayingStartTime = ZonedDateTime.now();
                                logger.info("Switched to music: {} (ID: {})", switchedToPlay.getName(), switchedToPlay.getId());
                                try {
                                    //noinspection BusyWait
                                    Thread.sleep(switchedToPlay.getDurationMillis() + musicIntervalMillis);
                                } catch (InterruptedException ignored) {
                                    logger.warn("Music data pusher interrupted");
                                    break;
                                }
                            } catch (Exception e) {
                                logger.error("Failed to push music: {} (id: {})",
                                        switchedToPlay != null ? switchedToPlay.getName() : "null",
                                        switchedToPlay != null ? switchedToPlay.getId() : "-1",
                                        e);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ignored) {
                                    break;
                                }
                            }
                        }
                        if (haveSentMusic) {
                            MusicPlayerServerService.this.stopSendingMusic();
                        } else {
                            musicDataPusher = null;
                        }
                    }

                    private Optional<MusicDetail> getRandomMusicFromIdleSources() {
                         if (idlePlaySources.isEmpty()) {
                            return Optional.empty();
                        }

                        List<Map.Entry<ServerPlayer, Set<Playlist>>> entryList =
                                new ArrayList<>(idlePlaySources.entrySet());

                        if (entryList.isEmpty()) {
                            return Optional.empty();
                        }

                        Map.Entry<ServerPlayer, Set<Playlist>> randomEntry =
                                entryList.get(MusicHud.RANDOM.nextInt(entryList.size()));

                        ServerPlayer sourcePlayer = randomEntry.getKey();
                        Set<Playlist> playlists = randomEntry.getValue();

                        List<MusicDetail> allTracks = playlists.stream()
                                .flatMap(playlist -> playlist.getTracks().stream())
                                .toList();

                        if (allTracks.isEmpty()) {
                            return Optional.empty();
                        }

                        MusicDetail randomTrack = allTracks.get(MusicHud.RANDOM.nextInt(allTracks.size()));

                        LoginApiService.PlayerLoginInfo loginInfo =
                                LoginApiService.getInstance().getLoginInfoByServerPlayer(sourcePlayer);
                        PusherInfo pusherInfo = new PusherInfo(
                                loginInfo.profile.getUserId(),
                                sourcePlayer.getUUID(),
                                sourcePlayer.getName().getString()
                        );
                        randomTrack.setPusherInfo(pusherInfo);

                        return Optional.of(randomTrack);
                    }
                });
                musicDataPusher = runnableReference.get();
                MusicHud.EXECUTOR.execute(runnableReference.get());
            }
        }
    }

    private void loadResourceInfoIfUnloaded(MusicDetail musicDetail) {
        if (musicDetail != null && !musicDetail.equals(MusicDetail.NONE)) {
            MusicResourceInfo musicResourceInfo = musicDetail.getMusicResourceInfo();
            if (musicResourceInfo == null || musicResourceInfo.equals(MusicResourceInfo.NONE)) {
                MusicResourceInfo resourceInfo = musicApiService.getResourceInfo(musicDetail);
                if (resourceInfo != null && !resourceInfo.equals(MusicResourceInfo.NONE)) {
                    musicDetail.setMusicResourceInfo(resourceInfo);
                } else {
                    throw new RuntimeException("Failed to get resource info for music: " + musicDetail.getName() + " (ID: " + musicDetail.getId() + ")");
                }
            }
        }
    }

    private void stopSendingMusic() {
        this.continuable = false;
        musicDataPusher = null;
        currentMusicDetail = MusicDetail.NONE;
        NetworkManager.sendToPlayers(
                LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                new SwitchMusicMessage(MusicDetail.NONE, MusicDetail.NONE)
        );
    }

    public void sendSyncPlayingStatusToPlayer(ServerPlayer serverPlayer) {
        NetworkManager.sendToPlayer(serverPlayer,
                new RefreshMusicQueueMessage(musicQueue));
        if (currentMusicDetail != MusicDetail.NONE) {
            NetworkManager.sendToPlayer(serverPlayer,
                    new SyncCurrentPlayingMessage(currentMusicDetail, nowPlayingStartTime));
        }
    }

    public void pushMusicToQueue(long musicDetailId, ServerPlayer pusher) {
        List<MusicDetail> musicDetailByIds = musicApiService.getMusicDetailByIds(List.of(musicDetailId));
        if (musicDetailByIds.size() != 1) {
            throw new IllegalStateException();
        }
        MusicDetail musicDetail = musicDetailByIds.getFirst();
        LoginApiService.PlayerLoginInfo loginInfo = LoginApiService.getInstance().loginedPlayerInfoMap.get(pusher);
        if (loginInfo != null) {
            PusherInfo pusherInfo = new PusherInfo(
                    loginInfo.profile == null ? 0 : loginInfo.profile.getUserId(),
                    pusher.getUUID(),
                    pusher.getName().getString()
            );
            musicDetail.setPusherInfo(pusherInfo);
        }
        musicQueue.add(musicDetail);
        NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                new RefreshMusicQueueMessage(musicQueue));
        updateContinuable(true);
    }

    public void removeMusicDetailFromQueue(int index, long id, ServerPlayer serverPlayer) {
        ArrayList<MusicDetail> list = new ArrayList<>(musicQueue);
        MusicDetail musicDetail = list.get(index);
        try {
            if (musicDetail.getId() == id) {
                try {
                    removeMusicInternal(index, musicDetail, serverPlayer);
                } catch (IllegalAccessException ignored) {
                    tryPreviousOne(index, id, serverPlayer, list);
                }
            } else {
                tryPreviousOne(index, id, serverPlayer, list);
            }
        } catch (RuntimeException e) {
            trySimplyRemove(musicDetail, serverPlayer);
        }
    }

    @SneakyThrows
    private void tryPreviousOne(int index, long id, ServerPlayer serverPlayer, ArrayList<MusicDetail> list) {
        if (index > 1) {//in case the queue just pulled
            MusicDetail musicDetail1 = list.get(index - 1);
            if (musicDetail1.getId() == id) {
                removeMusicInternal(index - 1, musicDetail1, serverPlayer);
            } else {
                throw new RuntimeException("failed to remove music from queue");
            }
        } else {
            throw new RuntimeException("failed to remove music from queue");
        }
    }

    private void removeMusicInternal(int index, MusicDetail musicDetail, ServerPlayer player) throws IllegalAccessException {
        if (musicDetail.getPusherInfo().playerUUID().equals(player.getUUID())) {
            AtomicInteger index1 = new AtomicInteger(0);
            musicQueue.removeIf(musicDetail1 -> index == index1.getAndIncrement() && musicDetail.equals(musicDetail1));
            NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                    new RefreshMusicQueueMessage(musicQueue));
        } else {
            throw new IllegalAccessException();
        }
    }

    private void trySimplyRemove(MusicDetail musicDetail, ServerPlayer serverPlayer) {
        if (musicDetail.getPusherInfo().playerUUID().equals(serverPlayer.getUUID())) {
            musicQueue.remove(musicDetail);
            NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                    new RefreshMusicQueueMessage(musicQueue));
        }
    }

    public void addIdlePlaySource(long playlistId, ServerPlayer player) {
        Set<Playlist> playlists = idlePlaySources.getOrDefault(player, new HashSet<>());
        playlists.add(musicApiService.getPlaylistDetail(playlistId, player));
        idlePlaySources.put(player, playlists);
        updateContinuable(true);
    }

    public void removeIdlePlaySource(long playlistId, ServerPlayer player) {
        Set<Playlist> playlists = idlePlaySources.get(player);
        if (playlists != null) {
            playlists.removeIf(playlist -> playlist.getId() == playlistId);
        }
    }

    @RegisterMark
    public static class Register implements ServerRegister {
        @Override
        public void register() {
            LoginApiService.getInstance().getLoginStateChangeListeners().add((map) -> {
                if (instance != null) {
                    instance.updateContinuable(!map.isEmpty());
                }
            });
        }
    }
}
