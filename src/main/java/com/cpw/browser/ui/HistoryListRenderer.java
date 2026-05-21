// 历史列表渲染器，支持 String 分组标题、DayOfWeek 星期标题和 HistoryEntry 条目
package com.cpw.browser.ui;

import com.cpw.browser.history.HistoryEntry;
import com.cpw.browser.util.TranslationUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class HistoryListRenderer extends JPanel implements ListCellRenderer<Object> {

    // 历史条目标题标签
    private final JBLabel titleLabel;
    // 历史条目时间标签
    private final JBLabel timeLabel;
    // 内容容器面板
    private final JPanel contentPanel;
    // 删除按钮标签
    private final JLabel deleteLabel;

    // 构造历史列表项渲染器
    public HistoryListRenderer() {
        super(new BorderLayout());

        titleLabel = new JBLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(12f));

        timeLabel = new JBLabel();
        timeLabel.setFont(timeLabel.getFont().deriveFont(10f));
        timeLabel.setForeground(new JBColor(0x888888, 0x999999));

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(titleLabel);
        contentPanel.add(timeLabel);

        deleteLabel = new JLabel("×");
        deleteLabel.setFont(deleteLabel.getFont().deriveFont(Font.PLAIN, 13f));
        deleteLabel.setForeground(new JBColor(0xAAAAAA, 0x777777));
        deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteLabel.setBorder(new EmptyBorder(0, 4, 0, 6));

        add(contentPanel, BorderLayout.CENTER);
        add(deleteLabel, BorderLayout.EAST);
        setBorder(new EmptyBorder(2, 8, 2, 2));
    }

    // 渲染列表项（支持分组标题、星期标题和历史条目三种类型）
    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        setBackground(JBColor.WHITE);

        // 日期分组标题行
        if (value instanceof String) {
            String header = (String) value;
            titleLabel.setText(header);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
            titleLabel.setForeground(new JBColor(0x666666, 0xAAAAAA));
            timeLabel.setVisible(false);
            contentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new JBColor(0xE0E0E0, 0x555555)),
                    new EmptyBorder(4, 0, 2, 0)
            ));
            deleteLabel.setVisible(false);
        } else if (value instanceof DayOfWeek) { // 星期分组标题行
            DayOfWeek dow = (DayOfWeek) value;
            switch (dow) {
                case MONDAY: titleLabel.setText(TranslationUtil.getText("history.monday")); break;
                case TUESDAY: titleLabel.setText(TranslationUtil.getText("history.tuesday")); break;
                case WEDNESDAY: titleLabel.setText(TranslationUtil.getText("history.wednesday")); break;
                case THURSDAY: titleLabel.setText(TranslationUtil.getText("history.thursday")); break;
                case FRIDAY: titleLabel.setText(TranslationUtil.getText("history.friday")); break;
                case SATURDAY: titleLabel.setText(TranslationUtil.getText("history.saturday")); break;
                case SUNDAY: titleLabel.setText(TranslationUtil.getText("history.sunday")); break;
            }
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
            titleLabel.setForeground(new JBColor(0x666666, 0xAAAAAA));
            timeLabel.setVisible(false);
            contentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new JBColor(0xE0E0E0, 0x555555)),
                    new EmptyBorder(4, 0, 2, 0)
            ));
            deleteLabel.setVisible(false);
        } else if (value instanceof HistoryEntry) { // 具体历史条目
            HistoryEntry entry = (HistoryEntry) value;
            titleLabel.setText(entry.getTitle());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
            titleLabel.setForeground(new JBColor(0x000000, 0xDDDDDD));
            timeLabel.setText(formatTimestamp(entry.getTimestamp()));
            timeLabel.setVisible(true);
            contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
            deleteLabel.setVisible(true);
            deleteLabel.setToolTipText(TranslationUtil.getText("history.delete"));
        }
        return this;
    }

    // 格式化历史记录时间戳为可读文本
    private static String formatTimestamp(long millis) {
        ZoneId zone = ZoneId.systemDefault();
        java.time.ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(zone);
        LocalDate today = LocalDate.now(zone);
        LocalDate entryDate = zdt.toLocalDate();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 当天记录显示 HH:mm
        if (entryDate.equals(today)) {
            return zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (!entryDate.isBefore(monday)) { // 本周记录显示 "周X HH:mm"
            String wd;
            switch (zdt.getDayOfWeek()) {
                case MONDAY: wd = TranslationUtil.getText("history.monday"); break;
                case TUESDAY: wd = TranslationUtil.getText("history.tuesday"); break;
                case WEDNESDAY: wd = TranslationUtil.getText("history.wednesday"); break;
                case THURSDAY: wd = TranslationUtil.getText("history.thursday"); break;
                case FRIDAY: wd = TranslationUtil.getText("history.friday"); break;
                case SATURDAY: wd = TranslationUtil.getText("history.saturday"); break;
                case SUNDAY: wd = TranslationUtil.getText("history.sunday"); break;
                default: wd = ""; break;
            }
            return wd + " " + zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else { // 更早记录显示 "MM-dd HH:mm"
            return zdt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }
    }
}
