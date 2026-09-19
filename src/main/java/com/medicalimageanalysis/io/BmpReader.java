package com.medicalimageanalysis.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.medicalimageanalysis.model.BmpImage;

/**
 * Minimal handwritten BMP reader. It intentionally does not use ImageIO.
 *
 * Supported formats are uncompressed 8-bit paletted, 24-bit BGR, and
 * 32-bit BGRA BMP files.
 */
public final class BmpReader {
    public BmpImage read(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            int signature = readUnsignedShort(input);
            if (signature != 0x4D42) {
                throw new IOException("The selected file is not a BMP file.");
            }

            int fileSize = readInt(input);
            skip(input, 4); // reserved
            int pixelOffset = readInt(input);
            int headerSize = readInt(input);
            if (headerSize < 40) {
                throw new IOException("Unsupported BMP header.");
            }

            int width = readInt(input);
            int signedHeight = readInt(input);
            int planes = readUnsignedShort(input);
            int bitsPerPixel = readUnsignedShort(input);
            int compression = readInt(input);
            int imageSize = readInt(input); // biSizeImage
            skip(input, 8); // xPels + yPels
            int colorsUsed = readInt(input);
            skip(input, 4); // important colors

            // If header is larger than 40, skip the rest
            if (headerSize > 40) {
                skip(input, headerSize - 40);
            }

            if (width <= 0 || signedHeight == 0 || planes != 1 || compression != 0) {
                throw new IOException("Only uncompressed, single-plane BMP files are supported.");
            }
            if (bitsPerPixel != 8 && bitsPerPixel != 24 && bitsPerPixel != 32) {
                throw new IOException("Supported BMP depths are 8, 24, and 32 bits.");
            }

            int height = Math.abs(signedHeight);
            boolean topDown = signedHeight < 0;

            int paletteEntries = 0;
            int[] palette = null;
            if (bitsPerPixel == 8) {
                paletteEntries = (colorsUsed == 0) ? 256 : colorsUsed;
                palette = new int[paletteEntries];
                for (int i = 0; i < paletteEntries; i++) {
                    int blue = input.read();
                    int green = input.read();
                    int red = input.read();
                    int reserved = input.read();
                    if ((blue | green | red | reserved) < 0) {
                        throw new EOFException("Unexpected end of BMP palette.");
                    }
                    palette[i] = 0xFF000000 | (red << 16) | (green << 8) | blue;
                }
            }

            // Move to pixel data
            int bytesAlreadyRead = 14 + headerSize + paletteEntries * 4;
            skipTo(input, pixelOffset, bytesAlreadyRead);

            int rowSize = ((bitsPerPixel * width + 31) / 32) * 4;
            byte[] row = new byte[rowSize];
            int[] pixels = new int[width * height];

            for (int fileRow = 0; fileRow < height; fileRow++) {
                readFully(input, row);
                int imageRow = topDown ? fileRow : height - 1 - fileRow;
                readRow(row, imageRow, width, bitsPerPixel, palette, pixels);
            }

            return new BmpImage(width, height, bitsPerPixel, fileSize, pixelOffset, pixels);
        }
    }

    private static void readRow(byte[] row, int imageRow, int width, int depth,
                                int[] palette, int[] pixels) {
        int rowStart = imageRow * width;

        if (depth == 8) {
            for (int x = 0; x < width; x++) {
                int index = row[x] & 0xFF;
                pixels[rowStart + x] = palette[index];
            }
        } else if (depth == 24) {
            for (int x = 0; x < width; x++) {
                int offset = x * 3;
                int blue = row[offset] & 0xFF;
                int green = row[offset + 1] & 0xFF;
                int red = row[offset + 2] & 0xFF;
                pixels[rowStart + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        } else { // 32-bit
            for (int x = 0; x < width; x++) {
                int offset = x * 4;
                int blue = row[offset] & 0xFF;
                int green = row[offset + 1] & 0xFF;
                int red = row[offset + 2] & 0xFF;
                int alpha = row[offset + 3] & 0xFF;
                pixels[rowStart + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
    }

    private static int readUnsignedShort(InputStream input) throws IOException {
        int b1 = input.read();
        int b2 = input.read();
        if ((b1 | b2) < 0) throw new EOFException("Unexpected end of stream.");
        return b1 | (b2 << 8);
    }

    private static int readInt(InputStream input) throws IOException {
        int b1 = input.read();
        int b2 = input.read();
        int b3 = input.read();
        int b4 = input.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new EOFException("Unexpected end of stream.");
        return b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
    }

    private static void readFully(InputStream input, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int count = input.read(bytes, offset, bytes.length - offset);
            if (count < 0) {
                throw new EOFException("Unexpected end of BMP pixel data.");
            }
            offset += count;
        }
    }

    private static void skipTo(InputStream input, int offset, int bytesAlreadyRead)
            throws IOException {
        if (offset < bytesAlreadyRead) {
            throw new IOException("BMP pixel data offset overlaps the header.");
        }
        skip(input, offset - bytesAlreadyRead);
    }

    private static void skip(InputStream input, long count) throws IOException {
        while (count > 0) {
            long skipped = input.skip(count);
            if (skipped <= 0) {
                if (input.read() < 0) {
                    throw new EOFException("Unexpected end of BMP header.");
                }
                skipped = 1;
            }
            count -= skipped;
        }
    }
}