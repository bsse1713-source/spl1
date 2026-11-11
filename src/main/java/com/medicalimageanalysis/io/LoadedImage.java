package com.medicalimageanalysis.io;

import com.medicalimageanalysis.model.DicomImage;

import java.awt.image.BufferedImage;
import java.util.Optional;

public final class LoadedImage {
    private final BufferedImage image;
    private final String formatName;
    private final String description;
    private final DicomImage dicomImage;

    public LoadedImage(BufferedImage image, String formatName, String description) {
        this(image, formatName, description, null);
    }

    public LoadedImage(BufferedImage image, String formatName, String description,
                       DicomImage dicomImage) {
        this.image = image;
        this.formatName = formatName;
        this.description = description;
        this.dicomImage = dicomImage;
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

    public Optional<DicomImage> getDicomImage() {
        return Optional.ofNullable(dicomImage);
    }
}
