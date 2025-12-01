package indi.etern.musichud.client.ui.utils;

import indi.etern.musichud.beans.music.LyricInfo;
import indi.etern.musichud.beans.music.LyricLine;

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

public class LyricDecoder {

    private static final Pattern mainPattern = Pattern.compile("\\[[^]]+].*");
    private static final Pattern timestampPattern = Pattern.compile("\\[[^]]+]");

    public static ArrayDeque<LyricLine> decode(LyricInfo lyricInfo) {
        String lyric = lyricInfo.getLrc().getLyric();
        String translatedLyric = lyricInfo.getTlyric().getLyric();
        LinkedHashMap<Duration, LyricLine> map = new LinkedHashMap<>();
        List<LyricLine> lyricLinesWithoutValidTimestamp = new ArrayList<>(0);
        matchLine(lyric, (duration, s) -> {
            LyricLine lyricLine = map.get(duration);
            if (lyricLine == null) {
                lyricLine = new LyricLine();
                if (duration != null) {
                    map.put(duration, lyricLine);
                } else {
                    lyricLinesWithoutValidTimestamp.add(lyricLine);
                }
            }
            lyricLine.setStartTime(duration);
            if (s != null) {
                lyricLine.setText(s.trim());
            } else {
                lyricLine.setText("");
            }
        });
        matchLine(translatedLyric, (duration, s) -> {
            LyricLine lyricLine = map.get(duration);
            if (lyricLine == null) {
                lyricLine = new LyricLine();
                if (duration != null) {
                    map.put(duration, lyricLine);
                } else {
                    lyricLinesWithoutValidTimestamp.add(lyricLine);
                }
            }
            lyricLine.setStartTime(duration);
            if (s != null) {
                lyricLine.setTranslatedText(s.trim());
            } else {
                lyricLine.setTranslatedText("");
            }
        });
        ArrayDeque<LyricLine> lyricLines = new ArrayDeque<>(lyricLinesWithoutValidTimestamp);
        lyricLines.addAll(map.values());
        return lyricLines;
    }

    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(java.time.temporal.ChronoField.MILLI_OF_SECOND, 1, 3, true)
            .toFormatter();

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
        Matcher matcher = mainPattern.matcher(lyric);
        while (matcher.find()) {
            String item = matcher.group();
            if (!item.contains(".")) {
                int i = item.lastIndexOf(":");
                StringBuilder stringBuilder = new StringBuilder(item);
                stringBuilder.setCharAt(i, '.');
                item = stringBuilder.toString();
            }
            Matcher timestampMatcher = timestampPattern.matcher(item);
            if (timestampMatcher.find()) {
                String timestamp = timestampMatcher.group();
                int timestampLength = timestamp.length();
                timestamp = timestamp.substring(1,timestamp.length()-1);
                try {
                    Duration duration = parseToDuration(timestamp);
                    matchedConsumer.accept(duration, item.substring(timestampLength));
                } catch (Exception ignored) {
                    matchedConsumer.accept(null, item.substring(timestampLength));
                }
            }
        }
    }
}