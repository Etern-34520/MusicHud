package indi.etern.musichud.client.ui.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RGBColor {
    private int red;
    private int green;
    private int blue;
    private float alpha;

    public static RGBColor of(int r, int g, int b) {
        return new RGBColor(r,g,b, 1.0F);
    }
    public static RGBColor of(int r, int g, int b, float a) {
        return new RGBColor(r,g,b,a);
    }
    public int toIntValue() {
        return toIntValueWithAlpha(1.0f);
    }
    public int toIntValueWithAlpha(float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(alpha * 255)));
        int r = Math.min(255, Math.max(0, red));
        int g = Math.min(255, Math.max(0, green));
        int b = Math.min(255, Math.max(0, blue));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
