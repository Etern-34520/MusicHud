package indi.etern.musichud.client.ui.pages;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.LyricLine;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.musicPlayer.NowPlayingInfo;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.components.MusicListItem;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@Slf4j
public class HomeView extends LinearLayout {
    @Getter
    private static HomeView instance;
    private TextView lastHighlightLine;
    private LinearLayout lyricLinesView;
    private HashMap<LyricLine, TextView> textViewMap = new LinkedHashMap<>();
    private final Consumer<LyricLine> lyricLineUpdateListener = new Consumer<>() {
        @Override
        public void accept(LyricLine lyricLine) {
            if (lyricLine == null) {
                if (lastHighlightLine != null) {
                    lastHighlightLine.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                }
            } else {
                TextView lineTextView = textViewMap.get(lyricLine);
                if (lineTextView != null) {
                    MuiModApi.postToUiThread(() -> {
                        lineTextView.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
                        lineTextView.setTextStyle(TextPaint.BOLD);
                        if (lastHighlightLine != null) {
                            lastHighlightLine.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                            lastHighlightLine.setTextStyle(TextPaint.NORMAL);
                        }
                        lastHighlightLine = lineTextView;
                    });
                }
            }
        }
    };

    public HomeView(Context context) {
        super(context);
        refresh();
    }

    public void refresh() {
        instance = this;
        Context context = getContext();
        removeAllViews();

        boolean enabled = ClientConfigDefinition.enable.get();
        if (!MusicHud.isConnected() || !enabled) {
            setGravity(Gravity.CENTER);
            TextView textView = new TextView(context);
            textView.setTextSize(textView.dp(8f));
            int color = Theme.EMPHASIZE_TEXT_COLOR;
            textView.setTextColor(color);
            if (enabled) {
                textView.setText("需要安装了 Music Hud 的服务器支持");
            } else {
                textView.setText("Music Hud 已禁用");
            }
            textView.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            addView(textView);
            return;
        }

        setOrientation(HORIZONTAL);
        {
            LinearLayout lyricsView = new LinearLayout(context);
            lyricsView.setOrientation(VERTICAL);
            LayoutParams lyricsViewParams = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2);
            addView(lyricsView, lyricsViewParams);

            TextView title = new TextView(context);
            title.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            title.setText("歌词");
            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(32));
            lyricsView.addView(title, params);

            lyricLinesView = new LinearLayout(context);
            lyricLinesView.setOrientation(VERTICAL);
            lyricsView.addView(lyricLinesView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }
        {
            LinearLayout queueView = new LinearLayout(context);
            queueView.setOrientation(VERTICAL);
            LayoutParams queueViewParams = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3);
            queueViewParams.setMargins(dp(48), 0, 0, 0);
            addView(queueView, queueViewParams);

            TextView title = new TextView(context);
            title.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            title.setText("播放列表");
            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(32));
            queueView.addView(title, params);

            LinearLayout playQueueListView = new LinearLayout(context);
            playQueueListView.setOrientation(VERTICAL);
            LayoutParams queueViewParams1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            queueView.addView(playQueueListView, queueViewParams1);

            playQueueListView.removeAllViews();

            Queue<MusicDetail> queue = MusicService.getInstance().getMusicQueue();
            for (MusicDetail musicDetail : queue) {
                addMusicQueueItem(musicDetail, playQueueListView);
            }

            MusicService.getInstance().getMusicQueuePushListeners().add(musicDetail -> {
                MuiModApi.postToUiThread(() -> {
                    addMusicQueueItem(musicDetail, playQueueListView);
                });
            });
            MusicService.getInstance().getMusicQueueRemoveListeners().add((removeIndex, musicDetail) -> {
                MuiModApi.postToUiThread(() -> {
                    if (removeIndex >= 0 && removeIndex < playQueueListView.getChildCount()) {
                        playQueueListView.removeViewAt(removeIndex);
                    }
                });
            });

            ArrayDeque<LyricLine> lyricLines = NowPlayingInfo.getInstance().getLyricLines();
            if (lyricLines != null && !lyricLines.isEmpty()) {
                switchMusic(lyricLines);
            }

            NowPlayingInfo.getInstance().getLyricLineUpdateListener().add(lyricLineUpdateListener);

            addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    lyricLineUpdateListener.accept(NowPlayingInfo.getInstance().getCurrentLyricLine());
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    NowPlayingInfo.getInstance().getLyricLineUpdateListener().remove(lyricLineUpdateListener);
                    instance = null;
                }
            });
        }
    }

    private void addMusicQueueItem(MusicDetail musicDetail, LinearLayout playQueueView) {
        MusicListItem item = new MusicListItem(getContext(), musicDetail);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, dp(16));
        LinearLayout actions = new LinearLayout(getContext());

        if (musicDetail.getPusherInfo().uid() == Profile.getCurrent().getUserId()) {
            Button removeButton = new Button(getContext());
            removeButton.setText("移除");
            removeButton.setTextSize(dp(8));
            removeButton.setTextColor(Theme.SECONDARY_TEXT_COLOR);
            Drawable background = ButtonInsetBackground.builder()
                    .inset(1)
                    .padding(new ButtonInsetBackground.Padding(dp(8), dp(2), dp(2), dp(8)))
                    .cornerRadius(dp(4))
                    .build().get();
            removeButton.setBackground(background);
            removeButton.setOnClickListener(v -> {
                MusicService.getInstance().sendRemoveMusicFromQueue(playQueueView.indexOfChild(item), musicDetail);
            });
            actions.addView(removeButton, new LayoutParams(WRAP_CONTENT, dp(52)));
        }
        item.addView(actions);
        item.setLayoutParams(layoutParams);
        playQueueView.addView(item, layoutParams);
    }

    public void switchMusic(Queue<LyricLine> lyricLines) {
        MuiModApi.postToUiThread(() -> {
            textViewMap.clear();
            if (lyricLinesView != null) {
                lyricLinesView.removeAllViews();
                if (lyricLines != null) {
                    for (LyricLine lyricLine : lyricLines) {
                        Context context = getContext();
                        if (context != null) {
                            String text = lyricLine.getText();
                            String translatedText = lyricLine.getTranslatedText();
                            if ((text == null || text.isEmpty()) && (translatedText == null || translatedText.isEmpty())) {
                                continue;
                            }
                            TextView lyricText = new TextView(context);
                            if (lyricLine.equals(NowPlayingInfo.getInstance().getCurrentLyricLine())) {
                                lyricText.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
                                lyricText.setTextStyle(TextPaint.BOLD);
                                lastHighlightLine = lyricText;
                            } else {
                                lyricText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                            }
                            lyricText.setText(text == null ? "" : text);
                            textViewMap.put(lyricLine, lyricText);

                            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                            params.setMargins(0, 0, 0, dp(8));

                            if (translatedText != null) {
                                lyricLinesView.addView(lyricText);
                                TextView subLyricText = new TextView(context);
                                subLyricText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                                subLyricText.setTextSize(dp(8));
                                subLyricText.setText(translatedText);
                                lyricLinesView.addView(subLyricText, params);
                            } else {
                                lyricLinesView.addView(lyricText, params);
                            }
                        }
                    }
                }
            }
        });
    }
}
