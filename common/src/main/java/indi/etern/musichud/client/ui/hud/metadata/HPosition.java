package indi.etern.musichud.client.ui.hud.metadata;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
public enum HPosition {
    LEFT("左侧") {
        @Override
        float calcX(float x, GuiGraphics graphics, Layout hudLayout) {
            return x;
        }
    }, CENTER("中间") {
        @Override
        float calcX(float x, GuiGraphics graphics, Layout hudLayout) {
            return (float) graphics.guiWidth() / 2 + x - hudLayout.width / 2;
        }
    }, RIGHT("右侧") {
        @Override
        float calcX(float x, GuiGraphics graphics, Layout hudLayout) {
            return graphics.guiWidth() - hudLayout.width - x;
        }
    };

    private final String displayName;

    HPosition(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
    abstract float calcX(float x, GuiGraphics graphics, Layout hudLayout);
}
