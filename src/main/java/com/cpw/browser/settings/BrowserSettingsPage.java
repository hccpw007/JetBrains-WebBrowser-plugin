// 插件设置页面—极简卡片式设计
package com.cpw.browser.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Objects;

public class BrowserSettingsPage implements Configurable {

    // 卡片圆角半径
    private static final int CARD_ARC = 10;
    // 卡片左侧装饰条宽度
    private static final int ACCENT_WIDTH = 4;

    // 色板—琥珀色主色调，冷暖互补
    private static final Color ACCENT_AMBER = new JBColor(0xC9902B, 0xD4A02B);
    private static final Color ACCENT_AMBER_DIM = new JBColor(0xE8D5A8, 0x8A6D2B);
    private static final Color CARD_BG = new JBColor(0xF8F6F2, 0x242538);
    private static final Color CARD_BORDER = new JBColor(0xE8E4DC, 0x2E2F45);
    private static final Color PAGE_BG = new JBColor(0xF0EEE8, 0x1A1B2E);
    private static final Color TEXT_SECONDARY = new JBColor(0x888888, 0x999999);
    private static final Color FIELD_BG = new JBColor(0xFFFFFF, 0x1E1F32);
    private static final Color FIELD_BORDER = new JBColor(0xD8D4CC, 0x3A3B52);

    // 字段
    private JBTextField homePageField;
    private JBCheckBox openHomeCheckBox;
    private JBTextField maxHistoryDaysField;
    private JBTextField maxHistoryCountField;
    private JComboBox<String> displayPositionCombo;
    private JComboBox<String> devToolsModeCombo;

    @Override
    public String getDisplayName() {
        return "WebBrowser";
    }

    @Override
    public JComponent createComponent() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(PAGE_BG);
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));

        // 中间对齐的内容列
        JPanel centerCol = new JPanel();
        centerCol.setOpaque(false);
        centerCol.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.NORTH;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // ---- 页面标题 ----
        centerCol.add(createHeader(), gc);

        // ---- 卡片 1: 常规 ----
        gc.gridy = 1;
        gc.insets = new Insets(20, 0, 0, 0);
        centerCol.add(createGeneralCard(), gc);

        // ---- 卡片 2: 历史记录 ----
        gc.gridy = 2;
        gc.insets = new Insets(14, 0, 0, 0);
        centerCol.add(createHistoryCard(), gc);

        // ---- 卡片 3: 界面 ----
        gc.gridy = 3;
        gc.insets = new Insets(14, 0, 0, 0);
        centerCol.add(createDisplayCard(), gc);

        // ---- 页脚 ----
        gc.gridy = 4;
        gc.insets = new Insets(20, 0, 0, 0);
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.SOUTH;
        centerCol.add(createFooter(), gc);

        page.add(centerCol, BorderLayout.NORTH);
        return page;
    }

    // ────────────── 标题 ──────────────
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JBLabel title = new JBLabel("WebBrowser", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        header.add(title, BorderLayout.WEST);

        JBLabel ver = new JBLabel("1.0.0", SwingConstants.RIGHT);
        ver.setFont(ver.getFont().deriveFont(12f));
        ver.setForeground(TEXT_SECONDARY);
        header.add(ver, BorderLayout.EAST);

        return header;
    }

    // ────────────── 通用卡片壳 ──────────────
    private JPanel wrapCard(String icon, String title, JComponent body) {
        // 卡片容器—自定义绘制背景与边框
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 卡片背景
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, w, h, CARD_ARC, CARD_ARC);

                // 左侧饰条
                g2.setColor(ACCENT_AMBER);
                g2.fillRoundRect(0, 8, ACCENT_WIDTH, h - 16, 3, 3);

                // 边框
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, w - 1, h - 1, CARD_ARC, CARD_ARC);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));

        // 卡片标题行
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleWrap = new JPanel(new BorderLayout(8, 0));
        titleWrap.setOpaque(false);

        JBLabel iconLbl = new JBLabel(icon);
        iconLbl.setFont(iconLbl.getFont().deriveFont(16f));
        titleWrap.add(iconLbl, BorderLayout.WEST);

        JBLabel titleLbl = new JBLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 13f));
        titleWrap.add(titleLbl, BorderLayout.CENTER);

        headerRow.add(titleWrap, BorderLayout.WEST);
        card.add(headerRow, BorderLayout.NORTH);

        // 正文
        JPanel bodyWrap = new JPanel(new BorderLayout());
        bodyWrap.setOpaque(false);
        bodyWrap.setBorder(BorderFactory.createEmptyBorder(14, 24, 0, 0));
        bodyWrap.add(body, BorderLayout.CENTER);
        card.add(bodyWrap, BorderLayout.CENTER);

        return card;
    }

    // ────────────── 常规卡片 ──────────────
    private JPanel createGeneralCard() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        // 主页 URL 标签
        JBLabel urlLabel = new JBLabel("主页地址");
        urlLabel.setFont(urlLabel.getFont().deriveFont(12f));
        body.add(urlLabel, c);

        // 主页 URL 输入框
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        c.weightx = 1.0;
        homePageField = new JBTextField();
        homePageField.setBackground(FIELD_BG);
        homePageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        body.add(homePageField, c);

        // 提示文字
        c.gridy = 2;
        c.weightx = 0.0;
        c.insets = new Insets(3, 0, 0, 0);
        JBLabel hint = new JBLabel("首次启动或新建标签页时打开的页面");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(TEXT_SECONDARY);
        body.add(hint, c);

        // 复选框
        c.gridy = 3;
        c.insets = new Insets(10, 0, 0, 0);
        openHomeCheckBox = new JBCheckBox("新标签页时前往主页");
        openHomeCheckBox.setFont(openHomeCheckBox.getFont().deriveFont(12f));
        openHomeCheckBox.setOpaque(false);
        body.add(openHomeCheckBox, c);

        return wrapCard("⚙", "常规", body);
    }

    // ────────────── 历史记录卡片 ──────────────
    private JPanel createHistoryCard() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        JBLabel daysLabel = new JBLabel("保留天数");
        daysLabel.setFont(daysLabel.getFont().deriveFont(12f));
        body.add(daysLabel, c);

        c.gridx = 1;
        c.insets = new Insets(0, 24, 0, 0);
        JBLabel countLabel = new JBLabel("最大条数");
        countLabel.setFont(countLabel.getFont().deriveFont(12f));
        body.add(countLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        maxHistoryDaysField = new JBTextField();
        maxHistoryDaysField.setBackground(FIELD_BG);
        maxHistoryDaysField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        maxHistoryDaysField.setPreferredSize(new Dimension(100, maxHistoryDaysField.getPreferredSize().height));
        body.add(maxHistoryDaysField, c);

        c.gridx = 1;
        c.insets = new Insets(4, 24, 0, 0);
        c.weightx = 0.3;
        maxHistoryCountField = new JBTextField();
        maxHistoryCountField.setBackground(FIELD_BG);
        maxHistoryCountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        maxHistoryCountField.setPreferredSize(new Dimension(100, maxHistoryCountField.getPreferredSize().height));
        body.add(maxHistoryCountField, c);

        // 提示行
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(6, 0, 0, 0);
        c.weightx = 1.0;
        JBLabel hint = new JBLabel("设为 0 表示不限制");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(TEXT_SECONDARY);
        body.add(hint, c);

        return wrapCard("🕐", "历史记录", body);
    }

    // ────────────── 界面卡片 ──────────────
    private JPanel createDisplayCard() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        // 开发者工具
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 0.0;
        c.gridwidth = 1;
        JBLabel dtLabel = new JBLabel("开发者工具");
        dtLabel.setFont(dtLabel.getFont().deriveFont(12f));
        body.add(dtLabel, c);

        c.gridx = 1;
        c.insets = new Insets(0, 16, 0, 0);
        c.weightx = 1.0;
        devToolsModeCombo = new JComboBox<>(new String[]{"当前页面下方", "独立窗口"});
        devToolsModeCombo.setBackground(FIELD_BG);
        body.add(devToolsModeCombo, c);

        // 显示位置
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(12, 0, 0, 0);
        c.weightx = 0.0;
        JBLabel posLabel = new JBLabel("显示位置");
        posLabel.setFont(posLabel.getFont().deriveFont(12f));
        body.add(posLabel, c);

        c.gridx = 1;
        c.insets = new Insets(12, 16, 0, 0);
        c.weightx = 1.0;
        displayPositionCombo = new JComboBox<>(new String[]{"工具栏", "编辑区"});
        displayPositionCombo.setBackground(FIELD_BG);
        body.add(displayPositionCombo, c);

        // 提示
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(6, 0, 0, 0);
        c.weightx = 1.0;
        JBLabel hint = new JBLabel("更改后需要重启 IDE 才能生效");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(TEXT_SECONDARY);
        body.add(hint, c);

        return wrapCard("🎨", "界面", body);
    }

    // ────────────── 页脚 ──────────────
    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JBLabel devLabel = new JBLabel("Developed by 陈彭伟", SwingConstants.RIGHT);
        devLabel.setFont(devLabel.getFont().deriveFont(11f));
        devLabel.setForeground(TEXT_SECONDARY);
        footer.add(devLabel, BorderLayout.EAST);

        return footer;
    }

    // ════════════════════════════════════════════
    // 以下是 Configurable 生命周期方法
    // ════════════════════════════════════════════

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
        if (devToolsModeCombo != null) {
            String selected = (String) devToolsModeCombo.getSelectedItem();
            state.setDevToolsMode(devToolsModeToSetting(selected));
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
        if (devToolsModeCombo != null) {
            devToolsModeCombo.setSelectedItem(settingToDevToolsMode(state.getDevToolsMode()));
        }
    }

    // ────── 转换辅助方法 ──────

    private static String positionToSetting(String display) {
        return "编辑区".equals(display) ? "editor" : "toolbar";
    }

    private static String settingToPosition(String setting) {
        return "editor".equals(setting) ? "编辑区" : "工具栏";
    }

    private static String devToolsModeToSetting(String display) {
        return "独立窗口".equals(display) ? "window" : "split";
    }

    private static String settingToDevToolsMode(String setting) {
        return "window".equals(setting) ? "独立窗口" : "当前页面下方";
    }

    private static Integer parseNullableInt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
