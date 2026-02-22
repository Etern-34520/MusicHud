package indi.etern.musichud.beans.music;

import com.google.gson.annotations.SerializedName;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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
    public static final AlbumInfo NONE = new AlbumInfo();
    @Getter
    long id;
    String name = "";
    String picUrl = "";
    @SerializedName("pic")
    @Getter
    long picSize;

    public String getThumbnailPicUrl(int size) {
        return picUrl + "?param=" + size + "y" + size;
    }

    public String getName() {
        return Objects.requireNonNullElse(name, "");
    }

    public String getPicUrl() {
        return Objects.requireNonNullElse(picUrl, "");
    }
}