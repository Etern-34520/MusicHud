package indi.etern.musichud.client.ui.utils.image;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import icyllis.modernui.graphics.Bitmap;
import indi.etern.musichud.MusicHud;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static indi.etern.musichud.client.ui.utils.image.ImageUtils.convertBitmapToNativeImage;

public class ImageBlurPostProcessor {
    // 缓存高斯核，避免重复计算
    private static final Cache<Integer, float[]> GAUSSIAN_KERNEL_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(20)
            .build();
    private static final int MAX_THUMBNAIL_DIMENSION = 200;
    private static final int OPTIMIZATION_THRESHOLD = 15;

    public static ImageTextureData blur(ImageTextureData originalImageData, int radius) {
        synchronized (originalImageData.getSource()) {
            String cacheKey = originalImageData.getSource() + "_blurred_" + radius;
            ImageTextureData cachedData = ImageUtils.getCachedTexturesData().getIfPresent(cacheKey);
            if (cachedData != null) {
                return cachedData;
            }

            Bitmap bitmap = originalImageData.convertToBitmap();
            var result = applyGaussianBlurOptimized(bitmap, radius);

            NativeImage nativeImage = convertBitmapToNativeImage(result);
            assert nativeImage != null;
            ResourceLocation imageBlurredLocation = ResourceLocation.fromNamespaceAndPath(MusicHud.MOD_ID,
                    "image_blurred_" + radius + "_" + bitmap.hashCode());
            AtomicReference<DynamicTexture> texture = new AtomicReference<>();
            Minecraft.getInstance().submit(() -> {
                texture.set(new DynamicTexture(() -> "downloaded_blurred_" + originalImageData.getSource().hashCode(), nativeImage));
            }).join();
            ImageTextureData imageTextureData = new ImageTextureData(
                    originalImageData.getSource(),
                    imageBlurredLocation,
                    texture.get(),
                    false
            );

            ImageUtils.getCachedTexturesData().put(cacheKey, imageTextureData);
            return imageTextureData;
        }
    }

    private static Bitmap applyGaussianBlurOptimized(Bitmap source, int radius) {
        if (radius <= 0) {
            return source;
        }

        // 对于大半径模糊，使用缩略图优化
        if (radius > OPTIMIZATION_THRESHOLD &&
                (source.getWidth() > MAX_THUMBNAIL_DIMENSION || source.getHeight() > MAX_THUMBNAIL_DIMENSION)) {
            return applyGaussianBlurWithThumbnail(source, radius);
        }

        return applyStandardGaussianBlur(source, radius);
    }

    private static Bitmap applyGaussianBlurWithThumbnail(Bitmap source, int radius) {
        String thumbnailKey = source.getWidth() + "x" + source.getHeight() + "_" + radius;
        Bitmap thumbnail;
        float scale = calculateOptimalScale(source.getWidth(), source.getHeight(), MAX_THUMBNAIL_DIMENSION);
        int thumbWidth = Math.max(1, (int) (source.getWidth() * scale));
        int thumbHeight = Math.max(1, (int) (source.getHeight() * scale));

        int adjustedRadius = Math.max(1, (int) (radius * scale));

        Bitmap scaledBitmap = createScaledBitmap(source, thumbWidth, thumbHeight);
        thumbnail = applyStandardGaussianBlur(scaledBitmap, adjustedRadius);

        return createScaledBitmap(thumbnail, source.getWidth(), source.getHeight());
    }

    private static float calculateOptimalScale(int width, int height, int maxDimension) {
        float scaleX = (float) maxDimension / width;
        float scaleY = (float) maxDimension / height;
        return Math.min(scaleX, scaleY);
    }

    private static Bitmap createScaledBitmap(Bitmap source, int newWidth, int newHeight) {
        // 简单的双线性缩放实现
        Bitmap result = Bitmap.createBitmap(newWidth, newHeight, source.getFormat());
        int[] sourcePixels = new int[source.getWidth() * source.getHeight()];
        source.getPixels(sourcePixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());

        int[] resultPixels = new int[newWidth * newHeight];

        float xRatio = (float) source.getWidth() / newWidth;
        float yRatio = (float) source.getHeight() / newHeight;

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                float srcX = x * xRatio;
                float srcY = y * yRatio;

                // 双线性插值采样
                resultPixels[x + y * newWidth] = bilinearSample(sourcePixels,
                        source.getWidth(), source.getHeight(), srcX, srcY);
            }
        }

        result.setPixels(resultPixels, 0, newWidth, 0, 0, newWidth, newHeight);
        return result;
    }

    private static int bilinearSample(int[] pixels, int width, int height, float x, float y) {
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int x2 = Math.min(x1 + 1, width - 1);
        int y2 = Math.min(y1 + 1, height - 1);

        float xWeight = x - x1;
        float yWeight = y - y1;

        int p11 = pixels[x1 + y1 * width >= pixels.length ? pixels.length - 1 : x1 + y1 * width];
        int p12 = pixels[x1 + y2 * width >= pixels.length ? pixels.length - 1 : x1 + y2 * width];
        int p21 = pixels[x2 + y1 * width >= pixels.length ? pixels.length - 1 : x2 + y1 * width];
        int p22 = pixels[x2 + y2 * width >= pixels.length ? pixels.length - 1 : x2 + y2 * width];

        // 对每个通道进行插值
        int a = bilinearInterpolate(
                (p11 >> 24) & 0xFF, (p12 >> 24) & 0xFF,
                (p21 >> 24) & 0xFF, (p22 >> 24) & 0xFF, xWeight, yWeight);
        int r = bilinearInterpolate(
                (p11 >> 16) & 0xFF, (p12 >> 16) & 0xFF,
                (p21 >> 16) & 0xFF, (p22 >> 16) & 0xFF, xWeight, yWeight);
        int g = bilinearInterpolate(
                (p11 >> 8) & 0xFF, (p12 >> 8) & 0xFF,
                (p21 >> 8) & 0xFF, (p22 >> 8) & 0xFF, xWeight, yWeight);
        int b = bilinearInterpolate(
                p11 & 0xFF, p12 & 0xFF,
                p21 & 0xFF, p22 & 0xFF, xWeight, yWeight);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int bilinearInterpolate(int v11, int v12, int v21, int v22, float xWeight, float yWeight) {
        float top = v11 * (1 - xWeight) + v21 * xWeight;
        float bottom = v12 * (1 - xWeight) + v22 * xWeight;
        float value = top * (1 - yWeight) + bottom * yWeight;
        return Math.min(255, Math.max(0, Math.round(value)));
    }

    private static Bitmap applyStandardGaussianBlur(Bitmap source, int radius) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap temp = Bitmap.createBitmap(width, height, source.getFormat());
        Bitmap result = Bitmap.createBitmap(width, height, source.getFormat());

        int[] sourcePixels = new int[width * height];
        source.getPixels(sourcePixels, 0, width, 0, 0, width, height);

        int[] tempPixels = new int[width * height];
        int[] resultPixels = new int[width * height];

        // 使用缓存的高斯核
        float[] kernel = getCachedGaussianKernel(radius);
        int kernelSize = kernel.length;
        int halfKernel = kernelSize / 2;

        // 第一步:水平方向卷积 - 优化采样方式
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blurPixelHorizontal(sourcePixels, tempPixels, width, height, x, y, kernel, halfKernel);
            }
        }

        // 第二步:垂直方向卷积 - 优化采样方式
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blurPixelVertical(tempPixels, resultPixels, width, height, x, y, kernel, halfKernel);
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height);
        temp.close();

        return result;
    }

    private static void blurPixelHorizontal(int[] sourcePixels, int[] targetPixels,
                                            int width, int height, int x, int y,
                                            float[] kernel, int halfKernel) {
        float r = 0, g = 0, b = 0, a = 0;
        float weightSum = 0;

        for (int k = -halfKernel; k <= halfKernel; k++) {
            // 优化采样：使用边缘扩展而非循环采样，更符合视觉预期且性能更好
            int nx = clamp(x + k, 0, width - 1);

            int pixel = sourcePixels[nx + y * width];
            float weight = kernel[k + halfKernel];

            a += ((pixel >> 24) & 0xFF) * weight;
            r += ((pixel >> 16) & 0xFF) * weight;
            g += ((pixel >> 8) & 0xFF) * weight;
            b += (pixel & 0xFF) * weight;
            weightSum += weight;
        }

        // 归一化
        if (weightSum > 0) {
            a /= weightSum;
            r /= weightSum;
            g /= weightSum;
            b /= weightSum;
        }

        targetPixels[x + y * width] =
                ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    private static void blurPixelVertical(int[] sourcePixels, int[] targetPixels,
                                          int width, int height, int x, int y,
                                          float[] kernel, int halfKernel) {
        float r = 0, g = 0, b = 0, a = 0;
        float weightSum = 0;

        for (int k = -halfKernel; k <= halfKernel; k++) {
            // 优化采样：使用边缘扩展
            int ny = clamp(y + k, 0, height - 1);

            int pixel = sourcePixels[x + ny * width];
            float weight = kernel[k + halfKernel];

            a += ((pixel >> 24) & 0xFF) * weight;
            r += ((pixel >> 16) & 0xFF) * weight;
            g += ((pixel >> 8) & 0xFF) * weight;
            b += (pixel & 0xFF) * weight;
            weightSum += weight;
        }

        // 归一化
        if (weightSum > 0) {
            a /= weightSum;
            r /= weightSum;
            g /= weightSum;
            b /= weightSum;
        }

        targetPixels[x + y * width] =
                ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @SneakyThrows//TODO
    private static float[] getCachedGaussianKernel(int radius) {
        return GAUSSIAN_KERNEL_CACHE.get(radius, () -> computeGaussianKernel(radius));
    }

    private static float[] computeGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigma = Math.max(radius / 2.0f, 0.5f); // 避免sigma为0
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sum = 0;

        for (int i = 0; i < size; i++) {
            int x = i - radius;
            kernel[i] = (float) Math.exp(-(x * x) / twoSigmaSquare);
            sum += kernel[i];
        }

        // 归一化
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }
}