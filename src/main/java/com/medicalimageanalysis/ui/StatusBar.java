package com.medicalimageanalysis.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class StatusBar extends JPanel {
    private final JLabel actionLabel = new JLabel("Ready");
    private final JLabel imageLabel = new JLabel("No image | — | —");
    private String imageSummary = "No image | — | —";
    private String pixelInfo = "";

    public StatusBar() {
        super(new BorderLayout(12, 0));
        setBackground(Theme.PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        actionLabel.setForeground(Theme.TEXT);
        imageLabel.setForeground(Theme.MUTED_TEXT);
        add(actionLabel, BorderLayout.WEST);
        add(imageLabel, BorderLayout.EAST);
    }

    public void setLastAction(String action) {
        actionLabel.setText(action);
    }

    public void setImageStatus(int width, int height, String format, int zoomPercent) {
        imageSummary = width + "x" + height + " | " + format + " | " + zoomPercent + "%";
        refreshImageLabel();
    }

    public void setZoomPercent(int zoomPercent) {
        if (!"No image | — | —".equals(imageSummary)) {
            int lastSeparator = imageSummary.lastIndexOf(" | ");
            imageSummary = imageSummary.substring(0, lastSeparator + 3) + zoomPercent + "%";
            refreshImageLabel();
        }
    }

    public void setPixelInfo(String pixelInfo) {
        this.pixelInfo = pixelInfo;
        refreshImageLabel();
    }

    private void refreshImageLabel() {
        imageLabel.setText(pixelInfo.isEmpty() ? imageSummary : imageSummary + " | " + pixelInfo);
    }
}
