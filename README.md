# Medical Image Analysis System

This is a plain Java 21 Swing application with no external libraries.

## Compile and run

From the repository root:

```text
javac -d out src/main/java/com/medicalimageanalysis/Main.java src/main/java/com/medicalimageanalysis/model/BmpImage.java src/main/java/com/medicalimageanalysis/model/TiffImage.java src/main/java/com/medicalimageanalysis/io/BmpReader.java src/main/java/com/medicalimageanalysis/io/TiffReader.java src/main/java/com/medicalimageanalysis/ui/MainWindow.java
java -cp out com.medicalimageanalysis.Main
```

The application supports uncompressed 8-bit, 24-bit, and 32-bit BMP files,
plus uncompressed 8-bit grayscale and 24-bit RGB TIFF files. Image bytes are
read directly with Java input streams, converted into small custom image
models, displayed with Swing, and copied without modifying the original file.