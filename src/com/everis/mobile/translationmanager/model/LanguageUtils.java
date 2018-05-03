package com.everis.mobile.translationmanager.model;

import java.util.HashMap;
import java.util.ResourceBundle;

public class LanguageUtils {

    public static final String BUNDLE_MESSAGES = "com/everis/mobile/translationmanager/i18n/Messages";

    private static LanguageUtils sInstance;

    private HashMap<String, ResourceBundle> mResourceBundle;

    public static LanguageUtils getInstance() {
        if (sInstance == null) {
            sInstance = new LanguageUtils();
        }
        return sInstance;
    }

    private LanguageUtils() {
        mResourceBundle = new HashMap<>();
    }

    public String getString(String key) {
        return getString(BUNDLE_MESSAGES, key);
    }

    public String getString(String bundle, String key) {
        if (!mResourceBundle.containsKey(bundle)) {
            mResourceBundle.put(bundle, ResourceBundle.getBundle(bundle));
        }
        return mResourceBundle.get(bundle).getString(key);
    }
}
