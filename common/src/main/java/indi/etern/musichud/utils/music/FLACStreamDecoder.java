package indi.etern.musichud.utils.music;

import org.jflac.FLACDecoder;
import org.jflac.frame.Frame;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.lwjgl.openal.AL10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FLACStreamDecoder implements AudioDecoder {
    private final FLACDecoder decoder;
    private final InputStream inputStream;
    private final int format;
    private final int sampleRate;

    public FLACStreamDecoder(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.decoder = new FLACDecoder(inputStream);

        // 读取FLAC流信息
        try {
            StreamInfo streamInfo = decoder.readStreamInfo();
            this.sampleRate = streamInfo.getSampleRate();
            int channels = streamInfo.getChannels();
            int bitsPerSample = streamInfo.getBitsPerSample();

            // 根据声道数和位深度确定OpenAL格式
            if (channels == 1) {
                if (bitsPerSample == 16) {
                    this.format = AL10.AL_FORMAT_MONO16;
                } else {
                    this.format = AL10.AL_FORMAT_MONO8;
                }
            } else {
                if (bitsPerSample == 16) {
                    this.format = AL10.AL_FORMAT_STEREO16;
                } else {
                    this.format = AL10.AL_FORMAT_STEREO8;
                }
            }

        } catch (Exception e) {
            throw new IOException("Failed to initialize FLAC decoder", e);
        }
    }

    @Override
    public byte[] readChunk(int maxSize) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while (output.size() < maxSize) {
                // 使用readNextFrame()获取Frame对象
                Frame frame = decoder.readNextFrame();
                if (frame == null) break;

                // 调用decodeFrame(Frame, ByteData)方法
                ByteData byteData = decoder.decodeFrame(frame, null);
                if (byteData == null) break;

                // 从ByteData获取字节数组
                byte[] frameData = byteData.getData();
                output.write(frameData, 0, byteData.getLen());
            }

            if (output.size() == 0) return null;
            return output.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getFormat() {
        return format;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            // 忽略关闭错误
        }
    }
}
