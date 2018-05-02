package com.everis.mobile;

public class CopyTemplate {
    public String translationsPath;
    public String androidPath;
    public String iosPath;

    public CopyTemplate(String translationsPath, String androidPath, String iosPath) {
        this.translationsPath = translationsPath;
        this.androidPath = androidPath;
        this.iosPath = iosPath;
    }

    public boolean isAndroidSelected() {
        return androidPath != null && androidPath.length() > 0;
    }

    public boolean isIosSelected() {
        return iosPath != null && iosPath.length() > 0;
    }
}
