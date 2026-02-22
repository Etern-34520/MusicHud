package indi.etern.musichud.client.ui.utils.lyrics.beans;

import com.google.gson.annotations.SerializedName;

import java.time.Duration;
import java.util.List;

public class MetaInfoLine {
    @SerializedName("t")
    protected int timestampMillis;
    @SerializedName("c")
    protected List<MetaInfoStringWrapper> metaInfoStrings;

    public Duration getTimestampDuration() {
        return Duration.ofMillis(timestampMillis);
    }

    public String getText() {
        return metaInfoStrings.stream().map(metaInfoStringWrapper -> metaInfoStringWrapper.string).reduce((s1, s2) -> s1 + " " + s2).orElse(null);
    }

    public static class MetaInfoStringWrapper {
        @SerializedName("tx")
        protected String string;
    }
}
