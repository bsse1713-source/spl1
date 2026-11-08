package com.medicalimageanalysis.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

public final class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private transient BufferedImage image;
    private final JLabel placeholder = new JLabel("No image loaded", SwingConstants.CENTER);

    public ImagePanel() {
        setPreferredSize(new Dimension(700, 600));
        setBackground(Theme.IMAGE_BACKGROUND);
        placeholder.setForeground(Theme.MUTED_TEXT);
        add(placeholder);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        removeAll();
        revalidate();
        repaint();
    }

    public int getZoomPercent() {
        return image == null ? 100 : (int) Math.round(getScale() * 100);
    }

    private double getScale() {
        int availableWidth = Math.max(1, getWidth() - 20);
        int availableHeight = Math.max(1, getHeight() - 20);
        double scale = Math.min((double) availableWidth / image.getWidth(),
                (double) availableHeight / image.getHeight());
        return Math.min(1.0, scale);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) {
            return;
        }
        double scale = getScale();
        int width = Math.max(1, (int) (image.getWidth() * scale));
        int height = Math.max(1, (int) (image.getHeight() * scale));
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        graphics.drawImage(scaled, (getWidth() - width) / 2, (getHeight() - height) / 2, this);
    }
}
