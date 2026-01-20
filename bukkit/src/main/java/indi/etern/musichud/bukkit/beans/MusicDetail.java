package indi.etern.musichud.bukkit.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Complete music detail information.
 */
public class MusicDetail {
    public static final MusicDetail NONE = new MusicDetail();
    
    private String name = "";
    private long id;
    private List<Artist> artists = new ArrayList<>();
    private List<String> alias = new ArrayList<>();
    private AlbumInfo album = AlbumInfo.NONE;
    private int durationMillis;
    private List<String> translations = new ArrayList<>();
    private PusherInfo pusherInfo = PusherInfo.EMPTY;
    private MusicResourceInfo musicResourceInfo = MusicResourceInfo.NONE;
    
    public MusicDetail() {
    }
    
    public MusicDetail(String name, long id, List<Artist> artists, List<String> alias,
                       AlbumInfo album, int durationMillis, List<String> translations,
                       PusherInfo pusherInfo, MusicResourceInfo musicResourceInfo) {
        this.name = name;
        this.id = id;
        this.artists = artists != null ? artists : new ArrayList<>();
        this.alias = alias != null ? alias : new ArrayList<>();
        this.album = album != null ? album : AlbumInfo.NONE;
        this.durationMillis = durationMillis;
        this.translations = translations != null ? translations : new ArrayList<>();
        this.pusherInfo = pusherInfo != null ? pusherInfo : PusherInfo.EMPTY;
        this.musicResourceInfo = musicResourceInfo != null ? musicResourceInfo : MusicResourceInfo.NONE;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public List<Artist> getArtists() {
        return artists;
    }
    
    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }
    
    public List<String> getAlias() {
        return alias;
    }
    
    public void setAlias(List<String> alias) {
        this.alias = alias;
    }
    
    public AlbumInfo getAlbum() {
        return album;
    }
    
    public void setAlbum(AlbumInfo album) {
        this.album = album;
    }
    
    public int getDurationMillis() {
        return durationMillis;
    }
    
    public void setDurationMillis(int durationMillis) {
        this.durationMillis = durationMillis;
    }
    
    public List<String> getTranslations() {
        return translations;
    }
    
    public void setTranslations(List<String> translations) {
        this.translations = translations;
    }
    
    public PusherInfo getPusherInfo() {
        return pusherInfo;
    }
    
    public void setPusherInfo(PusherInfo pusherInfo) {
        this.pusherInfo = pusherInfo;
    }
    
    public MusicResourceInfo getMusicResourceInfo() {
        return musicResourceInfo;
    }
    
    public void setMusicResourceInfo(MusicResourceInfo musicResourceInfo) {
        this.musicResourceInfo = musicResourceInfo;
    }
    
    public String getArtistNames() {
        if (artists == null || artists.isEmpty()) {
            return "";
        }
        return String.join(", ", artists.stream().map(Artist::getName).toList());
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof MusicDetail other && this.id == other.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    @Override
    public String toString() {
        return name + " - " + getArtistNames();
    }
}
