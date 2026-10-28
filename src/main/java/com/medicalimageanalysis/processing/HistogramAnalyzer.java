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

    public static double mean(long[] histogram, int totalPixels) {
        validateHistogram(histogram);
        validateTotalPixels(totalPixels);

        double weightedSum = 0;
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            weightedSum += (double) intensity * histogram[intensity];
        }
        return weightedSum / totalPixels;
    }

    public static double standardDeviation(long[] histogram, int totalPixels, double mean) {
        validateHistogram(histogram);
        validateTotalPixels(totalPixels);
        if (!Double.isFinite(mean)) {
            throw new IllegalArgumentException("Mean must be finite.");
        }

        double squaredDifferenceSum = 0;
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            double difference = intensity - mean;
            squaredDifferenceSum += difference * difference * histogram[intensity];
        }
        return Math.sqrt(squaredDifferenceSum / totalPixels);
    }

    public static long[] cumulativeDistribution(long[] histogram) {
        validateHistogram(histogram);
        long[] cumulative = new long[histogram.length];
        long total = 0;
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            total = Math.addExact(total, histogram[intensity]);
            cumulative[intensity] = total;
        }
        return cumulative;
    }

    private static void validateHistogram(long[] histogram) {
        Objects.requireNonNull(histogram, "histogram");
        if (histogram.length != 256) {
            throw new IllegalArgumentException("Histogram must contain 256 intensity bins.");
        }
        for (long frequency : histogram) {
            if (frequency < 0) {
                throw new IllegalArgumentException("Histogram frequencies must be non-negative.");
            }
        }
    }

    private static void validateTotalPixels(int totalPixels) {
        if (totalPixels <= 0) {
            throw new IllegalArgumentException("Total pixels must be greater than zero.");
        }
    }
}
