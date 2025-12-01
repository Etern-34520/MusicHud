package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import indi.etern.musichud.network.Codecs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MusicDetail {
    public static final StreamCodec<RegistryFriendlyByteBuf, MusicDetail> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            MusicDetail::getName,
            ByteBufCodecs.LONG,
            MusicDetail::getId,
            Codecs.ofList(Artist.CODEC),
            MusicDetail::getArtists,
            Codecs.ofList(ByteBufCodecs.STRING_UTF8),
            MusicDetail::getAlias,
            AlbumInfo.CODEC,
            MusicDetail::getAlbum,
            ByteBufCodecs.INT,
            MusicDetail::getDurationMillis,
            Codecs.ofList(ByteBufCodecs.STRING_UTF8),
            MusicDetail::getTranslations,
            PrivilegeInfo.CODEC,
            MusicDetail::getPrivilege,
            PusherInfo.CODEC,
            MusicDetail::getPusherInfo,
            MusicDetail::new
    );
    public static final MusicDetail NONE = new MusicDetail();

    //    protected MusicDetail() {}
    protected MusicDetail(
            String name,
            long id,
            List<Artist> artists,
            List<String> alias,
            AlbumInfo album,
            int durationMillis,
            List<String> translations,
            PrivilegeInfo privilege,
            PusherInfo pusherInfo
    ) {
        this.name = name;
        this.id = id;
        this.artists = artists;
        this.alias = alias;
        this.album = album;
        this.durationMillis = durationMillis;
        this.translations = translations;
        this.privilege = privilege;
        this.pusherInfo = pusherInfo;
    }

    private String name = "";
    private long id;
    private MusicResourceType resourceType = MusicResourceType.NORMAL;
    @JsonProperty("ar")
    private List<Artist> artists = List.of();
    @JsonProperty("alia")
    private List<String> alias = List.of();
    @JsonProperty("pop")
    private int popularity;
    @JsonProperty("v")
    private int infoVersion;
    @JsonProperty("version")
    private int musicVersion;
    @JsonProperty("al")
    private AlbumInfo album = AlbumInfo.NONE;
    @JsonProperty("dt")
    private int durationMillis;
    @JsonProperty("hr")
    private QualityInfo hiRes = QualityInfo.NONE;
    @JsonProperty("sq")
    private QualityInfo sq = QualityInfo.NONE;
    @JsonProperty("h")
    private QualityInfo high = QualityInfo.NONE;
    @JsonProperty("m")
    private QualityInfo medium = QualityInfo.NONE;
    @JsonProperty("l")
    private QualityInfo low = QualityInfo.NONE;
    @JsonProperty
    private long mark; // bit mask
    private OriginCoverType originCoverType;
    @JsonProperty("tns")
    private List<String> translations = List.of();
    @Setter
    PusherInfo pusherInfo = PusherInfo.EMPTY;
    PrivilegeInfo privilege = PrivilegeInfo.NONE;

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof MusicDetail other && this.id == other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}