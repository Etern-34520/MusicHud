package indi.etern.musichud.client.ui.hud.metadata;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
public enum VPosition {
    TOP("顶部") {
        @Override
        float calcY(float y, GuiGraphics graphics, Layout hudLayout) {
            return y;
        }
    }, CENTER("中间") {
        @Override
        float calcY(float y, GuiGraphics graphics, Layout hudLayout) {
            return (float) graphics.guiHeight() / 2 + y - hudLayout.height / 2;
        }
    }, BOTTOM("底部") {
        @Override
        float calcY(float y, GuiGraphics graphics, Layout hudLayout) {
            return graphics.guiHeight() - hudLayout.height - y;
        }
    };

    private final String displayName;

    VPosition(String displayName) {
        this.displayName = displayName;
    }


    abstract float calcY(float y, GuiGraphics graphics, Layout hudLayout);

    @Override
    public String toString() {
        return displayName;
    }
}
