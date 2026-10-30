package com.medicalimageanalysis.io;

import com.medicalimageanalysis.model.DicomImage;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Reads uncompressed, single-frame grayscale DICOM files with little-endian data.
 */
public final class DicomReader {
    private static final String EXPLICIT_VR_LITTLE_ENDIAN = "1.2.840.10008.1.2.1";
    private static final String IMPLICIT_VR_LITTLE_ENDIAN = "1.2.840.10008.1.2";
    private static final long UNDEFINED_LENGTH = 0xFFFFFFFFL;
    private static final int MAX_TEXT_LENGTH = 1024 * 1024;

    public DicomImage read(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = file.length();
            if (fileLength < 132) {
                throw new IOException("The selected file is too short to be a DICOM Part 10 file.");
            }

            file.seek(128);
            byte[] marker = new byte[4];
            file.readFully(marker);
            if (marker[0] != 'D' || marker[1] != 'I' || marker[2] != 'C' || marker[3] != 'M') {
                throw new IOException("The selected file does not have a DICOM preamble and DICM marker.");
            }

            // Part 10 files have a fixed preamble; group 0002 is Explicit VR Little Endian,
            // and its TransferSyntaxUID controls how the following dataset is parsed.
            String transferSyntaxUid = null;
            String patientName = "";
            String patientId = "";
            String modality = "";
            String studyDate = "";
            String photometricInterpretation = "";
            double windowCenter = Double.NaN;
            double windowWidth = Double.NaN;
            double rescaleSlope = 1.0;
            double rescaleIntercept = 0.0;
            int rows = 0;
            int columns = 0;
            int bitsAllocated = 0;
            int pixelRepresentation = 0;
            int samplesPerPixel = 1;
            int numberOfFrames = 1;

            while (file.getFilePointer() < fileLength) {
                ensureRemaining(file, fileLength, 8, "DICOM data element header");
                int group = readUnsignedShortLE(file);
                int element = readUnsignedShortLE(file);
                boolean explicitVr = group == 0x0002
                        || EXPLICIT_VR_LITTLE_ENDIAN.equals(transferSyntaxUid);
                if (group != 0x0002 && transferSyntaxUid == null) {
                    throw new IOException("DICOM transfer syntax is missing from the file meta information.");
                }
                if (group != 0x0002
                        && !EXPLICIT_VR_LITTLE_ENDIAN.equals(transferSyntaxUid)
                        && !IMPLICIT_VR_LITTLE_ENDIAN.equals(transferSyntaxUid)) {
                    throw unsupported("transfer syntax " + transferSyntaxUid);
                }

                long valueLength = readValueLength(file, explicitVr, fileLength);
                if (valueLength == UNDEFINED_LENGTH) {
                    if (group == 0x7FE0 && element == 0x0010) {
                        throw unsupported("encapsulated or undefined-length PixelData");
                    }
                    skipUndefinedLengthValue(file, explicitVr, fileLength);
                    continue;
                }
                ensureRemaining(file, fileLength, valueLength, "DICOM element value");

                if (group == 0x0002 && element == 0x0010) {
                    transferSyntaxUid = readText(file, valueLength);
                    if (!EXPLICIT_VR_LITTLE_ENDIAN.equals(transferSyntaxUid)
                            && !IMPLICIT_VR_LITTLE_ENDIAN.equals(transferSyntaxUid)) {
                        throw unsupported("transfer syntax " + transferSyntaxUid);
                    }
                } else if (group == 0x0010 && element == 0x0010) {
                    patientName = readText(file, valueLength);
                } else if (group == 0x0010 && element == 0x0020) {
                    patientId = readText(file, valueLength);
                } else if (group == 0x0008 && element == 0x0060) {
                    modality = readText(file, valueLength);
                } else if (group == 0x0008 && element == 0x0020) {
                    studyDate = readText(file, valueLength);
                } else if (group == 0x0028 && element == 0x0002) {
                    samplesPerPixel = readUnsignedShortValue(file, valueLength);
                } else if (group == 0x0028 && element == 0x0004) {
                    photometricInterpretation = readText(file, valueLength);
                } else if (group == 0x0028 && element == 0x0008) {
                    numberOfFrames = parseInteger(readText(file, valueLength), "NumberOfFrames");
                } else if (group == 0x0028 && element == 0x0010) {
                    rows = readUnsignedShortValue(file, valueLength);
                } else if (group == 0x0028 && element == 0x0011) {
                    columns = readUnsignedShortValue(file, valueLength);
                } else if (group == 0x0028 && element == 0x0100) {
                    bitsAllocated = readUnsignedShortValue(file, valueLength);
                } else if (group == 0x0028 && element == 0x0103) {
                    pixelRepresentation = readUnsignedShortValue(file, valueLength);
                } else if (group == 0x0028 && element == 0x1050) {
                    windowCenter = parseDecimal(readText(file, valueLength), "WindowCenter");
                } else if (group == 0x0028 && element == 0x1051) {
                    windowWidth = parseDecimal(readText(file, valueLength), "WindowWidth");
                } else if (group == 0x0028 && element == 0x1052) {
                    rescaleIntercept = parseDecimal(readText(file, valueLength), "RescaleIntercept");
                } else if (group == 0x0028 && element == 0x1053) {
                    rescaleSlope = parseDecimal(readText(file, valueLength), "RescaleSlope");
                } else if (group == 0x7FE0 && element == 0x0010) {
                    validateImageDetails(rows, columns, bitsAllocated, pixelRepresentation,
                            samplesPerPixel, numberOfFrames, photometricInterpretation);
                    int[] pixels = readDisplayPixels(file, valueLength, rows, columns,
                            bitsAllocated, pixelRepresentation, windowCenter, windowWidth,
                            rescaleSlope, rescaleIntercept, fileLength);
                    return new DicomImage(columns, rows, bitsAllocated, pixelRepresentation,
                            patientName, patientId, modality, studyDate, windowCenter,
                            windowWidth, rescaleSlope, rescaleIntercept, transferSyntaxUid, pixels);
                } else {
                    file.seek(file.getFilePointer() + valueLength);
                }
            }

            throw new IOException("DICOM PixelData (7FE0,0010) was not found.");
        } catch (EOFException exception) {
            throw new IOException("The DICOM file ends unexpectedly inside a data element.", exception);
        }
    }

    private static long readValueLength(RandomAccessFile file, boolean explicitVr, long fileLength)
            throws IOException {
        if (!explicitVr) {
            return readUnsignedIntLE(file);
        }

        // Explicit VR has a short length except for the long-form VRs, which add
        // two reserved bytes before their four-byte length.
        ensureRemaining(file, fileLength, 2, "DICOM value representation");
        int firstVrByte = file.readUnsignedByte();
        int secondVrByte = file.readUnsignedByte();
        String vr = new String(new byte[]{(byte) firstVrByte, (byte) secondVrByte},
                StandardCharsets.US_ASCII);
        if ("OB".equals(vr) || "OW".equals(vr) || "OF".equals(vr)
                || "SQ".equals(vr) || "UT".equals(vr) || "UN".equals(vr)) {
            ensureRemaining(file, fileLength, 6, "long-form DICOM element header");
            file.skipBytes(2);
            return readUnsignedIntLE(file);
        }
        ensureRemaining(file, fileLength, 2, "short-form DICOM element header");
        return readUnsignedShortLE(file);
    }

    private static String readText(RandomAccessFile file, long length) throws IOException {
        if (length > MAX_TEXT_LENGTH) {
            throw new IOException("DICOM text element is too large to read safely.");
        }
        byte[] bytes = new byte[(int) length];
        file.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII).replace("\u0000", "").trim();
    }

    private static int readUnsignedShortValue(RandomAccessFile file, long length) throws IOException {
        if (length < 2) {
            throw new IOException("DICOM unsigned-short value is shorter than two bytes.");
        }
        int value = readUnsignedShortLE(file);
        file.seek(file.getFilePointer() + length - 2);
        return value;
    }

    private static int parseInteger(String value, String name) throws IOException {
        try {
            return Integer.parseInt(firstValue(value));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid DICOM " + name + " value.", exception);
        }
    }

    private static double parseDecimal(String value, String name) throws IOException {
        try {
            double parsed = Double.parseDouble(firstValue(value));
            if (!Double.isFinite(parsed)) {
                throw new NumberFormatException("Non-finite value");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid DICOM " + name + " value.", exception);
        }
    }

    private static String firstValue(String value) {
        int separator = value.indexOf('\\');
        return (separator < 0 ? value : value.substring(0, separator)).trim();
    }

    private static void validateImageDetails(int rows, int columns, int bitsAllocated,
                                            int pixelRepresentation, int samplesPerPixel,
                                            int numberOfFrames, String photometric)
            throws IOException {
        if (rows <= 0 || columns <= 0) {
            throw new IOException("DICOM Rows and Columns must be present and positive.");
        }
        if (bitsAllocated != 8 && bitsAllocated != 16) {
            throw unsupported("BitsAllocated value " + bitsAllocated);
        }
        if (pixelRepresentation != 0 && pixelRepresentation != 1) {
            throw new IOException("Unsupported DICOM PixelRepresentation value "
                    + pixelRepresentation + ".");
        }
        if (numberOfFrames != 1) {
            throw unsupported("multi-frame images");
        }
        if (samplesPerPixel != 1 || (!photometric.isEmpty()
                && !"MONOCHROME1".equals(photometric)
                && !"MONOCHROME2".equals(photometric))) {
            throw unsupported("color images");
        }
        long pixelCount = (long) rows * columns;
        if (pixelCount > Integer.MAX_VALUE) {
            throw unsupported("image dimensions that exceed Java array limits");
        }
    }

    private static int[] readDisplayPixels(RandomAccessFile file, long pixelDataLength,
                                           int rows, int columns, int bitsAllocated,
                                           int pixelRepresentation, double windowCenter,
                                           double windowWidth, double rescaleSlope,
                                           double rescaleIntercept, long fileLength)
            throws IOException {
        int count = rows * columns;
        int bytesPerPixel = bitsAllocated / 8;
        long requiredLength = (long) count * bytesPerPixel;
        if (pixelDataLength < requiredLength) {
            throw new IOException("DICOM PixelData is shorter than the declared image dimensions.");
        }
        ensureRemaining(file, fileLength, pixelDataLength, "DICOM PixelData");
        if (!Double.isFinite(rescaleSlope) || !Double.isFinite(rescaleIntercept)) {
            throw new IOException("DICOM rescale values must be finite.");
        }
        if (Double.isFinite(windowCenter) && Double.isFinite(windowWidth) && windowWidth <= 0) {
            throw new IOException("DICOM WindowCenter and WindowWidth must be finite, and width positive.");
        }
        if ((!Double.isNaN(windowCenter) && !Double.isFinite(windowCenter))
                || (!Double.isNaN(windowWidth) && !Double.isFinite(windowWidth))) {
            throw new IOException("DICOM WindowCenter and WindowWidth must be finite.");
        }

        double[] modalityValues = new double[count];
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            int rawValue;
            if (bitsAllocated == 8) {
                int value = file.readUnsignedByte();
                rawValue = pixelRepresentation == 1 && value >= 128 ? value - 256 : value;
            } else {
                int value = readUnsignedShortLE(file);
                rawValue = pixelRepresentation == 1 && value >= 32768 ? value - 65536 : value;
            }
            // Convert stored pixels to modality values before applying display windowing.
            double modalityValue = rawValue * rescaleSlope + rescaleIntercept;
            if (!Double.isFinite(modalityValue)) {
                throw new IOException("DICOM rescale values produced a non-finite pixel value.");
            }
            modalityValues[i] = modalityValue;
            minimum = Math.min(minimum, modalityValue);
            maximum = Math.max(maximum, modalityValue);
        }

        boolean useProvidedWindow = Double.isFinite(windowCenter) && Double.isFinite(windowWidth);
        int[] pixels = new int[count];
        for (int i = 0; i < count; i++) {
            int gray;
            if (useProvidedWindow) {
                gray = window(modalityValues[i], windowCenter, windowWidth);
            } else if (maximum == minimum) {
                gray = 0;
            } else {
                // With no complete window pair, stretch the rescaled data range across 0-255.
                gray = (int) Math.round((modalityValues[i] - minimum) * 255.0
                        / (maximum - minimum));
            }
            pixels[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        }
        return pixels;
    }

    private static int window(double value, double center, double width) {
        // For width > 1, DICOM uses limits center-0.5 +/- (width-1)/2 and
        // linearly maps that interval to 0-255; width 1 acts as a binary threshold.
        if (width <= 1.0) {
            return value <= center - 0.5 ? 0 : 255;
        }
        double low = center - 0.5 - (width - 1.0) / 2.0;
        double high = center - 0.5 + (width - 1.0) / 2.0;
        if (value <= low) {
            return 0;
        }
        if (value > high) {
            return 255;
        }
        double normalized = ((value - (center - 0.5)) / (width - 1.0) + 0.5) * 255.0;
        return Math.max(0, Math.min(255, (int) Math.round(normalized)));
    }

    private static void skipUndefinedLengthValue(RandomAccessFile file, boolean explicitVr,
                                                 long fileLength) throws IOException {
        int[] delimiters = new int[32];
        int depth = 1;
        delimiters[0] = 0xE0DD;
        while (depth > 0) {
            ensureRemaining(file, fileLength, 8, "undefined-length DICOM element");
            int group = readUnsignedShortLE(file);
            int element = readUnsignedShortLE(file);
            if (group == 0xFFFE) {
                long length = readUnsignedIntLE(file);
                if (element == 0xE000) {
                    if (length == UNDEFINED_LENGTH) {
                        depth = pushDelimiter(delimiters, depth, 0xE00D);
                    } else {
                        ensureRemaining(file, fileLength, length, "DICOM sequence item");
                        file.seek(file.getFilePointer() + length);
                    }
                } else if (element == 0xE00D || element == 0xE0DD) {
                    if (depth == 0 || delimiters[depth - 1] != element) {
                        throw new IOException("Invalid DICOM sequence delimiter.");
                    }
                    depth--;
                } else {
                    throw new IOException("Invalid DICOM item tag in an undefined-length value.");
                }
                continue;
            }

            long length = readValueLength(file, explicitVr, fileLength);
            if (length == UNDEFINED_LENGTH) {
                depth = pushDelimiter(delimiters, depth, 0xE0DD);
            } else {
                ensureRemaining(file, fileLength, length, "DICOM sequence element");
                file.seek(file.getFilePointer() + length);
            }
        }
    }

    private static int pushDelimiter(int[] delimiters, int depth, int delimiter) throws IOException {
        if (depth == delimiters.length) {
            throw new IOException("DICOM sequence nesting is too deep.");
        }
        delimiters[depth] = delimiter;
        return depth + 1;
    }

    private static int readUnsignedShortLE(RandomAccessFile file) throws IOException {
        int low = file.readUnsignedByte();
        int high = file.readUnsignedByte();
        return low | high << 8;
    }

    private static long readUnsignedIntLE(RandomAccessFile file) throws IOException {
        long b0 = file.readUnsignedByte();
        long b1 = file.readUnsignedByte();
        long b2 = file.readUnsignedByte();
        long b3 = file.readUnsignedByte();
        return b0 | b1 << 8 | b2 << 16 | b3 << 24;
    }

    private static void ensureRemaining(RandomAccessFile file, long fileLength,
                                        long required, String description) throws IOException {
        if (required < 0 || file.getFilePointer() > fileLength
                || required > fileLength - file.getFilePointer()) {
            throw new IOException(description + " extends past the end of the DICOM file.");
        }
    }

    private static IOException unsupported(String feature) {
        return new IOException("Unsupported DICOM " + feature + ".");
    }
}
