package indi.etern.musichud.beans.music;

import indi.etern.musichud.interfaces.IntegerCodeEnum;
import lombok.Getter;

public enum OriginCoverType implements IntegerCodeEnum {
    UNKNOWN(0), ORIGINAL(1), REPRINT(2);

    @Getter
    private final int code;
    OriginCoverType(int code) {
        this.code = code;
    }
}
