package indi.etern.musichud.client.ui.utils.lyrics;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.LyricInfo;
import indi.etern.musichud.beans.music.LyricLine;
import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.client.ui.utils.lyrics.beans.MetaInfoLine;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParser {

    private static final Pattern mainPattern = Pattern.compile("\\[[0-9:.]+].*");
    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(java.time.temporal.ChronoField.MILLI_OF_SECOND, 1, 3, true)
            .toFormatter();

    public static ArrayDeque<LyricLine> parse(LyricInfo lyricInfo) {
        String lyric = lyricInfo.getLrc().getLyric();
        String translatedLyric = lyricInfo.getTlyric().getLyric();
        LinkedHashMap<Duration, LyricLine> map = new LinkedHashMap<>();
        List<LyricLine> lyricLinesWithoutValidTimestamp = new ArrayList<>(0);
        matchLine(lyric, (duration, s) -> {
            LyricLine lyricLine = map.get(duration);
            String lyricString = s == null ? "" : s.replace('\u00A0', ' ').trim();
            if (lyricLine == null) {
                lyricLine = new LyricLine();
                lyricLine.setStartTime(duration);
                lyricLine.setText(lyricString);
                if (duration != null) {
                    map.put(duration, lyricLine);
                } else if (lyricLine.getText() != null && !lyricLine.getText().startsWith("}")) {
                    lyricLinesWithoutValidTimestamp.add(lyricLine);
                }
            } else {
                lyricLine.setText(lyricLine.getText() + "\n" + lyricString);
            }
        });
        if (ClientConfigDefinition.showTranslatedCnLyrics.get()) {
            matchLine(translatedLyric, (duration, s) -> {
                LyricLine lyricLine = map.get(duration);
                if (lyricLine == null) {
                    lyricLine = new LyricLine();
                    lyricLine.setStartTime(duration);
                    if (duration != null) {
                        map.put(duration, lyricLine);
                    } else {
                        lyricLinesWithoutValidTimestamp.add(lyricLine);
                    }
                }
                if (s != null) {
                    lyricLine.setTranslatedText(s.replace('\u00A0', ' ').trim());
                } else {
                    lyricLine.setTranslatedText("");
                }
            });
        }
        ArrayDeque<LyricLine> lyricLines = new ArrayDeque<>(lyricLinesWithoutValidTimestamp);
        lyricLines.addAll(map.values());
        return lyricLines;
    }

    static Duration parseToDuration(String timeString) {
        try {
            String normalizedTime = timeString;
            String[] parts = timeString.split(":");

            if (parts.length == 2) {
                normalizedTime = "00:" + timeString;
            } else if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid time format: " + timeString);
            }
            LocalTime localTime = LocalTime.parse(normalizedTime, TIME_FORMATTER);
            return Duration.between(LocalTime.MIDNIGHT, localTime);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format: " + timeString, e);
        }
    }

    static void matchLine(String lyric, BiConsumer<Duration, String> matchedConsumer) {
        List<MetaInfoLine> metaInfoLines = RegexJsonExtractor.extractJsonObjectsSafely(lyric, MetaInfoLine.class);
        metaInfoLines.forEach(metaInfoLine -> {
            matchedConsumer.accept(metaInfoLine.getTimestampDuration(), metaInfoLine.getText());
        });
        Matcher matcher = mainPattern.matcher(lyric);
        while (matcher.find()) {
            String item = matcher.group();
            try {
                if (!item.contains(".")) {
                    int colonCount = Math.toIntExact(item.chars().filter(c -> c == ':').count());
                    int i = item.lastIndexOf(":");
                    StringBuilder stringBuilder = new StringBuilder(item);
                    if (colonCount == 2) {
                        stringBuilder.setCharAt(i, '.');
                    } else if (colonCount == 1) {
                        stringBuilder.insert(i + 3, ".000");
                    }
                    item = stringBuilder.toString();
                } else {
                    int i = item.indexOf(']');
                    int millisDigit = i - item.indexOf('.') - 1;
                    if (millisDigit < 3) {
                        StringBuilder stringBuilder = new StringBuilder(item);
                        stringBuilder.insert(i, "0".repeat(3 - millisDigit));
                        item = stringBuilder.toString();
                    }
                }
                String[] split = item.split("]", 2);
                String timestamp = split[0];
                String lyricLineContent = split[1];
                try {
                    Duration duration = parseToDuration(timestamp.substring(1, timestamp.length() - 1));
                    matchedConsumer.accept(duration, lyricLineContent);
                } catch (Exception ignored) {
                    matchedConsumer.accept(null, lyricLineContent);
                }
            } catch (Exception e) {
                MusicHud.getLogger(LyricParser.class).debug("failed to parse line \"{}\"", item);
            }
        }
    }
}