package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class HistogramAnalyzer {
    private HistogramAnalyzer() {
    }

    public static long[] calculate(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        long[] frequencies = new long[256];
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

        for (int pixel : pixels) {
            int red = (pixel >>> 16) & 0xFF;
            int green = (pixel >>> 8) & 0xFF;
            int blue = pixel & 0xFF;
            int intensity = (299 * red + 587 * green + 114 * blue + 500) / 1000;
            frequencies[intensity]++;
        }

        return frequencies;
    }
}
