package indi.etern.musichud.beans.music;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class LyricLine {
    Duration startTime;
    String text;
    String translatedText;

    public String getTranslatedText() {
        return Objects.requireNonNullElse(translatedText, "");
    }

    public String getText() {
        return Objects.requireNonNullElse(text, "");
    }

    public Duration getStartTime() {
        return Objects.requireNonNullElse(startTime, Duration.ZERO);
    }
}