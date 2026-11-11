package com.medicalimageanalysis.analysis;

public final class RegionFeature {
    private final int label;
    private final int area;
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final double centroidX;
    private final double centroidY;
    private final double meanIntensity;
    private final double physicalAreaMm2;

    public RegionFeature(int label, int area, int minX, int minY, int maxX, int maxY,
                         double centroidX, double centroidY, double meanIntensity) {
        this(label, area, minX, minY, maxX, maxY, centroidX, centroidY,
                meanIntensity, Double.NaN);
    }

    public RegionFeature(int label, int area, int minX, int minY, int maxX, int maxY,
                         double centroidX, double centroidY, double meanIntensity,
                         double physicalAreaMm2) {
        this.label = label;
        this.area = area;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.centroidX = centroidX;
        this.centroidY = centroidY;
        this.meanIntensity = meanIntensity;
        this.physicalAreaMm2 = Double.isFinite(physicalAreaMm2) && physicalAreaMm2 > 0
                ? physicalAreaMm2 : Double.NaN;
    }

    public int getLabel() { return label; }
    public int getArea() { return area; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public double getCentroidX() { return centroidX; }
    public double getCentroidY() { return centroidY; }
    public double getMeanIntensity() { return meanIntensity; }
    public double getPhysicalAreaMm2() { return physicalAreaMm2; }
    public boolean hasPhysicalAreaMm2() { return Double.isFinite(physicalAreaMm2); }
}
