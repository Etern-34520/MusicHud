package indi.etern.musichud.bukkit.beans.login;

import java.time.ZonedDateTime;

public class LoginCookieInfo {
    private LoginType type;
    private String rawCookie;
    private ZonedDateTime generateTime;

    public LoginCookieInfo(LoginType type, String rawCookie, ZonedDateTime generateTime) {
        this.type = type;
        this.rawCookie = rawCookie;
        this.generateTime = generateTime;
    }

    public LoginType getType() {
        return type;
    }

    public void setType(LoginType type) {
        this.type = type;
    }

    public String getRawCookie() {
        return rawCookie;
    }

    public void setRawCookie(String rawCookie) {
        this.rawCookie = rawCookie;
    }

    public ZonedDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(ZonedDateTime generateTime) {
        this.generateTime = generateTime;
    }
}
