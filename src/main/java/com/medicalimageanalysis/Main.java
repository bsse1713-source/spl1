package com.medicalimageanalysis;

import com.medicalimageanalysis.ui.MainWindow;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().show());
    }
}
