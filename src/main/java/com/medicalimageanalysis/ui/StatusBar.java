package com.medicalimageanalysis.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class StatusBar extends JPanel {
    private final JLabel actionLabel = new JLabel("Ready");
    private final JLabel imageLabel = new JLabel("No image | — | —");

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
        imageLabel.setText(width + "x" + height + " | " + format + " | "
                + zoomPercent + "%");
    }
}
