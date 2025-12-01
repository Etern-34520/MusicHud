package indi.etern.musichud.beans.music;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist {
    public static final StreamCodec<ByteBuf,Artist> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            Artist::getId,
            ByteBufCodecs.STRING_UTF8,
            Artist::getName,
            Artist::new
    );
    private long id;
    private String name;
}
