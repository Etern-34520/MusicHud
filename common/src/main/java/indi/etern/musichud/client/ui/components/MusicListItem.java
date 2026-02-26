package indi.etern.musichud.client.ui.components;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import indi.etern.musichud.beans.music.Artist;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.PusherInfo;
import indi.etern.musichud.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class MusicListItem extends LinearLayout {

    private final DateTimeFormatter timeFormatterWithHour = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("mm:ss");
    public static final int imageSize = 54;
    private UrlImageView albumImage;
    private TextView musicName;
    private TextView musicArtistAndAlbum;
    private TextView durationText;
    private TextView pusherText;

    public MusicListItem(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        setOrientation(HORIZONTAL);
        LayoutParams musicLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(musicLayoutParams);

        albumImage = new UrlImageView(context);
        albumImage.setCornerRadius(dp(8));
        addView(albumImage, new LayoutParams(dp(imageSize), dp(imageSize)));

        LinearLayout musicTexts = new LinearLayout(context);
        musicTexts.setOrientation(VERTICAL);
        musicTexts.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams textsParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        textsParams.setMargins(dp(12), 0, 0, 0);
        addView(musicTexts, textsParams);

        musicName = new TextView(context);
        musicName.setSingleLine(true);
        musicName.setTextSize(Theme.TEXT_SIZE_LARGE);
        musicName.setTextColor(Theme.NORMAL_TEXT_COLOR);
        musicTexts.addView(musicName);

        musicArtistAndAlbum = new TextView(context);
        musicArtistAndAlbum.setSingleLine(true);
        musicArtistAndAlbum.setTextSize(Theme.TEXT_SIZE_NORMAL);
        musicArtistAndAlbum.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        musicTexts.addView(musicArtistAndAlbum);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(HORIZONTAL);
        musicTexts.addView(linearLayout);

        durationText = new TextView(context);
        durationText.setTextSize(Theme.TEXT_SIZE_NORMAL);
        durationText.setSingleLine(true);
        durationText.setTextColor(Theme.SECONDARY_TEXT_COLOR);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(16), 0);
        linearLayout.addView(durationText, params);

        pusherText = new TextView(getContext());
        pusherText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        pusherText.setTextSize(Theme.TEXT_SIZE_NORMAL);
        linearLayout.addView(pusherText);
    }

    public void bindData(MusicDetail musicDetail) {
        albumImage.loadUrl(musicDetail.getAlbum().getThumbnailPicUrl(dp(imageSize)));

        musicName.setText(musicDetail.getName());

        String artistsName = musicDetail.getArtists().stream()
                .map(Artist::getName).collect(Collectors.joining(" / "));
        String string = artistsName
                + "  -  " + musicDetail.getAlbum().getName();
        musicArtistAndAlbum.setText(string);

        Duration duration = Duration.of(musicDetail.getDurationMillis(), ChronoUnit.MILLIS);
        DateTimeFormatter formatter = duration.toHoursPart() >= 1 ?
                timeFormatterWithHour :
                timeFormatter;
        durationText.setText(formatter.format(
                java.time.LocalTime.MIDNIGHT.plusSeconds(duration.toSeconds())
        ));

        PusherInfo pusherInfo = musicDetail.getPusherInfo();
        if (!pusherInfo.playerName().isEmpty()) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null) throw new IllegalStateException();
            pusherText.setText(pusherInfo.playerName());
        }
    }

}