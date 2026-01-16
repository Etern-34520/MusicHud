package indi.etern.musichud.client.ui.hud.metadata;

public class BackgroundColor {
    public volatile int colorTL, colorTR, colorBR, colorBL;

    public BackgroundColor(int colorTL, int colorTR, int colorBL, int colorBR) {
        this.colorTL = colorTL;
        this.colorTR = colorTR;
        this.colorBR = colorBR;
        this.colorBL = colorBL;
    }

    public static BackgroundColor solid(int color) {
        return new BackgroundColor(color, color, color, color);
    }
}
