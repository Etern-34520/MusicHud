package indi.etern.musichud.client.ui.hud.metadata;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;

public class Layout {
    public volatile float x, y, width, height;
    public volatile float radius;
    public volatile HorizontalAlign hPosition;
    public volatile VerticalAlign verticalAlign;
    @Setter
    @Getter
    private volatile Layout parent;

    public Layout(float x, float y, float width, float height, float radius) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.radius = radius;
        hPosition = HorizontalAlign.LEFT;
        verticalAlign = VerticalAlign.TOP;
    }

    public Layout(float x, float y, float width, float height, float radius, HorizontalAlign hPosition, VerticalAlign verticalAlign) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.hPosition = hPosition;
        this.verticalAlign = verticalAlign;
    }

    public record AbsolutePosition(float x, float y) {}
    public AbsolutePosition calcAbsolutePosition(GuiGraphics graphics) {
        if (parent != null) {
            AbsolutePosition absolutePosition = parent.calcAbsolutePosition(graphics);
            float xOffset = hPosition.calcX(x, graphics, getRootLayout());
            float yOffset = verticalAlign.calcY(y, graphics, getRootLayout());
            return new AbsolutePosition(absolutePosition.x + xOffset, absolutePosition.y + yOffset);
        } else {
            float xOffset = hPosition.calcX(x, graphics, getRootLayout());
            float yOffset = verticalAlign.calcY(y, graphics, getRootLayout());
            return new AbsolutePosition(xOffset, yOffset);
        }
    }

    public AbsolutePosition calcAbsoluteCenterPosition(GuiGraphics graphics) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        AbsolutePosition absolutePosition = calcAbsolutePosition(graphics);
        float centerX = absolutePosition.x() + halfWidth;
        float centerY = absolutePosition.y() + halfHeight;
        return new AbsolutePosition(centerX, centerY);
    }

    public Layout getRootLayout() {
        if (parent != null) {
            return parent.getRootLayout();
        } else {
            return this;
        }
    }

    public static Layout ofTextLayout(float x, float y, float maxWidth, float fontSize) {
        return new Layout(x, y, maxWidth, fontSize, 0);
    }
}
