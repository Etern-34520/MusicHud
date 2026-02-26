package indi.etern.musichud.client.ui.pages;

import dev.architectury.networking.NetworkManager;
import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.Artist;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.client.ui.Theme;
import indi.etern.musichud.client.ui.components.MusicListItem;
import indi.etern.musichud.client.ui.utils.ButtonInsetBackground;
import indi.etern.musichud.network.requestResponseCycle.SearchRequest;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.stream.Collectors;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class SearchView extends LinearLayout {
    @Getter
    private static SearchView instance = null;

    private EditText searchTextInput;
    private LinearLayout resultArea;

    public SearchView(Context context) {
        super(context);
        instance = this;
        refresh();
    }

    public void refresh() {
        Context context = getContext();
        removeAllViews();
        setOrientation(VERTICAL);

        boolean enabled = ClientConfigDefinition.enable.get();
        if (MusicHud.getStatus() != MusicHud.ConnectStatus.CONNECTED || !enabled) {
            setGravity(Gravity.CENTER);
            TextView textView = Theme.getNotificationTextView(context, enabled);
            addView(textView);
            return;
        }

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(HORIZONTAL);
        LayoutParams topParams = new LayoutParams(MATCH_PARENT, dp(38));
        topParams.setMargins(0, dp(16), 0, dp(16));
        addView(top, topParams);

        top.addView(new View(context), new LayoutParams(0, WRAP_CONTENT, 2));
        searchTextInput = new EditText(context, null, R.attr.editTextOutlinedStyle);
        searchTextInput.setTextAlignment(SearchView.TEXT_ALIGNMENT_CENTER);
        searchTextInput.setHint(I18n.get("music_hud.field.hint.searchMusic"));
        searchTextInput.setSingleLine();
        LayoutParams params = new LayoutParams(0, WRAP_CONTENT, 6);
        params.setMargins(dp(52), 0, 0, 0);
        top.addView(searchTextInput, params);

        Button searchButton = new Button(context);
        searchButton.setText(I18n.get("music_hud.button.searchMusic"));
        LayoutParams buttonParams = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        Drawable background = ButtonInsetBackground.builder()
                .inset(0).padding(new ButtonInsetBackground.Padding(dp(8), 0, dp(8), 0))
                .cornerRadius(dp(4)).build().get();
        searchButton.setBackground(background);
        buttonParams.setMargins(dp(8), 0, 0, 0);
        top.addView(searchButton, buttonParams);

        top.addView(new View(context), new LayoutParams(0, WRAP_CONTENT, 2));

        var scrollView = new ScrollView(context);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        scrollView.setFillViewport(true);
        addView(scrollView, new LayoutParams(MATCH_PARENT, 0, 1));

        resultArea = new LinearLayout(context);
        resultArea.setOrientation(VERTICAL);
        LayoutParams resultAreaParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        resultAreaParams.setMargins(dp(32), 0, dp(32), 0);
        scrollView.addView(resultArea, resultAreaParams);

        searchTextInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEY_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                search();
                return true;
            }
            return false;
        });
        searchButton.setOnClickListener((v) -> search());

        addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {}

            @Override
            public void onViewDetachedFromWindow(View v) {
                instance = null;
            }
        });
    }

    private void search() {
        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setIndeterminate(true);
        resultArea.addView(progressBar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String text = searchTextInput.getText().toString();
        NetworkManager.sendToServer(new SearchRequest(text));
    }

    public void setResult(List<MusicDetail> result) {
        resultArea.removeAllViews();
        if (result.isEmpty()) {
            TextView noResultText = new TextView(getContext());
            noResultText.setText(I18n.get("music_hud.text.searchMusicNoResult"));
            noResultText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
            noResultText.setTextSize(Theme.TEXT_SIZE_NORMAL);
            noResultText.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            resultArea.addView(noResultText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else {
            for (MusicDetail musicDetail : result) {
                addItem(getContext(), musicDetail);
            }
        }
    }

    private void addItem(Context context, MusicDetail musicDetail) {
        var musicLayout = new MusicListItem(context);
        musicLayout.bindData(musicDetail);
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
            Toast.makeText(context, I18n.get("music_hud.text.pushedMusicToPlaylist")+ "\n" + musicDetail.getName() + " - " + artistsName, Toast.LENGTH_SHORT).show();
        });
        resultArea.addView(musicLayout);
    }
}