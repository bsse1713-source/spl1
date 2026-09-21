package com.medicalimageanalysis.model;

import java.awt.image.BufferedImage;

public final class TiffImage {
    private final int width;
    private final int height;
    private final int bitsPerSample;
    private final int samplesPerPixel;
    private final String byteOrder;
    private final int[] pixels;

    public TiffImage(int width, int height, int bitsPerSample, int samplesPerPixel,
                      String byteOrder, int[] pixels) {
        this.width = width;
        this.height = height;
        this.bitsPerSample = bitsPerSample;
        this.samplesPerPixel = samplesPerPixel;
        this.byteOrder = byteOrder;
        this.pixels = pixels.clone();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBitsPerSample() { return bitsPerSample; }
    public int getSamplesPerPixel() { return samplesPerPixel; }
    public String getByteOrder() { return byteOrder; }

    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
}