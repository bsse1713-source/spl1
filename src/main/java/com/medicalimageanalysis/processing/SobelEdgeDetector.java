package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class SobelEdgeDetector {
    private SobelEdgeDetector() {
    }

    public static BufferedImage detect(BufferedImage source) {
        Objects.requireNonNull(source, "source");
        int width = source.getWidth();
        int height = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, width, height, null, 0, width);
        int[] outputPixels = new int[sourcePixels.length];

        for (int y = 0; y < height; y++) {
            int top = Math.max(0, y - 1);
            int bottom = Math.min(height - 1, y + 1);
            for (int x = 0; x < width; x++) {
                int left = Math.max(0, x - 1);
                int right = Math.min(width - 1, x + 1);

                int topLeft = intensity(sourcePixels[top * width + left]);
                int topCenter = intensity(sourcePixels[top * width + x]);
                int topRight = intensity(sourcePixels[top * width + right]);
                int middleLeft = intensity(sourcePixels[y * width + left]);
                int middleRight = intensity(sourcePixels[y * width + right]);
                int bottomLeft = intensity(sourcePixels[bottom * width + left]);
                int bottomCenter = intensity(sourcePixels[bottom * width + x]);
                int bottomRight = intensity(sourcePixels[bottom * width + right]);

                int horizontal = -topLeft + topRight - 2 * middleLeft + 2 * middleRight
                        - bottomLeft + bottomRight;
                int vertical = -topLeft - 2 * topCenter - topRight + bottomLeft
                        + 2 * bottomCenter + bottomRight;
                int magnitude = Math.min(255,
                        (int) Math.round(Math.hypot(horizontal, vertical) / 4.0));
                int alpha = sourcePixels[y * width + x] & 0xFF000000;
                outputPixels[y * width + x] = alpha | (magnitude << 16)
                        | (magnitude << 8) | magnitude;
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, outputPixels, 0, width);
        return result;
    }

    private static int intensity(int pixel) {
        int red = (pixel >>> 16) & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return (299 * red + 587 * green + 114 * blue + 500) / 1000;
    }
}
