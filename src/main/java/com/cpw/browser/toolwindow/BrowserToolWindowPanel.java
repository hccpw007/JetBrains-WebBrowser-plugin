// 浏览器工具窗口主面板，包含标签页管理器、地址栏、书签侧边栏、缩放提示等核心 UI
package com.cpw.browser.toolwindow;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.action.GoBackAction;
import com.cpw.browser.action.GoForwardAction;
import com.cpw.browser.action.GoHomeAction;
import com.cpw.browser.action.OpenDevToolsAction;
import com.cpw.browser.action.RefreshAction;
import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.history.BrowsingHistoryState;
import com.cpw.browser.settings.BrowserSettingsPage;
import com.cpw.browser.settings.BrowserSettingsState;
import com.cpw.browser.ui.AddressBar;
import com.cpw.browser.ui.BookmarkSidebar;
import com.cpw.browser.ui.ChromeTab;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BrowserToolWindowPanel {

    // 标题变更回调
    private Consumer<String> onTitleChanged;

    // 当前项目
    private final Project project;

    // 标签页管理器
    private final BrowserTabManager tabManager;

    // 地址栏
    private final AddressBar addressBar;

    // 书签侧边栏
    private BookmarkSidebar bookmarkSidebar;

    // 标签页栏面板
    private final JPanel tabStripPanel;

    // 浏览器内容面板
    private final JPanel browserContentPanel;

    // 缩放提示标签
    private final JBLabel zoomToast;

    // 浏览器层叠面板
    private final JLayeredPane browserLayer;

    // 缩放提示定时器
    private Timer zoomToastTimer;

    // 居中区域面板
    private final JPanel centerPanel;

    // 底部状态栏标签
    private final JBLabel statusLabel;

    // 主面板
    private final JBPanel<?> mainPanel;

    // 标签页与 ChromeTab 映射
    private final Map<BrowserTabPanel, ChromeTab> chromeTabs;

    // 新建标签页按钮
    private final JButton addTabButton;

    // 构造浏览器工具窗口主面板
    public BrowserToolWindowPanel(Project project) {
        this.project = project;

        // 初始化标签页管理器
        tabManager = new BrowserTabManager();

        // 初始化地址栏
        addressBar = new AddressBar(
                rawUrl -> onNavigateRequested(rawUrl),
                url -> BookmarkPersistentState.getInstance().contains(url),
                url -> toggleBookmark(url)
        );

        // 浏览器内容区域
        browserContentPanel = new JPanel(new BorderLayout());

        // 缩放提示 toast（半透明圆角背景）
        zoomToast = new JBLabel() {
            private final java.awt.Color toastBg = new JBColor(0x444444, 0xCCCCCC);

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
                g2.setComposite(AlphaComposite.SrcOver.derive(0.65f));
                g2.setColor(toastBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        zoomToast.setOpaque(false);
        zoomToast.setForeground(new JBColor(0xFFFFFF, 0x333333));
        zoomToast.setFont(zoomToast.getFont().deriveFont(Font.BOLD, 13f));
        zoomToast.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        zoomToast.setVisible(false);

        // 浏览器层叠面板（承载 browserContentPanel 和 zoomToast 叠加层）
        browserLayer = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                browserContentPanel.setBounds(0, 0, w, h);
                // 如果缩放提示可见，则调整其位置到居中
                if (zoomToast.isVisible()) {
                    Dimension pref = zoomToast.getPreferredSize();
                    zoomToast.setBounds(
                            Math.max(0, (w - pref.width) / 2),
                            50,
                            pref.width,
                            pref.height
                    );
                }
            }
        };
        browserLayer.setLayer(browserContentPanel, JLayeredPane.DEFAULT_LAYER);
        browserLayer.setLayer(zoomToast, JLayeredPane.PALETTE_LAYER);
        browserLayer.add(browserContentPanel, JLayeredPane.DEFAULT_LAYER);
        browserLayer.add(zoomToast, JLayeredPane.PALETTE_LAYER);

        zoomToastTimer = null;

        // 居中区域：书签(可隐藏) + 浏览器内容
        centerPanel = new JPanel(new BorderLayout());
        statusLabel = new JBLabel("就绪", SwingConstants.LEFT);
        mainPanel = new JBPanel<>(new BorderLayout());
        chromeTabs = new HashMap<>();

        // 新建标签页按钮
        addTabButton = new JButton("+");
        addTabButton.setBorderPainted(false);
        addTabButton.setContentAreaFilled(false);
        addTabButton.setFocusPainted(false);
        addTabButton.setFont(addTabButton.getFont().deriveFont(16f));
        addTabButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addTabButton.setToolTipText("新建标签页");
        addTabButton.setPreferredSize(new Dimension(28, ChromeTab.TAB_HEIGHT));
        addTabButton.setMaximumSize(new Dimension(28, ChromeTab.TAB_HEIGHT));
        addTabButton.setMinimumSize(new Dimension(28, ChromeTab.TAB_HEIGHT));
        addTabButton.addActionListener(e -> tabManager.createTab());

        // 标签页栏（自定义灰色背景绘制）
        tabStripPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ChromeTab.STRIP_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        tabStripPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabStripPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));

        // ---- 初始化书签侧边栏 ----
        bookmarkSidebar = new BookmarkSidebar(
                bookmark -> onBookmarkSelected(bookmark),
                url -> onHistoryEntryClicked(url)
        );
        bookmarkSidebar.setVisible(false);
        int sideW = 200;
        bookmarkSidebar.setPreferredSize(new Dimension(sideW, 0));
        bookmarkSidebar.setMinimumSize(new Dimension(sideW, 0));
        bookmarkSidebar.setMaximumSize(new Dimension(sideW, Integer.MAX_VALUE));

        // 居中区域：[书签侧边栏(可隐藏)] [浏览器内容层(含 toast 叠加)]
        centerPanel.add(bookmarkSidebar, BorderLayout.WEST);
        centerPanel.add(browserLayer, BorderLayout.CENTER);

        // 设置标签页回调
        tabManager.setOnTabAdded(tab -> addTabToStrip(tab));
        tabManager.setOnTabRemoved(tab -> {
            removeTabFromStrip(tab);
            chromeTabs.remove(tab);
        });
        tabManager.setOnActiveTabChanged(tab -> onActiveTabChanged(tab));

        // 后退/前进/刷新/主页 mini 工具栏
        DefaultActionGroup navGroup = new DefaultActionGroup();
        navGroup.add(new GoBackAction(tabManager));
        navGroup.add(new GoForwardAction(tabManager));
        navGroup.add(new RefreshAction(tabManager));
        navGroup.add(new GoHomeAction(tabManager));
        ActionToolbar navToolbar = ActionManager.getInstance().createActionToolbar("WebBrowser.NavBar", navGroup, true);
        navToolbar.setTargetComponent(navToolbar.getComponent());

        // url 右侧工具栏：放大、缩小、开发者工具、书签侧边栏切换
        DefaultActionGroup rightGroup = new DefaultActionGroup();
        rightGroup.add(new ZoomInAction());
        rightGroup.add(new ZoomOutAction());
        rightGroup.add(createOpenDevToolsAction());
        rightGroup.addSeparator();
        rightGroup.add(new ToggleBookmarkSidebarAction());
        rightGroup.add(new OpenInSystemBrowserAction());
        rightGroup.add(new SettingsAction());
        ActionToolbar rightToolbar = ActionManager.getInstance().createActionToolbar("WebBrowser.RightActions", rightGroup, true);
        rightToolbar.setTargetComponent(rightToolbar.getComponent());

        // 地址栏行：[导航按钮] [地址栏] [开发者工具/书签/新标签页]
        JBPanel<?> navAddressBar = new JBPanel<>(new BorderLayout());
        navAddressBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        navAddressBar.add(navToolbar.getComponent(), BorderLayout.WEST);
        navAddressBar.add(addressBar, BorderLayout.CENTER);
        navAddressBar.add(rightToolbar.getComponent(), BorderLayout.EAST);

        // 顶部区域：[标签页栏] [地址栏]
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(tabStripPanel);
        topSection.add(navAddressBar);

        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        tabStripPanel.add(addTabButton);
        tabManager.createTab();
    }

    // 获取标题变更回调
    public Consumer<String> getOnTitleChanged() {
        return onTitleChanged;
    }

    // 设置标题变更回调
    public void setOnTitleChanged(Consumer<String> onTitleChanged) {
        this.onTitleChanged = onTitleChanged;
    }

    // 获取主面板组件
    public JBPanel<?> getContent() {
        return mainPanel;
    }

    // 释放所有浏览器标签页资源
    public void dispose() {
        ApplicationManager.getApplication().invokeLater(tabManager::disposeAll);
    }

    // 聚焦地址栏输入框
    public void focusAddressBar() {
        addressBar.requestFocusOnField();
    }

    // 打开/关闭开发者工具
    public void openDevTools() {
        BrowserTabPanel tab = tabManager.getActiveTab();
        // 无活跃标签页则直接返回
        if (tab == null) return;
        // 如果嵌入式 DevTools 已打开，先关闭
        if (tab.isEmbeddedDevToolsOpen()) {
            tab.closeEmbeddedDevTools();
            updateBrowserContent(tab);
            return;
        }
        // 获取用户设置的首选打开方式
        String mode = BrowserSettingsState.getInstance().getDevToolsMode();
        // 设置为"独立窗口"时直接弹出独立 DevTools 窗口
        if ("window".equals(mode)) {
            tab.openDevTools();
        } else { // 默认"当前页面下方"：尝试嵌入面板
            tab.openEmbeddedDevTools(devTools -> {
                // DevTools 组件创建成功则更新界面
                if (devTools != null) {
                    updateBrowserContent(tab);
                } else { // CDP 嵌入式失败时回退到弹出窗口
                    statusLabel.setText("嵌入式 DevTools 不可用，已打开独立窗口");
                    tab.openDevTools();
                }
            });
        }
    }

    // 创建 OpenDevToolsAction 实例
    private AnAction createOpenDevToolsAction() {
        return new AnAction("开发者工具", "打开或关闭开发者工具", WebBrowserIcons.DEV_TOOLS) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                openDevTools();
            }
        };
    }

    // 放大 Action
    private class ZoomInAction extends AnAction implements DumbAware {
        ZoomInAction() {
            super("放大", "放大网页 5%", WebBrowserIcons.ZOOM_IN);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomIn();
            showZoomToastForActiveTab("放大至");
        }
    }

    // 缩小 Action
    private class ZoomOutAction extends AnAction implements DumbAware {
        ZoomOutAction() {
            super("缩小", "缩小网页 5%", WebBrowserIcons.ZOOM_OUT);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomOut();
            showZoomToastForActiveTab("缩小至");
        }
    }

    // 书签侧边栏切换 Action
    private class ToggleBookmarkSidebarAction extends AnAction implements DumbAware {
        ToggleBookmarkSidebarAction() {
            super("显示书签", "显示或隐藏书签侧边栏", WebBrowserIcons.SHOW_BOOKMARK);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            bookmarkSidebar.setVisible(!bookmarkSidebar.isVisible());
            centerPanel.revalidate();
            centerPanel.repaint();
            e.getPresentation().setDescription(
                    bookmarkSidebar.isVisible() ? "隐藏书签侧边栏" : "显示书签侧边栏"
            );
        }
    }

    // 系统浏览器打开 Action
    private class OpenInSystemBrowserAction extends AnAction implements DumbAware {
        OpenInSystemBrowserAction() {
            super("系统浏览器打开", "在系统浏览器中打开当前网页", WebBrowserIcons.GOOGLE);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            // 仅在 URL 有效且非空白页时打开系统浏览器
            if (url != null && !url.isBlank() && !"about:blank".equals(url)) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    statusLabel.setText("打开系统浏览器失败: " + ex.getMessage());
                }
            }
        }

        @Override
        public void update(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            e.getPresentation().setEnabled(url != null && !url.isBlank() && !"about:blank".equals(url));
        }
    }

    // 设置 Action
    private class SettingsAction extends AnAction implements DumbAware {
        SettingsAction() {
            super("设置", "打开 WebBrowser 设置", AllIcons.General.Settings);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, BrowserSettingsPage.class);
        }
    }

    // 将标签页添加到标签栏
    private void addTabToStrip(BrowserTabPanel tab) {
        ChromeTab chromeTab = new ChromeTab(
                tab,
                () -> {
                    int idx = tabManager.getTabs().indexOf(tab);
                    // 索引有效则切换到该标签页
                    if (idx >= 0) tabManager.switchToTab(idx);
                },
                () -> {
                    int idx = tabManager.getTabs().indexOf(tab);
                    // 索引有效则关闭该标签页
                    if (idx >= 0) tabManager.closeTab(idx);
                }
        );

        chromeTabs.put(tab, chromeTab);
        tabStripPanel.add(chromeTab, tabStripPanel.getComponentCount() - 1);
        tabStripPanel.revalidate();
        tabStripPanel.repaint();
        updateTabStripHighlight();

        // 监听标题变更（即便非活跃标签也更新标题）
        Consumer<String> origTitleCb = tab.getOnTitleChanged();
        tab.setOnTitleChanged(title -> {
            ChromeTab ct = chromeTabs.get(tab);
            // 找到对应的 ChromeTab 则更新标题显示
            if (ct != null) {
                ct.titleLabel.setText(tab.getTabTitle());
            }
            // 有原始回调则继续传递
            if (origTitleCb != null) {
                origTitleCb.accept(title);
            }
            // 当前标签页为活跃标签页且已设置标题回调时通知外部
            if (tab == tabManager.getActiveTab() && onTitleChanged != null) {
                onTitleChanged.accept(tab.getTabTitle());
            }
        });

        // 页面加载完成后记录历史（标题和 URL 均不可为空）
        Consumer<Boolean> origLoadCb = tab.getOnLoadingStateChanged();
        tab.setOnLoadingStateChanged(loading -> {
            // 页面加载完成后记录浏览历史
            if (!loading) {
                String url = tab.getCurrentUrl();
                String title = tab.getPageTitle();
                // URL 和标题均不为空且非空白页时才记录
                if (url != null && !url.isBlank() && !"about:blank".equals(url)
                        && title != null && !title.isBlank()) {
                    BrowsingHistoryState.getInstance().addEntry(url, title);
                }
            }
            // 有原始回调则继续传递
            if (origLoadCb != null) {
                origLoadCb.accept(loading);
            }
        });
    }

    // 从标签栏移除标签页
    private void removeTabFromStrip(BrowserTabPanel tab) {
        ChromeTab chromeTab = chromeTabs.get(tab);
        // 未找到对应 ChromeTab 则直接返回
        if (chromeTab == null) return;
        tabStripPanel.remove(chromeTab);
        tabStripPanel.revalidate();
        tabStripPanel.repaint();
        updateTabStripHighlight();
    }

    // 更新标签栏高亮状态
    private void updateTabStripHighlight() {
        BrowserTabPanel activeTab = tabManager.getActiveTab();

        // 遍历所有标签页更新其活跃状态
        for (Map.Entry<BrowserTabPanel, ChromeTab> entry : chromeTabs.entrySet()) {
            entry.getValue().setActive(entry.getKey() == activeTab);
        }
    }

    // 活跃标签页变更回调
    private void onActiveTabChanged(BrowserTabPanel tab) {
        // 存在活跃标签页则更新界面状态
        if (tab != null) {
            addressBar.setUrl(tab.getCurrentUrl());
            statusLabel.setText(tab.isLoading() ? "加载中..." : "就绪");
            updateBrowserContent(tab);
            updateTabStripHighlight();
            updateTabTitle(tab);
            // 通知外部标题变更
            if (onTitleChanged != null) {
                onTitleChanged.accept(tab.getTabTitle());
            }
        } else { // 无活跃标签页，重置界面
            addressBar.setUrl("");
            statusLabel.setText("就绪");
            updateBrowserContent(null);
            // 恢复默认标题
            if (onTitleChanged != null) {
                onTitleChanged.accept("Web Browser");
            }
        }
    }

    // 更新浏览器内容区域（含 DevTools 拆分面板）
    private void updateBrowserContent(BrowserTabPanel tab) {
        browserContentPanel.removeAll();
        // 有标签页则添加其组件
        if (tab != null) {
            // DevTools 已打开则使用拆分面板
            if (tab.isEmbeddedDevToolsOpen()) {
                Component devToolsComp = tab.getEmbeddedDevToolsComponent();
                JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tab.component, devToolsComp);
                splitPane.setResizeWeight(0.7);
                splitPane.setDividerSize(4);
                splitPane.setContinuousLayout(true);
                browserContentPanel.add(splitPane, BorderLayout.CENTER);
            } else { // DevTools 未打开，只添加浏览器组件
                browserContentPanel.add(tab.component, BorderLayout.CENTER);
            }
        }
        browserContentPanel.revalidate();
        browserContentPanel.repaint();
    }

    // 切换书签状态（当前 URL 已收藏则删除，未收藏则弹出添加对话框）
    private void toggleBookmark(String url) {
        BookmarkPersistentState bookmarkState = BookmarkPersistentState.getInstance();
        // URL 已收藏则删除书签
        if (bookmarkState.contains(url)) {
            bookmarkState.removeBookmark(url);
            bookmarkSidebar.refreshBookmarks();
            addressBar.updateStarIcon(url);
        } else { // URL 未收藏则弹出添加对话框
            BrowserTabPanel tab = tabManager.getActiveTab();
            String defaultTitle = tab != null && !tab.getPageTitle().isBlank()
                    ? tab.getPageTitle() : url;
            String[] result = BookmarkSidebar.showBookmarkEditDialog(defaultTitle, url);
            // 用户确认添加则保存书签
            if (result != null) {
                bookmarkState.addBookmark(new Bookmark(result[0], result[1]));
                bookmarkSidebar.refreshBookmarks();
                addressBar.updateStarIcon(result[1]);
            }
        }
    }

    // 处理地址栏导航请求
    private void onNavigateRequested(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        BrowserTabPanel tab = tabManager.getActiveTab();
        // 有活跃标签页则直接导航，否则新建标签页
        if (tab != null) {
            tab.navigate(url);
        } else { // 无活跃标签页，创建新标签页并导航
            tabManager.createTab(url);
        }
    }

    // 处理书签选中事件
    private void onBookmarkSelected(Bookmark bookmark) {
        BrowserTabPanel tab = tabManager.getActiveTab();
        // 有活跃标签页则直接导航，否则新建标签页
        if (tab != null) {
            tab.navigate(bookmark.getUrl());
        } else { // 无活跃标签页，创建新标签页
            tabManager.createTab(bookmark.getUrl());
        }
    }

    // 处理历史记录条目点击事件
    private void onHistoryEntryClicked(String url) {
        BrowserTabPanel tab = tabManager.getActiveTab();
        // 有活跃标签页则直接导航，否则新建标签页
        if (tab != null) {
            tab.navigate(url);
        } else { // 无活跃标签页，创建新标签页
            tabManager.createTab(url);
        }
    }

    // 更新标签标题显示
    private void updateTabTitle(BrowserTabPanel tab) {
        ChromeTab chromeTab = chromeTabs.get(tab);
        // 找到对应的 ChromeTab 则更新标题文本
        if (chromeTab != null) {
            chromeTab.titleLabel.setText(tab.getTabTitle());
        }
    }

    // 显示缩放提示信息（1 秒后自动消失）
    private void showZoomToast(String text) {
        zoomToast.setText(text);
        zoomToast.setVisible(true);
        // 有之前的定时器则先停止
        if (zoomToastTimer != null) {
            zoomToastTimer.stop();
        }
        browserLayer.revalidate();
        browserLayer.repaint();
        zoomToastTimer = new Timer(1000, e -> {
            zoomToast.setVisible(false);
            browserLayer.repaint();
        });
        zoomToastTimer.setRepeats(false);
        zoomToastTimer.start();
    }

    // 为当前活跃标签页显示缩放百分比提示
    private void showZoomToastForActiveTab(String action) {
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        int pct = (int) ((activeTab != null ? activeTab.getZoomLevel() : 1.0) * 100);
        showZoomToast(action + pct + "%");
    }

    // 规范化 URL 输入（无协议头自动补充 https://，含空格视为搜索）
    private String normalizeUrl(String input) {
        String trimmed = input.trim();
        // 空输入返回空白页
        if (trimmed.isEmpty()) return "about:blank";
        // 已有协议头则直接返回
        if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) return trimmed;
        // 包含点且不含空格视为域名，补充 https://
        if (trimmed.contains(".") && !trimmed.contains(" ")) return "https://" + trimmed;
        try {
            return "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            return "about:blank";
        }
    }
}
