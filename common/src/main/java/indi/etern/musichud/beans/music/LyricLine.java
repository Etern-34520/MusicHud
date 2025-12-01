package indi.etern.musichud.beans.music;

import lombok.Data;

import java.time.Duration;

@Data
public class LyricLine {
    Duration startTime;
    String text;
    String translatedText;
}
