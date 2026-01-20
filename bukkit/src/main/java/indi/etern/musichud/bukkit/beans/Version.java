package indi.etern.musichud.bukkit.beans;

/**
 * Represents mod version for compatibility checking.
 */
public record Version(long mayor, long minor, long patch, BuildType build) implements Comparable<Version> {
    public static final Version CURRENT = new Version(1, 0, 2, BuildType.STABLE);
    public static final Version LEAST_CAPABLE = new Version(1, 0, 0, BuildType.STABLE);
    
    public enum BuildType {
        ALPHA("alpha"),
        BETA("beta"),
        PRE_RELEASE("pre-release"),
        STABLE("stable");
        
        private final String name;
        
        BuildType(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public static BuildType fromOrdinal(int ordinal) {
            BuildType[] values = values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
            return STABLE;
        }
    }
    
    public static boolean capableWith(Version clientVersion) {
        return clientVersion.compareTo(LEAST_CAPABLE) >= 0;
    }
    
    public long[] toLongArray() {
        return new long[]{mayor, minor, patch, build.ordinal()};
    }
    
    public static Version fromLongArray(long[] longs) {
        if (longs.length < 4) {
            return CURRENT;
        }
        return new Version(longs[0], longs[1], longs[2], BuildType.fromOrdinal((int) longs[3]));
    }
    
    @Override
    public int compareTo(Version other) {
        int result = Long.compare(mayor, other.mayor);
        if (result != 0) return result;
        result = Long.compare(minor, other.minor);
        if (result != 0) return result;
        result = Long.compare(patch, other.patch);
        if (result != 0) return result;
        return Integer.compare(build.ordinal(), other.build.ordinal());
    }
    
    @Override
    public String toString() {
        return mayor + "." + minor + "." + patch + "-" + build;
    }
}
