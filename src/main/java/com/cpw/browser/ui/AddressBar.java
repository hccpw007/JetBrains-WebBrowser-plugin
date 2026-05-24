package com.cpw.browser.ui;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.history.BrowsingHistoryState;
import com.cpw.browser.history.HistoryEntry;
import com.cpw.browser.util.TranslationUtil;
import com.cpw.browser.util.UrlUtils;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// 地址栏组件，包含 URL 输入框和书签星标
public class AddressBar extends JPanel {

    // URL 输入框，用于显示和编辑当前页面地址
    public final JBTextField urlField;
    // 书签星标图标，指示当前页面是否已加入书签
    private final JBLabel starLabel;
    // 导航回调，用户输入 URL 按下回车后触发
    private final Consumer<String> onNavigate;
    // 判断指定 URL 是否已加入书签的函数
    private final Predicate<String> isUrlBookmarked;
    // 建议弹窗，根据历史记录和书签提供 URL 自动提示
    private JBPopup suggestionsPopup;
    // 建议列表，用于键盘上下键切换选中项
    private JBList<String> suggestionsList;
    // 回车后禁止弹窗重新弹出，避免 setUrl 触发的 DocumentListener 再次显示弹窗
    private boolean suppressSuggestions;
    // 导航前地址栏中的旧 URL，仅过滤掉这一个回调，避免闪烁
    private String preNavigationUrl;
    // 最后一次由 setUrl 设置的稳定页面 URL，用于精确过滤旧页面地址回调
    private String lastStableUrl;

    // 建议列表最多查询条数
    private static final int MAX_SUGGESTIONS = 100;
    // 触发建议的最小输入字符数
    private static final int MIN_SUGGESTION_CHARS = 3;
    // 切换书签状态的回调
    private final Consumer<String> onToggleBookmark;

    public AddressBar(
            Consumer<String> onNavigate,
            Predicate<String> isUrlBookmarked,
            Consumer<String> onToggleBookmark
    ) {
        super(new BorderLayout());
        this.onNavigate = onNavigate;
        this.isUrlBookmarked = isUrlBookmarked;
        this.onToggleBookmark = onToggleBookmark;

        // 初始化 URL 输入框
        urlField = new JBTextField();
        urlField.getEmptyText().setText("about:blank");

        // 初始化书签星标
        starLabel = new JBLabel(WebBrowserIcons.STAR);
        starLabel.setOpaque(false);
        starLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        starLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 点击星标触发书签切换
                String url = urlField.getText().trim();
                // URL 不为空且不是空白页时才切换书签
                if (!url.isEmpty() && !"about:blank".equals(url)) {
                    onToggleBookmark.accept(url);
                }
            }
        });

        // 自定义圆角边框：聚焦时不用蓝色
        Border normalBorder = BorderFactory.createCompoundBorder(
                new RoundedBorder(new JBColor(0xF0F0F0, 0x4A4A4A), 15),
                BorderFactory.createEmptyBorder(0, 4, 0, 27)
        );
        Border focusBorder = BorderFactory.createCompoundBorder(
                new RoundedBorder(new JBColor(0xD0D0D0, 0x888888), 15),
                BorderFactory.createEmptyBorder(0, 4, 0, 27)
        );
        urlField.setBorder(normalBorder);
        urlField.setFont(urlField.getFont().deriveFont(13f));

        // 聚焦/失焦时切换边框
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                urlField.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                urlField.setBorder(normalBorder);
            }
        });

        // 按下回车时导航，上下键切换建议选择
        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 非回车键清除抑制标记，恢复弹窗功能
                if (e.getKeyCode() != KeyEvent.VK_ENTER &&
                    e.getKeyCode() != KeyEvent.VK_DOWN &&
                    e.getKeyCode() != KeyEvent.VK_UP) {
                    suppressSuggestions = false;
                    preNavigationUrl = null;
                }
                // 回车处理：先保存弹窗选中项，关闭弹窗，再导航
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String selected = null;
                    // 弹窗可见且有用户选中项时记下选中 URL
                    if (suggestionsPopup != null && suggestionsPopup.isVisible() && suggestionsList != null && suggestionsList.getSelectedIndex() >= 0) {
                        selected = suggestionsList.getSelectedValue();
                    }
                    if (selected != null) {
                        // 对选中的 URL 进行规范化并启动导航
                        startNavigation(UrlUtils.normalize(selected));
                    } else {
                        // 对原始输入进行规范化（如 "github.com" → "https://github.com"）
                        String rawText = urlField.getText().trim();
                        String normalizedUrl = UrlUtils.normalize(rawText);
                        // 地址栏内容不一致时先更新
                        if (!normalizedUrl.equals(rawText)) {
                            urlField.setText(normalizedUrl);
                        }
                        startNavigation(normalizedUrl);
                    }
                    e.consume();
                    return;
                }
                // 弹窗可见时的上下键选择
                if ((e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) &&
                    suggestionsPopup != null && suggestionsPopup.isVisible() && suggestionsList != null) {
                    int idx = suggestionsList.getSelectedIndex();
                    if (e.getKeyCode() == KeyEvent.VK_DOWN && idx < suggestionsList.getModel().getSize() - 1) {
                        suggestionsList.setSelectedIndex(idx + 1);
                        suggestionsList.ensureIndexIsVisible(idx + 1);
                    } else if (e.getKeyCode() == KeyEvent.VK_UP && idx > 0) {
                        suggestionsList.setSelectedIndex(idx - 1);
                        suggestionsList.ensureIndexIsVisible(idx - 1);
                    }
                    e.consume();
                }
            }
        });

        // 监听输入变化，显示历史记录和书签建议
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSuggestions();
            }
        });

        // 使用 JLayeredPane 将星标叠加在文本字段右侧内部
        JLayeredPane fieldLayer = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                urlField.setBounds(0, 0, w, h);
                int starSize = 14;
                starLabel.setBounds(w - starSize - 9, (h - starSize) / 2, starSize, starSize);
                setLayer(starLabel, JLayeredPane.PALETTE_LAYER);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension ps = urlField.getPreferredSize();
                return new Dimension(ps.width, ps.height - 4);
            }
        };

        fieldLayer.add(urlField, JLayeredPane.DEFAULT_LAYER);
        fieldLayer.add(starLabel, JLayeredPane.PALETTE_LAYER);

        add(fieldLayer, BorderLayout.CENTER);
    }

    public void setUrl(String url) {
        // 仅过滤导航前的那一次旧 URL 回调，重定向等任何不同 URL 都正常更新
        if (preNavigationUrl != null && url.equals(preNavigationUrl)) {
            preNavigationUrl = null;
            return;
        }
        preNavigationUrl = null;
        // 记录当前稳定页面 URL，用于后续导航时精确过滤旧页面地址
        lastStableUrl = url;
        // 设置地址栏文本，临时禁止弹窗（避免程序化 setUrl 触发提示框）
        suppressSuggestions = true;
        String displayText = "about:blank".equals(url) ? "" : url;
        if (!displayText.equals(urlField.getText())) {
            urlField.setText(displayText);
        }
        updateStarIcon(url);
    }

    public void updateStarIcon(String url) {
        // 根据书签状态更新星标图标和提示
        // URL 不为空且不是空白页时检查书签状态
        if (!url.isEmpty() && !"about:blank".equals(url)) {
            boolean bookmarked = isUrlBookmarked.test(url);
            starLabel.setIcon(bookmarked ? WebBrowserIcons.STAR_FILLED : WebBrowserIcons.STAR);
            starLabel.setToolTipText(bookmarked ? TranslationUtil.getText("address.bookmark.remove") : TranslationUtil.getText("address.bookmark.add"));
        } else { // URL 为空或空白页时显示默认星标
            starLabel.setIcon(WebBrowserIcons.STAR);
            starLabel.setToolTipText(TranslationUtil.getText("address.bookmark.add"));
        }
    }

    // 获取地址栏文本（去前后空格）
    public String getUrl() {
        return urlField.getText().trim();
    }

    // 聚焦地址栏并全选文本
    public void requestFocusOnField() {
        // 聚焦地址栏并全选文本
        urlField.requestFocus();
        urlField.selectAll();
    }

    // 启动导航的公共逻辑：过滤旧 URL 回调、抑制弹窗、关闭弹窗、更新地址栏、导航
    private void startNavigation(String url) {
        // 保存导航前的页面 URL，精确过滤旧页面回调
        preNavigationUrl = lastStableUrl;
        // 抑制弹窗再次弹出（setUrl 会触发 DocumentListener）
        suppressSuggestions = true;
        // 关闭弹窗
        hideSuggestionsPopup();
        // 更新地址栏
        urlField.setText(url);
        // 导航
        onNavigate.accept(url);
    }

    // 触发地址栏导航
    private void navigate() {
        String text = getUrl();
        // 地址栏内容不为空时触发导航回调
        if (!text.isEmpty()) {
            onNavigate.accept(text);
        }
    }

    // 更新建议弹窗：根据当前输入查询并显示匹配的历史记录和书签
    private void updateSuggestions() {
        // 回车后暂时禁止弹窗，避免 setUrl 触发的文本变化重新弹出
        if (suppressSuggestions) return;
        String text = urlField.getText().trim();
        // 输入少于指定字符数或为空白页时关闭弹窗
        if (text.length() < MIN_SUGGESTION_CHARS || "about:blank".equals(text)) {
            hideSuggestionsPopup();
            return;
        }
        // 查询匹配的 URL 建议
        List<Suggestion> suggestions = querySuggestions(text);
        if (suggestions.isEmpty()) {
            hideSuggestionsPopup();
            return;
        }
        showSuggestionsPopup(suggestions);
    }

    // 建议条目信息：URL 和对应的页面标题
    private static class Suggestion {
        final String url;
        final String title;

        Suggestion(String url, String title) {
            this.url = url;
            this.title = title;
        }
    }

    // 从历史记录和书签中查询匹配输入文本的 URL
    private List<Suggestion> querySuggestions(String prefix) {
        String lower = prefix.toLowerCase();
        // 保存结果和已出现的 URL（用于去重）
        List<Suggestion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 从浏览历史中匹配
        for (HistoryEntry entry : BrowsingHistoryState.getInstance().getEntries()) {
            if (result.size() >= MAX_SUGGESTIONS) break;
            String url = entry.getUrl();
            if (url.isEmpty() || "about:blank".equals(url)) continue;
            String title = entry.getTitle();
            // 匹配 URL 或标题（不区分大小写）
            if (url.toLowerCase().contains(lower) || (title != null && title.toLowerCase().contains(lower))) {
                if (seen.add(url)) {
                    result.add(new Suggestion(url, title != null ? title : ""));
                }
            }
        }
        // 从书签中补充匹配（历史中没有的 URL）
        for (Bookmark bookmark : BookmarkPersistentState.getInstance().getBookmarks()) {
            if (result.size() >= MAX_SUGGESTIONS) break;
            String url = bookmark.getUrl();
            if (url.isEmpty() || "about:blank".equals(url)) continue;
            String title = bookmark.getTitle();
            if (url.toLowerCase().contains(lower) || (title != null && title.toLowerCase().contains(lower))) {
                if (seen.add(url)) {
                    result.add(new Suggestion(url, title != null ? title : ""));
                }
            }
        }
        return result;
    }

    // 在地址栏下方显示建议弹窗（左对齐）
    private void showSuggestionsPopup(List<Suggestion> suggestions) {
        hideSuggestionsPopup();

        // 获取当前输入框内容作为固定建议
        String inputText = urlField.getText().trim();
        // 构建建议列表，第一行固定为当前输入的内容
        DefaultListModel<String> model = new DefaultListModel<>();
        // 第一行始终是当前输入内容
        model.addElement(inputText);
        for (Suggestion s : suggestions) {
            // 不重复添加与输入相同的内容
            if (!s.url.equals(inputText)) {
                model.addElement(s.url);
            }
        }
        // 为渲染器构建包含输入内容的完整建议列表
        List<Suggestion> allSuggestions = new ArrayList<>();
        allSuggestions.add(new Suggestion(inputText, inputText));
        for (Suggestion s : suggestions) {
            if (!s.url.equals(inputText)) {
                allSuggestions.add(s);
            }
        }
        suggestionsList = new JBList<>(model);
        // 默认显示 8 条，多余的可滑动查看
        suggestionsList.setVisibleRowCount(8);
        // 先设置渲染器，再计算高度，确保多行渲染器生效
        suggestionsList.setCellRenderer(new SuggestionRenderer(allSuggestions));
        // 鼠标点击建议项时导航
        suggestionsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String selected = suggestionsList.getModel().getElementAt(index);
                    startNavigation(UrlUtils.normalize(selected));
                }
            }
        });

        // 列表放入滚动面板，超出默认高度时垂直滑动查看
        JBScrollPane scrollPane = new JBScrollPane(suggestionsList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // 计算 8 行的固定高度（使用渲染器获取单行高度）
        int rowHeight = suggestionsList.getCellRenderer().getListCellRendererComponent(
                suggestionsList, model.getElementAt(0), 0, false, false).getPreferredSize().height;
        int visibleRows = Math.min(model.getSize(), 8);
        int popupHeight = visibleRows * rowHeight + 5;
        // 弹窗宽度与地址栏保持一致，高度固定为 8 行
        scrollPane.setPreferredSize(new Dimension(urlField.getWidth(), popupHeight));

        suggestionsPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, suggestionsList)
                .setRequestFocus(false)
                .setResizable(true)
                .createPopup();

        suggestionsPopup.showUnderneathOf(urlField);
        // 默认选中第一行（当前输入内容）
        suggestionsList.setSelectedIndex(0);

        // 将弹窗左移使其左边与 urlField 对齐
        try {
            java.awt.Window popupWindow = SwingUtilities.getWindowAncestor(suggestionsPopup.getContent());
            Point urlLoc = urlField.getLocationOnScreen();
            if (popupWindow != null && urlLoc != null) {
                popupWindow.setLocation(urlLoc.x, popupWindow.getY());
            }
        } catch (Exception ignored) {
        }
    }

    // 关闭建议弹窗
    private void hideSuggestionsPopup() {
        if (suggestionsPopup != null) {
            suggestionsPopup.cancel();
            suggestionsPopup = null;
            suggestionsList = null;
        }
    }

    // 根据可用宽度动态截断文本，只在实际超出时添加 ...
    private static String truncateToWidth(String text, JList<?> list, Font font, int padding) {
        int availableWidth = list.getWidth() - padding;
        // 列表尚未布局时用字符数兜底
        if (availableWidth <= 0) {
            return text.length() > 60 ? text.substring(0, 60) + "..." : text;
        }
        FontMetrics fm = list.getFontMetrics(font);
        if (fm.stringWidth(text) <= availableWidth) {
            return text; // 未超出，不截断
        }
        // 逐字缩减直到适配可用宽度
        for (int i = text.length(); i > 0; i--) {
            String truncated = text.substring(0, i) + "...";
            if (fm.stringWidth(truncated) <= availableWidth) {
                return truncated;
            }
        }
        return "...";
    }

    // 建议列表的单元格渲染器：标题（加粗）+ 网址（灰色），左右留间距
    private static class SuggestionRenderer extends DefaultListCellRenderer {
        private final List<Suggestion> suggestions;

        SuggestionRenderer(List<Suggestion> suggestions) {
            this.suggestions = suggestions;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String url = (String) value;
            // 从建议列表中查找对应的标题
            Suggestion match = null;
            for (Suggestion s : suggestions) {
                if (s.url.equals(url)) {
                    match = s;
                    break;
                }
            }

            // 使用两行布局的面板
            JPanel panel = new JPanel(new BorderLayout(0, 2));
            panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            // 第一行：页面标题（超出可用宽度时自动截断）
            String displayTitle = (match != null && !match.title.isEmpty()) ? match.title : url;
            Font titleFont = new JLabel().getFont().deriveFont(Font.BOLD, 13f);
            JLabel titleLabel = new JLabel(truncateToWidth(displayTitle, list, titleFont, 20));
            titleLabel.setFont(titleFont);

            // 第二行：完整 URL（超出可用宽度时自动截断）
            Font urlFont = new JLabel().getFont().deriveFont(11f);
            JLabel urlLabel = new JLabel(truncateToWidth(url, list, urlFont, 20));
            urlLabel.setFont(urlFont);
            urlLabel.setForeground(com.intellij.ui.JBColor.GRAY);

            panel.add(titleLabel, BorderLayout.NORTH);
            panel.add(urlLabel, BorderLayout.SOUTH);

            // 选中状态的高亮色
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
            } else {
                panel.setBackground(list.getBackground());
            }

            return panel;
        }
    }

    // 圆角边框
    private static class RoundedBorder extends AbstractBorder {
        private final JBColor color;
        private final int radius;

        RoundedBorder(JBColor color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 1;
            insets.left = 1;
            insets.bottom = 1;
            insets.right = 1;
            return insets;
        }
    }
}
