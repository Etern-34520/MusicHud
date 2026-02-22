package indi.etern.musichud.beans.music;

import indi.etern.musichud.client.music.decoder.AudioDecoder;
import indi.etern.musichud.client.music.decoder.AudioFormatDetector;
import indi.etern.musichud.client.music.decoder.FLACStreamDecoder;
import indi.etern.musichud.client.music.decoder.MP3StreamDecoder;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;

public enum FormatType {
    FLAC {
        @Override
        @SneakyThrows
        public AudioDecoder newDecoder(BufferedInputStream inputStream) {
            return new FLACStreamDecoder(inputStream);
        }
    },
    MP3 {
        @Override
        public AudioDecoder newDecoder(BufferedInputStream inputStream) {
            return new MP3StreamDecoder(inputStream);
        }
    },
    AUTO {
        @Override
        @SneakyThrows
        public AudioDecoder newDecoder(BufferedInputStream inputStream) {
            return AudioFormatDetector.detectFormat(inputStream).newDecoder(inputStream);
        }
    };

    public abstract AudioDecoder newDecoder(BufferedInputStream inputStream);
}
