package indi.etern.musichud.client.musicPlayer;

import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.LyricInfo;
import indi.etern.musichud.beans.music.LyricLine;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.MusicResourceInfo;
import indi.etern.musichud.client.ui.hud.HudRendererManager;
import indi.etern.musichud.client.ui.screen.MainFragment;
import indi.etern.musichud.client.ui.utils.LyricDecoder;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class NowPlayingInfo {
    private static volatile NowPlayingInfo instance = null;
    private final Logger logger = MusicHud.getLogger(NowPlayingInfo.class);
    @Getter
    private final Set<Consumer<LyricLine>> lyricLineUpdateListener = new HashSet<>();
    @Getter
    private final Set<BiConsumer<MusicDetail, MusicDetail>> musicSwitchListener = new HashSet<>();
    @Getter
    private MusicDetail currentlyPlayingMusicDetail;
    @Getter
    private MusicResourceInfo currentlyPlayingMusicResourceInfo;
    private volatile Duration musicDuration = null;
    private volatile LocalDateTime musicStartTime = null;
    private ArrayDeque<LyricLine> lyricLines;
    @Getter
    private LyricLine currentLyricLine;
    private Thread lyricUpdaterThread;

    public static NowPlayingInfo getInstance() {
        if (instance == null) {
            synchronized (NowPlayingInfo.class) {
                if (instance == null) {
                    instance = new NowPlayingInfo();
                }
            }
        }
        return instance;
    }

    public float getProgressRate() {
        if (musicDuration == null || musicStartTime == null) {
            return 0.0f;
        }
        return (float) Duration.between(musicStartTime, LocalDateTime.now()).toMillis() / musicDuration.toMillis();
    }

    public void switchMusic(MusicDetail musicDetail, MusicResourceInfo resourceInfo, LocalDateTime musicStartTime) {
        MusicDetail previous = currentlyPlayingMusicDetail;
        currentlyPlayingMusicDetail = musicDetail;
        currentlyPlayingMusicResourceInfo = resourceInfo;
        this.musicDuration = Duration.ofMillis(musicDetail.getDurationMillis());
        this.musicStartTime = musicStartTime;
        LyricInfo lyricInfo = resourceInfo.getLyricInfo();
        Queue<LyricLine> lyricLines = null;
        if (!lyricInfo.equals(LyricInfo.NONE)) {
            try {
                lyricLines = LyricDecoder.decode(lyricInfo);
                this.lyricLines = new ArrayDeque<>(lyricLines);
            } catch (Exception e) {
                logger.warn("Failed to load lyrics of music: {} (id:{}) ", musicDetail.getName(), musicDetail.getId());
            }
        } else {
            this.lyricLines = null;
        }
        MuiModApi.postToUiThread(() -> MainFragment.switchMusic(musicDetail, this.lyricLines));
        HudRendererManager.getInstance().switchMusic(musicDetail);
        if (lyricLines != null && !lyricLines.isEmpty()) {
            Queue<LyricLine> finalLyricLines = lyricLines;
            if (lyricUpdaterThread != null) {
                lyricUpdaterThread.interrupt();
            }
            Runnable lyricUpdater = () -> {
                Thread thread = Thread.currentThread();
                lyricUpdaterThread = thread;
                thread.setName("Lyrics Updater");
                while (!finalLyricLines.isEmpty() && musicDetail.equals(currentlyPlayingMusicDetail)) {
                    LyricLine line = finalLyricLines.peek();
                    if (line != null) {
                        if (line.getStartTime() != null) {
                            if (LocalDateTime.now().isAfter(musicStartTime.plus(line.getStartTime()))) {
                                finalLyricLines.poll();
                                currentLyricLine = line;
                                LyricLine next = finalLyricLines.peek();
                                if (next == null) {
                                    callLyricsUpdateListeners(line);
                                    logger.info("lyricsUpdater stopped due to no more lyrics");
                                    break;
                                } else if (LocalDateTime.now().isBefore(musicStartTime.plus(next.getStartTime()))) {
                                    callLyricsUpdateListeners(line);
                                    Duration startTime = next.getStartTime();
                                    if (sleepUntil(musicStartTime, startTime)) break;
                                }
                            } else {
                                if (sleepUntil(musicStartTime, line.getStartTime())) break;
                            }
                        } else {
                            finalLyricLines.poll();
                        }
                    }
                }
                lyricUpdaterThread = null;
            };
            MusicHud.EXECUTOR.execute(lyricUpdater);
            musicSwitchListener.forEach(consumer -> {
                consumer.accept(previous, musicDetail);
            });
        }
    }

    private void callLyricsUpdateListeners(LyricLine line) {
        lyricLineUpdateListener.forEach(c -> {
            try {
                c.accept(line);
            } catch (Exception e) {
                logger.warn(e);
            }
        });
    }

    private boolean sleepUntil(LocalDateTime musicStartTime, Duration startTime) {
        Duration between = Duration.between(LocalDateTime.now(), musicStartTime.plus(startTime));
        if (between.isPositive()) {
            try {
                Thread.sleep(between);
            } catch (InterruptedException ignored) {
                logger.warn("interrupted");
                return true;
            }
        }
        return false;
    }

    public Duration getPlayedDuration() {
        if (musicStartTime == null) {
            return Duration.ZERO;
        }
        Duration startedPlayingDuration = Duration.between(musicStartTime, LocalDateTime.now());
        if (startedPlayingDuration.compareTo(musicDuration) > 0) {
            return musicDuration;
        } else {
            return startedPlayingDuration;
        }
    }

    public PlayerInfo getPusherPlayerInfo() {//TODO
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            throw new IllegalStateException();
        }
        if (currentlyPlayingMusicDetail != null) {
            return connection.getPlayerInfo(currentlyPlayingMusicDetail.getPusherInfo().playerUUID());
        } else {
            return null;
        }
    }

    public void stop() {
        if (lyricUpdaterThread != null) {
            lyricUpdaterThread.interrupt();
        }
        lyricUpdaterThread = null;
        currentlyPlayingMusicDetail = null;
        currentlyPlayingMusicResourceInfo = null;
        musicDuration = null;
        musicStartTime = null;
        lyricLines = null;
        currentLyricLine = null;
    }
}