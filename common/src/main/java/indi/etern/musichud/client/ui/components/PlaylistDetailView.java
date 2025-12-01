package indi.etern.musichud.client.ui.components;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import indi.etern.musichud.beans.music.Artist;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;

import java.util.stream.Collectors;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PlaylistDetailView extends LinearLayout {
    public PlaylistDetailView(Context context, Playlist playlist) {
        super(context);

        setOrientation(VERTICAL);

        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(HORIZONTAL);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        topBar.setLayoutParams(params);

        Button backButton = new Button(context);
        backButton.setText("< 返回");
        backButton.setTextColor(Theme.NORMAL_TEXT_COLOR);
        backButton.setOnClickListener(view -> {
            RouterContainer.getInstance().popNavigate();
            backButton.setOnClickListener(null);
        });
        Drawable drawable = ButtonInsetBackground.builder()
                .inset(0)
                .cornerRadius(dp(16))
                .padding(new ButtonInsetBackground.Padding(dp(16), 0, dp(16), 0))
                .build().get();
        backButton.setBackground(drawable);
        LayoutParams buttonParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        buttonParam.setMargins(0, 0, dp(8), 0);
        topBar.addView(backButton, buttonParam);

        UrlImageView imageView = new UrlImageView(context);
        LayoutParams imageParams = new LayoutParams(dp(72), dp(72));
        topBar.addView(imageView, imageParams);
        imageView.loadUrl(playlist.getCoverImgUrl());

        LinearLayout texts = new LinearLayout(context);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        texts.setOrientation(VERTICAL);
        LayoutParams params1 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params1.setMargins(dp(16), 0, 0, 0);
        topBar.addView(texts, params1);

        TextView type = new TextView(context);
        type.setTextSize(dp(10));
        type.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        type.setText("歌单");
        LayoutParams params2 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.setMargins(0, 0, 0, dp(4));
        type.setLayoutParams(params2);
        texts.addView(type);

        TextView name = new TextView(context);
        name.setTextSize(dp(12));
        name.setTextColor(Theme.EMPHASIZE_TEXT_COLOR);
        name.setText(playlist.getName());
        texts.addView(name);

        addView(topBar);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        LayoutParams progressParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        progressParams.setMargins(0, dp(32), 0, 0);
        addView(progressBar, progressParams);

        LinearLayout tracks = new LinearLayout(context);
        tracks.setOrientation(VERTICAL);
        LayoutParams tracksParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        tracksParams.setMargins(0, dp(32), 0, 0);
        addView(tracks, tracksParams);

        MusicService.getInstance().loadPlaylistDetail(playlist.getId()).thenAcceptAsync(playlistDetail -> {
            MuiModApi.postToUiThread(() -> {
                type.setText("歌单  共 " + playlistDetail.getTracks().size() + " 项");
                removeView(progressBar);
                for (MusicDetail musicDetail : playlistDetail.getTracks()) {
                    addItem(context, musicDetail, tracks);
                }
            });
        });
    }

    private void addItem(Context context, MusicDetail musicDetail, LinearLayout tracks) {
        var musicLayout = new MusicListItem(context, musicDetail);
        var background = ButtonInsetBackground.builder()
                .cornerRadius(dp(12))
                .inset(dp(1))
                .padding(new ButtonInsetBackground.Padding(dp(4), dp(4), dp(4), dp(4))).build().get();
        musicLayout.setBackground(background);

        musicLayout.setClickable(true);
        String artistsName = musicDetail.getArtists().stream()
                .map(Artist::getName).collect(Collectors.joining(" / "));
        musicLayout.setOnClickListener((view) -> {
            MusicService.getInstance().sendPushMusicToQueue(musicDetail);
            Toast.makeText(context, "已添加到播放列表\n" + musicDetail.getName() + " - " + artistsName, Toast.LENGTH_SHORT).show();
        });
        tracks.addView(musicLayout);
    }
}
