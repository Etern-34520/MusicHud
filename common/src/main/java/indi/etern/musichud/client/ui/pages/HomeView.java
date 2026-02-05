package indi.etern.musichud.client.ui.pages;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.ScrollController;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.LyricLine;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.music.NowPlayingInfo;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.components.MusicListItem;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@Slf4j
public class HomeView extends LinearLayout {
    private static final int AUTO_RECENTER_DELAY = 3000; // 3秒后自动归位
    @Getter
    private static HomeView instance;
    private final ScrollController lyricScrollController;
    private TextView lastHighlightLine;
    private LinearLayout lyricLinesView;
    private final HashMap<LyricLine, TextView> textViewMap = new LinkedHashMap<>();
    private ScrollView lyricScrollView;
    private long lastUserScrollTime = 0;
    private boolean isUserManuallyScrolling = false;
    // 标记是否已经初始化滚动
    private boolean hasInitializedScroll = false;
    // 标记是否正在进行归位滚动
    private boolean isRecenterScroll = false;
    // 记录当前滚动位置
    private int currentScrollPosition = 0;
    // 标记是否正在进行自动滚动（由滚动控制器引起的）
    private boolean isAutoScrolling = false;
    private final Runnable autoRecenterRunnable = new Runnable() {
        @Override
        public void run() {
            // 如果用户已经停止了手动滚动一段时间
            if (isUserManuallyScrolling && System.currentTimeMillis() - lastUserScrollTime >= AUTO_RECENTER_DELAY) {
                isUserManuallyScrolling = false;
                isRecenterScroll = true; // 标记为归位滚动

                // 自动归位到当前歌词
                LyricLine currentLyric = NowPlayingInfo.getInstance().getCurrentLyricLine();
                if (currentLyric != null) {
                    TextView currentTextView = textViewMap.get(currentLyric);
                    if (currentTextView != null) {
                        scrollToLyric(currentTextView);
                    }
                }
            } else if (isUserManuallyScrolling) {
                // 继续等待
                postDelayed(this, 100);
            }
        }
    };
    private final Consumer<LyricLine> lyricLineUpdateListener = new Consumer<>() {
        @Override
        public void accept(LyricLine lyricLine) {
            if (lyricLine == null) {
                if (lastHighlightLine != null) {
                    lastHighlightLine.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                    lastHighlightLine.setTextStyle(TextPaint.NORMAL);
                }
            } else {
                TextView lineTextView = textViewMap.get(lyricLine);
                if (lineTextView != null) {
                    MuiModApi.postToUiThread(() -> {
                        lineTextView.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
                        lineTextView.setTextStyle(TextPaint.BOLD);
                        if (lastHighlightLine != null && lastHighlightLine != lineTextView) {
                            lastHighlightLine.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                            lastHighlightLine.setTextStyle(TextPaint.NORMAL);
                        }
                        lastHighlightLine = lineTextView;

                        // 只有在用户没有手动滚动时才自动滚动
                        if (!isUserManuallyScrolling) {
                            scrollToLyric(lineTextView);
                        }
                    });
                }
            }
        }
    };

    public HomeView(Context context) {
        super(context);

        // 初始化滚动控制器的监听器
        // 用于存储滚动控制器的监听器
        ScrollController.IListener scrollListener = (controller, amount) -> {
            if (lyricScrollView != null) {
                lyricScrollView.scrollTo(0, (int) amount);
                currentScrollPosition = (int) amount;
            }
        };

        // 初始化滚动控制器
        lyricScrollController = new ScrollController(scrollListener);

        refresh();
    }

    private void setupScrollListener() {
        if (lyricScrollView == null) return;

        lyricScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY != oldScrollY) {
                currentScrollPosition = scrollY;

                // 检查是否是滚动控制器引起的滚动
                if (isAutoScrolling) {
                    // 这是自动滚动，不标记为用户手动滚动
                    return;
                }

                long currentTime = System.currentTimeMillis();

                isUserManuallyScrolling = true;
                lastUserScrollTime = currentTime;
                isRecenterScroll = false; // 重置归位标记

                // 取消之前的自动归位计时器
                removeCallbacks(autoRecenterRunnable);

                // 设置新的自动归位计时器
                postDelayed(autoRecenterRunnable, AUTO_RECENTER_DELAY);

                // 用户滚动时，如果正在自动滚动，停止自动滚动
                if (lyricScrollController.isScrolling()) {
                    lyricScrollController.abortAnimation();
                    isAutoScrolling = false;
                }
            }
        });
    }

    public void refresh() {
        instance = this;
        Context context = getContext();
        removeAllViews();

        boolean enabled = ClientConfigDefinition.enable.get();
        if (!MusicHud.isConnected() || !enabled) {
            setGravity(Gravity.CENTER);
            TextView textView = Theme.getNotificationTextView(context, enabled);
            addView(textView);
            return;
        }

        setOrientation(HORIZONTAL);
        {
            LinearLayout lyricsView = new LinearLayout(context);
            lyricsView.setOrientation(VERTICAL);
            LayoutParams lyricsViewParams = new LayoutParams(0, MATCH_PARENT, 1);
            addView(lyricsView, lyricsViewParams);

            TextView title = new TextView(context);
            title.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            title.setText("歌词");
            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(32));
            lyricsView.addView(title, params);

            lyricScrollView = new ScrollView(context);
            lyricScrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            lyricScrollView.setVerticalScrollBarEnabled(false);
            lyricScrollView.setHorizontalScrollBarEnabled(false);
            lyricScrollView.setFillViewport(true);
            lyricsView.addView(lyricScrollView, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

            LinearLayout lyricLinesWrapper = new LinearLayout(context);
            lyricLinesWrapper.setOrientation(VERTICAL);

            lyricLinesView = new LinearLayout(context);
            lyricLinesView.setOrientation(VERTICAL);
            LayoutParams params1 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params1.setMargins(0, 0, 0, dp(256));
            lyricLinesWrapper.addView(lyricLinesView, params1);

            lyricScrollView.addView(lyricLinesWrapper, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            // 重新设置监听器
            setupScrollListener();
        }
        {
            LinearLayout queueView = new LinearLayout(context);
            queueView.setOrientation(VERTICAL);
            LayoutParams queueViewParams = new LayoutParams(0, MATCH_PARENT, 1);
            queueViewParams.setMargins(dp(48), 0, 0, 0);
            addView(queueView, queueViewParams);

            TextView title = new TextView(context);
            title.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            title.setText("播放列表");
            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(32));
            queueView.addView(title, params);

            var scrollView = new ScrollView(context);
            scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            scrollView.setFillViewport(true);
            queueView.addView(scrollView, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

            LinearLayout playQueueListView = new LinearLayout(context);
            playQueueListView.setOrientation(VERTICAL);
            LayoutParams queueViewParams1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            scrollView.addView(playQueueListView, queueViewParams1);

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
                    // 视图附加到窗口后，初始化滚动到当前歌词
                    initializeScrollToCurrentLyric();
                    // 开始更新滚动控制器
                    startScrollControllerUpdate();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    NowPlayingInfo.getInstance().getLyricLineUpdateListener().remove(lyricLineUpdateListener);

                    // 清理资源
                    stopScrollControllerUpdate();
                    if (lyricScrollController != null) {
                        lyricScrollController.abortAnimation();
                    }
                    removeCallbacks(autoRecenterRunnable);
                    instance = null;
                }
            });
        }
    }

    private void initializeScrollToCurrentLyric() {
        // 重置初始化标记
        hasInitializedScroll = false;

        LyricLine currentLyric = NowPlayingInfo.getInstance().getCurrentLyricLine();
        if (currentLyric != null) {
            TextView currentTextView = textViewMap.get(currentLyric);
            if (currentTextView != null && !hasInitializedScroll) {
                hasInitializedScroll = true;
                // 第一次加载直接跳转，不使用平滑滚动
                jumpToLyric(currentTextView);
            }
        }
        // 如果没有当前歌词，但歌词列表不为空，滚动到顶部
        else if (!textViewMap.isEmpty() && !hasInitializedScroll) {
            hasInitializedScroll = true;
            jumpToTop();
        }
    }

    private void jumpToTop() {
        if (lyricScrollView == null || lyricScrollController == null) return;

        // 停止当前动画
        lyricScrollController.abortAnimation();

        // 设置滚动范围
        int scrollViewHeight = lyricScrollView.getHeight();
        int maxScroll = Math.max(0, lyricLinesView.getHeight() - scrollViewHeight);
        lyricScrollController.setMaxScroll(maxScroll);

        // 设置起始值和目标值相同，然后直接跳转
        lyricScrollController.setStartValue(0);
        lyricScrollController.scrollTo(0, 0); // 0ms立即跳转
        currentScrollPosition = 0;
    }

    private void jumpToLyric(TextView targetLyric) {
        if (lyricScrollView == null || lyricScrollController == null || targetLyric == null) return;

        int scrollViewHeight = lyricScrollView.getHeight();
        if (scrollViewHeight <= 0) {
            // 延迟执行，直到视图布局完成
            post(() -> jumpToLyric(targetLyric));
            return;
        }

        // 计算目标歌词在ScrollView中的位置
        int targetTop = 0;
        View current = targetLyric;

        while (current != lyricLinesView) {
            targetTop += current.getTop();
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }

        // 计算目标滚动位置（让歌词位于ScrollView的1/3位置）
        int targetScrollY = targetTop - scrollViewHeight / 3;

        // 确保滚动位置在有效范围内
        int maxScroll = Math.max(0, lyricLinesView.getHeight() + dp(256) - scrollViewHeight);
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));

        isRecenterScroll = true;
        isAutoScrolling = true;
        // 停止当前动画
        lyricScrollController.abortAnimation();

        // 设置滚动范围
        lyricScrollController.setMaxScroll(maxScroll);

        // 设置起始值
        lyricScrollController.setStartValue(targetScrollY);

        // 立即跳转（0ms动画）
        lyricScrollController.scrollTo(targetScrollY, 0);
        currentScrollPosition = targetScrollY;

        postDelayed(() -> {
            isRecenterScroll = false;
            isAutoScrolling = false;
        }, 50);
    }

    private void scrollToLyric(TextView targetLyric) {
        if (lyricScrollView == null || lyricScrollController == null || targetLyric == null) return;

        int scrollViewHeight = lyricScrollView.getHeight();
        if (scrollViewHeight <= 0) {
            // 延迟执行，直到视图布局完成
            post(() -> scrollToLyric(targetLyric));
            return;
        }

        // 计算目标歌词在ScrollView中的位置
        int targetTop = 0;
        View current = targetLyric;

        while (current != lyricLinesView) {
            targetTop += current.getTop();
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }

        // 计算目标滚动位置（让歌词位于ScrollView的1/3位置）
        int targetScrollY = targetTop - scrollViewHeight / 3;

        // 确保滚动位置在有效范围内
        int maxScroll = Math.max(0, lyricLinesView.getHeight() + dp(256) - scrollViewHeight);
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));

        // 获取当前滚动位置
        int currentScrollY = currentScrollPosition;

        // 如果已经在目标位置附近，不执行滚动
        if (Math.abs(targetScrollY - currentScrollY) < 5) {
            isRecenterScroll = false; // 重置归位标记
            return;
        }

        lyricScrollController.scrollTo(currentScrollY, 0);
        // 停止当前的滚动动画
        lyricScrollController.abortAnimation();

        // 设置滚动范围
        lyricScrollController.setMaxScroll(maxScroll);

        // 设置起始值
        lyricScrollController.setStartValue(currentScrollY);

        // 计算动画时长（基于滚动距离）
        int scrollDistance = Math.abs(targetScrollY - currentScrollY);
        int animationDuration;

        // 如果是归位滚动，使用更长的动画时间，更平滑
        animationDuration = Math.min(300 + scrollDistance / 5, 600);

        // 标记为自动滚动
        isAutoScrolling = true;

        // 执行平滑滚动
        lyricScrollController.scrollTo(targetScrollY, animationDuration);

        // 如果是归位滚动，滚动完成后重置标记
        if (isRecenterScroll) {
            postDelayed(() -> {
                isRecenterScroll = false;
                isAutoScrolling = false;
                // 归位滚动完成后，确保用户滚动状态重置
                isUserManuallyScrolling = false;
            }, animationDuration + 100);
        } else {
            // 非归位滚动的自动滚动完成后重置标记
            postDelayed(() -> isAutoScrolling = false, animationDuration + 100);
        }
    }

    private void addMusicQueueItem(MusicDetail musicDetail, LinearLayout playQueueView) {
        MusicListItem item = new MusicListItem(getContext());
        item.bindData(musicDetail);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, dp(16));
        LinearLayout actions = new LinearLayout(getContext());

        assert Minecraft.getInstance().player != null;
        if (musicDetail.getPusherInfo().playerUUID().equals(Minecraft.getInstance().player.getUUID())) {
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
            actions.addView(removeButton, new LayoutParams(WRAP_CONTENT, dp(MusicListItem.imageSize)));
        }
        item.addView(actions);
        item.setLayoutParams(layoutParams);
        playQueueView.addView(item, layoutParams);
    }

    public void switchMusic(Queue<LyricLine> lyricLines) {
        MuiModApi.postToUiThread(() -> {
            textViewMap.clear();
            hasInitializedScroll = false;
            isUserManuallyScrolling = false;
            isRecenterScroll = false;
            isAutoScrolling = false;

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
                            lyricText.setTextSize(dp(12));

                            if (lyricLine.equals(NowPlayingInfo.getInstance().getCurrentLyricLine())) {
                                lyricText.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
                                lyricText.setTextStyle(TextPaint.BOLD);
                                lastHighlightLine = lyricText;
                            } else {
                                lyricText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                            }
                            lyricText.setText(text == null ? "" : text);
                            textViewMap.put(lyricLine, lyricText);

                            if (translatedText != null && !translatedText.isEmpty()) {
                                LayoutParams mainParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                                mainParams.setMargins(0, 0, 0, 0);
                                lyricLinesView.addView(lyricText, mainParams);

                                TextView subLyricText = new TextView(context);
                                subLyricText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                                subLyricText.setTextSize(dp(8));
                                subLyricText.setText(translatedText);

                                LayoutParams subParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                                subParams.setMargins(0, 0, 0, dp(8));
                                lyricLinesView.addView(subLyricText, subParams);
                            } else {
                                LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                                params.setMargins(0, 0, 0, dp(8));
                                lyricLinesView.addView(lyricText, params);
                            }
                        }
                    }
                } else {
                    lyricLinesView.removeAllViews();
                }

                lyricLinesView.post(() -> {
                    lyricLinesView.requestLayout();
                    lyricScrollView.requestLayout();

                    // 布局完成后，初始化滚动到当前歌词
                    initializeScrollToCurrentLyric();
                });
            }
        });
    }

    private void startScrollControllerUpdate() {
        post(() -> {
            lyricScrollController.update(MuiModApi.getElapsedTime());
            postDelayed(this::startScrollControllerUpdate, 15); // ~60fps
        });
    }

    private void stopScrollControllerUpdate() {
        removeCallbacks(this::startScrollControllerUpdate);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopScrollControllerUpdate();
        if (lyricScrollController != null) {
            lyricScrollController.abortAnimation();
        }
        removeCallbacks(autoRecenterRunnable);
        instance = null;
    }
}