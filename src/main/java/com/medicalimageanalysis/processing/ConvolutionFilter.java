package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class ConvolutionFilter {
    private ConvolutionFilter() {
    }

    public static BufferedImage apply(BufferedImage source, double[][] kernel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kernel, "kernel");
        if (kernel.length == 0 || (kernel.length & 1) == 0
                || kernel[0] == null || kernel[0].length == 0
                || (kernel[0].length & 1) == 0) {
            throw new IllegalArgumentException("Kernel dimensions must be positive and odd.");
        }

        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        for (double[] row : kernel) {
            if (row == null || row.length != kernelWidth) {
                throw new IllegalArgumentException("Kernel must be rectangular.");
            }
            for (double weight : row) {
                if (!Double.isFinite(weight)) {
                    throw new IllegalArgumentException("Kernel weights must be finite.");
                }
            }
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, width, height, null, 0, width);
        int[] outputPixels = new int[sourcePixels.length];
        int radiusX = kernelWidth / 2;
        int radiusY = kernelHeight / 2;

        // Clamp each sampled coordinate to the nearest image edge.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double red = 0;
                double green = 0;
                double blue = 0;
                for (int kernelY = 0; kernelY < kernelHeight; kernelY++) {
                    int sourceY = Math.max(0, Math.min(height - 1, y + kernelY - radiusY));
                    for (int kernelX = 0; kernelX < kernelWidth; kernelX++) {
                        int sourceX = Math.max(0, Math.min(width - 1, x + kernelX - radiusX));
                        int pixel = sourcePixels[sourceY * width + sourceX];
                        double weight = kernel[kernelY][kernelX];
                        red += ((pixel >>> 16) & 0xFF) * weight;
                        green += ((pixel >>> 8) & 0xFF) * weight;
                        blue += (pixel & 0xFF) * weight;
                    }
                }

                int alpha = sourcePixels[y * width + x] & 0xFF000000;
                outputPixels[y * width + x] = alpha
                        | (clampChannel(red) << 16)
                        | (clampChannel(green) << 8)
                        | clampChannel(blue);
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, outputPixels, 0, width);
        return result;
    }

    private static int clampChannel(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}
