package com.everis.mobile;

import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Main {

    private static final int OS_MACOS = 0;
    private static final int OS_WINDOWS = 1;
    private static final int PLATFORM_ANDROID = 0;
    private static final int PLATFORM_IOS = 1;
    private static final String NAME_ANDROID = "android";
    private static final String NAME_IOS = "ios";

    private static final String LOG_FILE_FORMAT = "keylist_%s.txt";

    private static final String PREFS_TRANSLATIONS_PATH = "PREFS_TRANSLATIONS_PATH";
    private static final String PREFS_ANDROID_PATH = "PREFS_ANDROID_PATH";
    private static final String PREFS_IOS_PATH = "PREFS_IOS_PATH";

    private JLabel translationsLabel;
    private JCheckBox androidCheckbox;
    private JCheckBox iosCheckbox;
    private JTextField translationsPathTextField;
    private JTextField androidPathTextField;
    private JTextField iosPathTextField;
    private JButton translationsPathButton;
    private JButton androidPathButton;
    private JButton iosPathButton;
    private JButton copyButton;
    private JComboBox templateComboBox;
    private JButton newTemplateButton;
    private JPanel mainPanel;
    private JButton testButton;

    private final JFileChooser fc = new JFileChooser();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Main");
        frame.setContentPane(new Main().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(new Dimension(400, 200));
        frame.setVisible(true);
    }

    private void openFileChooser(String title, String initialPath, JTextField textFieldToUpdate) {
        fc.setCurrentDirectory(new java.io.File((initialPath == null || initialPath.length() == 0) ? "." : initialPath));
        fc.setDialogTitle(title);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            textFieldToUpdate.setText(file.getAbsolutePath());
        }
    }

    private void updateGui() {
        iosPathTextField.setEnabled(iosCheckbox.isSelected());
        iosPathButton.setEnabled(iosCheckbox.isSelected());
        androidPathTextField.setEnabled(androidCheckbox.isSelected());
        androidPathButton.setEnabled(androidCheckbox.isSelected());
        copyButton.setEnabled(iosCheckbox.isSelected() || androidCheckbox.isSelected());
    }

    public Main() {
        translationsPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser("Translations Project", translationsPathTextField.getText(), translationsPathTextField);
            }
        });
        androidPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser("Android Project", androidPathTextField.getText(), androidPathTextField);
            }
        });
        iosPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser("iOS Project", iosPathTextField.getText(), iosPathTextField);
            }
        });
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    performCopy();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        iosCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGui();
            }
        });
        androidCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGui();
            }
        });

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                test();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                savePreferences();
            }
        });

        loadPreferences();
        updateGui();
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);

        prefs.put(PREFS_TRANSLATIONS_PATH, translationsPathTextField.getText());

        if (androidCheckbox.isSelected()) {
            prefs.put(PREFS_ANDROID_PATH, androidPathTextField.getText());
        } else {
            prefs.remove(PREFS_ANDROID_PATH);
        }

        if (iosCheckbox.isSelected()) {
            prefs.put(PREFS_IOS_PATH, iosPathTextField.getText());
        } else {
            prefs.remove(PREFS_IOS_PATH);
        }
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);

        String pref;

        pref = prefs.get(PREFS_TRANSLATIONS_PATH, null);
        if (pref != null) {
            translationsPathTextField.setText(pref);
        }

        pref = prefs.get(PREFS_ANDROID_PATH, null);
        if (pref != null) {
            androidCheckbox.setSelected(true);
            androidPathTextField.setText(pref);
        }

        pref = prefs.get(PREFS_IOS_PATH, null);
        if (pref != null) {
            iosCheckbox.setSelected(true);
            iosPathTextField.setText(pref);
        }
    }

    private String getLogFileName(int platformId) {
        return String.format(LOG_FILE_FORMAT, getNameForOs(platformId));
    }

    private void generateStrings(File translationsPath, int platformId) {
        executeCommandWithArgs(getGenerateScriptFilePath(), translationsPath.getAbsolutePath(), getNameForOs(platformId));
    }

    private String getGenerateScriptFilePath() {
        String res = "";

        if (OSValidator.isMac()) {
            res = "assets/scripts/generate.sh";
        } else if (OSValidator.isWindows()) {
            res = "assets/scripts/generate.bat";
        }

        return res;
    }

    private void copyStrings(File translationsPath, int platformId) throws IOException {
        copyFolder(new File(Paths.get(translationsPath.getAbsolutePath(), getNameForOs(platformId)).toString()), new File(Paths.get(getDestinationPathForOs(platformId).getAbsolutePath()).toString()));
    }

    private String getNameForOs(int platformId) {
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

    private File getDestinationPathForOs(int platformId) {
        File res = null;

        switch (platformId) {
            case PLATFORM_ANDROID:
                res = new File(androidPathTextField.getText());
                break;

            case PLATFORM_IOS:
                res = new File(iosPathTextField.getText());
                break;
        }

        return res;
    }

    private void performCopy() throws IOException {
        final File translationsPath = new File(translationsPathTextField.getText());

        if (androidCheckbox.isSelected()) {
            generateStrings(translationsPath, PLATFORM_ANDROID);
            deleteLogFile(translationsPath, PLATFORM_ANDROID);
            copyStrings(translationsPath, PLATFORM_ANDROID);
        }

        if (iosCheckbox.isSelected()) {
            generateStrings(translationsPath, PLATFORM_IOS);
            deleteLogFile(translationsPath, PLATFORM_IOS);
            copyStrings(translationsPath, PLATFORM_IOS);
        }
    }

    private void test() {
        File translationsPath = new File("/Users/jromegom/repos/bsmobil-translations");
        final File androidPath = new File("/Users/jromegom/repos/bsmobil-android/sab3.0/src/main/res");
        int platformId = PLATFORM_ANDROID;

        deleteLogFile(translationsPath, platformId);
        copyFolder(new File(Paths.get(translationsPath.getAbsolutePath(), getNameForOs(platformId)).toString()), androidPath);
    }

    private void deleteLogFile(File translationsPath, int platformId) {
        new File(Paths.get(translationsPath.getAbsolutePath(), getNameForOs(platformId)).toString(), getLogFileName(platformId)).delete();
    }

    private void executeCommandWithArgs(String... args) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(args);
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            int exitVal = proc.waitFor();
            System.out.println("Process exitValue: " + exitVal);

            if (sb.length() > 0) {
                System.out.println("<ERROR>");
                System.out.println(sb.toString());
                System.out.println("</ERROR>");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void copyFolder(File source, File destination) {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void executeCommand(String command) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<ERROR>");
            while ((line = br.readLine()) != null)
                System.out.println(line);
            System.out.println("</ERROR>");
            int exitVal = proc.waitFor();
            System.out.println("Process exitValue: " + exitVal);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void executeCommand(String... args) {
        assert (args.length < 1);

        try {
            String cmd = args[0];
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(args);

            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERR");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUT");

            errorGobbler.start();
            outputGobbler.start();

            int exitVal = proc.waitFor();
            System.out.println("ExitValue: " + exitVal);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
