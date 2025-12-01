package indi.etern.musichud.utils.music;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.FormatType;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
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
    private final int maxRetries = 3;
    private final int retryDelayMs = 1000;
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
            if (initialized.get()) {
                stop();
            }

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

    private long calculateSamplesToSkip(AudioDecoder audioDecoder,LocalDateTime originalStartTime) {
        if (originalStartTime == null) {
            return 0;
        }
        java.time.Duration totalDuration = java.time.Duration.between(originalStartTime, LocalDateTime.now());
        long totalSeconds = totalDuration.getSeconds();
        return totalSeconds * audioDecoder.getSampleRate();
    }

    // 带重试的播放方法
    private CompletableFuture<LocalDateTime> streamingPlayWithRetry(
            String urlString,
            FormatType formatType,
            LocalDateTime startTime,
            int retryCount) {
        try {
            AudioDecoder decoder = getAudioDecoder(urlString, formatType);

            // 统一计算需要跳过的样本数
            long samplesToSkip = calculateSamplesToSkip(decoder, startTime);
            if (samplesToSkip > 0) {
                skipAudioSamples(decoder, samplesToSkip);
            }

            return startPlayingAsync(urlString, formatType, startTime, retryCount, decoder);
        } catch (Exception e) {
            if (retryCount < maxRetries) {
                reconnecting.set(true);

                try {
                    Thread.sleep(retryDelayMs);
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
        CompletableFuture<LocalDateTime> future = new CompletableFuture<>();
        MusicHud.EXECUTOR.execute(() -> {
            playingThread = Thread.currentThread();
            try {
                int buffersQueued = 0;
                synchronized (StreamAudioPlayer.class) {
                    if (!initialized.get() || source == 0) return;

                    for (int i = 0; i < BUFFER_COUNT && playing.get(); i++) {
                        byte[] audioData = decoder.readChunk(BUFFER_SIZE);
                        if (audioData == null) break;

                        ByteBuffer directBuffer = ByteBuffer.allocateDirect(audioData.length);
                        directBuffer.put(audioData);
                        directBuffer.flip();

                        AL10.alBufferData(buffers[i], decoder.getFormat(), directBuffer, decoder.getSampleRate());
                        checkALError("alBufferData");
                        AL10.alSourceQueueBuffers(source, buffers[i]);
                        checkALError("alSourceQueueBuffers");
                        buffersQueued++;
                    }
                }

                if (buffersQueued > 0) {
                    synchronized (StreamAudioPlayer.class) {
                        if (initialized.get() && source != 0) {
                            AL10.alSourcePlay(source);
                            checkALError("alSourcePlay");
                            startPlayingTime = startTime == null ? LocalDateTime.now() : startTime;
                            future.complete(startPlayingTime);
                        }
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
                        // 网络异常处理
                        LOGGER.error("Playback error: " + e.getMessage());

                        if (retryCount < maxRetries && playing.get()) {
                            reconnecting.set(true);
                            cleanup();

                            // 延迟重连
                            try {
                                Thread.sleep(retryDelayMs);
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

    private static AudioDecoder getAudioDecoder(String urlString, FormatType formatType) throws URISyntaxException, IOException {
        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        InputStream inputStream = connection.getInputStream();

        return formatType.newDecoder(inputStream);
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
        Thread.sleep(100);
        lastVolume = 1;
        playing.set(false);
        cleanup();
    }

    private void cleanup() {
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
    }

    public boolean isPlaying() {
        return playing.get();
    }
}
