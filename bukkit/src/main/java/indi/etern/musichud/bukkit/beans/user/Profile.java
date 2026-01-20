package indi.etern.musichud.bukkit.beans.user;

public class Profile {
    public static final Profile ANONYMOUS = new Profile("anonymous", "", "", 0L);

    private String nickname;
    private String avatarUrl;
    private String backgroundUrl;
    private long userId;

    public Profile(String nickname, String avatarUrl, String backgroundUrl, long userId) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.backgroundUrl = backgroundUrl;
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public long getUserId() {
        return userId;
    }
}
