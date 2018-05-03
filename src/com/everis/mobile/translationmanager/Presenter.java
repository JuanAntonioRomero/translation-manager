package com.everis.mobile.translationmanager;

import com.everis.mobile.translationmanager.model.LanguageUtils;
import com.everis.mobile.translationmanager.model.OSValidator;
import com.everis.mobile.translationmanager.model.StreamGobbler;
import com.everis.mobile.translationmanager.model.entities.CopyTemplate;
import com.everis.mobile.translationmanager.model.entities.TemplateList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Presenter {

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
    private static final String PREFS_TEMPLATE_LIST = "PREFS_TEMPLATE_LIST";

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
    private JButton saveAsTemplateButton;
    private JPanel mainPanel;
    private JButton helpButton;

    private final JFileChooser fc = new JFileChooser();
    private TemplateList mTemplates = new TemplateList();
    private String mSelectedTemplateKey = null;

    public JPanel getMainPanel() {
        return mainPanel;
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
        updateTemplateSelector();

        iosPathTextField.setEnabled(iosCheckbox.isSelected());
        iosPathButton.setEnabled(iosCheckbox.isSelected());
        androidPathTextField.setEnabled(androidCheckbox.isSelected());
        androidPathButton.setEnabled(androidCheckbox.isSelected());
        copyButton.setEnabled(iosCheckbox.isSelected() || androidCheckbox.isSelected());
    }

    private void updateGuiFromTemplateSelector() {
        if (mSelectedTemplateKey != null) {
            CopyTemplate template = mTemplates.templates.get(mSelectedTemplateKey);
            if (template != null) {
                translationsPathTextField.setText(template.translationsPath);
                androidPathTextField.setText(template.androidPath);
                iosPathTextField.setText(template.iosPath);
                androidCheckbox.setSelected(template.isAndroidSelected());
                iosCheckbox.setSelected(template.isIosSelected());
            }
        }
    }

    public Presenter() {
        translationsPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser(LanguageUtils.getInstance().getString("label_translations_project"), translationsPathTextField.getText(), translationsPathTextField);
            }
        });
        androidPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser(LanguageUtils.getInstance().getString("label_android_project"), androidPathTextField.getText(), androidPathTextField);
            }
        });
        iosPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser(LanguageUtils.getInstance().getString("label_ios_project"), iosPathTextField.getText(), iosPathTextField);
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

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });
        saveAsTemplateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAsTemplate();
            }
        });

        mTemplateSelectorItemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    mSelectedTemplateKey = (String) item;
                    updateGuiFromTemplateSelector();
                    updateGui();
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                savePreferences();
            }
        });

        loadPreferences();
        updateGui();
    }

    private void saveAsTemplate() {
        String text = JOptionPane.showInputDialog(mainPanel, LanguageUtils.getInstance().getString("input_save_template"));
        if (text != null) {
            text = text.trim();
            if (text.length() == 0) {
                JOptionPane.showMessageDialog(mainPanel, LanguageUtils.getInstance().getString("input_save_template_error_empty"));
            } else if (mTemplates.templates.keySet().contains(text)) {
                JOptionPane.showMessageDialog(mainPanel, LanguageUtils.getInstance().getString("input_save_template_error_duplicate"));
            } else {
                mTemplates.templates.put(text, new CopyTemplate(
                        translationsPathTextField.getText(),
                        androidCheckbox.isSelected() ? androidPathTextField.getText() : null,
                        iosCheckbox.isSelected() ? iosPathTextField.getText() : null));
                updateGui();
            }
        }
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Presenter.class);

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

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String templatesJson = gson.toJson(mTemplates);
        prefs.put(PREFS_TEMPLATE_LIST, templatesJson);
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Presenter.class);

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

        pref = prefs.get(PREFS_TEMPLATE_LIST, null);
        if (pref != null) {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            mTemplates = gson.fromJson(pref, TemplateList.class);
        }
    }

    ItemListener mTemplateSelectorItemListener;

    private void updateTemplateSelector() {
        templateComboBox.removeItemListener(mTemplateSelectorItemListener);

        templateComboBox.removeAllItems();
        ArrayList<String> sortedKeys = new ArrayList();
        sortedKeys.addAll(mTemplates.templates.keySet());
        Collections.sort(sortedKeys);
        for (String templateName : sortedKeys) {
            templateComboBox.addItem(templateName);
        }
        if (mSelectedTemplateKey != null) {
            templateComboBox.setSelectedIndex(sortedKeys.indexOf(mSelectedTemplateKey));
        }

        templateComboBox.addItemListener(mTemplateSelectorItemListener);
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

    private void showHelp() {
        JOptionPane.showMessageDialog(mainPanel, LanguageUtils.getInstance().getString("input_help"));
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
