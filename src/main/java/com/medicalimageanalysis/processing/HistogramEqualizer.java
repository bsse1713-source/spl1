package com.medicalimageanalysis.processing;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class HistogramEqualizer {
    private HistogramEqualizer() {
    }

    public static BufferedImage equalize(BufferedImage source) {
        Objects.requireNonNull(source, "source");
        int width = source.getWidth();
        int height = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, width, height, null, 0, width);
        int totalPixels = sourcePixels.length;

        // The analyzer's luminance histogram is the correct intensity distribution for grayscale input.
        long[] grayscaleHistogram = HistogramAnalyzer.calculate(source);
        boolean isGrayscale = true;
        for (int pixel : sourcePixels) {
            int red = (pixel >>> 16) & 0xFF;
            int green = (pixel >>> 8) & 0xFF;
            int blue = pixel & 0xFF;
            if (red != green || green != blue) {
                isGrayscale = false;
                break;
            }
        }

        int[] outputPixels = new int[sourcePixels.length];
        if (isGrayscale) {
            int[] lookup = createLookup(grayscaleHistogram, totalPixels);
            for (int i = 0; i < sourcePixels.length; i++) {
                int pixel = sourcePixels[i];
                int intensity = (pixel >>> 16) & 0xFF;
                int equalized = lookup[intensity];
                outputPixels[i] = (pixel & 0xFF000000)
                        | (equalized << 16) | (equalized << 8) | equalized;
            }
        } else {
            long[][] channelHistograms = new long[3][256];
            for (int pixel : sourcePixels) {
                channelHistograms[0][(pixel >>> 16) & 0xFF]++;
                channelHistograms[1][(pixel >>> 8) & 0xFF]++;
                channelHistograms[2][pixel & 0xFF]++;
            }
            int[][] channelLookups = {
                    createLookup(channelHistograms[0], totalPixels),
                    createLookup(channelHistograms[1], totalPixels),
                    createLookup(channelHistograms[2], totalPixels)
            };

            for (int i = 0; i < sourcePixels.length; i++) {
                int pixel = sourcePixels[i];
                int red = channelLookups[0][(pixel >>> 16) & 0xFF];
                int green = channelLookups[1][(pixel >>> 8) & 0xFF];
                int blue = channelLookups[2][pixel & 0xFF];
                outputPixels[i] = (pixel & 0xFF000000) | (red << 16) | (green << 8) | blue;
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, outputPixels, 0, width);
        return result;
    }

    private static int[] createLookup(long[] histogram, int totalPixels) {
        long[] cumulative = HistogramAnalyzer.cumulativeDistribution(histogram);
        long minimumCumulative = 0;
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            if (histogram[intensity] > 0) {
                minimumCumulative = cumulative[intensity];
                break;
            }
        }

        int[] lookup = new int[256];
        long range = totalPixels - minimumCumulative;
        if (range == 0) {
            // A constant channel has no contrast to expand, so preserve its values.
            for (int intensity = 0; intensity < lookup.length; intensity++) {
                lookup[intensity] = intensity;
            }
            return lookup;
        }

        // Subtracting the first occupied CDF value maps the image's lowest level to 0
        // and its highest level to 255 while spreading occupied levels across the range.
        for (int intensity = 0; intensity < lookup.length; intensity++) {
            long adjustedCumulative = Math.max(0, cumulative[intensity] - minimumCumulative);
            double normalized = (double) adjustedCumulative / range;
            lookup[intensity] = (int) Math.round(normalized * 255);
        }
        return lookup;
    }
}
