package com.medicalimageanalysis.io;

import java.awt.image.BufferedImage;

public final class LoadedImage {
    private final BufferedImage image;
    private final String formatName;
    private final String description;

    public LoadedImage(BufferedImage image, String formatName, String description) {
        this.image = image;
        this.formatName = formatName;
        this.description = description;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getFormatName() {
        return formatName;
    }

    public String getDescription() {
        return description;
    }
}
