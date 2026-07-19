// 横向书签栏组件，类似 Chrome 书签栏，在地址栏下方横向展示书签按钮
package com.cpw.browser.ui;

import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// 横向书签栏，横向排列书签按钮，溢出书签通过 ">>" 按钮以下拉菜单展示
public class BookmarkBar extends JBPanel<BookmarkBar> {

    // 书签按钮最大宽度（像素）
    private static final int MAX_BUTTON_WIDTH = 160;
    // 书签按钮左右内边距
    private static final int BUTTON_PADDING = 12;
    // 溢出按钮预留宽度
    private static final int OVERFLOW_BUTTON_WIDTH = 36;
    // 书签按钮字体大小
    private static final float BUTTON_FONT_SIZE = 11f;

    // 可见书签按钮容器
    private final JPanel visiblePanel;
    // 溢出按钮
    private final JButton overflowButton;
    // 当前所有书签
    private List<Bookmark> bookmarks = new ArrayList<>();
    // 书签选中回调
    private final Consumer<Bookmark> onBookmarkSelected;
    // 书签变更回调（编辑/删除后通知外部刷新）
    private final Runnable onBookmarkChanged;
    // 溢出弹窗
    private JBPopup overflowPopup;

    // 构造横向书签栏
    // onBookmarkSelected 为点击书签导航回调
    // onBookmarkChanged 为编辑/删除后外部刷新回调
    public BookmarkBar(Consumer<Bookmark> onBookmarkSelected, Runnable onBookmarkChanged) {
        super(new BorderLayout());
        this.onBookmarkSelected = onBookmarkSelected;
        this.onBookmarkChanged = onBookmarkChanged;

        // 可见书签按钮容器，横向排列
        visiblePanel = new JPanel();
        visiblePanel.setLayout(new BoxLayout(visiblePanel, BoxLayout.X_AXIS));
        visiblePanel.setOpaque(false);

        // 溢出按钮，点击弹出溢出书签列表
        overflowButton = new JButton("»");
        overflowButton.setFont(overflowButton.getFont().deriveFont(BUTTON_FONT_SIZE));
        overflowButton.setBorderPainted(false);
        overflowButton.setContentAreaFilled(false);
        overflowButton.setFocusPainted(false);
        overflowButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overflowButton.setToolTipText(TranslationUtil.getText("bookmark.bar.overflow"));
        overflowButton.setVisible(false);
        overflowButton.setPreferredSize(new Dimension(OVERFLOW_BUTTON_WIDTH, 28));
        overflowButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        overflowButton.addActionListener(e -> showOverflowPopup());

        // 监听尺寸变化重新布局
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });

        add(visiblePanel, BorderLayout.CENTER);
        add(overflowButton, BorderLayout.EAST);

        refreshBookmarks();
    }

    // 刷新书签列表并重新布局
    public void refreshBookmarks() {
        bookmarks = BookmarkPersistentState.getInstance().getBookmarks();
        relayout();
    }

    // 语言切换后刷新文本
    public void refreshLabels() {
        overflowButton.setToolTipText(TranslationUtil.getText("bookmark.bar.overflow"));
        relayout();
    }

    // 计算给定可用宽度和书签按钮宽度列表下，能放下的书签数量
    // availableWidth 为可用宽度
    // buttonWidths 为各书签按钮宽度
    // 返回可放下的书签数量
    static int computeVisibleCount(int availableWidth, List<Integer> buttonWidths) {
        int used = 0;
        for (int i = 0; i < buttonWidths.size(); i++) {
            int w = buttonWidths.get(i);
            // 累计宽度超过可用宽度则返回当前索引
            if (used + w > availableWidth) {
                return i;
            }
            used += w;
        }
        return buttonWidths.size();
    }

    // 获取书签按钮字体的 FontMetrics
    private FontMetrics getButtonFontMetrics() {
        return getFontMetrics(getFont().deriveFont(BUTTON_FONT_SIZE));
    }

    // 重新布局书签按钮
    private void relayout() {
        visiblePanel.removeAll();
        // 计算每个书签按钮的偏好宽度
        List<Integer> widths = new ArrayList<>();
        for (Bookmark b : bookmarks) {
            widths.add(measureButtonWidth(b.getTitle()));
        }
        // 可用宽度（预留溢出按钮宽度，确保溢出时可显示）
        int available = Math.max(0, getWidth() - OVERFLOW_BUTTON_WIDTH);
        int visibleCount = computeVisibleCount(available, widths);

        // 添加可见书签按钮
        for (int i = 0; i < visibleCount; i++) {
            visiblePanel.add(createBookmarkButton(bookmarks.get(i)));
        }
        // 有溢出时显示溢出按钮
        boolean hasOverflow = visibleCount < bookmarks.size();
        overflowButton.setVisible(hasOverflow);

        visiblePanel.revalidate();
        visiblePanel.repaint();
    }

    // 测量书签标题对应的按钮宽度
    private int measureButtonWidth(String title) {
        FontMetrics fm = getButtonFontMetrics();
        int textWidth = fm.stringWidth(title);
        return Math.min(MAX_BUTTON_WIDTH, textWidth + BUTTON_PADDING * 2);
    }

    // 截断标题以适配最大宽度，超出加省略号
    private String truncateTitle(String title, FontMetrics fm) {
        // 文本宽度未超则原样返回
        if (fm.stringWidth(title) <= MAX_BUTTON_WIDTH - BUTTON_PADDING * 2) {
            return title;
        }
        // 逐字缩减直到适配
        for (int i = title.length() - 1; i > 0; i--) {
            String truncated = title.substring(0, i) + "…";
            // 适配则返回
            if (fm.stringWidth(truncated) <= MAX_BUTTON_WIDTH - BUTTON_PADDING * 2) {
                return truncated;
            }
        }
        return "…";
    }

    // 创建单个书签按钮
    // bookmark 为书签数据
    // 返回配置好的书签按钮
    private JButton createBookmarkButton(Bookmark bookmark) {
        // 按钮字体小一号
        FontMetrics fm = getButtonFontMetrics();
        // 截断后的显示文本
        String display = truncateTitle(bookmark.getTitle(), fm);
        JButton button = new JButton(display);
        button.setFont(getFont().deriveFont(BUTTON_FONT_SIZE));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 左对齐
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 宽度自适应文本，最大不超过 MAX_BUTTON_WIDTH
        int btnWidth = measureButtonWidth(bookmark.getTitle());
        button.setMinimumSize(new Dimension(btnWidth, 28));
        button.setPreferredSize(new Dimension(btnWidth, 28));
        button.setMaximumSize(new Dimension(MAX_BUTTON_WIDTH, 28));
        button.setToolTipText(bookmark.getTitle() + " - " + bookmark.getUrl());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键导航
                if (e.getButton() == MouseEvent.BUTTON1) {
                    onBookmarkSelected.accept(bookmark);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // 右键弹出菜单（Windows/Linux 在 press 触发）
                if (e.isPopupTrigger()) {
                    showBookmarkContextMenu(bookmark, e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 右键弹出菜单（Mac 在 release 触发）
                if (e.isPopupTrigger()) {
                    showBookmarkContextMenu(bookmark, e);
                }
            }
        });
        return button;
    }

    // 显示书签右键菜单
    private void showBookmarkContextMenu(Bookmark bookmark, MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        // 编辑菜单项
        JMenuItem editItem = new JMenuItem(TranslationUtil.getText("bookmark.edit.tooltip"));
        editItem.addActionListener(ev -> editBookmark(bookmark));
        popup.add(editItem);
        // 删除菜单项
        JMenuItem deleteItem = new JMenuItem(TranslationUtil.getText("bookmark.delete.tooltip"));
        deleteItem.addActionListener(ev -> deleteBookmark(bookmark));
        popup.add(deleteItem);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    // 编辑书签
    private void editBookmark(Bookmark bookmark) {
        // 弹出编辑对话框
        String[] result = BookmarkSidebar.showBookmarkEditDialog(bookmark.getTitle(), bookmark.getUrl());
        // 用户确认则更新书签
        if (result != null) {
            BookmarkPersistentState.getInstance().updateBookmark(bookmark.getUrl(), result[0], result[1]);
            refreshBookmarks();
            // 通知外部刷新
            if (onBookmarkChanged != null) {
                onBookmarkChanged.run();
            }
        }
    }

    // 删除书签
    private void deleteBookmark(Bookmark bookmark) {
        // 确认删除
        int confirm = Messages.showYesNoDialog(
                TranslationUtil.getText("bookmark.delete.confirm", bookmark.getTitle()),
                TranslationUtil.getText("bookmark.delete.title"),
                null
        );
        // 用户确认则删除
        if (confirm == Messages.YES) {
            BookmarkPersistentState.getInstance().removeBookmark(bookmark.getUrl());
            refreshBookmarks();
            // 通知外部刷新
            if (onBookmarkChanged != null) {
                onBookmarkChanged.run();
            }
        }
    }

    // 显示溢出书签弹窗
    private void showOverflowPopup() {
        // 可见按钮数量
        int visibleCount = visiblePanel.getComponentCount();
        // 收集溢出书签
        List<Bookmark> overflow = new ArrayList<>();
        for (int i = visibleCount; i < bookmarks.size(); i++) {
            overflow.add(bookmarks.get(i));
        }
        // 无溢出则返回
        if (overflow.isEmpty()) {
            return;
        }

        Bookmark[] arr = overflow.toArray(new Bookmark[0]);
        JBList<Bookmark> list = new JBList<>(arr);
        list.setCellRenderer(new BookmarkListCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键点击导航
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    onBookmarkSelected.accept(arr[idx]);
                    // 关闭弹窗
                    if (overflowPopup != null) {
                        overflowPopup.closeOk(null);
                    }
                }
            }
        });

        overflowPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(list, list)
                .setRequestFocus(true)
                .createPopup();
        overflowPopup.showUnderneathOf(overflowButton);
    }
}
