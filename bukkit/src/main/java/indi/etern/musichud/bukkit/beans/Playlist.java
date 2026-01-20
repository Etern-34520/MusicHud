package indi.etern.musichud.bukkit.beans;

import indi.etern.musichud.bukkit.beans.user.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a playlist with tracks.
 */
public class Playlist {
    private long id;
    private String name = "";
    private long coverImgId;
    private String coverImgIdStr = "";
    private String coverImgUrl = "";
    private Profile creator = Profile.ANONYMOUS;
    private List<MusicDetail> tracks = new ArrayList<>();
    
    public Playlist() {
    }
    
    public Playlist(long id, String name, String coverImgUrl, List<MusicDetail> tracks) {
        this.id = id;
        this.name = name;
        this.coverImgUrl = coverImgUrl;
        this.tracks = tracks != null ? tracks : new ArrayList<>();
    }

    public Playlist(long id, String name, long coverImgId, String coverImgIdStr, String coverImgUrl, Profile creator, List<MusicDetail> tracks) {
        this.id = id;
        this.name = name;
        this.coverImgId = coverImgId;
        this.coverImgIdStr = coverImgIdStr;
        this.coverImgUrl = coverImgUrl;
        this.creator = creator != null ? creator : Profile.ANONYMOUS;
        this.tracks = tracks != null ? tracks : new ArrayList<>();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getCoverImgId() {
        return coverImgId;
    }
    
    public void setCoverImgId(long coverImgId) {
        this.coverImgId = coverImgId;
    }

    public String getCoverImgIdStr() {
        return coverImgIdStr;
    }

    public void setCoverImgIdStr(String coverImgIdStr) {
        this.coverImgIdStr = coverImgIdStr;
    }

    public String getCoverImgUrl() {
        return coverImgUrl;
    }

    public void setCoverImgUrl(String coverImgUrl) {
        this.coverImgUrl = coverImgUrl;
    }

    // Backward compatibility with earlier naming
    public String getCoverUrl() {
        return coverImgUrl;
    }
    
    public void setCoverUrl(String coverUrl) {
        this.coverImgUrl = coverUrl;
    }
    
    public Profile getCreator() {
        return creator;
    }

    public void setCreator(Profile creator) {
        this.creator = creator;
    }

    public List<MusicDetail> getTracks() {
        return tracks;
    }

    public void setTracks(List<MusicDetail> tracks) {
        this.tracks = tracks;
    }
}
