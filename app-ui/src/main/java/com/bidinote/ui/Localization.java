package com.bidinote.ui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化支持。
 */
public class Localization {
    private ResourceBundle bundle;

    public Localization(String locale) {
        Locale target = locale != null && locale.startsWith("en") ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
        bundle = ResourceBundle.getBundle("i18n.messages", target);
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
