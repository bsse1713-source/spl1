package com.medicalimageanalysis.analysis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class ConnectedComponents {
    private static final double GOLDEN_RATIO_CONJUGATE = 0.6180339887498949;

    private ConnectedComponents() {
    }

    public static int[][] label(BufferedImage binaryImage) {
        Objects.requireNonNull(binaryImage, "binaryImage");
        int width = binaryImage.getWidth();
        int height = binaryImage.getHeight();
        int pixelCount = Math.multiplyExact(width, height);
        int[] pixels = binaryImage.getRGB(0, 0, width, height, null, 0, width);
        int[][] labels = new int[height][width];
        int[] queue = new int[pixelCount];
        int nextLabel = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (labels[y][x] != 0 || !isForeground(pixels[index])) {
                    continue;
                }

                int label = ++nextLabel;
                int head = 0;
                int tail = 0;
                labels[y][x] = label;
                queue[tail++] = index;

                // Breadth-first search visits each white pixel once and joins diagonal neighbors too.
                while (head < tail) {
                    int current = queue[head++];
                    int currentX = current % width;
                    int currentY = current / width;

                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        int neighborY = currentY + offsetY;
                        if (neighborY < 0 || neighborY >= height) {
                            continue;
                        }
                        for (int offsetX = -1; offsetX <= 1; offsetX++) {
                            int neighborX = currentX + offsetX;
                            if ((offsetX == 0 && offsetY == 0)
                                    || neighborX < 0 || neighborX >= width
                                    || labels[neighborY][neighborX] != 0) {
                                continue;
                            }
                            int neighborIndex = neighborY * width + neighborX;
                            if (isForeground(pixels[neighborIndex])) {
                                labels[neighborY][neighborX] = label;
                                queue[tail++] = neighborIndex;
                            }
                        }
                    }
                }
            }
        }
        return labels;
    }

    public static BufferedImage colorize(int[][] labels) {
        int width = validateLabels(labels);
        int height = labels.length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[Math.multiplyExact(width, height)];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labels[y][x];
                if (label == 0) {
                    pixels[y * width + x] = Color.BLACK.getRGB();
                } else {
                    float hue = (float) ((label * GOLDEN_RATIO_CONJUGATE) % 1.0);
                    pixels[y * width + x] = Color.HSBtoRGB(hue, 0.8f, 1.0f);
                }
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static boolean isForeground(int pixel) {
        return (pixel >>> 24) != 0
                && ((pixel >>> 16) & 0xFF) == 255
                && ((pixel >>> 8) & 0xFF) == 255
                && (pixel & 0xFF) == 255;
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
}
