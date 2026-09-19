# Medical Image Analysis System

This is a plain Java 21 Swing application with no external libraries.

## Compile and run

From the repository root:

```text
javac -d out src/main/java/com/medicalimageanalysis/Main.java src/main/java/com/medicalimageanalysis/model/BmpImage.java src/main/java/com/medicalimageanalysis/io/BmpReader.java src/main/java/com/medicalimageanalysis/ui/MainWindow.java
java -cp out com.medicalimageanalysis.Main
```

The first milestone supports uncompressed 8-bit, 24-bit, and 32-bit BMP
files. BMP bytes are read directly with Java input streams, converted into a
small custom image model, displayed with Swing, and copied without modifying
the original file.