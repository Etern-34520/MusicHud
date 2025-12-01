package indi.etern.musichud.client.ui.hud.renderer;

import icyllis.modernui.mc.text.ModernStringSplitter;
import icyllis.modernui.mc.text.TextLayoutEngine;
import indi.etern.musichud.client.ui.hud.metadata.Layout;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;

@Getter
@Setter
public class TextRenderer {
    private TextStyle currentTextData;
    private Layout layout;
    private int baseColor;
    private Position position;

    public enum Position {
        LEFT {
            @Override
            float computeX(float startX, float scale, String text) {
                return startX;
            }
        }, CENTER {
            @Override
            float computeX(float startX, float scale, String text) {
                ModernStringSplitter splitter = TextLayoutEngine.getInstance().getStringSplitter();
                return startX - 0.5f * splitter.measureText(text) * scale;
            }
        }, RIGHT {
            @Override
            float computeX(float startX, float scale, String text) {
                ModernStringSplitter splitter = TextLayoutEngine.getInstance().getStringSplitter();
                return startX - splitter.measureText(text) * scale;
            }
        };
        abstract float computeX(float startX, float scale, String text);
    }

    public TextRenderer() {
    }

    public void configureLayout(Layout layout, int baseColor, Position position) {
        this.layout = layout;
        this.baseColor = baseColor;
        this.position = position;
    }

    public void setText(String text) {
        if (currentTextData == null) currentTextData = new TextStyle(text, baseColor);
        else currentTextData.text = text;
    }

    public void render(GuiGraphics gr, DeltaTracker deltaTracker) {
        if (currentTextData == null || layout.height <= 0 || layout.width <= 0) {
            return;
        }

        float scale = layout.height / 8;

        // 获取文本
        String text = currentTextData.text;
        if (text == null || text.isEmpty()) return;
        ModernStringSplitter splitter = TextLayoutEngine.getInstance().getStringSplitter();
        int maxIndex = splitter.indexByWidth(text, layout.width / scale, Style.EMPTY);

        String trimmedText = text.substring(0, maxIndex);
        if (maxIndex < text.length()) {
            int endIndex = trimmedText.length() - 2;
            if (endIndex <= 3) {
                return;
            }
            trimmedText = trimmedText.substring(0, endIndex) + "...";
        }

        // 保存当前变换状态
        gr.pose().pushMatrix();

        // 应用位置和缩放
        Layout.AbsolutePosition absolutePosition = layout.calcAbsolutePosition(gr);
        gr.pose().translate(position.computeX(absolutePosition.x(), scale, trimmedText), absolutePosition.y());
        gr.pose().scale(scale, scale);

        gr.drawString(Minecraft.getInstance().font, trimmedText, 0, 0, currentTextData.baseColor);

        // 恢复变换状态
        gr.pose().popMatrix();
    }

    public float calcDisplayWidth() {
        if (currentTextData == null || currentTextData.text == null || currentTextData.text.isEmpty()) {
            return 0f;
        } else {
            ModernStringSplitter splitter = TextLayoutEngine.getInstance().getStringSplitter();
            return splitter.measureText(currentTextData.text) * (layout.height / 8f);
        }
    }

    public static class TextStyle {
        public String text;
        public final int baseColor;  // RGB 部分

        public TextStyle(String text, int baseColor) {
            this.text = text;
            this.baseColor = baseColor;
        }
    }
}