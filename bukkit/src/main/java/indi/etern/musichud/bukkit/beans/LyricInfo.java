package indi.etern.musichud.bukkit.beans;

/**
 * Contains lyric information including main lyrics and translation.
 */
public class LyricInfo {
    public static final LyricInfo NONE = new LyricInfo();
    
    private int code = 0;
    private Lyric lrc = Lyric.NONE;
    private Lyric tlyric = Lyric.NONE;
    
    public LyricInfo() {
    }
    
    public LyricInfo(Lyric lrc, Lyric tlyric) {
        this.lrc = lrc;
        this.tlyric = tlyric;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public Lyric getLrc() {
        return lrc;
    }
    
    public void setLrc(Lyric lrc) {
        this.lrc = lrc;
    }
    
    public Lyric getTlyric() {
        return tlyric;
    }
    
    public void setTlyric(Lyric tlyric) {
        this.tlyric = tlyric;
    }
}
