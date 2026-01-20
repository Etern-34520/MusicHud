package indi.etern.musichud.bukkit.beans;

/**
 * Format type for audio files.
 */
public enum FormatType {
    FLAC,
    MP3,
    AUTO;
    
    public static FormatType fromString(String value) {
        if (value == null) return AUTO;
        try {
            return FormatType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
