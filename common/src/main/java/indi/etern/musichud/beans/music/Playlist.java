package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.network.Codecs;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
public class Playlist {
    public static final StreamCodec<RegistryFriendlyByteBuf, Playlist> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            Playlist::getId,
            ByteBufCodecs.STRING_UTF8,
            Playlist::getName,
            ByteBufCodecs.LONG,
            Playlist::getCoverImgId,
            ByteBufCodecs.STRING_UTF8,
            Playlist::getCoverImgId_str,
            ByteBufCodecs.STRING_UTF8,
            Playlist::getCoverImgUrl,
            Profile.STREAM_CODEC,
            Playlist::getCreator,
            Codecs.ofList(MusicDetail.CODEC),
            Playlist::getTracks,
            Playlist::new
    );
    @JsonSetter(nulls = Nulls.SKIP)
    long id = -1;
    @JsonSetter(nulls = Nulls.SKIP)
    String name = "";
    @JsonSetter(nulls = Nulls.SKIP)
    long coverImgId = -1;
    @JsonSetter(nulls = Nulls.SKIP)
    String coverImgId_str = "";
    @JsonSetter(nulls = Nulls.SKIP)
    String coverImgUrl = "";
    @JsonSetter(nulls = Nulls.SKIP)
    Profile creator = Profile.ANONYMOUS;
    @JsonSetter(nulls = Nulls.SKIP)
    List<MusicDetail> tracks = List.of();
    @JsonSetter(nulls = Nulls.SKIP)
    List<PrivilegeInfo> privileges = List.of();
    protected Playlist(
            long id,
            String name,
            long coverImgId,
            String coverImgId_str,
            String coverImgUrl,
            Profile creator,
            List<MusicDetail> tracks
    ) {
        this.id = id;
        this.name = name;
        this.coverImgId = coverImgId;
        this.coverImgId_str = coverImgId_str;
        this.coverImgUrl = coverImgUrl;
        this.creator = creator;
        this.tracks = tracks;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Playlist playlist && playlist.id == id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
