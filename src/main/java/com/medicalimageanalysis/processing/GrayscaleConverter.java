package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;

public final class GrayscaleConverter {
    private GrayscaleConverter() {
    }

    public static BufferedImage toGrayscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >>> 24) & 0xFF;
            int red = (pixel >>> 16) & 0xFF;
            int green = (pixel >>> 8) & 0xFF;
            int blue = pixel & 0xFF;
            int gray = (299 * red + 587 * green + 114 * blue + 500) / 1000;
            pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        }

        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        grayscale.setRGB(0, 0, width, height, pixels, 0, width);
        return grayscale;
    }
}
