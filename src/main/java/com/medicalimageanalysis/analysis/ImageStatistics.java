package com.medicalimageanalysis.analysis;

import com.medicalimageanalysis.processing.HistogramAnalyzer;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class ImageStatistics {
    private ImageStatistics() {
    }

    public static int min(BufferedImage image) {
        long[] histogram = calculateHistogram(image);
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            if (histogram[intensity] > 0) {
                return intensity;
            }
        }
        throw new IllegalArgumentException("Image must contain at least one pixel.");
    }

    public static int max(BufferedImage image) {
        long[] histogram = calculateHistogram(image);
        for (int intensity = histogram.length - 1; intensity >= 0; intensity--) {
            if (histogram[intensity] > 0) {
                return intensity;
            }
        }
        throw new IllegalArgumentException("Image must contain at least one pixel.");
    }

    public static double mean(BufferedImage image) {
        long[] histogram = calculateHistogram(image);
        return HistogramAnalyzer.mean(histogram, pixelCount(histogram));
    }

    public static double standardDeviation(BufferedImage image) {
        long[] histogram = calculateHistogram(image);
        int count = pixelCount(histogram);
        double mean = HistogramAnalyzer.mean(histogram, count);
        return HistogramAnalyzer.standardDeviation(histogram, count, mean);
    }

    public static double entropy(BufferedImage image) {
        long[] histogram = calculateHistogram(image);
        int count = pixelCount(histogram);
        double entropy = 0.0;
        for (long frequency : histogram) {
            if (frequency == 0) {
                continue;
            }
            double probability = (double) frequency / count;
            entropy -= probability * (Math.log(probability) / Math.log(2.0));
        }
        return entropy;
    }

    private static long[] calculateHistogram(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image must contain at least one pixel.");
        }
        return HistogramAnalyzer.calculate(image);
    }

    private static int pixelCount(long[] histogram) {
        long count = 0;
        for (long frequency : histogram) {
            count += frequency;
        }
        if (count == 0 || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image pixel count is outside the supported range.");
        }
        return (int) count;
    }
}
