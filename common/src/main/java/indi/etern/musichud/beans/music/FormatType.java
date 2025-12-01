package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import indi.etern.musichud.utils.music.AudioDecoder;
import indi.etern.musichud.utils.music.FLACStreamDecoder;
import indi.etern.musichud.utils.music.MP3StreamDecoder;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.Arrays;

public enum FormatType {
    FLAC {
        @Override
        @SneakyThrows
        public AudioDecoder newDecoder(InputStream inputStream) {
            return new FLACStreamDecoder(inputStream);
        }
    },
    MP3 {
        @Override
        public AudioDecoder newDecoder(InputStream inputStream) {
            return new MP3StreamDecoder(inputStream);
        }
    },
    UNSET {
        @Override
        public AudioDecoder newDecoder(InputStream inputStream) {
            throw new UnsupportedOperationException();
        }
    };
    @JsonCreator
    public static FormatType fromString(String value) {
        if (value == null) return null;
        return Arrays.stream(FormatType.values())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow();
    }

    public abstract AudioDecoder newDecoder(InputStream inputStream);
}
