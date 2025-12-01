package indi.etern.musichud.client.ui.components;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import icyllis.modernui.widget.Toast;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class PlaylistCard extends LinearLayout {
    private final MusicService musicService = MusicService.getInstance();
    private final Button addToWaitingListButton;
    @Getter
    Playlist playlist;

    public PlaylistCard(Context context, Playlist playlist) {
        super(context);
        this.playlist = playlist;

        setOrientation(VERTICAL);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);

        UrlImageView imageView = new UrlImageView(context);
        LayoutParams imageParams = new LayoutParams(dp(128), dp(128));
        imageParams.setMargins(0, 0, 0, dp(4));
        addView(imageView, imageParams);
        imageView.loadUrl(playlist.getCoverImgUrl());

        TextView name = new TextView(context);
        name.setTextSize(dp(8));
        name.setTextColor(Theme.NORMAL_TEXT_COLOR);
        name.setText(playlist.getName());
        LayoutParams params1 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params1.setMargins(dp(4), 0, 0, 0);
        addView(name, params1);

        addToWaitingListButton = new Button(context);
        updateButton();
        addToWaitingListButton.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        addToWaitingListButton.setTextSize(dp(6));
        Drawable background1 = ButtonInsetBackground.builder()
                .inset(0)
                .cornerRadius(dp(4))
                .padding(new ButtonInsetBackground.Padding(dp(4), dp(8), dp(4), dp(8)))
                .build().get();
        addToWaitingListButton.setBackground(background1);
        addToWaitingListButton.setOnClickListener((v) -> {
            if (musicService.getIdlePlaylists().contains(playlist)) {
                Toast.makeText(context, "已从空闲播放源移除\n" + playlist.getName(), Toast.LENGTH_SHORT).show();
                musicService.removeFromIdlePlaySource(playlist);
//                addToWaitingListButton.setText("添加到空闲播放源");
            } else {
                Toast.makeText(context, "已添加到空闲播放源\n" + playlist.getName(), Toast.LENGTH_SHORT).show();
                musicService.addToIdlePlaySource(playlist);
//                addToWaitingListButton.setText("从空闲播放源移除");
            }
        });
        addView(addToWaitingListButton, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> {
            RouterContainer routerContainer = RouterContainer.getInstance();
            if (routerContainer != null) {
                routerContainer.pushNavigate(
                        new PlaylistDetailView(context, playlist)
                );
            }
        });

        ButtonInsetBackground background = ButtonInsetBackground.builder().inset(dp(1))
                .cornerRadius(dp(12))
                .padding(new ButtonInsetBackground.Padding(dp(6), dp(6), dp(6), dp(6)))
                .build();
        setBackground(background.get());

        Consumer<Playlist> listener = playlist1 -> {
            if (playlist1.equals(playlist)) {
                MuiModApi.postToUiThread(this::updateButton);
            }
        };

        addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                musicService.getIdlePlaylistChangeListeners().add(listener);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                musicService.getIdlePlaylistChangeListeners().remove(listener);
            }
        });
    }

    private void updateButton() {
        if (musicService.getIdlePlaylists().contains(playlist)) {
            addToWaitingListButton.setText("从空闲播放源移除");
        } else {
            addToWaitingListButton.setText("添加到空闲播放源");
        }
    }
}
