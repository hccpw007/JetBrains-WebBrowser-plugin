package com.cpw.browser.toolwindow;

import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;

// 单个浏览器标签页，封装 JBCefBrowser，管理导航历史、缩放、DevTools
public class BrowserTabPanel {

    // 导航历史最大记录数，防止内存无限增长
    private static final int MAX_HISTORY_SIZE = 100;

    // JBCefBrowser 实例，用于加载和显示网页
    public final JBCefBrowser browser;
    // 浏览器组件，用于嵌入到 Swing 布局中
    public final JComponent component;

    // 导航历史栈，按访问顺序存储 URL
    private final ArrayDeque<String> navigationHistory = new ArrayDeque<>();

    // 当前在导航历史中的位置索引
    private int currentHistoryIndex = -1;
    // 当前页面的 URL
    private String currentUrl;
    // 当前页面的标题
    private String pageTitle;
    // 页面是否正在加载中
    private boolean isLoading = false;
    // 当前缩放级别（1.0 为 100%）
    private double zoomLevel = 1.0;
    // 标记当前是否正在后退或前进导航，用于 onAddressChange 中跳过 pushHistory
    private boolean skipHistoryPush = false;

    // URL 变更时的回调
    private Consumer<String> onUrlChanged = null;
    // 标题变更时的回调
    private Consumer<String> onTitleChanged = null;
    // 加载状态变更时的回调
    private Consumer<Boolean> onLoadingStateChanged = null;
    // 弹出窗口 URL 的回调
    private Consumer<String> onPopupUrl = null;

    // 嵌入式 DevTools 管理器，负责 DevTools 的端口发现、CDP 连接和生命周期
    private final EmbeddedDevToolsManager devToolsManager = new EmbeddedDevToolsManager(this);

    public BrowserTabPanel() {
        this("about:blank");
    }

    public BrowserTabPanel(String initialUrl) {
        this.currentUrl = initialUrl;
        this.pageTitle = TranslationUtil.getText("tab.new.tab");

        // 创建浏览器并设置背景色
        this.browser = new JBCefBrowser(initialUrl);
        this.browser.setPageBackgroundColor("white");
        // 确保新窗口页面从 100% 缩放开始
        this.browser.setZoomLevel(1.0);
        this.component = browser.getComponent();

        // 如果初始 URL 不是 about:blank，则记录历史
        if (!"about:blank".equals(initialUrl)) {
            pushHistory(initialUrl);
        }

        // 拦截弹出窗口（新标签页/新窗口）
        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                try {
                    // 如果目标 URL 非空且不是空白页，则通过回调通知外部
                    if (!targetUrl.isBlank() && !"about:blank".equals(targetUrl)) {
                        // 弹窗回调需要在 EDT 中执行，因为可能创建新的标签页
                        if (onPopupUrl != null) {
                            String url = targetUrl;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                try {
                                    onPopupUrl.accept(url);
                                } catch (Throwable t) {
                                    System.err.println("[WebBrowser] onPopupUrl callback error: " + t.getMessage());
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onBeforePopup error: " + t.getMessage());
                }
                return true;
            }
        }, browser.getCefBrowser());

        // 监听页面加载完成、加载状态变化、加载错误
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                try {
                    // 仅处理主框架的加载完成事件
                    if (frame.isMain()) {
                        // 重新应用缩放级别（JCEF 在页面加载后可能重置 CSS zoom）
                        double currentZoom = BrowserTabPanel.this.zoomLevel;
                        if (Math.abs(currentZoom - 1.0) > 0.001) {
                            BrowserTabPanel.this.applyZoomJs();
                        }
                        currentUrl = frame.getURL();
                        // 如果 URL 变更回调已注册，则在 EDT 中调用
                        if (onUrlChanged != null) {
                            String url = frame.getURL();
                            ApplicationManager.getApplication().invokeLater(() -> {
                                try {
                                    onUrlChanged.accept(url);
                                } catch (Throwable t) {
                                    System.err.println("[WebBrowser] onUrlChanged callback error: " + t.getMessage());
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onLoadEnd error: " + t.getMessage());
                }
            }

            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                try {
                    BrowserTabPanel.this.isLoading = isLoading;
                    // 如果加载状态变更回调已注册，则在 EDT 中调用
                    if (onLoadingStateChanged != null) {
                        boolean loading = isLoading;
                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                onLoadingStateChanged.accept(loading);
                            } catch (Throwable t) {
                                System.err.println("[WebBrowser] onLoadingStateChanged callback error: " + t.getMessage());
                            }
                        });
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onLoadingStateChange error: " + t.getMessage());
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                try {
                    // 仅处理主框架的加载错误
                    if (frame.isMain()) {
                        currentUrl = failedUrl;
                        // 如果 URL 变更回调已注册，则在 EDT 中调用
                        if (onUrlChanged != null) {
                            String url = failedUrl;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                try {
                                    onUrlChanged.accept(url);
                                } catch (Throwable t) {
                                    System.err.println("[WebBrowser] onUrlChanged callback error: " + t.getMessage());
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onLoadError error: " + t.getMessage());
                }
            }
        }, browser.getCefBrowser());

        // 监听地址变化和标题变化
        browser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                try {
                    // 仅处理主框架的地址变更
                    if (frame.isMain()) {
                        currentUrl = url;
                        // 页面内部导航（SPA pushState/link 点击等）：记录到历史栈
                        if (!skipHistoryPush) {
                            pushHistory(url);
                        }
                        skipHistoryPush = false;
                        // 如果 URL 变更回调已注册，则在 EDT 中调用
                        if (onUrlChanged != null) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                try {
                                    onUrlChanged.accept(url);
                                } catch (Throwable t) {
                                    System.err.println("[WebBrowser] onUrlChanged callback error: " + t.getMessage());
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onAddressChange error: " + t.getMessage());
                }
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                try {
                    pageTitle = title;
                    // 如果标题变更回调已注册，则在 EDT 中调用
                    if (onTitleChanged != null) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                onTitleChanged.accept(title);
                            } catch (Throwable t) {
                                System.err.println("[WebBrowser] onTitleChanged callback error: " + t.getMessage());
                            }
                        });
                    }
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] onTitleChange error: " + t.getMessage());
                }
            }
        }, browser.getCefBrowser());
    }

    // 获取当前历史索引
    public int getCurrentHistoryIndex() {
        return currentHistoryIndex;
    }

    // 获取当前 URL
    public String getCurrentUrl() {
        return currentUrl;
    }

    // 获取页面标题
    public String getPageTitle() {
        return pageTitle;
    }

    // 是否正在加载
    public boolean isLoading() {
        return isLoading;
    }

    // 获取缩放级别
    public double getZoomLevel() {
        return zoomLevel;
    }

    // 获取 URL 变更回调
    public Consumer<String> getOnUrlChanged() {
        return onUrlChanged;
    }

    // 获取标题变更回调
    public Consumer<String> getOnTitleChanged() {
        return onTitleChanged;
    }

    // 获取加载状态变更回调
    public Consumer<Boolean> getOnLoadingStateChanged() {
        return onLoadingStateChanged;
    }

    // 设置 URL 变更回调
    public void setOnUrlChanged(Consumer<String> onUrlChanged) {
        this.onUrlChanged = onUrlChanged;
    }

    // 设置标题变更回调
    public void setOnTitleChanged(Consumer<String> onTitleChanged) {
        this.onTitleChanged = onTitleChanged;
    }

    // 设置加载状态变更回调
    public void setOnLoadingStateChanged(Consumer<Boolean> onLoadingStateChanged) {
        this.onLoadingStateChanged = onLoadingStateChanged;
    }

    // 设置弹出窗口 URL 回调
    public void setOnPopupUrl(Consumer<String> onPopupUrl) {
        this.onPopupUrl = onPopupUrl;
    }

    // 嵌入式 DevTools 是否已打开
    public boolean isEmbeddedDevToolsOpen() {
        return devToolsManager.isOpen();
    }

    // 导航到指定 URL
    public void navigate(String url) {
        // 忽略 null 或空 URL，防止 ArrayDeque.addLast 抛 NPE
        if (url == null || url.isBlank()) return;
        skipHistoryPush = false;
        pushHistory(url);
        browser.loadURL(url);
    }

    // 后退
    public void goBack() {
        // 检查是否可以后退
        if (canGoBack()) {
            skipHistoryPush = true;
            currentHistoryIndex--;
            browser.getCefBrowser().goBack();
        }
    }

    // 前进
    public void goForward() {
        // 检查是否可以前进
        if (canGoForward()) {
            skipHistoryPush = true;
            currentHistoryIndex++;
            browser.getCefBrowser().goForward();
        }
    }

    // 刷新
    public void refresh() {
        browser.getCefBrowser().reload();
    }

    // 是否可以后退
    public boolean canGoBack() {
        return currentHistoryIndex > 0;
    }

    // 是否可以前进
    public boolean canGoForward() {
        return currentHistoryIndex < navigationHistory.size() - 1;
    }

    // 放大
    public void zoomIn() {
        zoomLevel += 0.05;
        applyZoomJs();
    }

    // 缩小
    public void zoomOut() {
        zoomLevel -= 0.05;
        applyZoomJs();
    }

    // 重置缩放
    public void zoomReset() {
        zoomLevel = 1.0;
        applyZoomJs();
    }

    // 通过 CSS zoom 属性设置缩放（避免 JCEF HostZoomMap 跨标签页共享）
    private void applyZoomJs() {
        // 使用 document.body.style.zoom 实现独立缩放
        String js = "document.body.style.zoom = '" + zoomLevel + "';";
        browser.getCefBrowser().executeJavaScript(js, "", 0);
    }

    // 打开独立 DevTools 弹出窗口
    public void openDevTools() {
        browser.openDevtools();
    }

    // 自动刷新定时器（null 表示未启用）
    private Timer autoRefreshTimer;
    // 自动刷新间隔（秒）
    private int autoRefreshInterval = 30;

    // 获取自动刷新状态
    public boolean isAutoRefreshEnabled() {
        return autoRefreshTimer != null && autoRefreshTimer.isRunning();
    }

    // 获取自动刷新间隔（秒）
    public int getAutoRefreshInterval() {
        return autoRefreshInterval;
    }

    // 设置自动刷新间隔（秒），最小为 1 秒
    public void setAutoRefreshInterval(int seconds) {
        this.autoRefreshInterval = Math.max(1, seconds);
        // 如果定时器正在运行，重新启动以应用新间隔
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
            autoRefreshTimer = new Timer(autoRefreshInterval * 1000, e -> refresh());
            autoRefreshTimer.start();
        }
    }

    // 切换自动刷新
    public void setAutoRefresh(boolean enabled) {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
        if (enabled) {
            autoRefreshTimer = new Timer(autoRefreshInterval * 1000, e -> refresh());
            autoRefreshTimer.start();
        }
    }

    // 清除浏览器缓存和 Cookie
    public void clearCache() {
        // 清除 localStorage 和 sessionStorage
        browser.getCefBrowser().executeJavaScript(
                "localStorage.clear();sessionStorage.clear();" +
                "caches.keys().then(function(ks){ks.forEach(function(k){caches.delete(k)})})",
                "", 0);
        // 清除 Cookie
        browser.getCefBrowser().executeJavaScript(
                "document.cookie.split(';').forEach(function(c){" +
                "document.cookie=c.trim().split('=')[0]+'=;expires=Thu,01 Jan 1970 00:00:00 UTC;path=/'})",
                "", 0);
    }

    // 获取标签页显示标题（截断超过 20 字符的长标题）
    public String getTabTitle() {
        String rawTitle;
        // 如果页面标题非空且不是空白页，则使用页面标题
        if (!pageTitle.isBlank() && !"about:blank".equals(pageTitle)) {
            rawTitle = pageTitle;
        } else if (!currentUrl.isBlank() && !"about:blank".equals(currentUrl)) { // 否则使用 URL（去掉协议前缀）
            rawTitle = currentUrl.replaceFirst("^https://", "").replaceFirst("^http://", "");
            // 如果 URL 末尾有斜杠则去掉
            if (rawTitle.endsWith("/")) {
                rawTitle = rawTitle.substring(0, rawTitle.length() - 1);
            }
        } else { // 默认显示新标签页
            rawTitle = TranslationUtil.getText("tab.new.tab");
        }
        return rawTitle.length() > 20 ? rawTitle.substring(0, 20) + "..." : rawTitle;
    }

    // 通过 CDP 远程调试端口打开嵌入式 DevTools
    public void openEmbeddedDevTools(Consumer<JBCefBrowser> callback) {
        devToolsManager.open(callback);
    }

    // 关闭嵌入式 DevTools
    public void closeEmbeddedDevTools() {
        devToolsManager.close();
    }

    // 获取嵌入式 DevTools 的 UI 组件
    public JComponent getEmbeddedDevToolsComponent() {
        return devToolsManager.getComponent();
    }

    // 释放资源
    public void dispose() {
        devToolsManager.close();
        try {
            browser.dispose();
        } catch (Throwable t) {
            System.err.println("[WebBrowser] browser.dispose error: " + t.getMessage());
        }
    }

    // 将 URL 加入导航历史，并清理当前位置之后的记录
    private void pushHistory(String url) {
        // 防止相同 URL 连续入栈（重复刷新等场景）
        if (!navigationHistory.isEmpty() && Objects.equals(navigationHistory.getLast(), url)) {
            return;
        }
        // 移除当前位置之后的所有历史记录（当从历史中间导航到新页面时）
        while (navigationHistory.size() > currentHistoryIndex + 1) {
            navigationHistory.removeLast();
        }
        navigationHistory.addLast(url);
        // 限制导航历史大小，超过上限时丢弃最旧的记录
        if (navigationHistory.size() > MAX_HISTORY_SIZE) {
            navigationHistory.removeFirst();
            // 移除最旧记录后，当前索引需要相应调整
            if (currentHistoryIndex > 0) {
                currentHistoryIndex--;
            }
        }
        currentHistoryIndex = navigationHistory.size() - 1;
    }
}
