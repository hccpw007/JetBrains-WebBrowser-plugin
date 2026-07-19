// 插件设置页面，实现 Configurable 接口
package com.cpw.browser.settings;

import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;

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
    // 编辑区模式下点击图标是否新建独立标签页复选框
    private JBCheckBox editorNewTabOnClickCheckBox;
    // 点击书签是否打开新标签页复选框
    private JBCheckBox bookmarkOpenNewTabCheckBox;
    // 开发者工具打开方式下拉框
    private JComboBox<String> devToolsModeCombo;
    // 界面语言下拉框
    private JComboBox<String> languageCombo;
    // 默认搜索引擎下拉框
    private JComboBox<String> searchEngineCombo;

    // 字段标签（语言切换后需刷新）
    private JLabel languageLabel;
    private JLabel homePageLabel;
    private JLabel searchEngineLabel;
    private JLabel historyDaysLabel;
    private JLabel historyCountLabel;
    private JLabel displayPositionLabel;
    private JLabel devToolsModeLabel;

    // 段分隔线（语言切换后需刷新）
    private TitledSeparator generalSeparator;
    private TitledSeparator homepageSearchSeparator;
    private TitledSeparator historySeparator;
    private TitledSeparator displaySeparator;
    private TitledSeparator devToolsSeparator;

    // 字段下方灰色说明（语言切换后需刷新）
    private JLabel languageComment;
    private JLabel homePageUrlComment;
    private JLabel searchEngineComment;
    private JLabel historyDaysComment;
    private JLabel historyCountComment;
    private JLabel displayPositionComment;
    private JLabel editorNewTabComment;
    private JLabel bookmarkNewTabComment;
    private JLabel devToolsModeComment;

    // 右下角开发者署名
    private JLabel developerLabel;

    // 语言代码数组
    private static final String[] LANGUAGE_CODES = {"default", "zh", "en", "ja", "ko", "fr", "de"};
    // 各语言自身名称（第一项占位，运行时从资源包读取）
    private static final String[] LANGUAGE_NATIVE_NAMES = {"", "简体中文", "English", "日本語", "한국어", "Français", "Deutsch"};
    // 搜索引擎代码列表
    private static final String[] SEARCH_ENGINE_CODES = {"google", "bing", "duckduckgo", "baidu"};
    // 搜索引擎下拉显示名称
    private static final String[] SEARCH_ENGINE_DISPLAY_NAMES = {"Google", "Bing", "DuckDuckGo", "Baidu"};
    // 字段说明文字字号
    private static final float COMMENT_FONT_SIZE = 11f;
    // 复选框下方说明文字的左缩进（对齐 checkbox 文本起点）
    private static final int CHECKBOX_COMMENT_INDENT = 24;
    // 数值字段列宽
    private static final int NUMERIC_FIELD_COLUMNS = 6;

    // 返回设置页显示名称
    @Override
    public String getDisplayName() {
        return "WebBrowser";
    }

    // 构建设置页面根面板，按 5 个功能段组织字段
    @Override
    public JComponent createComponent() {
        // 根面板使用 GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        // 段 1：常规
        generalSeparator = new TitledSeparator(TranslationUtil.getText("settings.section.general"));
        row = addSeparator(panel, c, row, generalSeparator);
        languageLabel = new JLabel(TranslationUtil.getText("settings.language"));
        languageCombo = new JComboBox<>(buildLanguageDisplayItems());
        languageComment = createCommentLabel(TranslationUtil.getText("settings.language.comment"));
        row = addLabeledRow(panel, c, row, languageLabel, languageCombo, true, languageComment);

        // 段 2：主页与搜索
        homepageSearchSeparator = new TitledSeparator(TranslationUtil.getText("settings.section.homepage.search"));
        row = addSeparator(panel, c, row, homepageSearchSeparator);
        homePageLabel = new JLabel(TranslationUtil.getText("settings.homepage.url"));
        homePageField = new JBTextField();
        homePageUrlComment = createCommentLabel(TranslationUtil.getText("settings.homepage.url.comment"));
        row = addLabeledRow(panel, c, row, homePageLabel, homePageField, true, homePageUrlComment);
        openHomeCheckBox = new JBCheckBox(TranslationUtil.getText("settings.homepage.open"));
        // 新标签页打开主页文案已自解释，无需说明
        row = addCheckbox(panel, c, row, openHomeCheckBox, null);
        searchEngineLabel = new JLabel(TranslationUtil.getText("settings.search.engine"));
        searchEngineCombo = new JComboBox<>(SEARCH_ENGINE_DISPLAY_NAMES);
        searchEngineComment = createCommentLabel(TranslationUtil.getText("settings.search.engine.comment"));
        row = addLabeledRow(panel, c, row, searchEngineLabel, searchEngineCombo, true, searchEngineComment);

        // 段 3：历史记录
        historySeparator = new TitledSeparator(TranslationUtil.getText("settings.history"));
        row = addSeparator(panel, c, row, historySeparator);
        historyDaysLabel = new JLabel(TranslationUtil.getText("settings.history.days"));
        maxHistoryDaysField = new JBTextField();
        maxHistoryDaysField.setColumns(NUMERIC_FIELD_COLUMNS);
        historyDaysComment = createCommentLabel(TranslationUtil.getText("settings.history.days.comment"));
        row = addLabeledRow(panel, c, row, historyDaysLabel, maxHistoryDaysField, false, historyDaysComment);
        historyCountLabel = new JLabel(TranslationUtil.getText("settings.history.entries"));
        maxHistoryCountField = new JBTextField();
        maxHistoryCountField.setColumns(NUMERIC_FIELD_COLUMNS);
        historyCountComment = createCommentLabel(TranslationUtil.getText("settings.history.entries.comment"));
        row = addLabeledRow(panel, c, row, historyCountLabel, maxHistoryCountField, false, historyCountComment);

        // 段 4：界面与显示
        displaySeparator = new TitledSeparator(TranslationUtil.getText("settings.section.display"));
        row = addSeparator(panel, c, row, displaySeparator);
        displayPositionLabel = new JLabel(TranslationUtil.getText("settings.display.position"));
        displayPositionCombo = new JComboBox<>(new String[]{
                TranslationUtil.getText("settings.position.toolbar"),
                TranslationUtil.getText("settings.position.editor")
        });
        displayPositionComment = createCommentLabel(TranslationUtil.getText("settings.display.position.comment"));
        row = addLabeledRow(panel, c, row, displayPositionLabel, displayPositionCombo, true, displayPositionComment);
        editorNewTabOnClickCheckBox = new JBCheckBox(TranslationUtil.getText("settings.editor.new.tab.on.click"));
        editorNewTabComment = createCommentLabel(TranslationUtil.getText("settings.editor.new.tab.on.click.comment"));
        // 仅在编辑区模式下可见
        editorNewTabOnClickCheckBox.setVisible(false);
        editorNewTabComment.setVisible(false);
        row = addCheckbox(panel, c, row, editorNewTabOnClickCheckBox, editorNewTabComment);
        bookmarkOpenNewTabCheckBox = new JBCheckBox(TranslationUtil.getText("settings.bookmark.open.new.tab"));
        bookmarkNewTabComment = createCommentLabel(TranslationUtil.getText("settings.bookmark.open.new.tab.comment"));
        row = addCheckbox(panel, c, row, bookmarkOpenNewTabCheckBox, bookmarkNewTabComment);

        // 显示位置变化时同步控制 editorNewTabOnClick 及其说明文字显隐
        displayPositionCombo.addItemListener(e -> {
            // 选中 editor（索引 1）时显示，否则隐藏
            boolean isEditor = displayPositionCombo.getSelectedIndex() == 1;
            editorNewTabOnClickCheckBox.setVisible(isEditor);
            editorNewTabComment.setVisible(isEditor);
        });

        // 段 5：开发者工具
        devToolsSeparator = new TitledSeparator(TranslationUtil.getText("settings.devtools"));
        row = addSeparator(panel, c, row, devToolsSeparator);
        devToolsModeLabel = new JLabel(TranslationUtil.getText("settings.devtools.mode"));
        devToolsModeCombo = new JComboBox<>(new String[]{
                TranslationUtil.getText("settings.devtools.below"),
                TranslationUtil.getText("settings.devtools.window")
        });
        devToolsModeComment = createCommentLabel(TranslationUtil.getText("settings.devtools.mode.comment"));
        row = addLabeledRow(panel, c, row, devToolsModeLabel, devToolsModeCombo, true, devToolsModeComment);

        // 页脚：右下角开发者署名
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1.0;
        // 占据剩余纵向空间，把署名推到面板底部
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.insets = new Insets(20, 0, 0, 0);
        // 页脚容器，右对齐
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footerPanel.setOpaque(false);
        developerLabel = new JLabel(TranslationUtil.getText("settings.developer"), SwingConstants.RIGHT);
        developerLabel.setFont(developerLabel.getFont().deriveFont(COMMENT_FONT_SIZE));
        footerPanel.add(developerLabel);
        panel.add(footerPanel, c);

        return panel;
    }

    // 添加段分隔线，返回下一行行号
    private int addSeparator(JPanel panel, GridBagConstraints c, int row, TitledSeparator separator) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        // 首段不加上间距，后续段加 12px 顶部间距
        int topInset = row == 0 ? 0 : 12;
        c.insets = new Insets(topInset, 0, 8, 0);
        panel.add(separator, c);
        return row + 1;
    }

    // 添加"标签 + 控件 + 下方说明"行，返回下一行行号。fillHorizontal 控制控件是否横向填充
    private int addLabeledRow(JPanel panel, GridBagConstraints c, int row, JLabel label, JComponent field, boolean fillHorizontal, JLabel comment) {
        // 标签列，固定宽度
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 8);
        panel.add(label, c);
        // 控件列
        c.gridx = 1;
        if (fillHorizontal) {
            // 文本框/下拉框横向填充
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
        } else {
            // 数值字段保持自然宽度
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
        }
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(field, c);
        // 字段下方灰色说明
        row++;
        c.gridy = row;
        c.gridx = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(2, 0, 0, 0);
        panel.add(comment, c);
        return row + 1;
    }

    // 添加跨整行的复选框，可选下方缩进说明，返回下一行行号
    private int addCheckbox(JPanel panel, GridBagConstraints c, int row, JBCheckBox checkBox, JLabel comment) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 0, 0);
        panel.add(checkBox, c);
        // 无说明时直接返回
        if (comment == null) {
            return row + 1;
        }
        // 复选框下方缩进的灰色说明
        row++;
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, CHECKBOX_COMMENT_INDENT, 0, 0);
        panel.add(comment, c);
        return row + 1;
    }

    // 创建灰色小字说明标签
    private JLabel createCommentLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setFont(label.getFont().deriveFont(COMMENT_FONT_SIZE));
        return label;
    }

    // 构建语言下拉框显示项
    private String[] buildLanguageDisplayItems() {
        String[] items = new String[LANGUAGE_CODES.length];
        items[0] = TranslationUtil.getText("settings.language.default");
        System.arraycopy(LANGUAGE_NATIVE_NAMES, 1, items, 1, LANGUAGE_NATIVE_NAMES.length - 1);
        return items;
    }

    // 判断设置页是否有未保存的修改
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

        // editorNewTabOnClick 仅在 checkbox 可见时（editor 模式）比较
        boolean newTabModified = editorNewTabOnClickCheckBox != null
                && editorNewTabOnClickCheckBox.isVisible()
                && editorNewTabOnClickCheckBox.isSelected() != state.isEditorNewTabOnClick();

        // bookmarkOpenNewTab 始终可见，直接比较选中状态
        boolean bookmarkNewTabModified = bookmarkOpenNewTabCheckBox != null
                && bookmarkOpenNewTabCheckBox.isSelected() != state.isBookmarkOpenNewTab();

        return !Objects.equals(homeText, state.getHomePageUrl())
                || !Objects.equals(openHome, state.isOpenHomeOnNewTab())
                || !Objects.equals(days, state.getMaxHistoryDays())
                || !Objects.equals(count, state.getMaxHistoryCount())
                || !Objects.equals(position, state.getDisplayPosition())
                || !Objects.equals(devToolsMode, state.getDevToolsMode())
                || !Objects.equals(lang, state.getLanguage())
                || !Objects.equals(searchEngine, state.getSearchEngine())
                || newTabModified
                || bookmarkNewTabModified;
    }

    // 应用设置页修改到持久化状态
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
        // editorNewTabOnClick 仅在 checkbox 可见时（editor 模式）写入
        if (editorNewTabOnClickCheckBox != null && editorNewTabOnClickCheckBox.isVisible()) {
            state.setEditorNewTabOnClick(editorNewTabOnClickCheckBox.isSelected());
        }

        // bookmarkOpenNewTab 始终写入
        if (bookmarkOpenNewTabCheckBox != null) {
            state.setBookmarkOpenNewTab(bookmarkOpenNewTabCheckBox.isSelected());
        }

        // 语言发生变更则刷新当前页面标签并通知所有 UI 组件
        String newLang = state.getLanguage();
        if (!Objects.equals(oldLang, newLang)) {
            refreshLabels();
            TranslationUtil.notifyLanguageChanged();
        }
    }

    // 语言切换后刷新所有标签、分隔线与说明文字
    private void refreshLabels() {
        // 记录各下拉框当前选中索引，刷新后恢复
        int posIdx = displayPositionCombo.getSelectedIndex();
        int devIdx = devToolsModeCombo.getSelectedIndex();
        int langIdx = languageCombo.getSelectedIndex();
        int searchIdx = searchEngineCombo.getSelectedIndex();

        // 段分隔线
        generalSeparator.setText(TranslationUtil.getText("settings.section.general"));
        homepageSearchSeparator.setText(TranslationUtil.getText("settings.section.homepage.search"));
        historySeparator.setText(TranslationUtil.getText("settings.history"));
        displaySeparator.setText(TranslationUtil.getText("settings.section.display"));
        devToolsSeparator.setText(TranslationUtil.getText("settings.devtools"));

        // 字段标签
        languageLabel.setText(TranslationUtil.getText("settings.language"));
        homePageLabel.setText(TranslationUtil.getText("settings.homepage.url"));
        searchEngineLabel.setText(TranslationUtil.getText("settings.search.engine"));
        historyDaysLabel.setText(TranslationUtil.getText("settings.history.days"));
        historyCountLabel.setText(TranslationUtil.getText("settings.history.entries"));
        displayPositionLabel.setText(TranslationUtil.getText("settings.display.position"));
        devToolsModeLabel.setText(TranslationUtil.getText("settings.devtools.mode"));

        // 复选框文本
        openHomeCheckBox.setText(TranslationUtil.getText("settings.homepage.open"));
        editorNewTabOnClickCheckBox.setText(TranslationUtil.getText("settings.editor.new.tab.on.click"));
        bookmarkOpenNewTabCheckBox.setText(TranslationUtil.getText("settings.bookmark.open.new.tab"));

        // 字段下方灰色说明
        languageComment.setText(TranslationUtil.getText("settings.language.comment"));
        homePageUrlComment.setText(TranslationUtil.getText("settings.homepage.url.comment"));
        searchEngineComment.setText(TranslationUtil.getText("settings.search.engine.comment"));
        historyDaysComment.setText(TranslationUtil.getText("settings.history.days.comment"));
        historyCountComment.setText(TranslationUtil.getText("settings.history.entries.comment"));
        displayPositionComment.setText(TranslationUtil.getText("settings.display.position.comment"));
        editorNewTabComment.setText(TranslationUtil.getText("settings.editor.new.tab.on.click.comment"));
        bookmarkNewTabComment.setText(TranslationUtil.getText("settings.bookmark.open.new.tab.comment"));
        devToolsModeComment.setText(TranslationUtil.getText("settings.devtools.mode.comment"));

        // 页脚署名
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

    // 从持久化状态重置设置页控件
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
        if (editorNewTabOnClickCheckBox != null) {
            // displayPositionCombo 的 ItemListener 会先更新 checkbox 显隐，此处再设置选中值
            editorNewTabOnClickCheckBox.setSelected(state.isEditorNewTabOnClick());
        }

        // bookmarkOpenNewTab 始终可见，直接读取持久化值
        if (bookmarkOpenNewTabCheckBox != null) {
            bookmarkOpenNewTabCheckBox.setSelected(state.isBookmarkOpenNewTab());
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
