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

    private JBTextField homePageField;
    private JBCheckBox openHomeCheckBox;
    private JBTextField maxHistoryDaysField;
    private JBTextField maxHistoryCountField;
    private JComboBox<String> displayPositionCombo;

    @Override
    public String getDisplayName() {
        return "WebBrowser";
    }

    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        // 主页 URL — 标签
        panel.add(new JLabel("主页 URL:"), c);

        // 主页 URL — 输入框
        c.gridy = 1;
        c.weightx = 1.0;
        c.insets = new Insets(2, 0, 6, 0);
        homePageField = new JBTextField();
        panel.add(homePageField, c);

        // 新标签页时打开主页
        c.gridy = 2;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 0);
        openHomeCheckBox = new JBCheckBox("新标签页时打开主页");
        panel.add(openHomeCheckBox, c);

        // 分隔线 — 历史设置区域
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 8, 0);
        panel.add(new TitledSeparator("历史记录"), c);

        // 历史天数 — 标签和输入框
        c.gridy = 4;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 2, 16);
        c.weightx = 0.5;
        JLabel daysLabel = new JLabel("最多保存天数:");
        panel.add(daysLabel, c);
        c.gridx = 1;
        JLabel countLabel = new JLabel("最多记录条数:");
        panel.add(countLabel, c);

        c.gridy = 5;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 8);
        c.weightx = 0.5;
        maxHistoryDaysField = new JBTextField();
        maxHistoryDaysField.setColumns(4);
        panel.add(maxHistoryDaysField, c);
        c.gridx = 1;
        c.insets = new Insets(0, 8, 0, 0);
        maxHistoryCountField = new JBTextField();
        maxHistoryCountField.setColumns(4);
        panel.add(maxHistoryCountField, c);

        // 位置 — 标签
        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(12, 0, 4, 0);
        c.weightx = 0.0;
        panel.add(new TitledSeparator("显示"), c);

        // 位置 — 下拉框
        c.gridy = 7;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 0.0;
        panel.add(new JLabel("位置:"), c);
        c.gridx = 1;
        displayPositionCombo = new JComboBox<>(new String[]{"工具栏", "编辑区"});
        panel.add(displayPositionCombo, c);

        // 右下角签名
        c.gridy = 8;
        c.gridx = 1;
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

    @Override
    public boolean isModified() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
        String homeText = homePageField != null ? homePageField.getText() : null;
        Boolean openHome = openHomeCheckBox != null ? openHomeCheckBox.isSelected() : null;
        Integer days = maxHistoryDaysField != null ? parseNullableInt(maxHistoryDaysField.getText()) : null;
        Integer count = maxHistoryCountField != null ? parseNullableInt(maxHistoryCountField.getText()) : null;
        String selectedItem = displayPositionCombo != null ? (String) displayPositionCombo.getSelectedItem() : null;
        String position = selectedItem != null ? positionToSetting(selectedItem) : null;

        return !Objects.equals(homeText, state.getHomePageUrl())
                || !Objects.equals(openHome, state.isOpenHomeOnNewTab())
                || !Objects.equals(days, state.getMaxHistoryDays())
                || !Objects.equals(count, state.getMaxHistoryCount())
                || !Objects.equals(position, state.getDisplayPosition());
    }

    @Override
    public void apply() {
        BrowserSettingsState state = BrowserSettingsState.getInstance();
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
            String selected = (String) displayPositionCombo.getSelectedItem();
            state.setDisplayPosition(positionToSetting(selected));
        }
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
            displayPositionCombo.setSelectedItem(settingToPosition(state.getDisplayPosition()));
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
