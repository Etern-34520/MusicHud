package indi.etern.musichud.client.ui.hud.metadata;

import net.minecraft.resources.ResourceLocation;

// 背景图片数据(4张:模糊/非模糊 × 当前/下一张)
public class BackgroundImage {
    public volatile ResourceLocation currentBlurredLocation;
    public volatile ResourceLocation currentUnblurredLocation;

    public volatile float currentAspect;

    public BackgroundImage(ResourceLocation currentBlurred, ResourceLocation currentUnblurred, float currentAspect) {
        this.currentBlurredLocation = currentBlurred;
        this.currentUnblurredLocation = currentUnblurred;
        this.currentAspect = currentAspect;
    }
}
