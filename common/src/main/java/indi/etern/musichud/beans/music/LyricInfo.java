package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
public class LyricInfo {
    public static final StreamCodec<RegistryFriendlyByteBuf, LyricInfo> CODEC = StreamCodec.composite(
            Lyric.CODEC,
            LyricInfo::getLrc,
            Lyric.CODEC,
            LyricInfo::getTlyric,
            LyricInfo::new
    );
    public static final LyricInfo NONE = new LyricInfo();

    public LyricInfo (
            Lyric lrc,
            Lyric tlyric
    ) {
        this.lrc = lrc;
        this.tlyric = tlyric;
    }

    int code = 0;
    Lyric lrc = Lyric.NONE;
    Lyric tlyric = Lyric.NONE;
}