package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class BrightnessContrastAdjuster {
    private BrightnessContrastAdjuster() {
    }

    public static BufferedImage adjust(BufferedImage source, int brightness, double contrast) {
        Objects.requireNonNull(source, "source");
        if (!Double.isFinite(contrast) || contrast < 0) {
            throw new IllegalArgumentException("Contrast must be finite and non-negative.");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >>> 24) & 0xFF;
            int red = adjustChannel((pixel >>> 16) & 0xFF, brightness, contrast);
            int green = adjustChannel((pixel >>> 8) & 0xFF, brightness, contrast);
            int blue = adjustChannel(pixel & 0xFF, brightness, contrast);
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        BufferedImage adjusted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        adjusted.setRGB(0, 0, width, height, pixels, 0, width);
        return adjusted;
    }

    private static int adjustChannel(int channel, int brightness, double contrast) {
        double adjusted = (channel - 128) * contrast + 128 + brightness;
        return (int) Math.max(0, Math.min(255, Math.round(adjusted)));
    }
}
