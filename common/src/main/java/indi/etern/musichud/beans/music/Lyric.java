package indi.etern.musichud.beans.music;

import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
public class Lyric {
    public static final StreamCodec<RegistryFriendlyByteBuf, Lyric> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            Lyric::getVersion,
            ByteBufCodecs.STRING_UTF8,
            Lyric::getLyric,
            Lyric::new
    );
    public static final Lyric NONE = new Lyric(-1, "");
    @Getter
    int version;
    String lyric;

    public String getLyric() {
        return Objects.requireNonNullElse(lyric, "");
    }
}
