package indi.etern.musichud.bukkit.beans;

/**
 * Contains information about music resource URL and quality.
 */
public class MusicResourceInfo {
    public static final MusicResourceInfo NONE = new MusicResourceInfo();
    
    private long id;
    private String url = "";
    private int bitrate;
    private long size;
    private FormatType type = FormatType.AUTO;
    private String md5 = "";
    private Fee fee = Fee.UNSET;
    private int time;
    private LyricInfo lyricInfo = LyricInfo.NONE;
    
    public MusicResourceInfo() {
    }
    
    public MusicResourceInfo(long id, String url, int bitrate, long size, FormatType type, 
                            String md5, Fee fee, int time, LyricInfo lyricInfo) {
        this.id = id;
        this.url = url;
        this.bitrate = bitrate;
        this.size = size;
        this.type = type;
        this.md5 = md5;
        this.fee = fee;
        this.time = time;
        this.lyricInfo = lyricInfo;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public FormatType getType() {
        return type;
    }
    
    public void setType(FormatType type) {
        this.type = type;
    }
    
    public String getMd5() {
        return md5;
    }
    
    public void setMd5(String md5) {
        this.md5 = md5;
    }
    
    public Fee getFee() {
        return fee;
    }
    
    public void setFee(Fee fee) {
        this.fee = fee;
    }
    
    public int getTime() {
        return time;
    }
    
    public void setTime(int time) {
        this.time = time;
    }
    
    public LyricInfo getLyricInfo() {
        return lyricInfo;
    }
    
    public void setLyricInfo(LyricInfo lyricInfo) {
        this.lyricInfo = lyricInfo;
    }
    
    public static MusicResourceInfo from(String url, MusicDetail musicDetail) {
        MusicResourceInfo info = new MusicResourceInfo();
        info.url = url;
        info.id = musicDetail.getId();
        info.time = musicDetail.getDurationMillis();
        return info;
    }
}
