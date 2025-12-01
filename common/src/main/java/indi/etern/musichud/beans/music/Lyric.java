package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
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
    int version;
    String lyric;
}
