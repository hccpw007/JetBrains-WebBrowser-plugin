// 书签与历史记录侧边栏，包含分段切换器（Element Plus 风格）
package com.cpw.browser.ui;

import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.history.BrowsingHistoryState;
import com.cpw.browser.history.HistoryEntry;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class BookmarkSidebar extends JBPanel<BookmarkSidebar> {

    // 书签标签页索引
    private static final int TAB_BOOKMARKS = 0;
    // 历史标签页索引
    private static final int TAB_HISTORY = 1;

    // ---- 书签 ----
    // 书签列表数据模型
    private final CollectionListModel<Bookmark> bookmarkListModel;
    // 书签列表组件
    private final JBList<Bookmark> bookmarkList;
    // 书签列表滚动面板
    private final JBScrollPane bookmarkScroll;

    // ---- 历史 ----
    // 历史列表数据模型
    private final HistoryEntriesModel historyListModel;
    // 历史列表组件
    private final JBList<Object> historyList;
    // 历史列表滚动面板
    private final JBScrollPane historyScroll;

    // ---- 布局 ----
    // 卡片布局内容面板
    private final JPanel contentPanel;
    // 历史记录面板
    private final JPanel historyPanel;
    // 分段切换器宽度
    private final int segmentWidth = 140;
    // 分段切换器高度
    private final int segmentHeight = 24;

    // ---- 分段切换器 (Element Plus 风格) ----
    // 当前选中的标签页索引
    private int currentTab = TAB_BOOKMARKS;
    // 分段切换器面板
    private final JPanel segmentBar;
    // 书签标签文字
    private final JLabel bookmarkLabel;
    // 历史标签文字
    private final JLabel historyLabel;
    // 清空历史记录标签
    private final JBLabel clearLabel;
    // 选中态白色滑块
    private final JLabel activeBg;

    // 书签选中回调
    private final Consumer<Bookmark> onBookmarkSelected;
    // 历史条目选中回调
    private final Consumer<String> onHistoryEntrySelected;

    // 构造书签侧边栏，包含分段切换器、书签列表和历史列表
    public BookmarkSidebar(
            Consumer<Bookmark> onBookmarkSelected,
            Consumer<String> onHistoryEntrySelected
    ) {
        super(new BorderLayout());
        this.onBookmarkSelected = onBookmarkSelected;
        this.onHistoryEntrySelected = onHistoryEntrySelected;

        // ---- 书签 ----
        bookmarkListModel = new CollectionListModel<>(
                BookmarkPersistentState.getInstance().getBookmarks()
        );
        bookmarkList = new JBList<>(bookmarkListModel);
        bookmarkList.setCellRenderer(new BookmarkListCellRenderer());
        bookmarkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookmarkList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBookmarkClick(e);
            }
        });
        bookmarkScroll = new JBScrollPane(bookmarkList);

        // ---- 历史 ----
        historyListModel = new HistoryEntriesModel();
        historyList = new JBList<>(historyListModel);
        historyList.setCellRenderer(new HistoryListRenderer());
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleHistoryClick(e);
            }
        });
        historyScroll = new JBScrollPane(historyList);

        // ---- 面板 ----
        contentPanel = new JPanel(new CardLayout());
        historyPanel = new JPanel(new BorderLayout());

        // ---- 分段切换器 ----
        segmentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new JBColor(0xE8E8E8, 0x3C3C3C));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        segmentBar.setLayout(null);
        segmentBar.setPreferredSize(new Dimension(segmentWidth, segmentHeight));
        segmentBar.setMinimumSize(new Dimension(segmentWidth, segmentHeight));
        segmentBar.setMaximumSize(new Dimension(segmentWidth, segmentHeight));

        // 书签标签
        bookmarkLabel = new JLabel("书签", SwingConstants.CENTER);
        bookmarkLabel.setFont(bookmarkLabel.getFont().deriveFont(Font.BOLD, 11f));
        bookmarkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bookmarkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showBookmarks();
            }
        });

        // 历史标签
        historyLabel = new JLabel("历史", SwingConstants.CENTER);
        historyLabel.setFont(historyLabel.getFont().deriveFont(Font.PLAIN, 11f));
        historyLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showHistory();
            }
        });

        // 清空历史记录标签
        clearLabel = new JBLabel("清空记录");
        clearLabel.setFont(clearLabel.getFont().deriveFont(11f));
        clearLabel.setForeground(new JBColor(0x3366CC, 0x7799DD));
        clearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showClearPopup();
            }
        });

        // 选中态白色滑块
        activeBg = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new JBColor(0xFFFFFF, 0x585A5C));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        activeBg.setOpaque(false);
        activeBg.setBounds(0, 0, segmentWidth / 2, segmentHeight);

        // 组装分段切换器
        segmentBar.add(activeBg);
        segmentBar.add(bookmarkLabel);
        segmentBar.add(historyLabel);
        segmentBar.setComponentZOrder(activeBg, segmentBar.getComponentCount() - 1);
        segmentBar.setComponentZOrder(bookmarkLabel, 0);
        segmentBar.setComponentZOrder(historyLabel, 1);
        bookmarkLabel.setBounds(0, 0, segmentWidth / 2, segmentHeight);
        historyLabel.setBounds(segmentWidth / 2, 0, segmentWidth / 2, segmentHeight);

        // 标签栏容器
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new JBColor(0xC0C0C0, 0x4A4A4A)),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)
        ));
        tabBar.add(segmentBar);

        // 历史面板 — 清空链接
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        clearPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        clearPanel.add(clearLabel);
        historyPanel.add(clearPanel, BorderLayout.NORTH);
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        contentPanel.add(bookmarkScroll, "bookmarks");
        contentPanel.add(historyPanel, "history");

        add(tabBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    // 刷新书签列表
    public void refreshBookmarks() {
        bookmarkListModel.replaceAll(BookmarkPersistentState.getInstance().getBookmarks());
    }

    // 刷新历史列表
    public void refreshHistory() {
        historyListModel.refresh();
    }

    // 获取当前选中的书签 URL
    public String getSelectedBookmarkUrl() {
        Bookmark selected = bookmarkList.getSelectedValue();
        return selected != null ? selected.getUrl() : null;
    }

    // 切换到书签标签
    private void showBookmarks() {
        // 已是书签标签则跳过
        if (currentTab == TAB_BOOKMARKS) return;
        currentTab = TAB_BOOKMARKS;
        bookmarkLabel.setFont(bookmarkLabel.getFont().deriveFont(Font.BOLD));
        historyLabel.setFont(historyLabel.getFont().deriveFont(Font.PLAIN));
        // 移动选中背景到左侧
        Component lastComp = segmentBar.getComponent(segmentBar.getComponentCount() - 1);
        // 选中背景组件存在则更新其位置
        if (lastComp != null) {
            lastComp.setBounds(0, 0, segmentBar.getWidth() / 2, segmentBar.getHeight());
        }
        segmentBar.repaint();
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, "bookmarks");
    }

    // 切换到历史标签
    private void showHistory() {
        // 已是历史标签则跳过
        if (currentTab == TAB_HISTORY) return;
        currentTab = TAB_HISTORY;
        historyLabel.setFont(historyLabel.getFont().deriveFont(Font.BOLD));
        bookmarkLabel.setFont(bookmarkLabel.getFont().deriveFont(Font.PLAIN));
        // 移动选中背景到右侧
        Component lastComp = segmentBar.getComponent(segmentBar.getComponentCount() - 1);
        // 选中背景组件存在则更新其位置
        if (lastComp != null) {
            lastComp.setBounds(segmentBar.getWidth() / 2, 0, segmentBar.getWidth() / 2, segmentBar.getHeight());
        }
        segmentBar.repaint();
        refreshHistory();
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, "history");
    }

    // 处理书签列表点击事件（右侧 22px 删除，左侧 22px 编辑，其余选中）
    private void handleBookmarkClick(MouseEvent e) {
        int index = bookmarkList.locationToIndex(e.getPoint());
        // 点击位置无书签则返回
        if (index < 0) return;
        java.awt.Rectangle bounds = bookmarkList.getCellBounds(index, index);
        // 无法获取单元格边界则返回
        if (bounds == null) return;
        Bookmark bookmark = bookmarkListModel.getElementAt(index);
        int rightEdge = bounds.x + bounds.width;

        // 点击右侧 22px 区域触发删除操作
        if (e.getPoint().x >= rightEdge - 22) {
            // 点击右侧删除区域，确认后删除书签
            int result = Messages.showYesNoDialog(
                    "确定删除书签 \"" + bookmark.getTitle() + "\" 吗？",
                    "删除书签",
                    null
            );
            // 用户确认删除
            if (result == Messages.YES) {
                BookmarkPersistentState.getInstance().removeBookmark(bookmark.getUrl());
                refreshBookmarks();
            }
        } else if (e.getPoint().x >= rightEdge - 44) { // 点击编辑区域
            // 点击编辑区域，弹出编辑对话框
            String[] editResult = showBookmarkEditDialog(bookmark.getTitle(), bookmark.getUrl());
            // 用户确认编辑则更新书签
            if (editResult != null) {
                BookmarkPersistentState.getInstance().updateBookmark(
                        bookmark.getUrl(), editResult[0], editResult[1]
                );
                refreshBookmarks();
            }
        } else { // 点击主体区域，选中该书签
            onBookmarkSelected.accept(bookmark);
        }
    }

    // 处理历史列表点击事件（右侧 22px 删除，其余选中）
    private void handleHistoryClick(MouseEvent e) {
        int index = historyList.locationToIndex(e.getPoint());
        // 点击位置无条目则返回
        if (index < 0) return;
        Object item = historyListModel.getElementAt(index);
        // 非 HistoryEntry 类型则返回（分组标题不可点击）
        if (!(item instanceof HistoryEntry)) return;
        HistoryEntry entry = (HistoryEntry) item;
        java.awt.Rectangle bounds = historyList.getCellBounds(index, index);
        // 无法获取单元格边界则返回
        if (bounds == null) return;
        // 点击右侧 22px 区域触发删除操作
        if (e.getPoint().x >= bounds.x + bounds.width - 22) {
            int result = Messages.showYesNoDialog(
                    "确定删除该条历史记录吗？",
                    "删除历史记录",
                    null
            );
            // 用户确认删除
            if (result == Messages.YES) {
                BrowsingHistoryState.getInstance().removeEntry(entry.getUrl(), entry.getTimestamp());
                refreshHistory();
            }
        } else { // 点击主体区域，选中该历史条目
            onHistoryEntrySelected.accept(entry.getUrl());
        }
    }

    // 弹出清空历史记录菜单
    private void showClearPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setOpaque(true);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new JBColor(0xC0C0C0, 0x4A4A4A), 1, true),
                BorderFactory.createEmptyBorder(2, 0, 2, 0)
        ));
        popup.add(createClearItem("清空一小时内记录", 1L));
        popup.add(createClearItem("清空24小时内记录", 24L));
        popup.add(createClearItem("清空所有记录", null));
        // 在清空记录标签下方弹出
        popup.show(clearLabel, 0, clearLabel.getHeight() + 2);
    }

    // 创建清空历史记录的菜单项
    private JButton createClearItem(String text, Long hours) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(4, 12, 4, 12));
        button.addActionListener(e -> {
            // hours 为 null 则清空全部，否则清空指定小时数以内
            if (hours == null) {
                BrowsingHistoryState.getInstance().clearEntries();
            } else { // 清空指定小时数以内的记录
                BrowsingHistoryState.getInstance().clearEntries(hours);
            }
            refreshHistory();
        });
        return button;
    }


    // 弹出书签编辑/添加对话框，返回 [标题, 地址] 或 null
    public static String[] showBookmarkEditDialog(String title, String url) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("标题:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        JBTextField titleField = new JBTextField(title, 20);
        panel.add(titleField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        panel.add(new JLabel("地址:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        JBTextField urlField = new JBTextField(url, 20);
        panel.add(urlField, c);

        int result = javax.swing.JOptionPane.showOptionDialog(
                null, panel, "书签",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE,
                null, null, null
        );
        // 用户点击确认则返回编辑后的数据
        if (result == javax.swing.JOptionPane.OK_OPTION) {
            return new String[]{titleField.getText().trim(), urlField.getText().trim()};
        }
        return null;
    }
}
