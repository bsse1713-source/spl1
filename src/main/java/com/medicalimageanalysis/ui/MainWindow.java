package com.medicalimageanalysis.ui;

import com.medicalimageanalysis.io.BmpReader;
import com.medicalimageanalysis.io.TiffReader;
import com.medicalimageanalysis.model.BmpImage;
import com.medicalimageanalysis.model.TiffImage;
import com.medicalimageanalysis.processing.BrightnessContrastAdjuster;
import com.medicalimageanalysis.processing.GeometricTransformer;
import com.medicalimageanalysis.processing.GrayscaleConverter;
import com.medicalimageanalysis.processing.HistogramAnalyzer;
import com.medicalimageanalysis.processing.SobelEdgeDetector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class MainWindow {
    private final JFrame frame = new JFrame("Medical Image Analysis System");
    private final ImagePanel imagePanel = new ImagePanel();
    private final JTextArea information = new JTextArea();
    private final BmpReader bmpReader = new BmpReader();
    private final TiffReader tiffReader = new TiffReader();
    private Path openedPath;
    private BufferedImage currentImage;

    public MainWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationByPlatform(true);

        JButton openBmpButton = new JButton("Open BMP");
        openBmpButton.addActionListener(event -> openBmp());

        JButton openTiffButton = new JButton("Open TIFF");
        openTiffButton.addActionListener(event -> openTiff());

        JButton readDcmButton = new JButton("Read DCM");

        JButton grayscaleButton = new JButton("Grayscale");
        grayscaleButton.addActionListener(event -> convertToGrayscale());

        JButton sobelButton = new JButton("Sobel Edges");
        sobelButton.addActionListener(event -> detectEdges());

        JButton histogramButton = new JButton("Histogram");
        histogramButton.addActionListener(event -> showHistogram());

        JButton brightnessContrastButton = new JButton("Brightness / Contrast");
        brightnessContrastButton.addActionListener(event -> adjustBrightnessContrast());

        JButton transformButton = new JButton("Geometric Transform");
        transformButton.addActionListener(event -> transformImage());

        JButton saveButton = new JButton("Save Copy");
        saveButton.addActionListener(event -> saveCopy());

        JPanel buttons = new JPanel();
        buttons.add(openBmpButton);
        buttons.add(openTiffButton);
        buttons.add(readDcmButton);
        buttons.add(grayscaleButton);
        buttons.add(sobelButton);
        buttons.add(histogramButton);
        buttons.add(brightnessContrastButton);
        buttons.add(transformButton);
        buttons.add(saveButton);
        frame.add(buttons, BorderLayout.NORTH);
        frame.add(new JScrollPane(imagePanel), BorderLayout.CENTER);

        information.setEditable(false);
        information.setLineWrap(true);
        information.setWrapStyleWord(true);
        information.setBorder(BorderFactory.createTitledBorder("Image information"));
        information.setText("Open a BMP or TIFF file to inspect it.");
        JScrollPane informationPane = new JScrollPane(information);
        informationPane.setPreferredSize(new Dimension(260, 0));
        frame.add(informationPane, BorderLayout.EAST);
    }

    public void show() {
        frame.setVisible(true);
    }

    private void openBmp() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open BMP image");
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path selected = chooser.getSelectedFile().toPath();
            BmpImage image = bmpReader.read(selected);
            openedPath = selected;
            currentImage = image.toBufferedImage();
            imagePanel.setImage(currentImage);
            information.setText(formatInformation(image));
        } catch (IOException exception) {
            information.setText("Could not open BMP:\n" + exception.getMessage());
        }
    }

    private void openTiff() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open TIFF image");
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path selected = chooser.getSelectedFile().toPath();
            TiffImage image = tiffReader.read(selected);
            openedPath = selected;
            currentImage = image.toBufferedImage();
            imagePanel.setImage(currentImage);
            information.setText(formatInformation(image));
        } catch (IOException exception) {
            information.setText("Could not open TIFF:\n" + exception.getMessage());
        }
    }

    private void convertToGrayscale() {
        if (currentImage == null) {
            information.setText("Open an image file before converting to grayscale.");
            return;
        }

        currentImage = GrayscaleConverter.toGrayscale(currentImage);
        imagePanel.setImage(currentImage);
        information.append("\n\nDisplayed image converted to grayscale.");
    }

    private void showHistogram() {
        if (currentImage == null) {
            information.setText("Open an image file before analyzing its histogram.");
            return;
        }

        long[] frequencies = HistogramAnalyzer.calculate(currentImage);
        JDialog dialog = new JDialog(frame, "Pixel Intensity Histogram", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel summary = new JLabel("Weighted grayscale intensity (0-255) | "
                + currentImage.getWidth() + " x " + currentImage.getHeight() + " pixels");
        summary.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        dialog.add(summary, BorderLayout.NORTH);
        dialog.add(new HistogramPanel(frequencies), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> dialog.dispose());
        JPanel buttons = new JPanel();
        buttons.add(closeButton);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void detectEdges() {
        if (currentImage == null) {
            information.setText("Open an image file before detecting edges.");
            return;
        }

        currentImage = SobelEdgeDetector.detect(currentImage);
        imagePanel.setImage(currentImage);
        information.append("\n\nApplied Sobel edge detection to the displayed image.");
    }

    private void adjustBrightnessContrast() {
        if (currentImage == null) {
            information.setText("Open an image file before adjusting brightness or contrast.");
            return;
        }

        BufferedImage original = currentImage;
        JDialog dialog = new JDialog(frame, "Brightness / Contrast", true);
        JSlider brightnessSlider = new JSlider(-100, 100, 0);
        JSlider contrastSlider = new JSlider(0, 200, 100);
        JLabel brightnessLabel = new JLabel("Brightness: 0");
        JLabel contrastLabel = new JLabel("Contrast: 1.00");
        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        boolean[] applied = {false};

        Runnable updatePreview = () -> {
            int brightness = brightnessSlider.getValue();
            double contrast = contrastSlider.getValue() / 100.0;
            brightnessLabel.setText("Brightness: " + brightness);
            contrastLabel.setText(String.format("Contrast: %.2f", contrast));
            imagePanel.setImage(BrightnessContrastAdjuster.adjust(original, brightness, contrast));
        };
        brightnessSlider.addChangeListener(event -> updatePreview.run());
        contrastSlider.addChangeListener(event -> updatePreview.run());
        applyButton.addActionListener(event -> {
            currentImage = BrightnessContrastAdjuster.adjust(original,
                    brightnessSlider.getValue(), contrastSlider.getValue() / 100.0);
            applied[0] = true;
            dialog.dispose();
        });
        cancelButton.addActionListener(event -> dialog.dispose());

        JPanel controls = new JPanel(new GridLayout(0, 1, 0, 6));
        controls.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        controls.add(brightnessLabel);
        controls.add(brightnessSlider);
        controls.add(contrastLabel);
        controls.add(contrastSlider);

        JPanel buttons = new JPanel();
        buttons.add(applyButton);
        buttons.add(cancelButton);
        dialog.add(controls, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(applyButton);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        if (!applied[0]) {
            imagePanel.setImage(original);
        } else {
            imagePanel.setImage(currentImage);
            information.append("\n\nApplied brightness and contrast adjustments.");
        }
    }

    private void transformImage() {
        if (currentImage == null) {
            information.setText("Open an image file before applying a geometric transformation.");
            return;
        }

        String[] operations = {
                "Rotate clockwise",
                "Scale",
                "Translate",
                "Flip horizontally",
                "Flip vertically"
        };
        String operation = (String) JOptionPane.showInputDialog(frame,
                "Choose a geometric transformation:", "Geometric Transform",
                JOptionPane.PLAIN_MESSAGE, null, operations, operations[0]);
        if (operation == null) {
            return;
        }

        try {
            if (operations[0].equals(operation)) {
                String value = JOptionPane.showInputDialog(frame,
                        "Clockwise rotation angle in degrees:", "90");
                if (value == null) {
                    return;
                }
                currentImage = GeometricTransformer.rotate(currentImage, Double.parseDouble(value));
            } else if (operations[1].equals(operation)) {
                String value = JOptionPane.showInputDialog(frame,
                        "Horizontal and vertical scale factors (for example, 1.5, 1.5):",
                        "1.0, 1.0");
                if (value == null) {
                    return;
                }
                String[] factors = value.split(",", -1);
                if (factors.length != 2) {
                    throw new IllegalArgumentException("Enter exactly two scale factors separated by a comma.");
                }
                currentImage = GeometricTransformer.scale(currentImage,
                        Double.parseDouble(factors[0].trim()),
                        Double.parseDouble(factors[1].trim()));
            } else if (operations[2].equals(operation)) {
                String horizontal = JOptionPane.showInputDialog(frame,
                        "Horizontal translation in pixels (positive moves right):", "0");
                if (horizontal == null) {
                    return;
                }
                String vertical = JOptionPane.showInputDialog(frame,
                        "Vertical translation in pixels (positive moves down):", "0");
                if (vertical == null) {
                    return;
                }
                currentImage = GeometricTransformer.translate(currentImage,
                        Integer.parseInt(horizontal.trim()), Integer.parseInt(vertical.trim()));
            } else if (operations[3].equals(operation)) {
                currentImage = GeometricTransformer.flipHorizontal(currentImage);
            } else {
                currentImage = GeometricTransformer.flipVertical(currentImage);
            }
        } catch (IllegalArgumentException exception) {
            information.append("\n\nCould not apply transformation: " + exception.getMessage());
            return;
        }

        imagePanel.setImage(currentImage);
        information.append("\n\nApplied geometric transformation: " + operation
                + ".\nDisplayed dimensions: " + currentImage.getWidth() + " x "
                + currentImage.getHeight());
    }

    private void saveCopy() {
        if (openedPath == null) {
            information.setText("Open an image file before saving a copy.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save an unchanged copy");
        chooser.setSelectedFile(openedPath.getFileName().toFile());
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.copy(openedPath, chooser.getSelectedFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            information.append("\n\nCopy saved successfully.");
        } catch (IOException exception) {
            information.append("\n\nCould not save copy: " + exception.getMessage());
        }
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

    private static final class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient BufferedImage image;
        private final JLabel placeholder = new JLabel("No image loaded", SwingConstants.CENTER);

        private ImagePanel() {
            setPreferredSize(new Dimension(700, 600));
            add(placeholder);
        }

        private void setImage(BufferedImage image) {
            this.image = image;
            removeAll();
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) {
                return;
            }
            int availableWidth = Math.max(1, getWidth() - 20);
            int availableHeight = Math.max(1, getHeight() - 20);
            double scale = Math.min((double) availableWidth / image.getWidth(),
                    (double) availableHeight / image.getHeight());
            scale = Math.min(1.0, scale);
            int width = Math.max(1, (int) (image.getWidth() * scale));
            int height = Math.max(1, (int) (image.getHeight() * scale));
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            graphics.drawImage(scaled, (getWidth() - width) / 2, (getHeight() - height) / 2, this);
        }
    }

    private static final class HistogramPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int LEFT_MARGIN = 58;
        private static final int RIGHT_MARGIN = 18;
        private static final int TOP_MARGIN = 20;
        private static final int BOTTOM_MARGIN = 38;
        private final long[] frequencies;
        private final long maximumFrequency;

        private HistogramPanel(long[] frequencies) {
            this.frequencies = frequencies.clone();
            long maximum = 0;
            for (long frequency : frequencies) {
                maximum = Math.max(maximum, frequency);
            }
            maximumFrequency = maximum;
            setPreferredSize(new Dimension(680, 380));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graph = (Graphics2D) graphics.create();
            graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int plotWidth = getWidth() - LEFT_MARGIN - RIGHT_MARGIN;
            int plotHeight = getHeight() - TOP_MARGIN - BOTTOM_MARGIN;
            int plotBottom = TOP_MARGIN + plotHeight;
            graph.setColor(new Color(225, 225, 225));
            graph.drawLine(LEFT_MARGIN, TOP_MARGIN, LEFT_MARGIN, plotBottom);
            graph.drawLine(LEFT_MARGIN, plotBottom, LEFT_MARGIN + plotWidth, plotBottom);

            graph.setColor(new Color(230, 230, 230));
            graph.drawLine(LEFT_MARGIN, TOP_MARGIN + plotHeight / 2,
                    LEFT_MARGIN + plotWidth, TOP_MARGIN + plotHeight / 2);
            graph.setColor(Color.DARK_GRAY);
            graph.drawString(Long.toString(maximumFrequency), 6, TOP_MARGIN + 5);
            graph.drawString("Intensity", LEFT_MARGIN + plotWidth / 2 - 25, getHeight() - 8);

            FontMetrics metrics = graph.getFontMetrics();
            int[] levels = {0, 64, 128, 192, 255};
            for (int level : levels) {
                int x = LEFT_MARGIN + (int) ((long) level * plotWidth / 255);
                String label = Integer.toString(level);
                graph.drawLine(x, plotBottom, x, plotBottom + 4);
                graph.drawString(label, x - metrics.stringWidth(label) / 2, plotBottom + 18);
            }

            if (maximumFrequency > 0) {
                graph.setColor(new Color(65, 105, 145));
                for (int intensity = 0; intensity < frequencies.length; intensity++) {
                    int x1 = LEFT_MARGIN + (int) ((long) intensity * plotWidth / 256);
                    int x2 = LEFT_MARGIN + (int) ((long) (intensity + 1) * plotWidth / 256);
                    int barHeight = (int) ((double) frequencies[intensity] / maximumFrequency
                            * plotHeight);
                    graph.fillRect(x1, plotBottom - barHeight,
                            Math.max(1, x2 - x1), barHeight);
                }
            }
            graph.dispose();
        }
    }
}