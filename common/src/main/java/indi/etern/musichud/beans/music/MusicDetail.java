package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
            PusherInfo.CODEC,
            MusicDetail::getPusherInfo,
            LyricInfo.CODEC,
            MusicDetail::getLyricInfo,
            MusicDetail::new
    );
    public static final MusicDetail NONE = new MusicDetail();

    protected MusicDetail(
            String name,
            long id,
            List<Artist> artists,
            List<String> alias,
            AlbumInfo album,
            int durationMillis,
            List<String> translations,
            PusherInfo pusherInfo,
            LyricInfo lyricInfo
    ) {
        this.name = name;
        this.id = id;
        this.artists = artists;
        this.alias = alias;
        this.album = album;
        this.durationMillis = durationMillis;
        this.translations = translations;
        this.pusherInfo = pusherInfo;
        this.lyricInfo = lyricInfo;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    private String name = "";
    private long id;
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("ar")
    private List<Artist> artists = List.of();
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("alia")
    private List<String> alias = List.of();
    @JsonProperty("pop")
    private int popularity;
    @JsonProperty("v")
    private int infoVersion;
    @JsonProperty("version")
    private int musicVersion;
    @JsonSetter(nulls = Nulls.SKIP)
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
    @JsonSetter(nulls = Nulls.SKIP)
    private OriginCoverType originCoverType;
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("tns")
    private List<String> translations = List.of();
    PrivilegeInfo privilege = PrivilegeInfo.NONE;
    // Not contained in the original API response, set separately
    @Setter
    PusherInfo pusherInfo = PusherInfo.EMPTY;
    @Setter
    LyricInfo lyricInfo = LyricInfo.NONE;

//    @Setter
//    MusicResourceInfo musicResourceInfo = MusicResourceInfo.NONE;

    public String getName() {
        return name == null ? "" : name;
    }

    public List<Artist> getArtists() {
        if (artists == null || artists.isEmpty()) {
            return List.of();
        }
        return artists.stream().filter(Objects::nonNull).toList();
    }

    public List<String> getAlias() {
        if (alias == null || alias.isEmpty()) {
            return List.of();
        }
        return alias.stream().filter(Objects::nonNull).toList();
    }

    public AlbumInfo getAlbum() {
        return album == null ? AlbumInfo.NONE : album;
    }

    public List<String> getTranslations() {
        if (translations == null || translations.isEmpty()) {
            return List.of();
        }
        return translations.stream().filter(Objects::nonNull).toList();
    }

    public PusherInfo getPusherInfo() {
        return pusherInfo == null ? PusherInfo.EMPTY : pusherInfo;
    }

    public LyricInfo getLyricInfo() {
        return lyricInfo == null ? LyricInfo.NONE : lyricInfo;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof MusicDetail other && this.id == other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}