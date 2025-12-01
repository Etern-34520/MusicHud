package indi.etern.musichud.client.ui.hud.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import icyllis.modernui.mc.GradientRectangleRenderState;
import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.client.ui.hud.metadata.BackgroundImage;
import indi.etern.musichud.client.ui.hud.metadata.HudRenderData;
import indi.etern.musichud.client.ui.hud.metadata.HudUniformWriter;
import indi.etern.musichud.client.ui.hud.metadata.Layout;
import indi.etern.musichud.client.ui.hud.piplines.HudRenderPipelines;
import indi.etern.musichud.client.ui.utils.image.ImageTextureData;
import indi.etern.musichud.client.ui.utils.image.ImageUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2f;

public class BackgroundRenderer {
    private static volatile BackgroundRenderer instance;
    private final HudUniformWriter uniformWriter = new HudUniformWriter();
    private ResourceLocation defaultImageLocation;
    private GpuBufferSlice gpuBufferSlice;
    private HudRenderData currentData;

    public static BackgroundRenderer getInstance() {
        if (instance == null) {
            synchronized (BackgroundRenderer.class) {
                if (instance == null)
                    instance = new BackgroundRenderer();
            }
        }
        return instance;
    }

    public void configure(HudRenderData data) {
        this.currentData = data;
    }

    public void render(GuiGraphics gr, DeltaTracker deltaTracker) {
        if (currentData == null) {
            return;
        }

        gpuBufferSlice = uniformWriter.write(currentData, gr);

        Layout layout = currentData.getLayout();
        BackgroundImage bgImage = currentData.getBackgroundImage();

        var transitionStatus = HudRenderData.getTransitionStatus();
        var nextData = transitionStatus.getNextData();
        ResourceLocation nextBlurredLocation = nextData == null ? null : nextData.nextedBlurred();
        GpuTextureView currentTextureView = getTextureView(bgImage.currentBlurredLocation);
        GpuTextureView nextTextureView = getTextureView(nextBlurredLocation);
        GpuTextureView nextView = transitionStatus.isTransitioning() ?
                nextTextureView : currentTextureView;

        TextureSetup textureSetup;
        if (currentTextureView != null) {
            textureSetup = nextView != null ?
                    TextureSetup.doubleTexture(currentTextureView, nextView) :
                    TextureSetup.singleTexture(currentTextureView);
        } else {
            textureSetup = TextureSetup.noTexture();
        }

        float halfWidth = layout.width / 2f;
        float halfHeight = layout.height / 2f;

        ScreenRectangle scissor = MuiModApi.get().peekScissorStack(gr);
        MuiModApi.get().submitGuiElementRenderState(gr,
                new GradientRectangleRenderState(
                        HudRenderPipelines.BACKGROUND,
                        textureSetup,
                        new Matrix3x2f(gr.pose()),
                        -halfWidth, -halfHeight, halfWidth, halfHeight,
                        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                        scissor
                ));
    }

    private GpuTextureView getTextureView(ResourceLocation imageLocation) {
        if (imageLocation == null) {
            if (defaultImageLocation == null) {
                String greyImageBase64 = "data:bitmap/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAGElEQVQYV2OMiYn5z0AEYBxViC+UqB88ABNsFMnD0ASTAAAAAElFTkSuQmCC";
                ImageTextureData imageTextureData = ImageUtils.loadBase64(greyImageBase64);
                imageTextureData.register().join();
                defaultImageLocation = imageTextureData.getLocation();
            }
            return getTextureView(defaultImageLocation);
        }

        AbstractTexture texture = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(imageLocation);
        if (texture instanceof DynamicTexture dynamicTexture) {
            return dynamicTexture.getTextureView();
        }
        return null;
    }

    public void updateRenderPass(RenderPass renderPass) {
        if (gpuBufferSlice != null) {
            renderPass.setUniform("HudBackgroundParams", gpuBufferSlice);
        }
    }
}
