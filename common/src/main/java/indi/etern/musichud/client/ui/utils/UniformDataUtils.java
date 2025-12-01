package indi.etern.musichud.client.ui.utils;

public class UniformDataUtils {
    public static org.joml.Vector4f colorToVector(int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new org.joml.Vector4f(r, g, b, a);
    }
}
