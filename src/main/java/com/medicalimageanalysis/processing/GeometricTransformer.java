package com.medicalimageanalysis.processing;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class GeometricTransformer {
    private GeometricTransformer() {
    }

    public static BufferedImage rotate(BufferedImage source, double degrees) {
        Objects.requireNonNull(source, "source");
        if (!Double.isFinite(degrees)) {
            throw new IllegalArgumentException("Rotation angle must be finite.");
        }

        double radians = Math.toRadians(degrees);
        AffineTransform rotation = AffineTransform.getRotateInstance(radians);
        Point2D[] corners = {
                new Point2D.Double(0, 0),
                new Point2D.Double(source.getWidth(), 0),
                new Point2D.Double(0, source.getHeight()),
                new Point2D.Double(source.getWidth(), source.getHeight())
        };
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point2D corner : corners) {
            Point2D rotated = rotation.transform(corner, null);
            minX = Math.min(minX, rotated.getX());
            minY = Math.min(minY, rotated.getY());
            maxX = Math.max(maxX, rotated.getX());
            maxY = Math.max(maxY, rotated.getY());
        }

        int width = checkedDimension(Math.ceil(maxX - minX));
        int height = checkedDimension(Math.ceil(maxY - minY));
        rotation.preConcatenate(AffineTransform.getTranslateInstance(-minX, -minY));
        return render(source, rotation, width, height);
    }

    public static BufferedImage scale(BufferedImage source, double scaleX, double scaleY) {
        Objects.requireNonNull(source, "source");
        if (!Double.isFinite(scaleX) || !Double.isFinite(scaleY)
                || scaleX <= 0 || scaleY <= 0) {
            throw new IllegalArgumentException("Scale factors must be finite and greater than zero.");
        }

        int width = checkedDimension(Math.round(source.getWidth() * scaleX));
        int height = checkedDimension(Math.round(source.getHeight() * scaleY));
        return render(source, AffineTransform.getScaleInstance(scaleX, scaleY), width, height);
    }

    public static BufferedImage translate(BufferedImage source, int offsetX, int offsetY) {
        Objects.requireNonNull(source, "source");
        return render(source, AffineTransform.getTranslateInstance(offsetX, offsetY),
                source.getWidth(), source.getHeight());
    }

    public static BufferedImage flipHorizontal(BufferedImage source) {
        Objects.requireNonNull(source, "source");
        AffineTransform transform = new AffineTransform(-1, 0, 0, 1, source.getWidth(), 0);
        return render(source, transform, source.getWidth(), source.getHeight());
    }

    public static BufferedImage flipVertical(BufferedImage source) {
        Objects.requireNonNull(source, "source");
        AffineTransform transform = new AffineTransform(1, 0, 0, -1, 0, source.getHeight());
        return render(source, transform, source.getWidth(), source.getHeight());
    }

    private static BufferedImage render(BufferedImage source, AffineTransform transform,
                                        int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static int checkedDimension(long dimension) {
        if (dimension < 1 || dimension > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Transformed image dimensions are out of range.");
        }
        return (int) dimension;
    }

    private static int checkedDimension(double dimension) {
        if (!Double.isFinite(dimension) || dimension < 1 || dimension > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Transformed image dimensions are out of range.");
        }
        return (int) dimension;
    }
}
