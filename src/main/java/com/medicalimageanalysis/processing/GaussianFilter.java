package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class GaussianFilter {
    private GaussianFilter() {
    }

    public static BufferedImage apply(BufferedImage source, int kernelSize, double sigma) {
        Objects.requireNonNull(source, "source");
        if (kernelSize <= 0 || (kernelSize & 1) == 0) {
            throw new IllegalArgumentException("Kernel size must be a positive odd number.");
        }
        if (!Double.isFinite(sigma) || sigma <= 0) {
            throw new IllegalArgumentException("Sigma must be finite and greater than zero.");
        }

        double[][] kernel = new double[kernelSize][kernelSize];
        int radius = kernelSize / 2;
        double sum = 0;

        // Gaussian weights decrease with squared distance from the kernel center.
        for (int y = 0; y < kernelSize; y++) {
            for (int x = 0; x < kernelSize; x++) {
                double distanceX = (x - radius) / sigma;
                double distanceY = (y - radius) / sigma;
                double weight = Math.exp(-0.5 * (distanceX * distanceX + distanceY * distanceY));
                kernel[y][x] = weight;
                sum += weight;
            }
        }

        for (int y = 0; y < kernelSize; y++) {
            for (int x = 0; x < kernelSize; x++) {
                kernel[y][x] /= sum;
            }
        }
        return ConvolutionFilter.apply(source, kernel);
    }
}
