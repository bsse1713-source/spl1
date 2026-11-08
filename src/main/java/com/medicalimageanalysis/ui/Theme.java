package com.medicalimageanalysis.ui;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public final class Theme {
    public static final Color WINDOW_BACKGROUND = new Color(0x1E2530);
    public static final Color PANEL_BACKGROUND = new Color(0x262E3B);
    public static final Color ACCENT = new Color(0x2A9DB0);
    public static final Color TEXT = new Color(0xE6EAF0);
    public static final Color MUTED_TEXT = new Color(0x8D99A8);
    public static final Color IMAGE_BACKGROUND = new Color(0x11161D);
    public static final Font UI_FONT = new Font(resolveFontFamily(), Font.PLAIN, 13);
    public static final Font MONOSPACED_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private Theme() {
    }

    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException
                 | javax.swing.UnsupportedLookAndFeelException exception) {
            throw new IllegalStateException("Could not initialize the Nimbus look and feel.", exception);
        }

        UIManager.put("control", PANEL_BACKGROUND);
        UIManager.put("info", PANEL_BACKGROUND);
        UIManager.put("nimbusBase", new Color(0x202A36));
        UIManager.put("nimbusBlueGrey", PANEL_BACKGROUND);
        UIManager.put("nimbusLightBackground", WINDOW_BACKGROUND);
        UIManager.put("text", TEXT);
        UIManager.put("Panel.background", PANEL_BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_BACKGROUND);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("TextArea.background", WINDOW_BACKGROUND);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextField.background", WINDOW_BACKGROUND);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("Menu.background", PANEL_BACKGROUND);
        UIManager.put("Menu.foreground", TEXT);
        UIManager.put("MenuItem.background", PANEL_BACKGROUND);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuBar.background", PANEL_BACKGROUND);
        UIManager.put("MenuBar.foreground", TEXT);
        UIManager.put("ToolBar.background", PANEL_BACKGROUND);
        UIManager.put("OptionPane.background", PANEL_BACKGROUND);
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("Menu.font", UI_FONT);
        UIManager.put("MenuItem.font", UI_FONT);
        UIManager.put("MenuBar.font", UI_FONT);
        UIManager.put("TextArea.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("TitledBorder.titleColor", MUTED_TEXT);
        UIManager.put("defaultFont", UI_FONT);
    }

    private static String resolveFontFamily() {
        String[] availableFamilies = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        return Arrays.asList(availableFamilies).contains("Segoe UI")
                ? "Segoe UI" : Font.SANS_SERIF;
    }
}
