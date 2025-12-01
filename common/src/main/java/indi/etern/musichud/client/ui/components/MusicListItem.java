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
    public MusicListItem(Context context, MusicDetail musicDetail) {
        super(context);
        setOrientation(HORIZONTAL);
        LayoutParams musicLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(musicLayoutParams);

        UrlImageView albumImage = new UrlImageView(context);
        albumImage.setCornerRadius(dp(8));
        addView(albumImage, new LayoutParams(dp(52), dp(52)));
        albumImage.loadUrl(musicDetail.getAlbum().getPicUrl());

        LinearLayout musicTexts = new LinearLayout(context);
        musicTexts.setOrientation(VERTICAL);
        musicTexts.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams textsParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        textsParams.setMargins(dp(12), 0, 0, 0);
        addView(musicTexts, textsParams);

        TextView musicName = new TextView(context);
        musicName.setText(musicDetail.getName());
        musicName.setSingleLine(true);
        musicName.setTextSize(dp(10));
        musicName.setTextColor(Theme.NORMAL_TEXT_COLOR);
        musicTexts.addView(musicName);

        TextView musicArtistAndAlbum = new TextView(context);
        musicArtistAndAlbum.setSingleLine(true);
        musicArtistAndAlbum.setTextSize(dp(8));
        musicArtistAndAlbum.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        String artistsName = musicDetail.getArtists().stream()
                .map(Artist::getName).collect(Collectors.joining(" / "));
        String string = artistsName
                + "  -  " + musicDetail.getAlbum().getName();
        musicArtistAndAlbum.setText(string);
        musicTexts.addView(musicArtistAndAlbum);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(HORIZONTAL);
        musicTexts.addView(linearLayout);

        TextView durationText = new TextView(context);
        durationText.setTextSize(dp(8));
        durationText.setSingleLine(true);
        durationText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
        Duration duration = Duration.of(musicDetail.getDurationMillis(), ChronoUnit.MILLIS);
        DateTimeFormatter formatter = duration.toHoursPart() >= 1 ?
                DateTimeFormatter.ofPattern("HH:mm:ss") :
                DateTimeFormatter.ofPattern("mm:ss");
        durationText.setText(formatter.format(
                java.time.LocalTime.MIDNIGHT.plusSeconds(duration.toSeconds())
        ));
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(16), 0);
        linearLayout.addView(durationText, params);

        PusherInfo pusherInfo = musicDetail.getPusherInfo();
        if (!pusherInfo.playerName().isEmpty()) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null) throw new IllegalStateException();
//            FrameLayout playerHeadView = getPlayerHeadView(pusherInfo, connection);
//            if (playerHeadView != null) {
//                linearLayout.addView(playerHeadView, new LayoutParams(dp(12), dp(12)));
//            }

            TextView pusherText = new TextView(getContext());
            pusherText.setTextColor(Theme.SECONDARY_TEXT_COLOR);
            pusherText.setText(pusherInfo.playerName());
            pusherText.setTextSize(dp(8));
            linearLayout.addView(pusherText);
        }

    }

    /*private FrameLayout getPlayerHeadView(PusherInfo pusherInfo, ClientPacketListener connection) {
        UUID playerUUID = pusherInfo.playerUUID();
        PlayerInfo playerInfo = connection.getPlayerInfo(playerUUID);

        if (playerInfo != null) {
            ResourceLocation skinTextureLocation = playerInfo.getSkin().texture();

            // 1. 从 TextureManager 获取 AbstractTexture
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture texture = textureManager.getTexture(skinTextureLocation);

            // 2. 获取 Blaze3D 的 GpuTexture
            GpuTexture gpuTexture = texture.getTexture();

            // 3. 获取真实的 GpuTexture (绕过验证层)
            GpuTexture realGpuTexture = MuiModApi.get().getRealGpuTexture(gpuTexture);

            // 4. 直接使用 Blaze3D 的 GlTexture
            if (realGpuTexture instanceof com.mojang.blaze3d.opengl.GlTexture blaze3dTexture) {
                // 5. 从 GPU 读取像素到 Bitmap
                int width = blaze3dTexture.getWidth(0);
                int height = blaze3dTexture.getHeight(0);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Format.RGBA_8888);

                Minecraft.getInstance().submit(() -> {
                    GL33C.glPixelStorei(GL33C.GL_PACK_ROW_LENGTH, 0);
                    GL33C.glPixelStorei(GL33C.GL_PACK_SKIP_ROWS, 0);
                    GL33C.glPixelStorei(GL33C.GL_PACK_SKIP_PIXELS, 0);
                    GL33C.glPixelStorei(GL33C.GL_PACK_ALIGNMENT, 1);
                    GL33C.glBindBuffer(GL33C.GL_PIXEL_PACK_BUFFER, 0);

                    int boundTexture = GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);
                    GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, blaze3dTexture.glId());
                    GL33C.glGetTexImage(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA,
                            GL33C.GL_UNSIGNED_BYTE, bitmap.getAddress());
                    GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, boundTexture);
                }).join();


                // 6. 裁剪头部区域
                Bitmap headLayer1bitmap = Bitmap.createBitmap(8, 8, bitmap.getFormat());
                Bitmap headLayer2bitmap = Bitmap.createBitmap(8, 8, bitmap.getFormat());

                headLayer1bitmap.setPixels(bitmap, 0, 0, 8, 8, 8, 8);
                headLayer2bitmap.setPixels(bitmap, 0, 0, 40, 8, 8, 8);

                // 7. 从 Bitmap 创建 Image
                Image image1 = Image.createTextureFromBitmap(headLayer1bitmap);
                Image image2 = Image.createTextureFromBitmap(headLayer2bitmap);

                // 8. 在 ImageView 中使用
                FrameLayout frameLayout = new FrameLayout(getContext());
                frameLayout.setForegroundGravity(Gravity.CENTER);


                ImageView pusherHeadLayer1 = new ImageView(getContext());
                pusherHeadLayer1.setImage(image1);
                pusherHeadLayer1.setScaleX(0.9f);
                pusherHeadLayer1.setScaleY(0.9f);

                ImageView pusherHeadLayer2 = new ImageView(getContext());
                pusherHeadLayer2.setImage(image2);

                frameLayout.addView(pusherHeadLayer1, new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                frameLayout.addView(pusherHeadLayer2, new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                // 清理临时 Bitmap
                bitmap.close();
                headLayer1bitmap.close();
                headLayer2bitmap.close();

                return frameLayout;
            }
        }
        return null;
    }*/
}
