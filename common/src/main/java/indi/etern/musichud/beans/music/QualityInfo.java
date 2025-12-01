package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class QualityInfo {
    @JsonProperty("br")
    int bitRate;
    @JsonProperty("size")
    long sizeBytes;
    @JsonProperty("vd")
    int volumeDelta;
    @JsonProperty("sr")
    int sampleRate;

    public static final QualityInfo NONE = new QualityInfo(
            0, 0, 0, 0
    );
}