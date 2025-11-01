package com.bidinote.ui;

import javax.swing.UIManager;

/**
 * 主题切换：浅色/深色。
 */
public class ThemeManager {
    public void apply(String theme) {
        try {
            if ("dark".equalsIgnoreCase(theme)) {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            // ignore fallback
        }
    }
}
