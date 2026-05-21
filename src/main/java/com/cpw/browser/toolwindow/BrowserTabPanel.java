package com.cpw.browser.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.JComponent;
import java.util.ArrayDeque;
import java.util.function.Consumer;

// 单个浏览器标签页，封装 JBCefBrowser，管理导航历史、缩放、DevTools
public class BrowserTabPanel {

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
    private String pageTitle = "新标签页";
    // 页面是否正在加载中
    private boolean isLoading = false;
    // 当前缩放级别（1.0 为 100%）
    private double zoomLevel = 1.0;

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

        // 创建浏览器并设置背景色
        this.browser = new JBCefBrowser(initialUrl);
        this.browser.setPageBackgroundColor("white");
        this.component = browser.getComponent();

        // 如果初始 URL 不是 about:blank，则记录历史
        if (!"about:blank".equals(initialUrl)) {
            pushHistory(initialUrl);
        }

        // 拦截弹出窗口（新标签页/新窗口）
        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                // 如果目标 URL 非空且不是空白页，则通过回调通知外部
                if (!targetUrl.isBlank() && !"about:blank".equals(targetUrl)) {
                    // 如果弹窗 URL 回调已注册，则调用
                    if (onPopupUrl != null) {
                        onPopupUrl.accept(targetUrl);
                    }
                }
                return true;
            }
        }, browser.getCefBrowser());

        // 监听页面加载完成、加载状态变化、加载错误
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                // 仅处理主框架的加载完成事件
                if (frame.isMain()) {
                    currentUrl = frame.getURL();
                    // 如果 URL 变更回调已注册，则调用
                    if (onUrlChanged != null) {
                        onUrlChanged.accept(frame.getURL());
                    }
                }
            }

            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                BrowserTabPanel.this.isLoading = isLoading;
                // 如果加载状态变更回调已注册，则调用
                if (onLoadingStateChanged != null) {
                    onLoadingStateChanged.accept(isLoading);
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                // 仅处理主框架的加载错误
                if (frame.isMain()) {
                    currentUrl = failedUrl;
                    // 如果 URL 变更回调已注册，则调用
                    if (onUrlChanged != null) {
                        onUrlChanged.accept(failedUrl);
                    }
                }
            }
        }, browser.getCefBrowser());

        // 监听地址变化和标题变化
        browser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                // 仅处理主框架的地址变更
                if (frame.isMain()) {
                    currentUrl = url;
                    // 如果 URL 变更回调已注册，则调用
                    if (onUrlChanged != null) {
                        onUrlChanged.accept(url);
                    }
                }
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                pageTitle = title;
                // 如果标题变更回调已注册，则调用
                if (onTitleChanged != null) {
                    onTitleChanged.accept(title);
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
        pushHistory(url);
        browser.loadURL(url);
    }

    // 后退
    public void goBack() {
        // 检查是否可以后退
        if (canGoBack()) {
            currentHistoryIndex--;
            browser.getCefBrowser().goBack();
        }
    }

    // 前进
    public void goForward() {
        // 检查是否可以前进
        if (canGoForward()) {
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
        browser.setZoomLevel(zoomLevel);
    }

    // 缩小
    public void zoomOut() {
        zoomLevel -= 0.05;
        browser.setZoomLevel(zoomLevel);
    }

    // 重置缩放
    public void zoomReset() {
        zoomLevel = 1.0;
        browser.setZoomLevel(1.0);
    }

    // 打开独立 DevTools 弹出窗口
    public void openDevTools() {
        browser.openDevtools();
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
        } else { // 默认显示"新标签页"
            rawTitle = "新标签页";
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
        Runnable disposeRunnable = () -> {
            browser.dispose();
        };
        // 如果在调度线程中，直接释放浏览器；否则在调度线程中执行
        if (ApplicationManager.getApplication().isDispatchThread()) {
            disposeRunnable.run();
        } else { // 不在调度线程中，通过 invokeLater 调度
            ApplicationManager.getApplication().invokeLater(disposeRunnable);
        }
    }

    // 将 URL 加入导航历史，并清理当前位置之后的记录
    private void pushHistory(String url) {
        // 移除当前位置之后的所有历史记录（当从历史中间导航到新页面时）
        while (navigationHistory.size() > currentHistoryIndex + 1) {
            navigationHistory.removeLast();
        }
        navigationHistory.addLast(url);
        currentHistoryIndex = navigationHistory.size() - 1;
    }
}
