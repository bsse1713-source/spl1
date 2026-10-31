package com.medicalimageanalysis.analysis;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public final class RegionAnalyzer {
    private RegionAnalyzer() {
    }

    public static List<RegionFeature> analyze(int[][] labels, BufferedImage original) {
        return analyze(labels, original, 1);
    }

    public static List<RegionFeature> analyze(int[][] labels, BufferedImage original, int minimumArea) {
        Objects.requireNonNull(original, "original");
        if (minimumArea < 1) {
            throw new IllegalArgumentException("Minimum area must be at least 1.");
        }
        int width = validateLabels(labels);
        int height = labels.length;
        if (original.getWidth() != width || original.getHeight() != height) {
            throw new IllegalArgumentException("Label map and original image dimensions must match.");
        }

        int[] originalPixels = original.getRGB(0, 0, width, height, null, 0, width);
        TreeMap<Integer, Accumulator> regions = new TreeMap<>();

        // Accumulate area, bounds, coordinate sums, and intensity in one image pass.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labels[y][x];
                if (label == 0) {
                    continue;
                }
                int pixel = originalPixels[y * width + x];
                int red = (pixel >>> 16) & 0xFF;
                int green = (pixel >>> 8) & 0xFF;
                int blue = pixel & 0xFF;
                int intensity = (299 * red + 587 * green + 114 * blue + 500) / 1000;
                Accumulator region = regions.get(label);
                if (region == null) {
                    region = new Accumulator(x, y);
                    regions.put(label, region);
                }
                region.add(x, y, intensity);
            }
        }

        List<RegionFeature> features = new ArrayList<>();
        for (java.util.Map.Entry<Integer, Accumulator> entry : regions.entrySet()) {
            Accumulator region = entry.getValue();
            if (region.area >= minimumArea) {
                features.add(region.toFeature(entry.getKey()));
            }
        }
        return List.copyOf(features);
    }

    private static int validateLabels(int[][] labels) {
        Objects.requireNonNull(labels, "labels");
        if (labels.length == 0 || labels[0] == null || labels[0].length == 0) {
            throw new IllegalArgumentException("Label map must not be empty.");
        }
        int width = labels[0].length;
        for (int[] row : labels) {
            if (row == null || row.length != width) {
                throw new IllegalArgumentException("Label map must be rectangular.");
            }
            for (int label : row) {
                if (label < 0) {
                    throw new IllegalArgumentException("Labels must be non-negative.");
                }
            }
        }
        return width;
    }

    private static final class Accumulator {
        private int area;
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private long sumX;
        private long sumY;
        private long sumIntensity;

        private Accumulator(int x, int y) {
            minX = x;
            minY = y;
            maxX = x;
            maxY = y;
        }

        private void add(int x, int y, int intensity) {
            area++;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            sumX += x;
            sumY += y;
            sumIntensity += intensity;
        }

        private RegionFeature toFeature(int label) {
            return new RegionFeature(label, area, minX, minY, maxX, maxY,
                    (double) sumX / area, (double) sumY / area,
                    (double) sumIntensity / area);
        }
    }
}
