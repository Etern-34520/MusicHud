package indi.etern.musichud.client.ui.hud;

import com.mojang.blaze3d.systems.RenderPass;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.Artist;
import indi.etern.musichud.beans.music.LyricLine;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.musicPlayer.NowPlayingInfo;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.hud.metadata.*;
import indi.etern.musichud.client.ui.hud.renderer.*;
import indi.etern.musichud.client.ui.utils.TransitionStatus;
import indi.etern.musichud.client.ui.utils.image.ImageBlurPostProcessor;
import indi.etern.musichud.client.ui.utils.image.ImageTextureData;
import indi.etern.musichud.client.ui.utils.image.ImageUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

public class HudRendererManager {
    private static volatile HudRendererManager instance;
    @Getter
    private static volatile boolean loaded;
    private final BackgroundRenderer HUD_RENDERER = BackgroundRenderer.getInstance();
    private final AlbumImageRenderer IMAGE_RENDERER = AlbumImageRenderer.getInstance();
    private final PlayerHeadRenderer PLAYER_HEAD_RENDERER = PlayerHeadRenderer.getInstance();
    private final ProgressRenderer PROGRESS_RENDERER = ProgressRenderer.getInstance();
    private final TextRenderer TITLE_RENDERER = new TextRenderer();
    private final TextRenderer LYRICS_RENDERER = new TextRenderer();
    private final TextRenderer SUB_LYRICS_RENDERER = new TextRenderer();
    private final TextRenderer ARTISTS_AND_ALBUM_RENDERER = new TextRenderer();
    private final TextRenderer PLAY_TIME_RENDERER = new TextRenderer();
    private final NowPlayingInfo nowPlayingInfo = NowPlayingInfo.getInstance();
    private volatile HudRenderData hudBaseData;
    private volatile HudRenderData imageDisplayData;
    private volatile HudRenderData progressDisplayData;
    @Setter
    private volatile Layout baseLayout;
    @Setter
    private volatile BackgroundColor bgColor;

    protected HudRendererManager() {
        nowPlayingInfo.getLyricLineUpdateListener().add((lyricLine) -> {
            String text = lyricLine.getText();
            LYRICS_RENDERER.setText(text);
            String translatedText = lyricLine.getTranslatedText();
            SUB_LYRICS_RENDERER.setText(translatedText);
        });
    }

    public static HudRendererManager getInstance() {
        if (instance == null) {
            synchronized (HudRendererManager.class) {
                if (instance == null) {
                    instance = new HudRendererManager();
                    BackgroundColor bgColor = new BackgroundColor(
                            0xA01A1A1A, 0xF0202020,
                            0X80252525, 0xE42A2A2A
                    );
                    instance.setBgColor(bgColor);
                    instance.updateLayoutFromConfig();
                    instance.refreshStyle();

                    instance.TITLE_RENDERER.setText("暂无播放音乐");
                    loaded = true;
                }
            }
        }
        return instance;
    }

    public void updateLayoutFromConfig() {
        Layout layout = new Layout(
                ClientConfigDefinition.hudOffsetX.get(),
                ClientConfigDefinition.hudOffsetY.get(),
                ClientConfigDefinition.hudWidth.get(),
                ClientConfigDefinition.hudHeight.get(),
                ClientConfigDefinition.hudCornerRadius.get(),
                HPosition.valueOf(ClientConfigDefinition.hudHorizontalPosition.get()),
                VPosition.valueOf(ClientConfigDefinition.hudVerticalPosition.get())
        );
        setBaseLayout(layout);
    }

    public void refreshStyle() {
        if (baseLayout.radius > baseLayout.height / 2) {
            baseLayout.radius = baseLayout.height / 2;
        }


        BackgroundImage bgImage = getBackgroundImageOrElse(new BackgroundImage(null, null, 1));
        float padding = 4f;
        configureBaseRenderer(baseLayout, bgColor, bgImage);

        Layout baseLayout = hudBaseData.getLayout();

        float imageHeightAndWidth = baseLayout.height - 2 * padding;
        float imageRadius = Math.min(Math.max(0, baseLayout.radius - padding), imageHeightAndWidth / 2f);
        Layout imageLayout = new Layout(padding, padding, imageHeightAndWidth, imageHeightAndWidth, imageRadius);
        imageLayout.setParent(baseLayout);

        configureImageRenderer(imageLayout, bgImage);

        float progressWidth = baseLayout.width - imageHeightAndWidth - 3 * padding - baseLayout.radius / 2;
        float progressHeight = 2;
        float progressX = padding + imageHeightAndWidth + padding;
        float progressY = padding + imageHeightAndWidth - progressHeight - 1;
        float progressRadius = Math.min(Math.max(0, baseLayout.radius - padding), progressHeight / 2);
        Layout progressLayout = new Layout(progressX, progressY, progressWidth, progressHeight, progressRadius);
        progressLayout.setParent(baseLayout);

        configureProgressRenderer(progressLayout);

        float headSize = 8f;
        float headX = Math.max(progressX + progressWidth - headSize, imageHeightAndWidth + padding - headSize);
        float headY = padding + 2f;

        Layout layout1 = new Layout(headX, headY, headSize, headSize, 0f);
        layout1.setParent(baseLayout);
        PLAYER_HEAD_RENDERER.configureLayout(layout1);

        float titleSize = 7f;
        float normalTextSize = 5f;
        float rest = baseLayout.height - padding * 2 - titleSize - progressHeight - normalTextSize - 2;
        float lyricsSize = rest <= 8f ? 0 : 6f;
        float subLyricsSize = rest <= 14f ? 0 : 5f;
        float lyricsY = padding + 10f;
        float textStartX = progressX;
        float titleY = padding + 1f;
        float aboveProgressY = progressY - normalTextSize - 1f;
        float progressRightX = progressX + progressWidth;

        Layout titleLayout = Layout.ofTextLayout(textStartX, titleY, progressWidth - 8f, titleSize);
        titleLayout.setParent(baseLayout);
        TITLE_RENDERER.configureLayout(titleLayout, Theme.EMPHASIZE_TEXT_COLOR, TextRenderer.Position.LEFT);

        Layout lyricsLayout = Layout.ofTextLayout(textStartX, lyricsY, progressWidth, lyricsSize);
        lyricsLayout.setParent(baseLayout);
        Layout subLyricsLayout = Layout.ofTextLayout(textStartX, lyricsY + lyricsSize + 1, progressWidth, subLyricsSize);
        subLyricsLayout.setParent(baseLayout);
        LYRICS_RENDERER.configureLayout(lyricsLayout, Theme.NORMAL_TEXT_COLOR, TextRenderer.Position.LEFT);
        SUB_LYRICS_RENDERER.configureLayout(subLyricsLayout, Theme.SECONDARY_TEXT_COLOR, TextRenderer.Position.LEFT);

//        LYRICS_RENDERER.setText("This is EN lyrics");
//        SUB_LYRICS_RENDERER.setText("芝士歌词");

        Layout artistAndAlbumLayout = Layout.ofTextLayout(textStartX, aboveProgressY, progressWidth, normalTextSize);
        artistAndAlbumLayout.setParent(baseLayout);
        Layout playTimeLayout = Layout.ofTextLayout(progressRightX, aboveProgressY, progressWidth, normalTextSize);
        playTimeLayout.setParent(baseLayout);
        ARTISTS_AND_ALBUM_RENDERER.configureLayout(artistAndAlbumLayout, Theme.SECONDARY_TEXT_COLOR, TextRenderer.Position.LEFT);
        PLAY_TIME_RENDERER.configureLayout(playTimeLayout, Theme.SECONDARY_TEXT_COLOR, TextRenderer.Position.RIGHT);

        HudRenderData.getTransitionStatus().setOnCompleteCallback(nextData -> {
            bgImage.currentBlurredLocation = nextData.nextedBlurred();
            bgImage.currentUnblurredLocation = nextData.nextUnblurred();
            bgImage.currentAspect = nextData.nextAspect();
        });
    }

    private void configureProgressRenderer(Layout layout) {
        if (progressDisplayData == null) {
            progressDisplayData = new HudRenderData(layout, null, null);
            progressDisplayData.setProgressBar(new ProgressBar(
                    0x00A0A0A0,
                    0x50FFFFFF,
                    0x40A0A0A0,
                    12f,
                    2f,
                    0.01f
            ));
        } else {
            progressDisplayData.setLayout(layout);
        }
        PROGRESS_RENDERER.configure(progressDisplayData);
    }

    private void configureImageRenderer(Layout imageLayout, BackgroundImage bgImage) {
        if (imageDisplayData == null) {
            imageDisplayData = new HudRenderData(imageLayout, null, bgImage);
        } else {
            imageDisplayData.setLayout(imageLayout);
        }
        IMAGE_RENDERER.configure(imageDisplayData);
    }

    private void configureBaseRenderer(@NotNull Layout layout, @NotNull BackgroundColor bgColor, BackgroundImage bgImage) {
        if (hudBaseData == null) {
            hudBaseData = new HudRenderData(layout, bgColor, bgImage);
        } else {
            hudBaseData.setLayout(layout);
            hudBaseData.setBackgroundColor(bgColor);
        }
        HUD_RENDERER.configure(hudBaseData);
    }

    private BackgroundImage getBackgroundImageOrElse(BackgroundImage bgImage) {
        if (hudBaseData != null && hudBaseData.getBackgroundImage() != null) {
            bgImage = hudBaseData.getBackgroundImage();
        }
        return bgImage;
    }

    public void switchMusic(MusicDetail musicDetail) {
        if (musicDetail == null || musicDetail.equals(MusicDetail.NONE)) {
            reset();
        } else {
            TITLE_RENDERER.setText(musicDetail.getName());
            String artists = musicDetail.getArtists().stream()
                    .map(Artist::getName)
                    .reduce((a, b) -> a + " / " + b)
                    .orElse("");
            ARTISTS_AND_ALBUM_RENDERER.setText(artists + " - " + musicDetail.getAlbum().getName());
            LYRICS_RENDERER.setText("");
            SUB_LYRICS_RENDERER.setText("");
            ImageUtils.downloadAsync(musicDetail.getAlbum().getPicUrl())
                    .thenAccept(imageTextureData -> {
                        imageTextureData.register().thenAcceptAsync((v) -> {
                            MusicHud.EXECUTOR.execute(() -> {
                                ImageTextureData blurredImageTextureData = ImageBlurPostProcessor.blur(imageTextureData, 256);
                                blurredImageTextureData.register().thenAccept((v1) -> Minecraft.getInstance().execute(() -> {
                                    if (musicDetail.equals(nowPlayingInfo.getCurrentlyPlayingMusicDetail())) {
                                        var nextData = new TransitionNextData(blurredImageTextureData.getLocation(), imageTextureData.getLocation(), 1f);
                                        HudRenderData.getTransitionStatus().startTransition(nextData);
                                    }
                                }));
                            });
                        });
                    });
        }
    }

    public void reset() {
        TITLE_RENDERER.setText("暂无播放音乐");
        ARTISTS_AND_ALBUM_RENDERER.setText("");
        LYRICS_RENDERER.setText("");
        SUB_LYRICS_RENDERER.setText("");
        var nextData = new TransitionNextData(null, null, 1f);
        TransitionStatus<TransitionNextData> transitionStatus = HudRenderData.getTransitionStatus();
        transitionStatus.startTransition(nextData);
    }

    public void renderFrame(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientConfigDefinition.enable.get()) {
            return;
        }
        NowPlayingInfo nowPlayingInfo = this.nowPlayingInfo;
        MusicDetail musicDetail = nowPlayingInfo.getCurrentlyPlayingMusicDetail();
        if ((musicDetail == null || musicDetail.equals(MusicDetail.NONE)) &&
                ClientConfigDefinition.hideHudWhenNotPlaying.get()) {
            return;
        }
        ProgressBar progressBar = progressDisplayData.getProgressBar();

        HudRenderData.getTransitionStatus().updateTransition();
        progressBar.setProgress(nowPlayingInfo.getProgressRate());

        Duration playedDuration = nowPlayingInfo.getPlayedDuration();
        Duration musicDuration = nowPlayingInfo.getMusicDuration();
        if (playedDuration != null && musicDuration != null && !musicDuration.isZero()) {
            DateTimeFormatter formatter = musicDuration.toHoursPart() >= 1 ?
                    DateTimeFormatter.ofPattern("HH:mm:ss") :
                    DateTimeFormatter.ofPattern("mm:ss");
            String playTimeString = formatter.format(
                    java.time.LocalTime.MIDNIGHT.plusSeconds(playedDuration.toSeconds())
            ) + " / " + formatter.format(
                    java.time.LocalTime.MIDNIGHT.plusSeconds(musicDuration.toSeconds())
            );
            PLAY_TIME_RENDERER.setText(playTimeString);
        }

        PlayerInfo pusherPlayerInfo = this.nowPlayingInfo.getPusherPlayerInfo();
        PLAYER_HEAD_RENDERER.setPlayerInfo(pusherPlayerInfo);

        HUD_RENDERER.render(graphics, deltaTracker);
        IMAGE_RENDERER.render(graphics, deltaTracker);
        PLAYER_HEAD_RENDERER.render(graphics, deltaTracker);
        PROGRESS_RENDERER.render(graphics, deltaTracker);

        TITLE_RENDERER.render(graphics, deltaTracker);
        LYRICS_RENDERER.render(graphics, deltaTracker);
        SUB_LYRICS_RENDERER.render(graphics, deltaTracker);

        float progressWidth = PROGRESS_RENDERER.getCurrentData().getLayout().width;
        ARTISTS_AND_ALBUM_RENDERER.getLayout().width = progressWidth - PLAY_TIME_RENDERER.calcDisplayWidth() - 1f;
        ARTISTS_AND_ALBUM_RENDERER.render(graphics, deltaTracker);
        PLAY_TIME_RENDERER.render(graphics, deltaTracker);
    }

    public void updateRenderPass(RenderPass renderPass) {
        HUD_RENDERER.updateRenderPass(renderPass);
        IMAGE_RENDERER.updateRenderPass(renderPass);
        PROGRESS_RENDERER.updateRenderPass(renderPass);
    }
}
