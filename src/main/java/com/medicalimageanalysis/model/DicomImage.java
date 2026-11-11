package com.medicalimageanalysis.model;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DicomImage {
    private final int width;
    private final int height;
    private final int bitsAllocated;
    private final int pixelRepresentation;
    private final String patientName;
    private final String patientId;
    private final String modality;
    private final String studyDate;
    private final double windowCenter;
    private final double windowWidth;
    private final double rescaleSlope;
    private final double rescaleIntercept;
    private final String transferSyntaxUid;
    private final double pixelSpacingRow;
    private final double pixelSpacingColumn;
    private final int bitsStored;
    private final int samplesPerPixel;
    private final java.util.Map<String, String> metadata;
    private final int[] pixels;

    public DicomImage(int width, int height, int bitsAllocated, int pixelRepresentation,
                      String patientName, String patientId, String modality, String studyDate,
                      double windowCenter, double windowWidth, double rescaleSlope,
                      double rescaleIntercept, String transferSyntaxUid, int[] pixels,
                      double pixelSpacingRow, double pixelSpacingColumn, int bitsStored,
                      int samplesPerPixel, java.util.Map<String, String> metadata) {
        this.width = width;
        this.height = height;
        this.bitsAllocated = bitsAllocated;
        this.pixelRepresentation = pixelRepresentation;
        this.patientName = patientName;
        this.patientId = patientId;
        this.modality = modality;
        this.studyDate = studyDate;
        this.windowCenter = windowCenter;
        this.windowWidth = windowWidth;
        this.rescaleSlope = rescaleSlope;
        this.rescaleIntercept = rescaleIntercept;
        this.transferSyntaxUid = transferSyntaxUid;
        this.pixelSpacingRow = pixelSpacingRow;
        this.pixelSpacingColumn = pixelSpacingColumn;
        this.bitsStored = bitsStored;
        this.samplesPerPixel = samplesPerPixel;
        this.metadata = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(metadata));
        this.pixels = pixels.clone();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBitsAllocated() { return bitsAllocated; }
    public int getPixelRepresentation() { return pixelRepresentation; }
    public String getPatientName() { return patientName; }
    public String getPatientId() { return patientId; }
    public String getModality() { return modality; }
    public String getStudyDate() { return studyDate; }
    public double getWindowCenter() { return windowCenter; }
    public double getWindowWidth() { return windowWidth; }
    public double getRescaleSlope() { return rescaleSlope; }
    public double getRescaleIntercept() { return rescaleIntercept; }
    public String getTransferSyntaxUid() { return transferSyntaxUid; }
    public double getPixelSpacingRow() { return pixelSpacingRow; }
    public double getPixelSpacingColumn() { return pixelSpacingColumn; }
    public int getBitsStored() { return bitsStored; }
    public int getSamplesPerPixel() { return samplesPerPixel; }
    public java.util.Map<String, String> getMetadata() { return metadata; }

    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
}
