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

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BookmarkSidebar extends JBPanel<BookmarkSidebar> {

    // 书签标签页索引
    private static final int TAB_BOOKMARKS = 0;
    // 历史标签页索引
    private static final int TAB_HISTORY = 1;

    // ---- 书签 ----
    private final CollectionListModel<Bookmark> bookmarkListModel;
    private final JBList<Bookmark> bookmarkList;
    private final JBScrollPane bookmarkScroll;

    // ---- 历史 ----
    private final HistoryEntriesModel historyListModel;
    private final JBList<Object> historyList;
    private final JBScrollPane historyScroll;

    // ---- 布局 ----
    private final JPanel contentPanel;
    private final JPanel historyPanel;
    private final int segmentWidth = 140;
    private final int segmentHeight = 24;

    // ---- 分段切换器 (Element Plus 风格) ----
    private int currentTab = TAB_BOOKMARKS;
    private final JPanel segmentBar;
    private final JLabel bookmarkLabel;
    private final JLabel historyLabel;
    private final JBLabel clearLabel;
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

    // ---- 历史列表模型 ----
    private class HistoryEntriesModel extends AbstractListModel<Object> {

        // 历史分组：今天的条目标题
        private static final String HEADER_TODAY = "今天";
        // 历史分组：以前的条目标题
        private static final String HEADER_OLDER = "以前";

        private List<Object> items = buildItems();

        // 刷新历史列表数据
        public void refresh() {
            items = buildItems();
            fireContentsChanged(this, 0, Math.max(items.size(), 0));
        }

        // 获取列表项数量
        @Override
        public int getSize() {
            return items.size();
        }

        // 获取指定索引的列表项
        @Override
        public Object getElementAt(int index) {
            return items.get(index);
        }

        // 构建历史列表条目（按日期分组）
        private List<Object> buildItems() {
            List<HistoryEntry> entries = BrowsingHistoryState.getInstance().getEntries();
            // 无历史记录则返回空列表
            if (entries.isEmpty()) return List.of();

            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zone);
            long todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli();
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long mondayStart = monday.atStartOfDay(zone).toInstant().toEpochMilli();

            List<HistoryEntry> todayGroup = new ArrayList<>();
            Map<DayOfWeek, List<HistoryEntry>> weekGroups = new HashMap<>();
            List<HistoryEntry> earlierGroup = new ArrayList<>();

            // 按日期分组归类历史记录
            for (HistoryEntry e : entries) {
                // 当天记录
                if (e.getTimestamp() >= todayStart) {
                    todayGroup.add(e);
                } else if (e.getTimestamp() >= mondayStart) { // 本周记录
                    DayOfWeek dow = Instant.ofEpochMilli(e.getTimestamp()).atZone(zone).getDayOfWeek();
                    weekGroups.computeIfAbsent(dow, k -> new ArrayList<>()).add(e);
                } else { // 更早记录
                    earlierGroup.add(e);
                }
            }

            List<Object> result = new ArrayList<>();
            // 当天分组非空则添加标题和条目
            if (!todayGroup.isEmpty()) {
                result.add(HEADER_TODAY);
                result.addAll(todayGroup);
            }
            // 按星期顺序添加本周分组
            for (DayOfWeek dow : Arrays.asList(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )) {
                List<HistoryEntry> group = weekGroups.get(dow);
                // 该星期有记录则添加标题和条目
                if (group != null && !group.isEmpty()) {
                    result.add(dow);
                    result.addAll(group);
                }
            }
            // 更早的记录非空则添加标题和条目
            if (!earlierGroup.isEmpty()) {
                result.add(HEADER_OLDER);
                result.addAll(earlierGroup);
            }
            return result;
        }
    }

    // ---- 历史列表渲染器，支持 String 分组标题、DayOfWeek 星期标题和 HistoryEntry 条目 ----
    private class HistoryListRenderer extends JPanel implements ListCellRenderer<Object> {

        private final JBLabel titleLabel;
        private final JBLabel timeLabel;
        private final JPanel contentPanel;
        private final JLabel deleteLabel;

        // 构造历史列表项渲染器
        HistoryListRenderer() {
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
                // 日期分组标题行
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
                    case MONDAY: titleLabel.setText("周一"); break;
                    case TUESDAY: titleLabel.setText("周二"); break;
                    case WEDNESDAY: titleLabel.setText("周三"); break;
                    case THURSDAY: titleLabel.setText("周四"); break;
                    case FRIDAY: titleLabel.setText("周五"); break;
                    case SATURDAY: titleLabel.setText("周六"); break;
                    case SUNDAY: titleLabel.setText("周日"); break;
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
                deleteLabel.setToolTipText("删除");
            }
            return this;
        }
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
                case MONDAY: wd = "周一"; break;
                case TUESDAY: wd = "周二"; break;
                case WEDNESDAY: wd = "周三"; break;
                case THURSDAY: wd = "周四"; break;
                case FRIDAY: wd = "周五"; break;
                case SATURDAY: wd = "周六"; break;
                case SUNDAY: wd = "周日"; break;
                default: wd = ""; break;
            }
            return wd + " " + zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else { // 更早记录显示 "MM-dd HH:mm"
            return zdt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }
    }
}
