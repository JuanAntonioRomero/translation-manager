package com.everis.mobile.translationmanager.model;

import java.io.File;
import java.nio.file.Paths;

public class FileUtils {

    public static final int PLATFORM_ANDROID = 0;
    public static final int PLATFORM_IOS = 1;

    private static final int OS_MACOS = 0;
    private static final int OS_WINDOWS = 1;
    private static final String NAME_ANDROID = "android";
    private static final String NAME_IOS = "ios";

    private static final String LOG_FILE_FORMAT = "keylist_%s.txt";

    public static String getLogFileName(int platformId) {
        return String.format(LOG_FILE_FORMAT, getNameForOs(platformId));
    }

    public static String getGenerateScriptFilePath() {
        String res = "";

        if (OSValidator.isMac()) {
            res = "assets/scripts/generate.sh";
        } else if (OSValidator.isWindows()) {
            res = "assets/scripts/generate.bat";
        }

        return res;
    }

    public static String getNameForOs(int platformId) {
        String res = "";

        switch (platformId) {
            case PLATFORM_ANDROID:
                res = NAME_ANDROID;
                break;

            case PLATFORM_IOS:
                res = NAME_IOS;
                break;
        }

        return res;
    }

    public static void deleteLogFile(File translationsPath, int platformId) {
        new File(Paths.get(translationsPath.getAbsolutePath(), getNameForOs(platformId)).toString(), getLogFileName(platformId)).delete();
    }
}
