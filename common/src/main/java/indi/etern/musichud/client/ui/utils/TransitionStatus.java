package indi.etern.musichud.client.ui.utils;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class TransitionStatus<NextData> {
    private static int durationMs;
    @Getter
    public volatile float progress = 0.0f;
    @Getter
    private volatile boolean isTransitioning = false;
    private volatile long transitionStartTime = 0;
    @Getter
    private NextData nextData;

    @Setter
    private Consumer<NextData> onCompleteCallback;

    public TransitionStatus(int durationMs) {
        TransitionStatus.durationMs = durationMs;
    }

    public void startTransition(NextData nextData) {
        this.transitionStartTime = System.currentTimeMillis();
        this.isTransitioning = true;
        this.progress = 0.0f;
        this.nextData = nextData;
    }

    public void updateTransition() {
        if (!isTransitioning) return;

        long elapsed = System.currentTimeMillis() - transitionStartTime;
        this.progress = Mth.clamp((float) elapsed / durationMs, 0.0f, 1.0f);
        if (progress >= 1.0f) {
            progress = 0.0f;
            isTransitioning = false;
            if (onCompleteCallback != null) {
                onCompleteCallback.accept(nextData);
            }
        }
    }
}