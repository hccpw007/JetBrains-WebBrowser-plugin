// 插件设置页面，实现 Configurable 接口
package com.cpw.browser.settings;

import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

public class BrowserSettingsPage implements Configurable {

    // 主页 URL 输入框
    private JBTextField homePageField;
    // 新标签页打开主页复选框
    private JBCheckBox openHomeCheckBox;
    // 历史记录最多保存天数字段
    private JBTextField maxHistoryDaysField;
    // 历史记录最多保存条数字段
    private JBTextField maxHistoryCountField;
    // 显示位置下拉框
    private JComboBox<String> displayPositionCombo;
    // 开发者工具打开方式下拉框
    private JComboBox<String> devToolsModeCombo;
    // 界面语言下拉框
    private JComboBox<String> languageCombo;
    // 默认搜索引擎下拉框
    private JComboBox<String> searchEngineCombo;

    // 用于语言切换后刷新标签的字段
    private JLabel homePageLabel;
    private TitledSeparator historySeparator;
    private JLabel historyDaysLabel;
    private JLabel historyCountLabel;
    private TitledSeparator devToolsSeparator;
    private JLabel devToolsModeLabel;
    private JLabel displayPositionLabel;
    private TitledSeparator languageSeparator;
    private JLabel languageLabel;
    private JLabel searchEngineLabel;
    private JLabel developerLabel;

    // 语言代码数组
    private static final String[] LANGUAGE_CODES = {"default", "zh", "en", "ja", "ko", "fr", "de"};
    // 各语言自身名称（第一项占位，运行时从资源包读取）
    private static final String[] LANGUAGE_NATIVE_NAMES = {"", "简体中文", "English", "日本語", "한국어", "Français", "Deutsch"};

    @Override
    public String getDisplayName() {
        return "WebBrowser";
    }

    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        // Row 0: 分隔线 — 语言（放在最上面）
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        c.weightx = 0.0;
        languageSeparator = new TitledSeparator(TranslationUtil.getText("settings.language"));
        panel.add(languageSeparator, c);

        // Row 1: 语言下拉框
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        languageLabel = new JLabel(TranslationUtil.getText("settings.language") + ":");
        panel.add(languageLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        languageCombo = new JComboBox<>(buildLanguageDisplayItems());
        panel.add(languageCombo, c);

        // Row 2: 主页 URL
        c.gridy = 2;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(12, 0, 0, 8);
        homePageLabel = new JLabel(TranslationUtil.getText("settings.homepage.url"));
        panel.add(homePageLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(12, 0, 0, 0);
        homePageField = new JBTextField();
        panel.add(homePageField, c);

        // Row 3: 新标签页时打开主页
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 0);
        openHomeCheckBox = new JBCheckBox(TranslationUtil.getText("settings.homepage.open"));
        panel.add(openHomeCheckBox, c);

        // Row 4: 默认搜索引擎
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(8, 0, 0, 8);
        searchEngineLabel = new JLabel(TranslationUtil.getText("settings.search.engine") + ":");
        panel.add(searchEngineLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(8, 0, 0, 0);
        searchEngineCombo = new JComboBox<>(new String[]{
                "Google",
                "Bing",
                "DuckDuckGo",
                "Baidu"
        });
        panel.add(searchEngineCombo, c);

        // Row 5: 分隔线 — 历史记录
        c.gridy = 5;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 8, 0);
        c.weightx = 0.0;
        historySeparator = new TitledSeparator(TranslationUtil.getText("settings.history"));
        panel.add(historySeparator, c);

        // Row 6: 最多保存天数
        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        historyDaysLabel = new JLabel(TranslationUtil.getText("settings.history.days"));
        panel.add(historyDaysLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        c.insets = new Insets(0, 0, 0, 0);
        maxHistoryDaysField = new JBTextField();
        maxHistoryDaysField.setColumns(4);
        panel.add(maxHistoryDaysField, c);

        // Row 7: 最多记录条数
        c.gridy = 7;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 8);
        historyCountLabel = new JLabel(TranslationUtil.getText("settings.history.entries"));
        panel.add(historyCountLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        c.insets = new Insets(4, 0, 0, 0);
        maxHistoryCountField = new JBTextField();
        maxHistoryCountField.setColumns(4);
        panel.add(maxHistoryCountField, c);

        // Row 8: 分隔线 — 开发者工具
        c.gridy = 8;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 8, 0);
        c.weightx = 0.0;
        devToolsSeparator = new TitledSeparator(TranslationUtil.getText("settings.devtools"));
        panel.add(devToolsSeparator, c);

        // Row 8: 打开方式
        c.gridy = 8;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        devToolsModeLabel = new JLabel(TranslationUtil.getText("settings.devtools.mode"));
        panel.add(devToolsModeLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        devToolsModeCombo = new JComboBox<>(new String[]{
                TranslationUtil.getText("settings.devtools.below"),
                TranslationUtil.getText("settings.devtools.window")
        });
        panel.add(devToolsModeCombo, c);

        // Row 10: 显示位置
        c.gridy = 10;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 8);
        displayPositionLabel = new JLabel(TranslationUtil.getText("settings.display.position"));
        panel.add(displayPositionLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 0, 0, 0);
        displayPositionCombo = new JComboBox<>(new String[]{
                TranslationUtil.getText("settings.position.toolbar"),
                TranslationUtil.getText("settings.position.editor")
        });
        panel.add(displayPositionCombo, c);

        // Row 11: 右下角签名
        c.gridy = 11;
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 0, 0, 0);
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footerPanel.setOpaque(false);
        developerLabel = new JLabel(TranslationUtil.getText("settings.developer"), SwingConstants.RIGHT);
        developerLabel.setFont(developerLabel.getFont().deriveFont(11f));
        footerPanel.add(developerLabel);
        panel.add(footerPanel, c);

        return panel;
    }

    // 构建语言下拉框显示项
    private String[] buildLanguageDisplayItems() {
        String[] items = new String[LANGUAGE_CODES.length];
        items[0] = TranslationUtil.getText("settings.language.default");
        System.arraycopy(LANGUAGE_NATIVE_NAMES, 1, items, 1, LANGUAGE_NATIVE_NAMES.length - 1);
        return items;
    }

    @Override
    public boolean isModified() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        String homeText = homePageField != null ? homePageField.getText() : null;
        Boolean openHome = openHomeCheckBox != null ? openHomeCheckBox.isSelected() : null;
        Integer days = maxHistoryDaysField != null ? parseNullableInt(maxHistoryDaysField.getText()) : null;
        Integer count = maxHistoryCountField != null ? parseNullableInt(maxHistoryCountField.getText()) : null;
        String position = displayPositionCombo != null ? positionIndexToSetting(displayPositionCombo.getSelectedIndex()) : null;
        String devToolsMode = devToolsModeCombo != null ? devToolsIndexToSetting(devToolsModeCombo.getSelectedIndex()) : null;
        String lang = languageCombo != null ? languageDisplayIndexToCode(languageCombo.getSelectedIndex()) : null;
        String searchEngine = searchEngineCombo != null ? searchEngineIndexToCode(searchEngineCombo.getSelectedIndex()) : null;

        return !Objects.equals(homeText, state.getHomePageUrl())
                || !Objects.equals(openHome, state.isOpenHomeOnNewTab())
                || !Objects.equals(days, state.getMaxHistoryDays())
                || !Objects.equals(count, state.getMaxHistoryCount())
                || !Objects.equals(position, state.getDisplayPosition())
                || !Objects.equals(devToolsMode, state.getDevToolsMode())
                || !Objects.equals(lang, state.getLanguage())
                || !Objects.equals(searchEngine, state.getSearchEngine());
    }

    @Override
    public void apply() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        // 记录应用前的语言设置
        String oldLang = state.getLanguage();

        if (homePageField != null) {
            state.setHomePageUrl(homePageField.getText());
        }
        if (openHomeCheckBox != null) {
            state.setOpenHomeOnNewTab(openHomeCheckBox.isSelected());
        }
        if (maxHistoryDaysField != null) {
            Integer days = parseNullableInt(maxHistoryDaysField.getText());
            if (days != null) {
                state.setMaxHistoryDays(days);
            }
        }
        if (maxHistoryCountField != null) {
            Integer count = parseNullableInt(maxHistoryCountField.getText());
            if (count != null) {
                state.setMaxHistoryCount(count);
            }
        }
        if (displayPositionCombo != null) {
            state.setDisplayPosition(positionIndexToSetting(displayPositionCombo.getSelectedIndex()));
        }
        if (devToolsModeCombo != null) {
            state.setDevToolsMode(devToolsIndexToSetting(devToolsModeCombo.getSelectedIndex()));
        }
        if (languageCombo != null) {
            state.setLanguage(languageDisplayIndexToCode(languageCombo.getSelectedIndex()));
        }
        if (searchEngineCombo != null) {
            state.setSearchEngine(searchEngineIndexToCode(searchEngineCombo.getSelectedIndex()));
        }

        // 语言发生变更则刷新当前页面标签并通知所有 UI 组件
        String newLang = state.getLanguage();
        if (!Objects.equals(oldLang, newLang)) {
            refreshLabels();
            TranslationUtil.notifyLanguageChanged();
        }
    }

    // 语言切换后刷新所有标签文字
    private void refreshLabels() {
        int posIdx = displayPositionCombo.getSelectedIndex();
        int devIdx = devToolsModeCombo.getSelectedIndex();
        int langIdx = languageCombo.getSelectedIndex();
        int searchIdx = searchEngineCombo.getSelectedIndex();

        homePageLabel.setText(TranslationUtil.getText("settings.homepage.url"));
        openHomeCheckBox.setText(TranslationUtil.getText("settings.homepage.open"));
        searchEngineLabel.setText(TranslationUtil.getText("settings.search.engine") + ":");
        historySeparator.setText(TranslationUtil.getText("settings.history"));
        historyDaysLabel.setText(TranslationUtil.getText("settings.history.days"));
        historyCountLabel.setText(TranslationUtil.getText("settings.history.entries"));
        devToolsSeparator.setText(TranslationUtil.getText("settings.devtools"));
        devToolsModeLabel.setText(TranslationUtil.getText("settings.devtools.mode"));
        displayPositionLabel.setText(TranslationUtil.getText("settings.display.position"));
        languageSeparator.setText(TranslationUtil.getText("settings.language"));
        languageLabel.setText(TranslationUtil.getText("settings.language") + ":");
        developerLabel.setText(TranslationUtil.getText("settings.developer"));

        // 刷新显示位置下拉框
        displayPositionCombo.removeAllItems();
        displayPositionCombo.addItem(TranslationUtil.getText("settings.position.toolbar"));
        displayPositionCombo.addItem(TranslationUtil.getText("settings.position.editor"));
        displayPositionCombo.setSelectedIndex(Math.max(0, Math.min(posIdx, 1)));

        // 刷新开发者工具下拉框
        devToolsModeCombo.removeAllItems();
        devToolsModeCombo.addItem(TranslationUtil.getText("settings.devtools.below"));
        devToolsModeCombo.addItem(TranslationUtil.getText("settings.devtools.window"));
        devToolsModeCombo.setSelectedIndex(Math.max(0, Math.min(devIdx, 1)));

        // 刷新语言下拉框第一项
        languageCombo.removeItemAt(0);
        languageCombo.insertItemAt(TranslationUtil.getText("settings.language.default"), 0);
        languageCombo.setSelectedIndex(Math.max(0, Math.min(langIdx, LANGUAGE_CODES.length - 1)));

        // 恢复搜索引擎下拉框选中项
        searchEngineCombo.setSelectedIndex(Math.max(0, Math.min(searchIdx, 3)));
    }

    @Override
    public void reset() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        if (homePageField != null) {
            homePageField.setText(state.getHomePageUrl());
        }
        if (openHomeCheckBox != null) {
            openHomeCheckBox.setSelected(state.isOpenHomeOnNewTab());
        }
        if (maxHistoryDaysField != null) {
            maxHistoryDaysField.setText(String.valueOf(state.getMaxHistoryDays()));
        }
        if (maxHistoryCountField != null) {
            maxHistoryCountField.setText(String.valueOf(state.getMaxHistoryCount()));
        }
        if (displayPositionCombo != null) {
            displayPositionCombo.setSelectedIndex(settingToPositionIndex(state.getDisplayPosition()));
        }
        if (devToolsModeCombo != null) {
            devToolsModeCombo.setSelectedIndex(settingToDevToolsIndex(state.getDevToolsMode()));
        }
        if (languageCombo != null) {
            languageCombo.setSelectedIndex(codeToLanguageDisplayIndex(state.getLanguage()));
        }
        if (searchEngineCombo != null) {
            searchEngineCombo.setSelectedIndex(codeToSearchEngineIndex(state.getSearchEngine()));
        }
    }

    // 显示位置索引 -> 设置值
    private static String positionIndexToSetting(int index) {
        return index == 1 ? "editor" : "toolbar";
    }

    // 设置值 -> 显示位置索引
    private static int settingToPositionIndex(String setting) {
        return "editor".equals(setting) ? 1 : 0;
    }

    // 开发者工具索引 -> 设置值
    private static String devToolsIndexToSetting(int index) {
        return index == 1 ? "window" : "split";
    }

    // 设置值 -> 开发者工具索引
    private static int settingToDevToolsIndex(String setting) {
        return "window".equals(setting) ? 1 : 0;
    }

    // 语言下拉索引 -> 语言代码
    private static String languageDisplayIndexToCode(int index) {
        if (index >= 0 && index < LANGUAGE_CODES.length) {
            return LANGUAGE_CODES[index];
        }
        return "default";
    }

    // 语言代码 -> 语言下拉索引
    private static int codeToLanguageDisplayIndex(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) {
                return i;
            }
        }
        return 0;
    }

    // 搜索引擎代码列表
    private static final String[] SEARCH_ENGINE_CODES = {"google", "bing", "duckduckgo", "baidu"};

    // 搜索引擎下拉索引 -> 代码
    private static String searchEngineIndexToCode(int index) {
        if (index >= 0 && index < SEARCH_ENGINE_CODES.length) {
            return SEARCH_ENGINE_CODES[index];
        }
        return "google";
    }

    // 搜索引擎代码 -> 下拉索引
    private static int codeToSearchEngineIndex(String code) {
        for (int i = 0; i < SEARCH_ENGINE_CODES.length; i++) {
            if (SEARCH_ENGINE_CODES[i].equals(code)) {
                return i;
            }
        }
        return 0;
    }

    // 安全解析整数
    private static Integer parseNullableInt(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
