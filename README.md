# Medical Image Analysis System

This is a plain Java 21 Swing application with no external libraries.

## Compile and run

From the repository root:

```text
$sources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $sources
java -cp out com.medicalimageanalysis.Main
```

The application supports uncompressed 8-bit, 24-bit, and 32-bit BMP files,
TIFF images supported by the JDK's built-in ImageIO TIFF reader, and
uncompressed single-frame grayscale DICOM files with 8- or 16-bit pixels using
Explicit or Implicit VR Little Endian transfer syntax. Compressed, color, and
multi-frame DICOM images are not supported. No external image library is
required; the first image in a TIFF file is opened. Images are converted into
custom image models and displayed with Swing.
Use the Grayscale button to convert a displayed image to
grayscale using weighted luminance; alpha is preserved and the source file
remains unchanged. Use Geometric Transform to rotate, scale, translate, or
flip the displayed image. Rotation expands the canvas to fit; translation
keeps the original canvas size, with uncovered pixels transparent. Geometric
operations use nearest-neighbor sampling and do not modify the source file.
Use Histogram to view the displayed image's 256-level intensity frequencies;
color pixels are converted to weighted grayscale intensity for the histogram.
Use Sobel Edges to detect intensity gradients in the displayed image; the result
is a grayscale edge image with the original alpha preserved.
Use Brightness / Contrast to adjust the displayed image with live preview;
brightness ranges from -100 to 100 and contrast from 0.00 to 2.00. Cancel
restores the image as it was when the dialog opened, and Apply preserves alpha.