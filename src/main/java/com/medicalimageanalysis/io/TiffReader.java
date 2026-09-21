package com.medicalimageanalysis.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.medicalimageanalysis.model.TiffImage;

/**
 * Minimal handwritten TIFF reader. Supports baseline, uncompressed
 * TIFF files: 8-bit grayscale or 24-bit RGB, single image, either
 * byte order.
 */
public final class TiffReader {

    private static final int TAG_IMAGE_WIDTH = 256;
    private static final int TAG_IMAGE_HEIGHT = 257;
    private static final int TAG_BITS_PER_SAMPLE = 258;
    private static final int TAG_COMPRESSION = 259;
    private static final int TAG_PHOTOMETRIC = 262;
    private static final int TAG_STRIP_OFFSETS = 273;
    private static final int TAG_SAMPLES_PER_PIXEL = 277;
    private static final int TAG_ROWS_PER_STRIP = 278;
    private static final int TAG_STRIP_BYTE_COUNTS = 279;

    public TiffImage read(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            ByteOrder order = readByteOrder(file);
            int magic = readUnsignedShort(file, order);
            if (magic != 42) {
                throw new IOException("The selected file is not a TIFF file.");
            }

            long firstIfdOffset = readUnsignedInt(file, order);
            Map<Integer, long[]> tags = readIfd(file, order, firstIfdOffset);

            int width = (int) requireTag(tags, TAG_IMAGE_WIDTH)[0];
            int height = (int) requireTag(tags, TAG_IMAGE_HEIGHT)[0];
            int compression = (int) requireTag(tags, TAG_COMPRESSION)[0];
            int samplesPerPixel = tags.containsKey(TAG_SAMPLES_PER_PIXEL)
                    ? (int) tags.get(TAG_SAMPLES_PER_PIXEL)[0] : 1;
            int photometric = (int) requireTag(tags, TAG_PHOTOMETRIC)[0];
            long[] bitsPerSampleArr = requireTag(tags, TAG_BITS_PER_SAMPLE);
            int bitsPerSample = (int) bitsPerSampleArr[0];

            if (compression != 1) {
                throw new IOException("Only uncompressed TIFF files are supported.");
            }
            if (bitsPerSample != 8) {
                throw new IOException("Only 8-bit-per-sample TIFF files are supported.");
            }
            if (samplesPerPixel != 1 && samplesPerPixel != 3) {
                throw new IOException("Only grayscale or RGB TIFF files are supported.");
            }

            long[] stripOffsets = requireTag(tags, TAG_STRIP_OFFSETS);
            long[] stripByteCounts = requireTag(tags, TAG_STRIP_BYTE_COUNTS);

            int[] pixels = readPixels(file, width, height, samplesPerPixel,
                    photometric, stripOffsets, stripByteCounts);

            return new TiffImage(width, height, bitsPerSample, samplesPerPixel,
                    order == ByteOrder.LITTLE_ENDIAN ? "II" : "MM", pixels);
        }
    }

    private int[] readPixels(RandomAccessFile file, int width, int height,
                              int samplesPerPixel, int photometric,
                              long[] stripOffsets, long[] stripByteCounts) throws IOException {
        int[] pixels = new int[width * height];
        int row = 0;
        int bytesPerRow = width * samplesPerPixel;

        for (int strip = 0; strip < stripOffsets.length; strip++) {
            file.seek(stripOffsets[strip]);
            byte[] data = new byte[(int) stripByteCounts[strip]];
            file.readFully(data);

            int rowsInStrip = data.length / bytesPerRow;
            for (int r = 0; r < rowsInStrip && row < height; r++, row++) {
                int rowStart = row * width;
                int dataRowStart = r * bytesPerRow;
                for (int x = 0; x < width; x++) {
                    if (samplesPerPixel == 1) {
                        int gray = data[dataRowStart + x] & 0xFF;
                        // photometric 0 = WhiteIsZero, invert if so
                        if (photometric == 0) {
                            gray = 255 - gray;
                        }
                        pixels[rowStart + x] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                    } else {
                        int offset = dataRowStart + x * 3;
                        int red = data[offset] & 0xFF;
                        int green = data[offset + 1] & 0xFF;
                        int blue = data[offset + 2] & 0xFF;
                        pixels[rowStart + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
                    }
                }
            }
        }
        return pixels;
    }

    private Map<Integer, long[]> readIfd(RandomAccessFile file, ByteOrder order, long offset)
            throws IOException {
        file.seek(offset);
        int entryCount = readUnsignedShort(file, order);
        Map<Integer, long[]> tags = new HashMap<>();

        for (int i = 0; i < entryCount; i++) {
            int tag = readUnsignedShort(file, order);
            int type = readUnsignedShort(file, order);
            long count = readUnsignedInt(file, order);
            long valueOffsetField = file.getFilePointer();

            int valueSize = typeSize(type) * (int) count;
            long[] values;

            if (valueSize <= 4) {
                values = readInlineValues(file, order, type, count);
                file.seek(valueOffsetField + 4);
            } else {
                long dataOffset = readUnsignedInt(file, order);
                long returnPos = file.getFilePointer();
                file.seek(dataOffset);
                values = readValuesAt(file, order, type, count);
                file.seek(returnPos);
            }
            tags.put(tag, values);
        }
        return tags;
    }

    private long[] readInlineValues(RandomAccessFile file, ByteOrder order, int type, long count)
            throws IOException {
        return readValuesAt(file, order, type, count);
    }

    private long[] readValuesAt(RandomAccessFile file, ByteOrder order, int type, long count)
            throws IOException {
        long[] values = new long[(int) count];
        for (int i = 0; i < count; i++) {
            values[i] = switch (type) {
                case 1 -> file.read();                          // BYTE
                case 3 -> readUnsignedShort(file, order);        // SHORT
                case 4 -> readUnsignedInt(file, order);          // LONG
                default -> throw new IOException("Unsupported TIFF field type: " + type);
            };
        }
        return values;
    }

    private static int typeSize(int type) {
        return switch (type) {
            case 1 -> 1;  // BYTE
            case 3 -> 2;  // SHORT
            case 4 -> 4;  // LONG
            default -> throw new IllegalArgumentException("Unsupported TIFF field type: " + type);
        };
    }

    private static long[] requireTag(Map<Integer, long[]> tags, int tag) throws IOException {
        long[] value = tags.get(tag);
        if (value == null) {
            throw new IOException("Missing required TIFF tag: " + tag);
        }
        return value;
    }

    private static ByteOrder readByteOrder(RandomAccessFile file) throws IOException {
        int b1 = file.read();
        int b2 = file.read();
        if (b1 == 'I' && b2 == 'I') return ByteOrder.LITTLE_ENDIAN;
        if (b1 == 'M' && b2 == 'M') return ByteOrder.BIG_ENDIAN;
        throw new IOException("Not a valid TIFF byte-order marker.");
    }

    private static int readUnsignedShort(RandomAccessFile file, ByteOrder order) throws IOException {
        int b1 = file.read();
        int b2 = file.read();
        return order == ByteOrder.LITTLE_ENDIAN ? (b1 | (b2 << 8)) : ((b1 << 8) | b2);
    }

    private static long readUnsignedInt(RandomAccessFile file, ByteOrder order) throws IOException {
        int b1 = file.read();
        int b2 = file.read();
        int b3 = file.read();
        int b4 = file.read();
        long value = order == ByteOrder.LITTLE_ENDIAN
                ? (b1 | (b2 << 8) | (b3 << 16) | ((long) b4 << 24))
                : (((long) b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
        return value;
    }
}