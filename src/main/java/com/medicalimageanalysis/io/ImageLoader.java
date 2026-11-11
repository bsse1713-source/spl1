package com.medicalimageanalysis.io;

import com.medicalimageanalysis.model.BmpImage;
import com.medicalimageanalysis.model.DicomImage;
import com.medicalimageanalysis.model.TiffImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public final class ImageLoader {
    private ImageLoader() {
    }

    public static LoadedImage load(Path path) throws IOException {
        Format format = detectFormat(path);
        if (format == null) {
            throw new IOException("Unsupported or unrecognized image file.");
        }

        if (format == Format.BMP) {
            BmpImage image = new BmpReader().read(path);
            return new LoadedImage(image.toBufferedImage(), "BMP", formatInformation(image));
        }
        if (format == Format.TIFF) {
            TiffImage image = new TiffReader().read(path);
            return new LoadedImage(image.toBufferedImage(), "TIFF", formatInformation(image));
        }

        DicomImage image = new DicomReader().read(path);
        return new LoadedImage(image.toBufferedImage(), "DICOM", formatInformation(image), image);
    }

    private static Format detectFormat(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long length = file.length();
            if (length >= 2) {
                int first = file.readUnsignedByte();
                int second = file.readUnsignedByte();
                // BMP files begin with the ASCII bytes 'B' and 'M'.
                if (first == 'B' && second == 'M') {
                    return Format.BMP;
                }
            }

            if (length >= 4) {
                file.seek(0);
                int first = file.readUnsignedByte();
                int second = file.readUnsignedByte();
                int third = file.readUnsignedByte();
                int fourth = file.readUnsignedByte();
                // TIFF uses one of two four-byte signatures to indicate byte order.
                if ((first == 0x49 && second == 0x49 && third == 0x2A && fourth == 0x00)
                        || (first == 0x4D && second == 0x4D
                        && third == 0x00 && fourth == 0x2A)) {
                    return Format.TIFF;
                }
            }

            if (length >= 132) {
                file.seek(128);
                // DICOM Part 10 places the ASCII marker 'DICM' after its 128-byte preamble.
                if (file.readUnsignedByte() == 'D'
                        && file.readUnsignedByte() == 'I'
                        && file.readUnsignedByte() == 'C'
                        && file.readUnsignedByte() == 'M') {
                    return Format.DICOM;
                }
            }
        }
        return null;
    }

    private static String formatInformation(BmpImage image) {
        return "Format: BMP"
                + "\nWidth: " + image.getWidth()
                + "\nHeight: " + image.getHeight()
                + "\nBits per pixel: " + image.getBitsPerPixel()
                + "\nFile size from header: " + image.getFileSize() + " bytes"
                + "\nPixel data offset: " + image.getPixelDataOffset() + " bytes";
    }

    private static String formatInformation(TiffImage image) {
        return "Format: TIFF"
                + "\nWidth: " + image.getWidth()
                + "\nHeight: " + image.getHeight()
                + "\nBits per sample: " + image.getBitsPerSample()
                + "\nSamples per pixel: " + image.getSamplesPerPixel()
                + "\nByte order: " + image.getByteOrder();
    }

    private static String formatInformation(DicomImage image) {
        String window = Double.isFinite(image.getWindowCenter())
                && Double.isFinite(image.getWindowWidth())
                ? image.getWindowCenter() + " / " + image.getWindowWidth()
                : "Not specified (using data min/max)";
        return "Format: DICOM"
                + "\nPatient name: " + image.getPatientName()
                + "\nPatient ID: " + image.getPatientId()
                + "\nModality: " + image.getModality()
                + "\nStudy date: " + image.getStudyDate()
                + "\nWidth: " + image.getWidth()
                + "\nHeight: " + image.getHeight()
                + "\nPixel spacing (row × column, mm): " + formatPixelSpacing(image)
                + "\nBits allocated: " + image.getBitsAllocated()
                + "\nWindow center / width: " + window
                + "\nRescale slope: " + image.getRescaleSlope()
                + "\nRescale intercept: " + image.getRescaleIntercept();
    }

    private static String formatPixelSpacing(DicomImage image) {
        return image.hasPixelSpacing()
                ? image.getPixelSpacingRow() + " × " + image.getPixelSpacingColumn()
                : "Unavailable (missing or invalid)";
    }

    private enum Format {
        BMP,
        TIFF,
        DICOM
    }
}
