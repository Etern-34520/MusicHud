package indi.etern.musichud.client.ui.components;

import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ProgressBar;
import icyllis.modernui.widget.TextView;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.client.services.AccountService;
import indi.etern.musichud.client.services.LoginService;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;
import lombok.Getter;

import java.util.HashMap;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class AccountView extends LinearLayout {
    @Getter
    private static AccountView instance;
    private final AccountService accountService = AccountService.getInstance();
    private Context context;
    private MusicService musicService = MusicService.getInstance();
    private HashMap<Playlist, PlaylistCard> idlePlaylistCardMap = new HashMap<>();

    public AccountView(Context context) {
        super(context);
        this.context = context;
        refresh();
        instance = this;
        addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                AccountView.instance.context = null;
                instance = null;
            }
        });
    }

    public void refresh() {
        removeAllViews();
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        Profile currentProfile = Profile.getCurrent();
        if (currentProfile == null) {
            setGravity(Gravity.CENTER_HORIZONTAL);
            MusicHud.getLogger(AccountView.class).warn("Profile.current is null, the account view will not display");

            TextView textView = new TextView(context);
            textView.setTextSize(dp(8f));
            textView.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            textView.setText("获取账户信息出错");
            textView.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            LayoutParams params1 = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params1.setMargins(0, dp(64), 0, 0);

            Button retryButton = new Button(context);
            retryButton.setFocusable(true);
            retryButton.setClickable(true);
            retryButton.setTextColor(Theme.PRIMARY_COLOR);
            retryButton.setHeight(dp(36));
            retryButton.setWidth(dp(84));
            retryButton.setTextSize(dp(8));
            retryButton.setText("重试");

            ProgressBar progressRing = new ProgressBar(context);
            progressRing.setIndeterminate(true);
            progressRing.setIndeterminateTintList(ColorStateList.valueOf(Theme.PRIMARY_COLOR));
            progressRing.setVisibility(GONE);
            LayoutParams ringParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            ringParams.setMargins(0, dp(32), 0, 0);
            progressRing.setLayoutParams(ringParams);

            var background = ButtonInsetBackground.builder()
                    .padding(new ButtonInsetBackground.Padding(0, 0, 0, 0))
                    .cornerRadius(dp(4)).inset(dp(1)).build().get();
            retryButton.setBackground(background);
            LayoutParams buttonParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            buttonParams.setMargins(0, dp(8), 0, 0);
            retryButton.setLayoutParams(buttonParams);
            retryButton.setOnClickListener((view) -> {
                MuiModApi.postToUiThread(() -> {
                    retryButton.setVisibility(GONE);
                    progressRing.setVisibility(VISIBLE);
                });
                LoginService.getInstance().loginToServer();
            });

            addView(textView);
            addView(retryButton);
            addView(progressRing);
        } else {
            setGravity(Gravity.TOP);
            LinearLayout topPanel = new LinearLayout(context);
            topPanel.setOrientation(LinearLayout.HORIZONTAL);
            topPanel.setGravity(Gravity.LEFT);

            UrlImageView avatar = new UrlImageView(context);
            avatar.setCircular(true);
            LayoutParams layoutParams = new LayoutParams(dp(64), dp(64));
            avatar.setLayoutParams(layoutParams);
            topPanel.addView(avatar);
            avatar.loadUrl(currentProfile.getAvatarUrl());

            LayoutParams textsLayoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
            textsLayoutParams.setMargins(dp(16), 0, 0, 0);
            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(VERTICAL);
            texts.setGravity(Gravity.CENTER_VERTICAL);
            topPanel.addView(texts, textsLayoutParams);

            LayoutParams nameLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            TextView nickName = new TextView(context);
            nickName.setSingleLine(true);
            nickName.setTextSize(dp(12));
            nickName.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            nickName.setText(currentProfile.getNickname());
            texts.addView(nickName, nameLayoutParams);

            LayoutParams idLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            TextView id = new TextView(context);
            id.setSingleLine(true);
            id.setTextSize(dp(8));
            id.setTextColor(Theme.SECONDARY_TEXT_COLOR);
            id.setText(Long.toString(currentProfile.getUserId()));
            texts.addView(id, idLayoutParams);

            LayoutParams topPanelLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            topPanelLayoutParams.setMargins(0, 0, 0, dp(32));
            addView(topPanel, topPanelLayoutParams);

            ProgressBar progressBar = new ProgressBar(context);
            progressBar.setIndeterminate(true);
            addView(progressBar, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

            LinearLayout layout1 = new LinearLayout(context);
            layout1.setOrientation(VERTICAL);
            layout1.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            addView(layout1);

            TextView textView = new TextView(context);
            textView.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            textView.setTextSize(dp(10));
            textView.setText("我的歌单");
            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            layout1.addView(textView, params);

            FlexWrapLayout playlistCards = new FlexWrapLayout(context);
            playlistCards.setItemSpacing(dp(0));
            playlistCards.setLineSpacing(dp(0));
            LayoutParams params1 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params1.setMargins(0, dp(16), 0, 0);
            layout1.addView(playlistCards, params1);

            LinearLayout layout2 = new LinearLayout(context);
            layout2.setOrientation(VERTICAL);
            LayoutParams params5 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params5.setMargins(0, dp(32), 0, 0);
            layout2.setLayoutParams(params5);
            addView(layout2);

            TextView textView1 = new TextView(context);
            textView1.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
            textView1.setTextSize(dp(10));
            textView1.setText("空闲播放源");
            LayoutParams params2 = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            layout2.addView(textView1, params2);

            TextView textView2 = new TextView(context);
            textView2.setTextColor(Theme.SECONDARY_TEXT_COLOR);
            textView2.setTextSize(dp(8));
            textView2.setText("播放器空闲时可能会播放这些歌单中的音乐");
            LayoutParams params3 = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            layout2.addView(textView2, params3);

            FlexWrapLayout idlePlaylistCardsList = new FlexWrapLayout(context);
            idlePlaylistCardsList.setItemSpacing(dp(0));
            idlePlaylistCardsList.setLineSpacing(dp(0));
            LayoutParams params4 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params4.setMargins(0, dp(16), 0, 0);
            layout2.addView(idlePlaylistCardsList, params4);

            accountService.loadUserPlaylist().thenAcceptAsync(playlists -> {
                if (getContext() != null) {
                    MuiModApi.postToUiThread(() -> {
                        for (Playlist playlist : playlists) {
                            playlistCards.addView(new PlaylistCard(context, playlist));
                        }
                        removeView(progressBar);
                    });
                }
            });

            musicService.getIdlePlaylists().forEach(playlist -> {
                PlaylistCard child = new PlaylistCard(context, playlist);
                idlePlaylistCardsList.addView(child);
                idlePlaylistCardMap.put(playlist, child);
            });

            Consumer<Playlist> addListener = playlist -> {
                MuiModApi.postToUiThread(() -> {
                    PlaylistCard child = new PlaylistCard(context, playlist);
                    idlePlaylistCardsList.addView(child);
                    idlePlaylistCardMap.put(playlist, child);
                });
            };
            Consumer<Playlist> removeListener = playlist -> {
                MuiModApi.postToUiThread(() -> {
                    PlaylistCard view = idlePlaylistCardMap.get(playlist);
                    if (view != null) {
                        idlePlaylistCardsList.removeView(view);
                        idlePlaylistCardMap.remove(playlist);
                    }
                });
            };
            musicService.getIdlePlaylistAddListeners().add(addListener);
            musicService.getIdlePlaylistRemoveListeners().add(removeListener);

            addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {}

                @Override
                public void onViewDetachedFromWindow(View v) {
                    musicService.getIdlePlaylistRemoveListeners().remove(removeListener);
                    musicService.getIdlePlaylistAddListeners().remove(addListener);
                }
            });
        }
    }
}