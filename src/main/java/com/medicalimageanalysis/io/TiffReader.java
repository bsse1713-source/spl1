package com.medicalimageanalysis.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.medicalimageanalysis.model.TiffImage;

/**
 * Reads the first image in a TIFF file using the TIFF reader included with the JDK.
 */
public final class TiffReader {

    public TiffImage read(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new IOException("Could not open the selected TIFF file.");
            }

            String byteOrder = readByteOrder(input);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            ImageReader tiffReader = null;
            while (readers.hasNext()) {
                ImageReader candidate = readers.next();
                if ("TIFF".equalsIgnoreCase(candidate.getFormatName())
                        || "TIF".equalsIgnoreCase(candidate.getFormatName())) {
                    tiffReader = candidate;
                    break;
                }
                candidate.dispose();
            }

            if (tiffReader == null) {
                throw new IOException("The selected file is not a supported TIFF image.");
            }

            try {
                tiffReader.setInput(input, true, true);
                BufferedImage decoded = tiffReader.read(0);
                if (decoded == null) {
                    throw new IOException("The TIFF reader could not decode the first image.");
                }

                int width = decoded.getWidth();
                int height = decoded.getHeight();
                int samplesPerPixel = decoded.getRaster().getNumBands();
                int bitsPerSample = decoded.getSampleModel().getSampleSize(0);
                int[] pixels = toPixels(decoded, width, height, samplesPerPixel, bitsPerSample);
                return new TiffImage(width, height, bitsPerSample, samplesPerPixel,
                        byteOrder, pixels);
            } finally {
                tiffReader.dispose();
            }
        }
    }

    /**
     * Converts the decoded image to ARGB pixels.
     *
     * Single-band (grayscale) images are copied sample by sample. BufferedImage.getRGB would
     * run them through Java's gray-to-sRGB color conversion, which brightens every
     * intensity (for example 40 becomes 110) and would corrupt medical gray values.
     */
    private static int[] toPixels(BufferedImage decoded, int width, int height,
                                  int samplesPerPixel, int bitsPerSample) {
        if (samplesPerPixel != 1) {
            return decoded.getRGB(0, 0, width, height, null, 0, width);
        }

        int maximum = (int) Math.min((1L << Math.min(bitsPerSample, 30)) - 1, Integer.MAX_VALUE);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sample = decoded.getRaster().getSample(x, y, 0);
                int gray = maximum == 255 ? sample : (int) Math.round(sample * 255.0 / maximum);
                gray = Math.max(0, Math.min(255, gray));
                pixels[y * width + x] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
            }
        }
        return pixels;
    }

    private static String readByteOrder(ImageInputStream input) throws IOException {
        input.seek(0);
        int first = input.read();
        int second = input.read();
        input.seek(0);

        if (first == 'I' && second == 'I') {
            return "II";
        }
        if (first == 'M' && second == 'M') {
            return "MM";
        }
        throw new IOException("The selected file is not a TIFF file.");
    }
}