package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
            MusicResourceInfo::new
    );
    public static final MusicResourceInfo NONE = new MusicResourceInfo();
    long id;
    @JsonSetter(nulls = Nulls.SKIP)
    String url = "";
    @JsonProperty("br")
    int bitrate;
    long size;//byte
    @JsonSetter(nulls = Nulls.SKIP)
    FormatType type = FormatType.AUTO;
    @JsonSetter(nulls = Nulls.SKIP)
    String md5 = "";
    @JsonSetter(nulls = Nulls.SKIP)
    Fee fee = Fee.UNSET;
    int time;

    public static MusicResourceInfo from(String url, MusicDetail musicDetail) {
        MusicResourceInfo musicResourceInfo = new MusicResourceInfo();
        musicResourceInfo.url = url;
        musicResourceInfo.id = musicDetail.getId();
        if (musicResourceInfo.md5 == null)
            musicResourceInfo.md5 = "";
        musicResourceInfo.time = musicDetail.getDurationMillis();
        return musicResourceInfo;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public FormatType getType() {
        return type == null ? FormatType.AUTO : type;
    }

    public String getMd5() {
        return md5 == null ? "" : md5;
    }

    public Fee getFee() {
        return fee == null ? Fee.UNSET : fee;
    }
}
