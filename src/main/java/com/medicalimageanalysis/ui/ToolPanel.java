package com.medicalimageanalysis.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class ToolPanel extends JPanel {
    private static final Dimension BUTTON_SIZE = new Dimension(220, 34);

    private final JButton[] toolButtons;

    public ToolPanel(Runnable grayscale, Runnable brightnessContrast, Runnable equalize,
                     Runnable filter, Runnable sobel, Runnable transform, Runnable histogram,
                     Runnable threshold, Runnable analyze) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel adjust = createGroup("Adjust",
                createButton("Grayscale", grayscale),
                createButton("Brightness / Contrast", brightnessContrast),
                createButton("Equalize", equalize));
        JPanel filters = createGroup("Filters",
                createButton("Filter", filter),
                createButton("Sobel Edges", sobel));
        JPanel transforms = createGroup("Transform",
                createButton("Geometric Transform", transform));
        JPanel analysis = createGroup("Analysis",
                createButton("Histogram", histogram),
                createButton("Threshold", threshold),
                createButton("Analyze", analyze));

        toolButtons = new JButton[9];
        int index = 0;
        for (JPanel group : new JPanel[]{adjust, filters, transforms, analysis}) {
            for (java.awt.Component component : group.getComponents()) {
                if (component instanceof JButton) {
                    toolButtons[index++] = (JButton) component;
                }
            }
            add(group);
            add(Box.createVerticalStrut(8));
        }
        setToolsEnabled(false);
    }

    public void setToolsEnabled(boolean enabled) {
        for (JButton button : toolButtons) {
            button.setEnabled(enabled);
        }
    }

    private JPanel createGroup(String title, JButton... buttons) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(Theme.PANEL_BACKGROUND);
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleColor(Theme.MUTED_TEXT);
        group.setBorder(border);
        for (JButton button : buttons) {
            button.setAlignmentX(LEFT_ALIGNMENT);
            group.add(button);
            group.add(Box.createVerticalStrut(5));
        }
        return group;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setMaximumSize(BUTTON_SIZE);
        button.setPreferredSize(BUTTON_SIZE);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setBackground(Theme.PANEL_BACKGROUND);
        button.setForeground(Theme.TEXT);
        button.setFocusPainted(false);
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(Theme.ACCENT);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(Theme.PANEL_BACKGROUND);
            }
        });
        return button;
    }
}
