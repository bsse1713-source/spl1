package com.medicalimageanalysis.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;
import java.util.function.Consumer;

public final class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;
    private static final double ZOOM_STEP = 0.1;

    private BufferedImage originalImage;
    private BufferedImage currentImage;
    private boolean comparing;
    private boolean fitToWindow = true;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private Point2D dragStart;
    private double dragPanX;
    private double dragPanY;
    private Consumer<String> pixelInfoListener = text -> { };
    private IntConsumer zoomListener = percent -> { };

    public ImagePanel() {
        setPreferredSize(new Dimension(700, 600));
        setBackground(Theme.IMAGE_BACKGROUND);
        setFocusable(true);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoomAt(event.getX(), event.getY(), event.getWheelRotation());
                event.consume();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (currentImage != null && event.getButton() == MouseEvent.BUTTON1
                        && event.getClickCount() == 1) {
                    dragStart = event.getPoint();
                    dragPanX = panX;
                    dragPanY = panY;
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragStart != null) {
                    fitToWindow = false;
                    panX = dragPanX + event.getX() - dragStart.getX();
                    panY = dragPanY + event.getY() - dragStart.getY();
                    repaint();
                    notifyZoomChanged();
                    updatePixelInfo(event.getX(), event.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragStart = null;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
                    resetToFit();
                }
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                updatePixelInfo(event.getX(), event.getY());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                pixelInfoListener.accept("");
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    public void setPixelInfoListener(Consumer<String> listener) {
        pixelInfoListener = listener;
    }

    public void setZoomListener(IntConsumer listener) {
        zoomListener = listener;
    }

    public void setImage(BufferedImage image) {
        if (originalImage == null) {
            originalImage = image;
        }
        currentImage = image;
        if (fitToWindow) {
            updateFitScale();
        }
        repaint();
        notifyZoomChanged();
    }

    public void setImages(BufferedImage original, BufferedImage current) {
        originalImage = original;
        currentImage = current;
        if (fitToWindow) {
            updateFitScale();
        }
        repaint();
        notifyZoomChanged();
    }

    public void setComparing(boolean comparing) {
        this.comparing = comparing;
        if (fitToWindow) {
            updateFitScale();
        }
        repaint();
        updatePixelInfo(-1, -1);
        notifyZoomChanged();
    }

    public boolean isComparing() {
        return comparing;
    }

    public void resetToFit() {
        fitToWindow = true;
        panX = 0;
        panY = 0;
        updateFitScale();
        repaint();
        updatePixelInfo(-1, -1);
        notifyZoomChanged();
    }

    public int getZoomPercent() {
        return (int) Math.round(currentScale() * 100);
    }

    private void zoomAt(int mouseX, int mouseY, int wheelRotation) {
        if (currentImage == null || wheelRotation == 0) {
            return;
        }

        double oldScale = currentScale();
        AffineTransform oldTransform = imageToScreenTransform(oldScale);
        Point2D imagePoint;
        try {
            // Invert the drawing transform to anchor zoom at the same image-space point.
            imagePoint = oldTransform.inverseTransform(new Point2D.Double(mouseX, mouseY), null);
        } catch (java.awt.geom.NoninvertibleTransformException exception) {
            return;
        }

        double nextScale = clamp(zoom + (wheelRotation < 0 ? ZOOM_STEP : -ZOOM_STEP),
                MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(nextScale - zoom) < 0.0001 && !fitToWindow) {
            return;
        }
        fitToWindow = false;
        zoom = nextScale;
        double virtualWidth = virtualWidth();
        panX = mouseX - imagePoint.getX() * zoom - (getWidth() - virtualWidth * zoom) / 2.0;
        panY = mouseY - imagePoint.getY() * zoom
                - (getHeight() - virtualHeight() * zoom) / 2.0;
        repaint();
        updatePixelInfo(mouseX, mouseY);
        notifyZoomChanged();
    }

    private void updateFitScale() {
        if (currentImage == null || getWidth() <= 0 || getHeight() <= 0) {
            zoom = 1.0;
            return;
        }
        double fit = Math.min((double) Math.max(1, getWidth() - 40) / virtualWidth(),
                (double) Math.max(1, getHeight() - 60) / virtualHeight());
        zoom = clamp(fit, MIN_ZOOM, MAX_ZOOM);
    }

    private double currentScale() {
        if (fitToWindow) {
            updateFitScale();
        }
        return zoom;
    }

    private AffineTransform imageToScreenTransform(double scale) {
        double x = (getWidth() - virtualWidth() * scale) / 2.0 + panX;
        double y = (getHeight() - virtualHeight() * scale) / 2.0 + panY;
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.scale(scale, scale);
        return transform;
    }

    private double virtualWidth() {
        return comparing && originalImage != null
                ? (double) originalImage.getWidth() + currentImage.getWidth()
                : currentImage.getWidth();
    }

    private int virtualHeight() {
        return comparing && originalImage != null
                ? Math.max(originalImage.getHeight(), currentImage.getHeight())
                : currentImage.getHeight();
    }

    private void updatePixelInfo(int screenX, int screenY) {
        if (currentImage == null || screenX < 0 || screenY < 0) {
            pixelInfoListener.accept("");
            return;
        }
        try {
            // Convert the cursor from screen coordinates back to the shared image-space layout.
            Point2D point = imageToScreenTransform(currentScale()).inverseTransform(
                    new Point2D.Double(screenX, screenY), null);
            int x = (int) Math.floor(point.getX());
            int y = (int) Math.floor(point.getY());
            BufferedImage inspectedImage = currentImage;
            if (comparing && originalImage != null) {
                if (x < originalImage.getWidth()) {
                    inspectedImage = originalImage;
                } else {
                    x -= originalImage.getWidth();
                }
            }
            if (x < 0 || y < 0 || x >= inspectedImage.getWidth()
                    || y >= inspectedImage.getHeight()) {
                pixelInfoListener.accept("");
                return;
            }
            int rgb = inspectedImage.getRGB(x, y);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            int intensity = (299 * red + 587 * green + 114 * blue) / 1000;
            pixelInfoListener.accept("x: " + x + "  y: " + y + "  intensity: "
                    + intensity + "  (R " + red + " G " + green + " B " + blue + ")");
        } catch (java.awt.geom.NoninvertibleTransformException exception) {
            pixelInfoListener.accept("");
        }
    }

    private void notifyZoomChanged() {
        zoomListener.accept(getZoomPercent());
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                currentScale() > 1.0 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                        : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (currentImage == null) {
            String message = "Open an image or drop a file here (BMP, TIFF, DICOM)";
            g.setColor(Theme.MUTED_TEXT);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(message, Math.max(12, (getWidth() - metrics.stringWidth(message)) / 2),
                    getHeight() / 2);
            g.dispose();
            return;
        }

        double scale = currentScale();
        AffineTransform transform = imageToScreenTransform(scale);
        if (comparing && originalImage != null) {
            drawImage(g, originalImage, transform, 0);
            drawImage(g, currentImage, transform, originalImage.getWidth());
            drawLabel(g, "Original", transform, 0);
            drawLabel(g, "Processed", transform, originalImage.getWidth());
            g.setColor(new Color(141, 153, 168, 180));
            double dividerX = transform.getTranslateX() + originalImage.getWidth() * scale;
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine((int) dividerX,
                    (int) transform.getTranslateY(),
                    (int) dividerX,
                    (int) (transform.getTranslateY() + virtualHeight() * scale));
        } else {
            drawImage(g, currentImage, transform, 0);
        }
        g.dispose();
    }

    private static void drawImage(Graphics2D graphics, BufferedImage image,
                                  AffineTransform baseTransform, int xOffset) {
        AffineTransform imageTransform = new AffineTransform(baseTransform);
        imageTransform.translate(xOffset, 0);
        graphics.drawImage(image, imageTransform, null);
    }

    private static void drawLabel(Graphics2D graphics, String label,
                                  AffineTransform transform, int xOffset) {
        double x = transform.getTranslateX() + xOffset * transform.getScaleX() + 8;
        double y = transform.getTranslateY() + 20;
        graphics.setColor(new Color(17, 22, 29, 190));
        graphics.fillRoundRect((int) x - 4, (int) y - 15, 82, 22, 6, 6);
        graphics.setColor(Theme.TEXT);
        graphics.drawString(label, (int) x, (int) y);
    }
}
