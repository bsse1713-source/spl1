package com.medicalimageanalysis.io;

import com.medicalimageanalysis.model.TiffImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

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
                int[] pixels = decoded.getRGB(0, 0, width, height, null, 0, width);
                return new TiffImage(width, height, bitsPerSample, samplesPerPixel,
                        byteOrder, pixels);
            } finally {
                tiffReader.dispose();
            }
        }
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
