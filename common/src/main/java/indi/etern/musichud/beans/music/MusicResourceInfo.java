package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import indi.etern.musichud.network.Codecs;
import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MusicResourceInfo {
    public static final StreamCodec<RegistryFriendlyByteBuf, MusicResourceInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            MusicResourceInfo::getId,
            ByteBufCodecs.STRING_UTF8,
            MusicResourceInfo::getUrl,
            ByteBufCodecs.INT,
            MusicResourceInfo::getBitrate,
            ByteBufCodecs.LONG,
            MusicResourceInfo::getSize,
            Codecs.ofEnum(FormatType.class),
            MusicResourceInfo::getType,
            ByteBufCodecs.STRING_UTF8,
            MusicResourceInfo::getMd5,
            Codecs.ofEnum(Fee.class),
            MusicResourceInfo::getFee,
            ByteBufCodecs.INT,
            MusicResourceInfo::getTime,
            LyricInfo.CODEC,
            MusicResourceInfo::getLyricInfo,
            MusicResourceInfo::new
    );
    public static final MusicResourceInfo NONE = new MusicResourceInfo();
    long id;
    String url = "";
    @JsonProperty("br")
    int bitrate;
    long size;//byte
    FormatType type = FormatType.AUTO;
    String md5 = "";
    Fee fee = Fee.UNSET;
    int time;
    // Not contained in the original API response, set separately
    @Setter
    LyricInfo lyricInfo = LyricInfo.NONE;

    public static MusicResourceInfo from(String url, MusicDetail musicDetail) {
        MusicResourceInfo musicResourceInfo = new MusicResourceInfo();
        musicResourceInfo.url = url;
        musicResourceInfo.id = musicDetail.getId();
        if (musicResourceInfo.md5 == null)
            musicResourceInfo.md5 = "";
        musicResourceInfo.time = musicDetail.getDurationMillis();
        return musicResourceInfo;
    }
}
