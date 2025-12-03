package indi.etern.musichud.utils.music;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.FormatType;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamAudioPlayer {
    // 添加重连相关变量
    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_SIZE = 65536;
    private static final Logger LOGGER = MusicHud.getLogger(StreamAudioPlayer.class);
    private static volatile StreamAudioPlayer instance = null;

    private final int[] buffers = new int[BUFFER_COUNT];
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Minecraft minecraft = Minecraft.getInstance();
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final int maxRetries = 5;
    private final int retryDelayMs = 0;
    private final int retryDelayDelta = 1000;
    private int source = 0;
    private LocalDateTime startPlayingTime;
    private float lastVolume;
    private Thread playingThread;

    public static StreamAudioPlayer getInstance() {
        if (instance == null) {
            synchronized (StreamAudioPlayer.class) {
                if (instance == null) {
                    instance = new StreamAudioPlayer();
                }
            }
        }
        return instance;
    }

    public CompletableFuture<LocalDateTime> playAsyncFromUrl(String urlString, FormatType formatType, LocalDateTime startTime) {
        synchronized (StreamAudioPlayer.class) {
            try {
                source = AL10.alGenSources();
                checkALError("alGenSources");

                for (int i = 0; i < BUFFER_COUNT; i++) {
                    buffers[i] = AL10.alGenBuffers();
                    checkALError("alGenBuffers");
                }

                // 配置为非空间播放
                AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0);
                checkALError("source configuration");
                lastVolume = 1;

                initialized.set(true);
            } catch (Exception e) {
                cleanup();
                return CompletableFuture.failedFuture(e);
            }
        }

        playing.set(true);

        CompletableFuture<LocalDateTime> startPlayingFuture = new CompletableFuture<>();
        streamingPlayWithRetry(urlString, formatType, startTime, 0).thenAccept(startPlayingFuture::complete);
        return startPlayingFuture;
    }

    private long calculateSamplesToSkip(AudioDecoder audioDecoder, LocalDateTime originalStartTime) {
        if (originalStartTime == null) {
            return 0;
        }
        java.time.Duration totalDuration = java.time.Duration.between(originalStartTime, LocalDateTime.now());
        if (totalDuration.isPositive()) {
            long totalSeconds = totalDuration.getSeconds();
            return totalSeconds * audioDecoder.getSampleRate();
        } else {
            return 0;
        }
    }

    // 带重试的播放方法
    private CompletableFuture<LocalDateTime> streamingPlayWithRetry(
            String urlString,
            FormatType formatType,
            LocalDateTime startTime,
            int retryCount) {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            InputStream inputStream = connection.getInputStream();
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream, 8192);

            FormatType detectedFormatType = AudioFormatDetector.detectFormat(bufferedStream);
            if (detectedFormatType != formatType) {
                LOGGER.warn("Detected format type is not equals to resource format type, using detected");
            }

            AudioDecoder decoder = detectedFormatType.newDecoder(bufferedStream);
            // 统一计算需要跳过的样本数
            long samplesToSkip = calculateSamplesToSkip(decoder, startTime);
            if (samplesToSkip > 0) {
                skipAudioSamples(decoder, samplesToSkip);
            }

            return startPlayingAsync(urlString, formatType, startTime, retryCount, decoder);
        } catch (Exception e) {
            LOGGER.error("Catch an exception, retrying", e);
            if (retryCount < maxRetries) {
                reconnecting.set(true);

                try {
                    Thread.sleep(retryDelayMs + (long) retryCount * retryDelayDelta);
                    // 重连时保持原始开始时间
                    return streamingPlayWithRetry(urlString, formatType, startTime, retryCount + 1);
                } catch (InterruptedException ie) {
                    return CompletableFuture.failedFuture(ie);
                }
            }
            return CompletableFuture.failedFuture(e);
        }
    }

    private @NotNull CompletableFuture<LocalDateTime> startPlayingAsync(
            String urlString,
            FormatType formatType,
            LocalDateTime startTime,
            int retryCount,
            AudioDecoder decoder) {
        validateAudioFormat(decoder);

        CompletableFuture<LocalDateTime> future = new CompletableFuture<>();
        MusicHud.EXECUTOR.execute(() -> {
            playingThread = Thread.currentThread();
            try {
                synchronized (StreamAudioPlayer.class) {
                    if (!initialized.get() || source == 0) return;

                    for (int i = 0; i < BUFFER_COUNT && playing.get(); i++) {
                        byte[] audioData = decoder.readChunk(BUFFER_SIZE);
                        if (audioData == null) break;

                        // 验证音频数据有效性
                        int format = decoder.getFormat();
                        if (!isValidAudioData(audioData, format)) {
                            LOGGER.warn("Invalid audio data detected, skipping buffer");
                            continue;
                        }

                        if (audioData.length == 0) {
                            LOGGER.warn("Empty audio data, skipping buffer");
                            continue;
                        }

                        ByteBuffer directBuffer = ByteBuffer.allocateDirect(audioData.length);
                        directBuffer.put(audioData);
                        directBuffer.flip();

                        AL10.alBufferData(buffers[i], format, directBuffer, decoder.getSampleRate());
                        checkALError("alBufferData");
                        AL10.alSourceQueueBuffers(source, buffers[i]);
                        checkALError("alSourceQueueBuffers");
                    }
                }

                while (playing.get()) {
                    try {
                        synchronized (StreamAudioPlayer.class) {
                            float musicVolume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
                            if (lastVolume != musicVolume && source != 0 && AL10.alIsSource(source)) {
                                AL10.alSourcef(source, AL10.AL_GAIN, musicVolume);
                                lastVolume = musicVolume;
                            }
                            if (!initialized.get() || source == 0) break;

                            int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                            checkALError("alGetSourcei");

                            startPlayingTime = startTime == null ? LocalDateTime.now() : startTime;
                            future.complete(startPlayingTime);
                            while (processed-- > 0 && playing.get()) {
                                int[] buffer = new int[1];
                                AL10.alSourceUnqueueBuffers(source, buffer);
                                checkALError("alSourceUnqueueBuffers");

                                byte[] audioData = decoder.readChunk(BUFFER_SIZE);
                                if (audioData == null) {
                                    playing.set(false);
                                    break;
                                }

                                ByteBuffer directBuffer = ByteBuffer.allocateDirect(audioData.length);
                                directBuffer.put(audioData);
                                directBuffer.flip();

                                AL10.alBufferData(buffer[0], decoder.getFormat(), directBuffer, decoder.getSampleRate());
                                checkALError("alBufferData");
                                AL10.alSourceQueueBuffers(source, buffer[0]);
                                checkALError("alSourceQueueBuffers");
                            }

                            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                            if (state != AL10.AL_PLAYING && playing.get()) {
                                AL10.alSourcePlay(source);
                                checkALError("alSourcePlay");
                            }
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        reconnecting.set(false);
                        lastVolume = 1;
                        cleanup();
                        break;
                    } catch (Exception e) {
                        LOGGER.error("Playback error: " + e.getMessage());
                        cleanup();

                        if (retryCount < maxRetries && playing.get()) {
                            reconnecting.set(true);

                            // 延迟重连
                            try {
                                Thread.sleep(retryDelayMs + (long) retryCount * retryDelayDelta);
                            } catch (InterruptedException ignored) {
                                reconnecting.set(false);
                                lastVolume = 1;
                                cleanup();
                                break;
                            }

                            LOGGER.info("Attempting reconnection {} of {}", retryCount + 1, maxRetries);
                            streamingPlayWithRetry(urlString, formatType, startTime, retryCount + 1);
                            return;
                        } else {
                            playing.set(false);
                            future.completeExceptionally(e);
                            return;
                        }
                    }
                }
            } finally {
                reconnecting.set(false);
                lastVolume = 1;
                cleanup();
            }
        });
        return future;
    }

    private byte[] convert8BitTo16Bit(byte[] audioData) {
        if (audioData == null) return null;

        short[] samples = new short[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            // 将8位无符号转换为16位有符号
            samples[i] = (short) (((audioData[i] & 0xFF) - 128) * 256);
        }

        // 转换回字节数组（16位）
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    // 跳过音频样本
    private void skipAudioSamples(AudioDecoder decoder, long samplesToSkip) {
        long samplesSkipped = 0;
        int bytesPerSample = (decoder.getFormat() == AL10.AL_FORMAT_STEREO16 ? 4 : 2);

        while (samplesSkipped < samplesToSkip && playing.get()) {
            byte[] audioData = decoder.readChunk(BUFFER_SIZE);
            if (audioData == null) break;

            long chunkSamples = audioData.length / bytesPerSample;
            samplesSkipped += chunkSamples;
        }

        LOGGER.info("Skipped {} samples to resume playback", samplesSkipped);
    }

    private void checkALError(String operation) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            LOGGER.error("OpenAL Error during {}: {}", operation, error);
            throw new RuntimeException("OpenAL Error during " + operation + ": " + error);
        }
    }

    @SneakyThrows
    public void stop() {
        if (playingThread != null) {
            playingThread.interrupt();
        }
        lastVolume = 1;
        playing.set(false);
        cleanup();
    }

    private boolean isValidAudioData(byte[] audioData, int format) {
        if (audioData == null || audioData.length == 0) {
            return false;
        }

        // 检查数据长度是否符合格式要求
        int bytesPerSample = (format == AL10.AL_FORMAT_STEREO16 ? 4 : 2);
        if (audioData.length % bytesPerSample != 0) {
            LOGGER.warn("Audio data length {} not aligned to sample size {}", audioData.length, bytesPerSample);
            return false;
        }

        return true;
    }

    private void cleanup() {
        synchronized (StreamAudioPlayer.class) {
            try {
                if (source != 0 && AL10.alIsSource(source)) {
                    // 强制停止并清理所有缓冲区
                    AL10.alSourceStop(source);
                    checkALError("alSourceStop");


                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) {
                        int[] buffer = new int[1];
                        AL10.alSourceUnqueueBuffers(source, buffer);
                        checkALError("alSourceUnqueueBuffers");
                    }

                    // 清空所有排队的缓冲区
                    int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                    if (queued > 0) {
                        AL10.alSourcei(source, AL10.AL_BUFFER, 0); // 清空源的所有缓冲区
                        checkALError("alSourceUnqueueBuffers");
                    }

                    AL10.alDeleteSources(source);
                    checkALError("alDeleteSources");
                    source = 0;
                }

                // 彻底清理所有缓冲区
                for (int i = 0; i < buffers.length; i++) {
                    if (buffers[i] != 0 && AL10.alIsBuffer(buffers[i])) {
                        AL10.alDeleteBuffers(buffers[i]);
                        checkALError("alDeleteBuffers");
                        buffers[i] = 0;
                    }
                }

                initialized.set(false);
                lastVolume = 1;
            } catch (Exception e) {
                LOGGER.error("Cleanup error", e);
            }
        }
    }

    private void validateAudioFormat(AudioDecoder decoder) {
        int format = decoder.getFormat();
        int sampleRate = decoder.getSampleRate();

        // 验证格式是否受支持
        if (format != AL10.AL_FORMAT_STEREO16 && format != AL10.AL_FORMAT_MONO16) {
//        if (format != AL10.AL_FORMAT_STEREO16 &&
//                format != AL10.AL_FORMAT_MONO16 &&
//                format != AL10.AL_FORMAT_STEREO8 &&
//                format != AL10.AL_FORMAT_MONO8) {
            LOGGER.warn("Unsupported audio format: {}, may cause noise", format);
        }

        // 验证采样率是否合理
        if (sampleRate < 8000 || sampleRate > 96000) {
            LOGGER.warn("Unusual sample rate: {}, may cause playback issues", sampleRate);
        }
    }
    /*private void cleanup() {
        synchronized (StreamAudioPlayer.class) {
            try {
                if (source != 0 && AL10.alIsSource(source)) {
                    AL10.alSourceStop(source);
                    AL10.alDeleteSources(source);
                    source = 0;
                }

                for (int i = 0; i < buffers.length; i++) {
                    if (buffers[i] != 0 && AL10.alIsBuffer(buffers[i])) {
                        AL10.alDeleteBuffers(buffers[i]);
                        buffers[i] = 0;
                    }
                }

                initialized.set(false);
            } catch (Exception e) {
                LOGGER.error("Cleanup error", e);
            }
        }
    }*/

    public boolean isPlaying() {
        return playing.get();
    }
}
