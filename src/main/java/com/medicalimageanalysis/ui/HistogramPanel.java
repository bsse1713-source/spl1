package com.medicalimageanalysis.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class HistogramPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int LEFT_MARGIN = 58;
    private static final int RIGHT_MARGIN = 18;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 38;
    private final long[] frequencies;
    private final long maximumFrequency;

    public HistogramPanel(long[] frequencies) {
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
