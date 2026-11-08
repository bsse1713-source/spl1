package com.medicalimageanalysis;

import com.medicalimageanalysis.ui.MainWindow;
import com.medicalimageanalysis.ui.Theme;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply();
            new MainWindow().show();
        });
    }
}
