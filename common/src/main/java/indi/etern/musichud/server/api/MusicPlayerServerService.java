package indi.etern.musichud.server.api;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.MusicResourceInfo;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.beans.music.PusherInfo;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.interfaces.ServerRegister;
import indi.etern.musichud.network.pushMessages.s2c.RefreshMusicQueueMessage;
import indi.etern.musichud.network.pushMessages.s2c.SwitchMusicMessage;
import indi.etern.musichud.network.pushMessages.s2c.SyncCurrentPlayingMessage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
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
    private int musicIntervalMillis = 2000;
    @Getter
    private volatile MusicDetail currentMusicDetail = MusicDetail.NONE;
    @Getter
    private volatile LocalDateTime nowPlayingStartTime = LocalDateTime.MIN;

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
            synchronized (MusicPlayerServerService.class) {
                if (musicDataPusher == null) {
                    AtomicReference<Runnable> runnableReference = new AtomicReference<>();
                    runnableReference.set(new Runnable() {
                        private MusicDetail lastRandom = MusicDetail.NONE;

                        @Override
                        public void run() {
                            Thread.currentThread().setName("Music Data Pusher Thread");
                            while (MusicPlayerServerService.this.continuable && musicDataPusher == runnableReference.get()) {
                                MusicDetail nextToPlay = null;
                                MusicDetail preloadMusicDetail;
                                try {
                                    if (musicQueue.isEmpty()) {
                                        Optional<MusicDetail> optionalMusicDetail = getRandomMusicFromIdleSources();
                                        if (optionalMusicDetail.isEmpty()) {
                                            NetworkManager.sendToPlayers(
                                                    LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                                                    new SwitchMusicMessage(MusicDetail.NONE, MusicResourceInfo.NONE, MusicDetail.NONE, MusicResourceInfo.NONE)
                                            );
                                            MusicPlayerServerService.this.stopSendingMusic();
                                            break;
                                        } else {
                                            MusicDetail musicDetail = optionalMusicDetail.get();
                                            if (lastRandom == null || lastRandom.equals(MusicDetail.NONE)) {
                                                lastRandom = musicDetail;
                                                Optional<MusicDetail> optionalMusicDetail1 = getRandomMusicFromIdleSources();
                                                if (optionalMusicDetail1.isPresent()) {
                                                    nextToPlay = lastRandom;
                                                    preloadMusicDetail = optionalMusicDetail1.get();
                                                    lastRandom = preloadMusicDetail;
                                                } else {
                                                    nextToPlay = musicDetail;
                                                    lastRandom = null;
                                                }
                                            } else {
                                                nextToPlay = lastRandom;
                                                lastRandom = musicDetail;
                                            }
                                        }
                                    } else {
                                        nextToPlay = musicQueue.remove();
                                        NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                                                new RefreshMusicQueueMessage(musicQueue));
                                    }

                                    MusicResourceInfo resourceInfo = musicApiService.getResourceInfo(nextToPlay);
                                    MusicResourceInfo preloadResourceInfo;
                                    if (!musicQueue.isEmpty()) {
                                        preloadMusicDetail = musicQueue.getFirst();
                                    } else {
                                        preloadMusicDetail = lastRandom;
                                    }

                                    if (resourceInfo != null) {
                                        preloadResourceInfo = musicApiService.getResourceInfo(preloadMusicDetail);
                                        NetworkManager.sendToPlayers(
                                                LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                                                new SwitchMusicMessage(nextToPlay, resourceInfo, preloadMusicDetail, preloadResourceInfo)
                                        );
                                        currentMusicDetail = nextToPlay;
                                        nowPlayingStartTime = LocalDateTime.now();
                                        logger.info("Switched to music: {} (ID: {})", nextToPlay.getName(), nextToPlay.getId());
                                        try {
                                            //noinspection BusyWait
                                            Thread.sleep(nextToPlay.getDurationMillis() + musicIntervalMillis);
                                        } catch (InterruptedException ignored) {
                                            logger.warn("Music data pusher interrupted");
                                            MusicPlayerServerService.this.stopSendingMusic();
                                            break;
                                        }
                                    } else {
                                        logger.warn("Failed to get resource info for music ID: {}", nextToPlay.getId());
                                    }
                                } catch (Exception e) {
                                    logger.error("Failed to push music: {}", nextToPlay != null ? nextToPlay.getName() : "", e);
                                }
                            }
                        }

                        private Optional<MusicDetail> getRandomMusicFromIdleSources() {
                            Optional<Map.Entry<ServerPlayer, Set<Playlist>>> optionalRandomIdlePlaySource = idlePlaySources.entrySet().stream()
                                    .sorted((a, b) -> MusicHud.RANDOM.nextInt(-1, 1))
                                    .findAny();
                            if (optionalRandomIdlePlaySource.isPresent()) {
                                Map.Entry<ServerPlayer, Set<Playlist>> serverPlayerSetEntry = optionalRandomIdlePlaySource.get();
                                ServerPlayer sourcePlayer = serverPlayerSetEntry.getKey();
                                Optional<MusicDetail> musicDetailOptional = serverPlayerSetEntry.getValue().stream()
                                        .flatMap(playlist -> playlist.getTracks().stream())
                                        .sorted((a, b) -> MusicHud.RANDOM.nextInt(-1, 1))
                                        .findAny();
                                musicDetailOptional.ifPresent(musicDetail -> {
                                            LoginApiService.PlayerLoginInfo loginInfo =
                                                    LoginApiService.getInstance().getLoginInfoByServerPlayer(sourcePlayer);
                                            PusherInfo pusherInfo = new PusherInfo(
                                                    loginInfo.profile.getUserId(),
                                                    sourcePlayer.getUUID(),
                                                    sourcePlayer.getName().getString()
                                            );
                                            musicDetail.setPusherInfo(pusherInfo);
                                        }
                                );
                                return musicDetailOptional;
                            } else {
                                return Optional.empty();
                            }
                        }
                    });
                    musicDataPusher = runnableReference.get();
                    MusicHud.EXECUTOR.execute(runnableReference.get());
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
                new SwitchMusicMessage(MusicDetail.NONE, MusicResourceInfo.NONE, MusicDetail.NONE, MusicResourceInfo.NONE)
        );
    }

    public void sendSyncPlayingStatusToPlayer(ServerPlayer serverPlayer) {
        NetworkManager.sendToPlayer(serverPlayer,
                new RefreshMusicQueueMessage(musicQueue));
        if (currentMusicDetail != MusicDetail.NONE) {
            MusicResourceInfo resourceInfo = musicApiService.getResourceInfo(currentMusicDetail);
            if (resourceInfo != null) {
                NetworkManager.sendToPlayer(serverPlayer,
                        new SyncCurrentPlayingMessage(currentMusicDetail, resourceInfo, nowPlayingStartTime));
            } else {
                logger.warn("Failed to get resource info for current playing music ID: {}", currentMusicDetail.getId());
            }
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
                    loginInfo.profile.getUserId(),
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
        long userId = LoginApiService.getInstance().getLoginInfoByServerPlayer(player).profile.getUserId();
        if (musicDetail.getPusherInfo().uid() == userId) {
            AtomicInteger index1 = new AtomicInteger(0);
            musicQueue.removeIf(musicDetail1 -> index == index1.getAndIncrement() && musicDetail.equals(musicDetail1));
            NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                    new RefreshMusicQueueMessage(musicQueue));
        } else {
            throw new IllegalAccessException();
        }
    }

    private void trySimplyRemove(MusicDetail musicDetail, ServerPlayer serverPlayer) {
        int index = 0;
        for (MusicDetail detail : musicQueue) {
            if (detail.equals(musicDetail)) {
                break;
            }
            index++;
        }
        musicQueue.remove(musicDetail);
        NetworkManager.sendToPlayers(LoginApiService.getInstance().loginedPlayerInfoMap.keySet(),
                new RefreshMusicQueueMessage(musicQueue));
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

    @ForceLoad
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
