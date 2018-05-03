package com.everis.mobile.translationmanager;

import com.everis.mobile.translationmanager.model.CommandUtils;
import com.everis.mobile.translationmanager.model.FileUtils;
import com.everis.mobile.translationmanager.model.LanguageUtils;
import com.everis.mobile.translationmanager.model.entities.CopyTemplate;
import com.everis.mobile.translationmanager.model.entities.TemplateList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Presenter {

    private static final String PREFS_TRANSLATIONS_PATH = "PREFS_TRANSLATIONS_PATH";
    private static final String PREFS_ANDROID_PATH = "PREFS_ANDROID_PATH";
    private static final String PREFS_IOS_PATH = "PREFS_IOS_PATH";
    private static final String PREFS_TEMPLATE_LIST = "PREFS_TEMPLATE_LIST";

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

    private final JFileChooser mFileChooser = new JFileChooser();
    private TemplateList mTemplates = new TemplateList();
    private String mSelectedTemplateKey = null;
    private ItemListener mTemplateSelectorItemListener;

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
                performPlatformCopy();
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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void openFileChooser(String title, String initialPath, JTextField textFieldToUpdate) {
        mFileChooser.setCurrentDirectory(new java.io.File((initialPath == null || initialPath.length() == 0) ? "." : initialPath));
        mFileChooser.setDialogTitle(title);
        mFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        mFileChooser.setAcceptAllFileFilterUsed(false);

        int returnVal = mFileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = mFileChooser.getSelectedFile();
            textFieldToUpdate.setText(file.getAbsolutePath());
        }
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(mainPanel, LanguageUtils.getInstance().getString("input_help"));
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

    private void generateStrings(File translationsPath, int platformId) {
        CommandUtils.executeCommandWithArgs(FileUtils.getGenerateScriptFilePath(), translationsPath.getAbsolutePath(), FileUtils.getNameForOs(platformId));
    }


    private void performPlatformCopy() {
        final File translationsPath = new File(translationsPathTextField.getText());

        if (androidCheckbox.isSelected()) {
            performPlatformCopy(translationsPath, FileUtils.PLATFORM_ANDROID);
        }

        if (iosCheckbox.isSelected()) {
            performPlatformCopy(translationsPath, FileUtils.PLATFORM_IOS);
        }
    }

    private void performPlatformCopy(File translationsPath, int platformId) {
        generateStrings(translationsPath, platformId);
        FileUtils.deleteLogFile(translationsPath, platformId);
        CommandUtils.copyFolder(new File(Paths.get(translationsPath.getAbsolutePath(), FileUtils.getNameForOs(platformId)).toString()), new File(Paths.get(getDestinationPathForOs(platformId).getAbsolutePath()).toString()));
    }

    private File getDestinationPathForOs(int platformId) {
        File res = null;

        switch (platformId) {
            case FileUtils.PLATFORM_ANDROID:
                res = new File(androidPathTextField.getText());
                break;

            case FileUtils.PLATFORM_IOS:
                res = new File(iosPathTextField.getText());
                break;
        }

        return res;
    }
}
