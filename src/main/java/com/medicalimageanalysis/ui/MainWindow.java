package com.medicalimageanalysis.ui;

import com.medicalimageanalysis.io.BmpReader;
import com.medicalimageanalysis.io.TiffReader;
import com.medicalimageanalysis.model.BmpImage;
import com.medicalimageanalysis.model.TiffImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
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

    public MainWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationByPlatform(true);

        JButton openBmpButton = new JButton("Open BMP");
        openBmpButton.addActionListener(event -> openBmp());

        JButton openTiffButton = new JButton("Open TIFF");
        openTiffButton.addActionListener(event -> openTiff());

        JButton saveButton = new JButton("Save Copy");
        saveButton.addActionListener(event -> saveCopy());

        JPanel buttons = new JPanel();
        buttons.add(openBmpButton);
        buttons.add(openTiffButton);
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
            imagePanel.setImage(image.toBufferedImage());
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
            imagePanel.setImage(image.toBufferedImage());
            information.setText(formatInformation(image));
        } catch (IOException exception) {
            information.setText("Could not open TIFF:\n" + exception.getMessage());
        }
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
}