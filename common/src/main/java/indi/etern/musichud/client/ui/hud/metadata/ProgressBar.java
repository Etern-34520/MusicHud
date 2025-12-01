package indi.etern.musichud.client.ui.hud.metadata;

import lombok.Getter;

public class ProgressBar {
    @Getter
    private float progress;  // 0.0 - 1.0
    public int fillColorLeft;
    public int fillColorRight;
    public int backgroundColor;
    public float gradientLength;
    public float gradientRightOffset;
    public float transitionBorderRate;

    public ProgressBar(int fillColorLeft, int fillColorRight, int backgroundColor, float gradientLength, float gradientRightOffset, float transitionBorderRate) {
        this.fillColorLeft = fillColorLeft;
        this.fillColorRight = fillColorRight;
        this.backgroundColor = backgroundColor;
        this.gradientLength = gradientLength;
        this.gradientRightOffset = gradientRightOffset;
        this.transitionBorderRate = transitionBorderRate;
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
    }
}
