package indi.etern.musichud.bukkit.beans;

import java.util.Objects;

/**
 * Represents album information for a music track.
 */
public class AlbumInfo {
    public static final AlbumInfo NONE = new AlbumInfo();
    
    private long id;
    private String name = "";
    private String picUrl = "";
    private long picSize;
    
    public AlbumInfo() {
    }
    
    public AlbumInfo(long id, String name, String picUrl, long picSize) {
        this.id = id;
        this.name = name;
        this.picUrl = picUrl;
        this.picSize = picSize;
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
    
    public String getPicUrl() {
        return picUrl;
    }
    
    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
    
    public long getPicSize() {
        return picSize;
    }
    
    public void setPicSize(long picSize) {
        this.picSize = picSize;
    }
    
    public String getThumbnailPicUrl(int size) {
        return picUrl + "?param=" + size + "y" + size;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlbumInfo albumInfo = (AlbumInfo) o;
        return id == albumInfo.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
