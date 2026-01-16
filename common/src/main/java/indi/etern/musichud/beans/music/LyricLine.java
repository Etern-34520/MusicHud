package indi.etern.musichud.beans.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LyricLine {
    Duration startTime;
    String text;
    String translatedText;
}