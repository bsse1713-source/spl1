# Medical Image Analysis System

This is a plain Java 21 Swing application with no external libraries.

## Compile and run

From the repository root:

```text
javac -d out src/main/java/com/medicalimageanalysis/Main.java src/main/java/com/medicalimageanalysis/model/BmpImage.java src/main/java/com/medicalimageanalysis/model/TiffImage.java src/main/java/com/medicalimageanalysis/io/BmpReader.java src/main/java/com/medicalimageanalysis/io/TiffReader.java src/main/java/com/medicalimageanalysis/processing/GrayscaleConverter.java src/main/java/com/medicalimageanalysis/processing/BrightnessContrastAdjuster.java src/main/java/com/medicalimageanalysis/processing/GeometricTransformer.java src/main/java/com/medicalimageanalysis/processing/HistogramAnalyzer.java src/main/java/com/medicalimageanalysis/processing/SobelEdgeDetector.java src/main/java/com/medicalimageanalysis/ui/MainWindow.java
java -cp out com.medicalimageanalysis.Main
```

The application supports uncompressed 8-bit, 24-bit, and 32-bit BMP files,
plus TIFF images supported by the JDK's built-in ImageIO TIFF reader. No
external image library is required; the first image in a TIFF file is opened.
Images are converted into small custom image models, displayed with Swing, and
copied without modifying the original file.
Use the Grayscale button to convert a displayed BMP or TIFF image to
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