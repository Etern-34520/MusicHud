package indi.etern.musichud.client.music.decoder;

public interface AudioDecoder extends AutoCloseable {
    byte[] readChunk(long maxSize);
    int getFormat();
    int getSampleRate();
    void close();
}
