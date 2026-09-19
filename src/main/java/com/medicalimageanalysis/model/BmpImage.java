package com.medicalimageanalysis.model;

import java.awt.image.BufferedImage;

/**
 * Image pixels and the basic information read from a BMP file.
 *
 * <p>Pixels are stored as ARGB integers in row-major order, independent of
 * the BMP file's row order or color layout.</p>
 */
public final class BmpImage {
    private final int width;
    private final int height;
    private final int bitsPerPixel;
    private final int fileSize;
    private final int pixelDataOffset;
    private final int[] pixels;

    public BmpImage(int width, int height, int bitsPerPixel, int fileSize,
                    int pixelDataOffset, int[] pixels) {
        this.width = width;
        this.height = height;
        this.bitsPerPixel = bitsPerPixel;
        this.fileSize = fileSize;
        this.pixelDataOffset = pixelDataOffset;
        this.pixels = pixels.clone();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPixelDataOffset() {
        return pixelDataOffset;
    }

    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
}
