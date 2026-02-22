package indi.etern.musichud.beans.music;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PUBLIC)
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
    @Getter
    int code = 0;
    Lyric lrc = Lyric.NONE;
    Lyric tlyric = Lyric.NONE;

    public LyricInfo(
            Lyric lrc,
            Lyric tlyric
    ) {
        this.lrc = lrc;
        this.tlyric = tlyric;
    }

    public Lyric getLrc() {
        return Objects.requireNonNullElse(lrc, Lyric.NONE);
    }

    public Lyric getTlyric() {
        return Objects.requireNonNullElse(tlyric, Lyric.NONE);
    }
}