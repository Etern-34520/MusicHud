package indi.etern.musichud.bukkit.beans;

/**
 * Fee type for music resources.
 */
public enum Fee {
    FREE(0),
    VIP(1),
    SEPARATELY_PURCHASE(4),
    VIP_FOR_HIGHER_QUALITY(8),
    UNSET(-1);
    
    private final int code;
    
    Fee(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static Fee fromCode(int code) {
        for (Fee fee : values()) {
            if (fee.code == code) {
                return fee;
            }
        }
        return UNSET;
    }
}
