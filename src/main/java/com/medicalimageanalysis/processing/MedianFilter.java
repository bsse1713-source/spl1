package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

public final class MedianFilter {
    private MedianFilter() {
    }

    public static BufferedImage apply(BufferedImage source, int kernelSize) {
        Objects.requireNonNull(source, "source");
        if (kernelSize <= 0 || (kernelSize & 1) == 0) {
            throw new IllegalArgumentException("Kernel size must be a positive odd number.");
        }

        int neighborhoodSize = Math.multiplyExact(kernelSize, kernelSize);
        int radius = kernelSize / 2;
        int width = source.getWidth();
        int height = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, width, height, null, 0, width);
        int[] outputPixels = new int[sourcePixels.length];
        int[] redValues = new int[neighborhoodSize];
        int[] greenValues = new int[neighborhoodSize];
        int[] blueValues = new int[neighborhoodSize];
        int medianIndex = neighborhoodSize / 2;

        // Sort each channel's clamped neighborhood and select its middle value.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = 0;
                for (int offsetY = -radius; offsetY <= radius; offsetY++) {
                    int sourceY = Math.max(0, Math.min(height - 1, y + offsetY));
                    for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                        int sourceX = Math.max(0, Math.min(width - 1, x + offsetX));
                        int pixel = sourcePixels[sourceY * width + sourceX];
                        redValues[index] = (pixel >>> 16) & 0xFF;
                        greenValues[index] = (pixel >>> 8) & 0xFF;
                        blueValues[index] = pixel & 0xFF;
                        index++;
                    }
                }

                Arrays.sort(redValues);
                Arrays.sort(greenValues);
                Arrays.sort(blueValues);
                int alpha = sourcePixels[y * width + x] & 0xFF000000;
                outputPixels[y * width + x] = alpha
                        | (redValues[medianIndex] << 16)
                        | (greenValues[medianIndex] << 8)
                        | blueValues[medianIndex];
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, outputPixels, 0, width);
        return result;
    }
}
