package indi.etern.musichud.beans.music;

import indi.etern.musichud.interfaces.IntegerCodeEnum;
import lombok.Getter;

public enum MusicResourceType implements IntegerCodeEnum {
    NORMAL(0), CLOUD_DISK_UNIQUE(1), CLOUD_DISK_NON_UNIQUE(2);

    @Getter
    final int code;

    MusicResourceType(int code) {
        this.code = code;
    }
}
