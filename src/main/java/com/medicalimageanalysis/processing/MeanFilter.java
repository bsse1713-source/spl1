package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class MeanFilter {
    private MeanFilter() {
    }

    public static BufferedImage apply(BufferedImage source, int kernelSize) {
        Objects.requireNonNull(source, "source");
        if (kernelSize <= 0 || (kernelSize & 1) == 0) {
            throw new IllegalArgumentException("Kernel size must be a positive odd number.");
        }

        double weight = 1.0 / ((double) kernelSize * kernelSize);
        double[][] kernel = new double[kernelSize][kernelSize];
        // Equal weights produce the normalized average of the neighborhood.
        for (int y = 0; y < kernelSize; y++) {
            for (int x = 0; x < kernelSize; x++) {
                kernel[y][x] = weight;
            }
        }
        return ConvolutionFilter.apply(source, kernel);
    }
}
