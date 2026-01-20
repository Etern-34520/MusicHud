package indi.etern.musichud.bukkit.beans;

/**
 * Represents lyric information.
 */
public class Lyric {
    public static final Lyric NONE = new Lyric(-1, "");
    
    private int version;
    private String lyric;
    
    public Lyric() {
        this.version = -1;
        this.lyric = "";
    }
    
    public Lyric(int version, String lyric) {
        this.version = version;
        this.lyric = lyric;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getLyric() {
        return lyric;
    }
    
    public void setLyric(String lyric) {
        this.lyric = lyric;
    }
}
