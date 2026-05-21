// 插件设置页面，实现 Configurable 接口
package com.cpw.browser.settings;

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

    @Override
    public String getDisplayName() {
        return "WebBrowser";
    }

    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        // Row 0: 主页 URL — 标签和输入框在同一行
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        panel.add(new JLabel("主页 URL:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        homePageField = new JBTextField();
        panel.add(homePageField, c);

        // Row 1: 新标签页时打开主页
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 0);
        openHomeCheckBox = new JBCheckBox("新标签页时打开主页");
        panel.add(openHomeCheckBox, c);

        // Row 2: 分隔线 — 历史记录
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 8, 0);
        c.weightx = 0.0;
        panel.add(new TitledSeparator("历史记录"), c);

        // Row 3: 最多保存天数 — 标签和输入框
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        panel.add(new JLabel("最多保存天数:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        c.insets = new Insets(0, 0, 0, 0);
        maxHistoryDaysField = new JBTextField();
        maxHistoryDaysField.setColumns(4);
        panel.add(maxHistoryDaysField, c);

        // Row 4: 最多记录条数 — 标签和输入框
        c.gridy = 4;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 8);
        panel.add(new JLabel("最多记录条数:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        c.insets = new Insets(4, 0, 0, 0);
        maxHistoryCountField = new JBTextField();
        maxHistoryCountField.setColumns(4);
        panel.add(maxHistoryCountField, c);

        // Row 5: 分隔线 — 开发者工具
        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 8, 0);
        c.weightx = 0.0;
        panel.add(new TitledSeparator("开发者工具"), c);

        // Row 6: 打开方式 — 标签和下拉框
        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 8);
        panel.add(new JLabel("打开方式:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        devToolsModeCombo = new JComboBox<>(new String[]{"当前页面下方", "独立窗口"});
        panel.add(devToolsModeCombo, c);

        // Row 7: 显示位置 — 标签和下拉框
        c.gridy = 7;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(4, 0, 0, 8);
        panel.add(new JLabel("显示位置:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 0, 0, 0);
        displayPositionCombo = new JComboBox<>(new String[]{"工具栏", "编辑区"});
        panel.add(displayPositionCombo, c);

        // Row 8: 右下角签名
        c.gridy = 8;
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 0, 0, 0);
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footerPanel.setOpaque(false);
        JLabel devLabel = new JLabel("开发者:陈彭伟", SwingConstants.RIGHT);
        devLabel.setFont(devLabel.getFont().deriveFont(11f));
        footerPanel.add(devLabel);
        panel.add(footerPanel, c);

        return panel;
    }

    // 检查设置是否被修改
    @Override
    public boolean isModified() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        String homeText = homePageField != null ? homePageField.getText() : null;
        Boolean openHome = openHomeCheckBox != null ? openHomeCheckBox.isSelected() : null;
        Integer days = maxHistoryDaysField != null ? parseNullableInt(maxHistoryDaysField.getText()) : null;
        Integer count = maxHistoryCountField != null ? parseNullableInt(maxHistoryCountField.getText()) : null;
        String selectedItem = displayPositionCombo != null ? (String) displayPositionCombo.getSelectedItem() : null;
        String position = selectedItem != null ? positionToSetting(selectedItem) : null;
        String devToolsItem = devToolsModeCombo != null ? (String) devToolsModeCombo.getSelectedItem() : null;
        String devToolsMode = devToolsItem != null ? devToolsModeToSetting(devToolsItem) : null;

        return !Objects.equals(homeText, state.getHomePageUrl())
                || !Objects.equals(openHome, state.isOpenHomeOnNewTab())
                || !Objects.equals(days, state.getMaxHistoryDays())
                || !Objects.equals(count, state.getMaxHistoryCount())
                || !Objects.equals(position, state.getDisplayPosition())
                || !Objects.equals(devToolsMode, state.getDevToolsMode());
    }

    // 应用设置并保存到持久化状态
    @Override
    public void apply() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        // 主页 URL 字段存在时保存
        if (homePageField != null) {
            state.setHomePageUrl(homePageField.getText());
        }
        // 新标签页复选框存在时保存
        if (openHomeCheckBox != null) {
            state.setOpenHomeOnNewTab(openHomeCheckBox.isSelected());
        }
        // 历史天数字段存在时尝试解析并保存
        if (maxHistoryDaysField != null) {
            Integer days = parseNullableInt(maxHistoryDaysField.getText());
            // 解析成功时才更新
            if (days != null) {
                state.setMaxHistoryDays(days);
            }
        }
        // 历史条数字段存在时尝试解析并保存
        if (maxHistoryCountField != null) {
            Integer count = parseNullableInt(maxHistoryCountField.getText());
            // 解析成功时才更新
            if (count != null) {
                state.setMaxHistoryCount(count);
            }
        }
        // 显示位置下拉框存在时保存
        if (displayPositionCombo != null) {
            String selected = (String) displayPositionCombo.getSelectedItem();
            state.setDisplayPosition(positionToSetting(selected));
        }
        // 开发者工具下拉框存在时保存
        if (devToolsModeCombo != null) {
            String selected = (String) devToolsModeCombo.getSelectedItem();
            state.setDevToolsMode(devToolsModeToSetting(selected));
        }
    }

    // 重置 UI 控件为当前持久化设置的值
    @Override
    public void reset() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        // 主页 URL 字段存在时恢复
        if (homePageField != null) {
            homePageField.setText(state.getHomePageUrl());
        }
        // 新标签页复选框存在时恢复
        if (openHomeCheckBox != null) {
            openHomeCheckBox.setSelected(state.isOpenHomeOnNewTab());
        }
        // 历史天数字段存在时恢复
        if (maxHistoryDaysField != null) {
            maxHistoryDaysField.setText(String.valueOf(state.getMaxHistoryDays()));
        }
        // 历史条数字段存在时恢复
        if (maxHistoryCountField != null) {
            maxHistoryCountField.setText(String.valueOf(state.getMaxHistoryCount()));
        }
        // 显示位置下拉框存在时恢复
        if (displayPositionCombo != null) {
            displayPositionCombo.setSelectedItem(settingToPosition(state.getDisplayPosition()));
        }
        // 开发者工具下拉框存在时恢复
        if (devToolsModeCombo != null) {
            devToolsModeCombo.setSelectedItem(settingToDevToolsMode(state.getDevToolsMode()));
        }
    }

    // 将显示文本转换为设置值
    private static String positionToSetting(String display) {
        if ("编辑区".equals(display)) {
            return "editor";
        }
        return "toolbar";
    }

    // 将设置值转换为显示文本
    private static String settingToPosition(String setting) {
        if ("editor".equals(setting)) {
            return "编辑区";
        }
        return "工具栏";
    }

    // 将显示文本转换为设置值
    private static String devToolsModeToSetting(String display) {
        if ("独立窗口".equals(display)) {
            return "window";
        }
        return "split";
    }

    // 将设置值转换为显示文本
    private static String settingToDevToolsMode(String setting) {
        if ("window".equals(setting)) {
            return "独立窗口";
        }
        return "当前页面下方";
    }

    // 安全地将文本解析为整数，解析失败时返回 null
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
