package com.medicalimageanalysis.ui;

import com.medicalimageanalysis.analysis.ConnectedComponents;
import com.medicalimageanalysis.analysis.ImageStatistics;
import com.medicalimageanalysis.analysis.RegionAnalyzer;
import com.medicalimageanalysis.analysis.RegionFeature;
import com.medicalimageanalysis.analysis.Thresholding;
import com.medicalimageanalysis.io.ImageLoader;
import com.medicalimageanalysis.io.LoadedImage;
import com.medicalimageanalysis.model.DicomImage;
import com.medicalimageanalysis.processing.BrightnessContrastAdjuster;
import com.medicalimageanalysis.processing.GaussianFilter;
import com.medicalimageanalysis.processing.GeometricTransformer;
import com.medicalimageanalysis.processing.GrayscaleConverter;
import com.medicalimageanalysis.processing.HistogramAnalyzer;
import com.medicalimageanalysis.processing.HistogramEqualizer;
import com.medicalimageanalysis.processing.MeanFilter;
import com.medicalimageanalysis.processing.MedianFilter;
import com.medicalimageanalysis.processing.SobelEdgeDetector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.TransferHandler;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class MainWindow {
    private static final int MAX_UNDO_ENTRIES = 10;

    private final JFrame frame = new JFrame("Medical Image Analysis System");
    private final ImagePanel imagePanel = new ImagePanel();
    private final JTextArea information = new JTextArea();
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final ToolPanel toolPanel = new ToolPanel(
            this::convertToGrayscale,
            this::adjustBrightnessContrast,
            this::equalizeImage,
            this::filterImage,
            this::detectEdges,
            this::transformImage,
            this::showHistogram,
            this::thresholdImage,
            this::analyzeImage);
    private final StatusBar statusBar = new StatusBar();
    private final JButton[] imageToolbarButtons = new JButton[3];
    private JCheckBoxMenuItem compareMenuItem;
    private JToggleButton compareToolbarButton;
    private Path openedPath;
    private BufferedImage currentImage;
    private BufferedImage originalImage;
    private String currentFormat;
    private DicomImage currentDicomImage;

    public MainWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setSize(1280, 800);
        frame.setLocationByPlatform(true);
        frame.setJMenuBar(createMenuBar());

        JPanel top = new JPanel(new BorderLayout());
        top.add(createToolbar(), BorderLayout.NORTH);
        frame.add(top, BorderLayout.NORTH);

        JScrollPane tools = new JScrollPane(toolPanel);
        tools.setPreferredSize(new Dimension(250, 0));
        tools.getViewport().setBackground(Theme.PANEL_BACKGROUND);
        frame.add(tools, BorderLayout.WEST);

        JScrollPane imageViewport = new JScrollPane(imagePanel);
        imageViewport.getViewport().setBackground(Theme.IMAGE_BACKGROUND);
        frame.add(imageViewport, BorderLayout.CENTER);
        TransferHandler imageDropHandler = createImageDropHandler();
        frame.setTransferHandler(imageDropHandler);
        imagePanel.setTransferHandler(imageDropHandler);
        imageViewport.setTransferHandler(imageDropHandler);

        imagePanel.setPixelInfoListener(statusBar::setPixelInfo);
        imagePanel.setZoomListener(statusBar::setZoomPercent);

        information.setEditable(false);
        information.setLineWrap(true);
        information.setWrapStyleWord(true);
        information.setFont(Theme.MONOSPACED_FONT);
        information.setBackground(Theme.WINDOW_BACKGROUND);
        information.setForeground(Theme.TEXT);
        information.setCaretColor(Theme.TEXT);
        information.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Image information"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        information.setText("Open a BMP, TIFF, or DICOM file to inspect it.");
        JScrollPane informationPane = new JScrollPane(information);
        informationPane.setPreferredSize(new Dimension(290, 0));
        frame.add(informationPane, BorderLayout.EAST);

        frame.add(statusBar, BorderLayout.SOUTH);
        imagePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                refreshImageStatus();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        addMenuItem(file, "Open Image", this::openImage,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        addMenuItem(file, "Save Processed", this::saveProcessed,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        addMenuItem(file, "Save Copy", this::saveCopy, null);
        file.addSeparator();
        addMenuItem(file, "Exit", frame::dispose, null);

        JMenu edit = new JMenu("Edit");
        addMenuItem(edit, "Undo", this::undoImage,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        addMenuItem(edit, "Reset", this::resetImage,
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

        JMenu process = new JMenu("Process");
        addMenuItem(process, "Grayscale", this::convertToGrayscale, null);
        addMenuItem(process, "Brightness/Contrast", this::adjustBrightnessContrast, null);
        addMenuItem(process, "Equalize", this::equalizeImage, null);
        addMenuItem(process, "Filter", this::filterImage, null);
        addMenuItem(process, "Sobel Edges", this::detectEdges, null);
        addMenuItem(process, "Geometric Transform", this::transformImage, null);

        JMenu analyze = new JMenu("Analyze");
        addMenuItem(analyze, "Histogram", this::showHistogram, null);
        addMenuItem(analyze, "Threshold", this::thresholdImage, null);
        addMenuItem(analyze, "Analyze Regions", this::analyzeImage, null);

        JMenu view = new JMenu("View");
        compareMenuItem = new JCheckBoxMenuItem("Compare Original / Processed");
        compareMenuItem.setEnabled(false);
        compareMenuItem.addActionListener(event -> setCompareView(compareMenuItem.isSelected()));
        view.add(compareMenuItem);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(process);
        menuBar.add(analyze);
        menuBar.add(view);
        return menuBar;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(Theme.PANEL_BACKGROUND);
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JButton open = createToolbarButton("Open", "Open an image", this::openImage);
        JButton save = createToolbarButton(
                "Save", "Save the processed image", this::saveProcessed);
        JButton undo = createToolbarButton("Undo", "Undo the last image operation", this::undoImage);
        JButton reset = createToolbarButton("Reset", "Restore the original image", this::resetImage);
        compareToolbarButton = new JToggleButton("Compare");
        compareToolbarButton.setToolTipText("Show original and processed images side by side");
        compareToolbarButton.setBackground(Theme.PANEL_BACKGROUND);
        compareToolbarButton.setForeground(Theme.TEXT);
        compareToolbarButton.setEnabled(false);
        compareToolbarButton.addActionListener(
                event -> setCompareView(compareToolbarButton.isSelected()));
        imageToolbarButtons[0] = save;
        imageToolbarButtons[1] = undo;
        imageToolbarButtons[2] = reset;
        save.setEnabled(false);
        undo.setEnabled(false);
        reset.setEnabled(false);
        toolbar.add(open);
        toolbar.add(save);
        toolbar.add(undo);
        toolbar.add(reset);
        toolbar.add(compareToolbarButton);
        return toolbar;
    }

    private JButton createToolbarButton(String label, String tooltip, Runnable action) {
        JButton button = new JButton(label);
        button.setToolTipText(tooltip);
        button.setBackground(Theme.PANEL_BACKGROUND);
        button.setForeground(Theme.TEXT);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static void addMenuItem(JMenu menu, String label, Runnable action,
                                    KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> action.run());
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        menu.add(item);
    }

    public void show() {
        frame.setVisible(true);
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open image");
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Medical images", "bmp", "tif", "tiff", "dcm"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        openImage(chooser.getSelectedFile().toPath(), false);
    }

    private void openImage(Path selected, boolean dropped) {
        try {
            LoadedImage loadedImage = ImageLoader.load(selected);
            openedPath = selected;
            currentImage = loadedImage.getImage();
            originalImage = currentImage;
            currentFormat = loadedImage.getFormatName();
            currentDicomImage = loadedImage.getDicomImage().orElse(null);
            undoStack.clear();
            imagePanel.setImages(originalImage, currentImage);
            imagePanel.resetToFit();
            information.setText(loadedImage.getDescription());
            toolPanel.setToolsEnabled(true);
            for (JButton button : imageToolbarButtons) {
                button.setEnabled(true);
            }
            compareToolbarButton.setEnabled(true);
            compareMenuItem.setEnabled(true);
            statusBar.setPixelInfo("");
            refreshImageStatus();
            statusBar.setLastAction(dropped ? "Dropped image opened" : "Image opened");
        } catch (IOException exception) {
            information.setText((dropped ? "Could not open dropped image:\n"
                    : "Could not open image:\n") + exception.getMessage());
            statusBar.setLastAction("Open failed");
        }
    }

    private TransferHandler createImageDropHandler() {
        return new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    information.setText("Drop an image file to open it.");
                    return false;
                }
                try {
                    Transferable transferable = support.getTransferable();
                    Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(data instanceof List<?>)) {
                        information.setText("Could not open dropped image: invalid file drop.");
                        return false;
                    }
                    List<?> files = (List<?>) data;
                    if (files.isEmpty() || !(files.get(0) instanceof File)) {
                        information.setText("Could not open dropped image: no file was provided.");
                        return false;
                    }
                    openImage(((File) files.get(0)).toPath(), true);
                    return true;
                } catch (java.awt.datatransfer.UnsupportedFlavorException
                         | IOException exception) {
                    information.setText("Could not open dropped image: " + exception.getMessage());
                    statusBar.setLastAction("Open failed");
                    return false;
                }
            }
        };
    }

    private void setCompareView(boolean enabled) {
        imagePanel.setComparing(enabled);
        if (compareMenuItem != null) {
            compareMenuItem.setSelected(enabled);
        }
        if (compareToolbarButton != null) {
            compareToolbarButton.setSelected(enabled);
        }
        statusBar.setLastAction(enabled ? "Before/after comparison enabled"
                : "Before/after comparison disabled");
    }

    private void convertToGrayscale() {
        if (currentImage == null) {
            information.setText("Open an image file before converting to grayscale.");
            return;
        }

        updateCurrentImage(GrayscaleConverter.toGrayscale(currentImage));
        information.append("\n\nDisplayed image converted to grayscale.");
        statusBar.setLastAction("Grayscale applied");
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
        statusBar.setLastAction("Histogram displayed");
    }

    private void detectEdges() {
        if (currentImage == null) {
            information.setText("Open an image file before detecting edges.");
            return;
        }

        updateCurrentImage(SobelEdgeDetector.detect(currentImage));
        information.append("\n\nApplied Sobel edge detection to the displayed image.");
        statusBar.setLastAction("Sobel edges applied");
    }

    private void filterImage() {
        if (currentImage == null) {
            information.setText("Open an image file before applying a filter.");
            return;
        }

        String[] filters = {"Mean", "Gaussian", "Median"};
        String filter = (String) JOptionPane.showInputDialog(frame,
                "Choose a filter:", "Filter",
                JOptionPane.PLAIN_MESSAGE, null, filters, filters[0]);
        if (filter == null) {
            return;
        }

        try {
            String sizeValue = JOptionPane.showInputDialog(frame,
                    "Kernel size (positive odd integer):", "3");
            if (sizeValue == null) {
                return;
            }
            int kernelSize = Integer.parseInt(sizeValue.trim());
            if (kernelSize <= 0 || (kernelSize & 1) == 0) {
                throw new IllegalArgumentException("Kernel size must be a positive odd integer.");
            }

            BufferedImage filteredImage;
            String appliedDetails = "Applied " + filter + " filter with kernel size " + kernelSize;
            if ("Mean".equals(filter)) {
                filteredImage = MeanFilter.apply(currentImage, kernelSize);
            } else if ("Gaussian".equals(filter)) {
                String sigmaValue = JOptionPane.showInputDialog(frame,
                        "Gaussian sigma (greater than 0):", "1.0");
                if (sigmaValue == null) {
                    return;
                }
                double sigma = Double.parseDouble(sigmaValue.trim());
                if (!Double.isFinite(sigma) || sigma <= 0) {
                    throw new IllegalArgumentException("Sigma must be greater than 0.");
                }
                filteredImage = GaussianFilter.apply(currentImage, kernelSize, sigma);
                appliedDetails += " and sigma " + sigma;
            } else {
                filteredImage = MedianFilter.apply(currentImage, kernelSize);
            }

            updateCurrentImage(filteredImage);
            information.append("\n\n" + appliedDetails + ".");
            statusBar.setLastAction(filter + " filter applied");
        } catch (IllegalArgumentException exception) {
            information.append("\n\nCould not apply filter: " + exception.getMessage());
            statusBar.setLastAction("Filter failed");
        }
    }

    private void equalizeImage() {
        if (currentImage == null) {
            information.setText("Open an image file before equalizing its histogram.");
            return;
        }

        updateCurrentImage(HistogramEqualizer.equalize(currentImage));
        information.append("\n\nApplied histogram equalization to the displayed image.");
        statusBar.setLastAction("Histogram equalization applied");
    }

    private void thresholdImage() {
        if (currentImage == null) {
            information.setText("Open an image file before thresholding it.");
            return;
        }

        String[] methods = {"Global", "Otsu"};
        String method = (String) JOptionPane.showInputDialog(frame,
                "Choose a thresholding method:", "Threshold",
                JOptionPane.PLAIN_MESSAGE, null, methods, methods[0]);
        if (method == null) {
            return;
        }

        try {
            BufferedImage thresholdedImage;
            if ("Global".equals(method)) {
                String value = JOptionPane.showInputDialog(frame,
                        "Threshold value (0-255):", "128");
                if (value == null) {
                    return;
                }
                int threshold = Integer.parseInt(value.trim());
                if (threshold < 0 || threshold > 255) {
                    throw new IllegalArgumentException("Threshold value must be between 0 and 255.");
                }
                thresholdedImage = Thresholding.globalThreshold(currentImage, threshold);
                updateCurrentImage(thresholdedImage);
                information.append("\n\nApplied global thresholding with value " + threshold + ".");
                statusBar.setLastAction("Global threshold applied");
            } else {
                int threshold = Thresholding.calculateOtsuThreshold(currentImage);
                information.append("\n\nCalculated Otsu threshold: " + threshold + ".");
                thresholdedImage = Thresholding.globalThreshold(currentImage, threshold);
                updateCurrentImage(thresholdedImage);
                information.append("\nApplied Otsu thresholding.");
                statusBar.setLastAction("Otsu threshold applied");
            }
        } catch (IllegalArgumentException exception) {
            information.append("\n\nCould not apply threshold: " + exception.getMessage());
            statusBar.setLastAction("Threshold failed");
        }
    }

    private void analyzeImage() {
        if (currentImage == null) {
            information.setText("Open an image file before analyzing it.");
            return;
        }

        BufferedImage source = currentImage;
        BufferedImage binaryImage = Thresholding.otsuThreshold(source);
        int[][] labels = ConnectedComponents.label(binaryImage);
        double pixelSpacingRow = currentDicomImage == null
                ? Double.NaN : currentDicomImage.getPixelSpacingRow();
        double pixelSpacingColumn = currentDicomImage == null
                ? Double.NaN : currentDicomImage.getPixelSpacingColumn();
        List<RegionFeature> regions = RegionAnalyzer.analyze(labels, source, 1,
                pixelSpacingRow, pixelSpacingColumn);
        int minimum = ImageStatistics.min(source);
        int maximum = ImageStatistics.max(source);
        double mean = ImageStatistics.mean(source);
        double standardDeviation = ImageStatistics.standardDeviation(source);
        double entropy = ImageStatistics.entropy(source);

        StringBuilder report = new StringBuilder("Feature analysis (Otsu threshold)")
                .append("\nRegions: ").append(regions.size())
                .append("\nImage statistics (grayscale intensity):")
                .append("\nMin: ").append(minimum)
                .append("  Max: ").append(maximum)
                .append(String.format(Locale.ROOT, "\nMean: %.2f  Standard deviation: %.2f"
                        + "\nShannon entropy: %.3f bits", mean, standardDeviation, entropy))
                .append("\n\nFirst ").append(Math.min(10, regions.size()))
                .append(" regions (label, pixel area, physical area, centroid, bounds, mean intensity):")
                .append("\nLabel | Pixel area | Physical area (mm²) | Centroid (x, y) "
                        + "| Bounds (minX, minY - maxX, maxY) | Mean");

        if (regions.isEmpty()) {
            report.append("\nNo foreground regions found.");
        } else {
            for (int i = 0; i < Math.min(10, regions.size()); i++) {
                RegionFeature region = regions.get(i);
                String physicalArea = region.hasPhysicalAreaMm2()
                        ? String.format(Locale.ROOT, "%.4f", region.getPhysicalAreaMm2())
                        : "Unavailable";
                report.append(String.format(Locale.ROOT, "\n%d | %d | %s | (%.2f, %.2f) | "
                                + "(%d, %d - %d, %d) | %.2f",
                        region.getLabel(), region.getArea(),
                        physicalArea,
                        region.getCentroidX(), region.getCentroidY(),
                        region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY(),
                        region.getMeanIntensity()));
            }
        }

        updateCurrentImage(ConnectedComponents.colorize(labels));
        information.setText(report.toString());
        statusBar.setLastAction("Regions analyzed");
    }

    private void resetImage() {
        if (currentImage == null || originalImage == null) {
            information.setText("Open an image file before resetting it.");
            return;
        }

        updateCurrentImage(originalImage);
        information.append("\n\nRestored the original image.");
        statusBar.setLastAction("Image reset");
    }

    private void undoImage() {
        if (currentImage == null) {
            information.setText("Open an image file before undoing an edit.");
            return;
        }
        if (undoStack.isEmpty()) {
            information.setText("There are no edits to undo.");
            return;
        }

        currentImage = undoStack.pop();
        imagePanel.setImages(originalImage, currentImage);
        information.append("\n\nUndid the last image operation.");
        refreshImageStatus();
        statusBar.setLastAction("Undo");
    }

    private void updateCurrentImage(BufferedImage image) {
        if (currentImage != null) {
            if (undoStack.size() == MAX_UNDO_ENTRIES) {
                undoStack.removeLast();
            }
            undoStack.push(currentImage);
        }
        currentImage = image;
        imagePanel.setImages(originalImage, currentImage);
        refreshImageStatus();
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
            imagePanel.setImages(originalImage,
                    BrightnessContrastAdjuster.adjust(original, brightness, contrast));
        };
        brightnessSlider.addChangeListener(event -> updatePreview.run());
        contrastSlider.addChangeListener(event -> updatePreview.run());
        applyButton.addActionListener(event -> {
            updateCurrentImage(BrightnessContrastAdjuster.adjust(original,
                    brightnessSlider.getValue(), contrastSlider.getValue() / 100.0));
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
            imagePanel.setImages(originalImage, original);
            refreshImageStatus();
        } else {
            imagePanel.setImages(originalImage, currentImage);
            refreshImageStatus();
            information.append("\n\nApplied brightness and contrast adjustments.");
            statusBar.setLastAction("Brightness/contrast applied");
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

        BufferedImage transformedImage;
        try {
            if (operations[0].equals(operation)) {
                String value = JOptionPane.showInputDialog(frame,
                        "Clockwise rotation angle in degrees:", "90");
                if (value == null) {
                    return;
                }
                transformedImage = GeometricTransformer.rotate(currentImage, Double.parseDouble(value));
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
                transformedImage = GeometricTransformer.scale(currentImage,
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
                transformedImage = GeometricTransformer.translate(currentImage,
                        Integer.parseInt(horizontal.trim()), Integer.parseInt(vertical.trim()));
            } else if (operations[3].equals(operation)) {
                transformedImage = GeometricTransformer.flipHorizontal(currentImage);
            } else {
                transformedImage = GeometricTransformer.flipVertical(currentImage);
            }
        } catch (IllegalArgumentException exception) {
            information.append("\n\nCould not apply transformation: " + exception.getMessage());
            return;
        }

        updateCurrentImage(transformedImage);
        information.append("\n\nApplied geometric transformation: " + operation
                + ".\nDisplayed dimensions: " + currentImage.getWidth() + " x "
                + currentImage.getHeight());
        statusBar.setLastAction(operation + " applied");
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
            statusBar.setLastAction("Image copy saved");
        } catch (IOException exception) {
            information.append("\n\nCould not save copy: " + exception.getMessage());
            statusBar.setLastAction("Save copy failed");
        }
    }

    private void saveProcessed() {
        if (currentImage == null) {
            information.setText("Open an image file before saving the processed image.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save processed image");
        chooser.setSelectedFile(new File("processed.png"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedPath = chooser.getSelectedFile().toPath();
        String fileName = selectedPath.getFileName().toString();
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator <= 0 || extensionSeparator == fileName.length() - 1) {
            String baseName = extensionSeparator == fileName.length() - 1
                    ? fileName.substring(0, extensionSeparator) : fileName;
            selectedPath = selectedPath.resolveSibling(baseName + ".png");
            fileName = selectedPath.getFileName().toString();
            extensionSeparator = fileName.lastIndexOf('.');
        }
        String format = fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);

        try {
            if (!ImageIO.write(currentImage, format, selectedPath.toFile())) {
                information.append("\n\nCould not save processed image: unsupported format \""
                        + format + "\".");
                statusBar.setLastAction("Unsupported save format");
                return;
            }
            information.append("\n\nProcessed image saved to " + selectedPath + ".");
            statusBar.setLastAction("Processed image saved");
        } catch (IOException exception) {
            information.append("\n\nCould not save processed image: " + exception.getMessage());
            statusBar.setLastAction("Save processed failed");
        }
    }

    private void refreshImageStatus() {
        if (currentImage == null || currentFormat == null) {
            return;
        }
        statusBar.setImageStatus(currentImage.getWidth(), currentImage.getHeight(),
                currentFormat, imagePanel.getZoomPercent());
    }
}