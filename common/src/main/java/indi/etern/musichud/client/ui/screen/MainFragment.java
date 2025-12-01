package indi.etern.musichud.client.ui.screen;

import icyllis.modernui.R;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.*;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.musicPlayer.NowPlayingInfo;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.components.RouterContainer;
import indi.etern.musichud.client.ui.components.SideMenu;
import indi.etern.musichud.client.ui.components.UrlImageView;
import indi.etern.musichud.client.ui.pages.AccountBaseView;
import indi.etern.musichud.client.ui.pages.ConfigView;
import indi.etern.musichud.client.ui.pages.HomeView;
import indi.etern.musichud.client.ui.pages.SearchView;
import lombok.NonNull;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class MainFragment extends Fragment {
    private static volatile MainFragment instance = null;
    private final NowPlayingInfo playingInfo = NowPlayingInfo.getInstance();
    private UrlImageView albumImage;
    private TextView titleText;
    private TextView artistsText;
    @Setter
    private int defaultSelectedIndex = 0;
    private ProgressBar progressBar;
    private TextView progressText;

    public MainFragment() {
    }

    public static void refresh() {
        switchMusic(null, null);
        HomeView homeView = HomeView.getInstance();
        if (homeView != null) {
            homeView.refresh();
        }
        SearchView searchView = SearchView.getInstance();
        if (searchView != null) {
            searchView.refresh();
        }
        AccountBaseView accountBaseView = AccountBaseView.getInstance();
        if (accountBaseView != null) {
            accountBaseView.refresh();
        }
        if (instance != null && instance.titleText != null) {
            if (!ClientConfigDefinition.enable.get()) {
                instance.titleText.setText("已禁用 Music Hud");
            } else if (!MusicHud.isConnected()) {
                instance.titleText.setText("未连接");
            } else {
                instance.titleText.setText("暂无播放音乐");
            }
        }
    }

    public static void switchMusic(MusicDetail musicDetail, Queue<LyricLine> lyricLines) {
        if (instance != null) {
            if (musicDetail == null || musicDetail.equals(MusicDetail.NONE)) {
                instance.albumImage.clear();
                if (!MusicHud.isConnected()) {
                    instance.titleText.setText("未连接");
                } else if (!ClientConfigDefinition.enable.get()) {
                    instance.titleText.setText("已禁用 Music Hud");
                } else {
                    instance.titleText.setText("暂无播放音乐");
                }
                instance.titleText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                instance.artistsText.setText("");
                instance.progressBar.setVisibility(View.GONE);
                instance.progressText.setText("");
            } else {
                instance.titleText.setTextColor(Theme.NORMAL_TEXT_COLOR);
                instance.albumImage.loadUrl(musicDetail.getAlbum().getPicUrl());
                instance.titleText.setText(musicDetail.getName());
                instance.artistsText.setText(musicDetail.getArtists().stream()
                        .map(Artist::getName)
                        .reduce((a, b) -> a + " / " + b)
                        .orElse(""));
                instance.progressBar.setVisibility(View.VISIBLE);
                instance.progressBar.setMax(musicDetail.getDurationMillis());

                startProgressUpdater(musicDetail);
            }
            HomeView homeView = HomeView.getInstance();
            if (homeView != null) {
                homeView.switchMusic(lyricLines);
            }
        }
    }

    private static void startProgressUpdater(MusicDetail musicDetail) {
        NowPlayingInfo nowPlayingInfo = NowPlayingInfo.getInstance();
        MusicHud.EXECUTOR.execute(() -> {
            do {
                if (instance == null || instance.progressBar == null) {
                    return;
                }
                Duration playedDuration = nowPlayingInfo.getPlayedDuration();
                Duration musicDuration = nowPlayingInfo.getMusicDuration();
                DateTimeFormatter formatter = musicDuration.toHoursPart() >= 1 ?
                        DateTimeFormatter.ofPattern("HH:mm:ss") :
                        DateTimeFormatter.ofPattern("mm:ss");
                String playtimeText = formatter.format(
                        LocalTime.MIDNIGHT.plusSeconds(playedDuration.toSeconds())
                ) + " / " + formatter.format(
                        LocalTime.MIDNIGHT.plusSeconds(musicDuration.toSeconds())
                );
                MuiModApi.postToUiThread(() -> {
                    if (instance != null && instance.progressBar != null) {
                        instance.progressBar.setProgress((int) playedDuration.toMillis());
                        instance.progressText.setText(playtimeText);
                    }
                });
                try {
                    Thread.sleep(Duration.of(50, ChronoUnit.MILLIS));
                } catch (InterruptedException e) {
                    return;
                }
            } while (musicDetail.equals(nowPlayingInfo.getCurrentlyPlayingMusicDetail())
                    && nowPlayingInfo.getProgressRate() < 1);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        try {
            instance = this;
            var context = requireContext();
            var base = new LinearLayout(context);
            int dp24 = base.dp(24);
            int dp32 = base.dp(32);
            base.setPadding(dp32, dp24, dp24, dp32);

            var baseBackground = new ShapeDrawable();
            baseBackground.setColor(Theme.BASE_BACKGROUND_COLOR);
            base.setBackground(baseBackground);
            var baseParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            base.setLayoutParams(baseParams);
            base.setOrientation(LinearLayout.HORIZONTAL);

            var scrollView = new NestedScrollView(context);
            scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            var routerContainer = new RouterContainer(context);
            routerContainer.setTransitionType(RouterContainer.TransitionType.FADE);
            routerContainer.setAnimationDuration(300);

            {
                var sideMenu = new SideMenu(context, routerContainer);
                var homeNav = sideMenu.createNavigationPage("主页", HomeView::new);
                var searchNav = sideMenu.createNavigationPage("搜索", SearchView::new);
                var accountNav = sideMenu.createNavigationPage("账户", AccountBaseView::new);
                var settingsNav = sideMenu.createNavigationPage("设置", ConfigView::new);

                SideMenu.NavigationMeta defaultMeta = List.of(homeNav, searchNav, accountNav, settingsNav).get(defaultSelectedIndex);
                defaultMeta.select();

                int widthDp = base.dp(160);
                var params = new LinearLayout.LayoutParams(widthDp, MATCH_PARENT);
                params.gravity = Gravity.CENTER;
                var side = new LinearLayout(context);
                side.setOrientation(LinearLayout.VERTICAL);
                albumImage = new UrlImageView(context);
                //noinspection SuspiciousNameCombination
                var imageParams = new FrameLayout.LayoutParams(widthDp, widthDp);
                side.addView(albumImage, imageParams);

                LinearLayout musicInfo = new LinearLayout(context);
                musicInfo.setOrientation(LinearLayout.VERTICAL);

                titleText = new TextView(context);
                titleText.setTextSize(titleText.dp(10));
                titleText.setTextColor(Theme.NORMAL_TEXT_COLOR);
                if (!ClientConfigDefinition.enable.get()) {
                    titleText.setText("已禁用 Music Hud");
                } else if (!MusicHud.isConnected()) {
                    titleText.setText("未连接");
                } else {
                    titleText.setText("暂无播放音乐");
                }
                musicInfo.addView(titleText);

                artistsText = new TextView(context);
                artistsText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                artistsText.setTextSize(artistsText.dp(8));
                musicInfo.addView(artistsText);

                progressBar = new ProgressBar(context, null, R.attr.progressBarStyleHorizontal);
                progressBar.setMin(0);
                progressBar.setVisibility(View.GONE);
                LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(MATCH_PARENT, base.dp(4));
                params2.setMargins(0, 0, 0, side.dp(-4));
                musicInfo.addView(progressBar, params2);

                progressText = new TextView(context);
                progressText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
                progressText.setTextSize(progressText.dp(8));
                LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(MATCH_PARENT, base.dp(16));
                params3.setMargins(0, side.dp(4), 0, 0);
                musicInfo.addView(progressText, params3);

                var params1 = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params1.setMargins(side.dp(8), side.dp(4), side.dp(8), side.dp(24));
                side.addView(musicInfo, params1);
                side.addView(sideMenu, params);
                base.addView(side, params);

                MusicDetail musicDetail = NowPlayingInfo.getInstance().getCurrentlyPlayingMusicDetail();
                if (musicDetail != null) {
                    switchMusic(musicDetail, playingInfo.getLyricLines());
                }
            }

            var params = new ScrollView.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            params.setMargins(routerContainer.dp(80), 0, routerContainer.dp(64), 0);
            scrollView.addView(routerContainer, params);
            scrollView.setFillViewport(true);
            base.addView(scrollView, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            return base;
        } catch (Exception e) {
            instance = null;
            throw e;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        instance = null;
    }
}