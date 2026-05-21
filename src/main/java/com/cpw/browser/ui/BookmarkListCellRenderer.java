package com.cpw.browser.ui;

import com.cpw.browser.bookmark.Bookmark;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

// 书签列表项渲染器，显示标题、编辑和删除按钮
public class BookmarkListCellRenderer extends JPanel implements ListCellRenderer<Object> {

    // 书签标题标签
    private final JBLabel titleLabel;
    // 编辑按钮标签
    private final JLabel editLabel;
    // 删除按钮标签
    private final JLabel deleteLabel;

    public BookmarkListCellRenderer() {
        super(new BorderLayout());

        // 书签标题标签
        titleLabel = new JBLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(12f));

        // 编辑按钮
        editLabel = new JLabel("✎");
        editLabel.setFont(editLabel.getFont().deriveFont(Font.PLAIN, 13f));
        editLabel.setForeground(new JBColor(0xAAAAAA, 0x777777));
        editLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
        editLabel.setToolTipText("编辑");

        // 删除按钮
        deleteLabel = new JLabel("×");
        deleteLabel.setFont(deleteLabel.getFont().deriveFont(Font.PLAIN, 13f));
        deleteLabel.setForeground(new JBColor(0xAAAAAA, 0x777777));
        deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteLabel.setBorder(new EmptyBorder(0, 4, 0, 6));
        deleteLabel.setToolTipText("删除书签");

        // 右侧操作按钮面板
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(editLabel);
        eastPanel.add(deleteLabel);

        add(titleLabel, BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);
        setBorder(new EmptyBorder(6, 8, 6, 2));
    }

    // 渲染书签列表项的组件
    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        // 设置背景色
        setBackground(JBColor.WHITE);

        // 如果值是书签对象，显示其标题和 URL 提示
        if (value instanceof Bookmark) {
            Bookmark bookmark = (Bookmark) value;
            // 标题为空时兜底显示"未命名书签"（getter 已保证不返回 null）
            String title = bookmark.getTitle();
            titleLabel.setText(!title.isBlank() ? title : "未命名书签");
            // URL 为空时不显示工具提示
            String url = bookmark.getUrl();
            titleLabel.setToolTipText(!url.isBlank() ? url : null);
        }

        return this;
    }
}
