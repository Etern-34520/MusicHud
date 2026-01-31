package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AlbumInfo {
    public static final StreamCodec<ByteBuf, AlbumInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            AlbumInfo::getId,
            ByteBufCodecs.STRING_UTF8,
            AlbumInfo::getName,
            ByteBufCodecs.STRING_UTF8,
            AlbumInfo::getPicUrl,
            ByteBufCodecs.LONG,
            AlbumInfo::getPicSize,
            AlbumInfo::new
    );
    @JsonSetter(nulls = Nulls.SKIP)
    long id;
    @JsonSetter(nulls = Nulls.SKIP)
    String name = "";
    @JsonSetter(nulls = Nulls.SKIP)
    String picUrl = "";
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("pic")
    long picSize;

    public static final AlbumInfo NONE = new AlbumInfo();
    public String getThumbnailPicUrl(int size) {
        return picUrl + "?param="+ size + "y" + size;
    }
}