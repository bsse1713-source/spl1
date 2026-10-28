package com.medicalimageanalysis.analysis;

import com.medicalimageanalysis.processing.GrayscaleConverter;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class Thresholding {
    private Thresholding() {
    }

    public static BufferedImage globalThreshold(BufferedImage source, int threshold) {
        Objects.requireNonNull(source, "source");
        validateThreshold(threshold);

        BufferedImage grayscale = GrayscaleConverter.toGrayscale(source);
        int width = grayscale.getWidth();
        int height = grayscale.getHeight();
        int[] pixels = grayscale.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = pixel & 0xFF000000;
            int intensity = (pixel >>> 16) & 0xFF;
            int binary = intensity >= threshold ? 255 : 0;
            pixels[i] = alpha | (binary << 16) | (binary << 8) | binary;
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, pixels, 0, width);
        return result;
    }

    public static BufferedImage otsuThreshold(BufferedImage source) {
        Objects.requireNonNull(source, "source");
        return globalThreshold(source, calculateOtsuThreshold(source));
    }

    public static int calculateOtsuThreshold(BufferedImage source) {
        Objects.requireNonNull(source, "source");

        BufferedImage grayscale = GrayscaleConverter.toGrayscale(source);
        int width = grayscale.getWidth();
        int height = grayscale.getHeight();
        int[] pixels = grayscale.getRGB(0, 0, width, height, null, 0, width);
        long[] histogram = new long[256];
        long totalIntensity = 0;

        // 1. Build the grayscale histogram and the sum of all pixel intensities.
        for (int pixel : pixels) {
            int intensity = (pixel >>> 16) & 0xFF;
            histogram[intensity]++;
            totalIntensity += intensity;
        }

        long totalPixels = pixels.length;
        long backgroundPixels = 0;
        long backgroundIntensity = 0;
        double maximumBetweenClassVariance = -1;
        int bestThreshold = 0;

        // Test each possible split: intensities below the threshold form the background,
        // and intensities at or above it form the foreground.
        for (int threshold = 0; threshold <= 255; threshold++) {
            long foregroundPixels = totalPixels - backgroundPixels;

            if (backgroundPixels > 0 && foregroundPixels > 0) {
                // 2. Compute each class's weight and mean for this candidate split.
                double backgroundWeight = (double) backgroundPixels / totalPixels;
                double foregroundWeight = (double) foregroundPixels / totalPixels;
                double backgroundMean = (double) backgroundIntensity / backgroundPixels;
                double foregroundMean = (double) (totalIntensity - backgroundIntensity) / foregroundPixels;
                double meanDifference = backgroundMean - foregroundMean;

                // 3. Within-class variance is the weighted sum of the two class variances.
                // Total image variance is fixed, so minimizing it is equivalent to maximizing
                // between-class variance: wBackground * wForeground * (meanBackground - meanForeground)^2.
                double betweenClassVariance = backgroundWeight * foregroundWeight
                        * meanDifference * meanDifference;

                // 4. Keep the split with the largest between-class variance.
                if (betweenClassVariance > maximumBetweenClassVariance) {
                    maximumBetweenClassVariance = betweenClassVariance;
                    bestThreshold = threshold;
                }
            }

            backgroundPixels += histogram[threshold];
            backgroundIntensity += (long) threshold * histogram[threshold];
        }

        return bestThreshold;
    }

    private static void validateThreshold(int threshold) {
        if (threshold < 0 || threshold > 255) {
            throw new IllegalArgumentException("Threshold must be between 0 and 255.");
        }
    }
}
