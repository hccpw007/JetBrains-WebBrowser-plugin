package com.cpw.browser.toolwindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.JComponent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// 单个浏览器标签页，封装 JBCefBrowser，管理导航历史、缩放、DevTools
public class BrowserTabPanel {

    public final JBCefBrowser browser;
    public final JComponent component;

    private final String initialUrl;
    private final ArrayDeque<String> navigationHistory = new ArrayDeque<>();

    private int currentHistoryIndex = -1;
    private String currentUrl;
    private String pageTitle = "新标签页";
    private boolean isLoading = false;
    private double zoomLevel = 1.0;

    private Consumer<String> onUrlChanged = null;
    private Consumer<String> onTitleChanged = null;
    private Consumer<Boolean> onLoadingStateChanged = null;
    private Consumer<String> onPopupUrl = null;

    // 嵌入式 DevTools（直接加载 CDP 返回的 devtoolsFrontendUrl）
    private JBCefBrowser embeddedDevTools = null;

    public BrowserTabPanel() {
        this("about:blank");
    }

    public BrowserTabPanel(String initialUrl) {
        this.initialUrl = initialUrl;
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
                if (!targetUrl.isBlank() && !"about:blank".equals(targetUrl)) {
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
                if (frame.isMain()) {
                    currentUrl = frame.getURL();
                    if (onUrlChanged != null) {
                        onUrlChanged.accept(frame.getURL());
                    }
                }
            }

            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                BrowserTabPanel.this.isLoading = isLoading;
                if (onLoadingStateChanged != null) {
                    onLoadingStateChanged.accept(isLoading);
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                if (frame.isMain()) {
                    currentUrl = failedUrl;
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
                if (frame.isMain()) {
                    currentUrl = url;
                    if (onUrlChanged != null) {
                        onUrlChanged.accept(url);
                    }
                }
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                pageTitle = title;
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
        return embeddedDevTools != null;
    }

    // 导航到指定 URL
    public void navigate(String url) {
        pushHistory(url);
        browser.loadURL(url);
    }

    // 后退
    public void goBack() {
        if (canGoBack()) {
            currentHistoryIndex--;
            browser.getCefBrowser().goBack();
        }
    }

    // 前进
    public void goForward() {
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
        if (!pageTitle.isBlank() && !"about:blank".equals(pageTitle)) {
            rawTitle = pageTitle;
        } else if (!currentUrl.isBlank() && !"about:blank".equals(currentUrl)) {
            rawTitle = currentUrl.replaceFirst("^https://", "").replaceFirst("^http://", "");
            if (rawTitle.endsWith("/")) {
                rawTitle = rawTitle.substring(0, rawTitle.length() - 1);
            }
        } else {
            rawTitle = "新标签页";
        }
        return rawTitle.length() > 20 ? rawTitle.substring(0, 20) + "..." : rawTitle;
    }

    // 通过 CDP 远程调试端口打开嵌入式 DevTools
    public void openEmbeddedDevTools(Consumer<JBCefBrowser> callback) {
        // 如果已经打开，直接返回
        if (embeddedDevTools != null) {
            callback.accept(embeddedDevTools);
            return;
        }

        // 在后台线程查找 DevTools 端口
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Integer port = findDevToolsPort();
            if (port != null && port > 0) {
                connectDevTools(port, callback);
            } else {
                System.err.println("[WebBrowser] DevTools port not found");
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
            }
        });
    }

    // 查找 JCEF 远程调试端口
    private Integer findDevToolsPort() {
        // 尝试通过 JBCefApp API 获取端口
        try {
            JBCefApp app = JBCefApp.getInstance();
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            app.getRemoteDebuggingPort(port -> {
                result.set(port);
                latch.countDown();
            });
            if (latch.await(3, TimeUnit.SECONDS)) {
                Integer port = result.get();
                if (port != null && port > 0) {
                    System.err.println("[WebBrowser] DevTools port via JBCefApp: " + port);
                    return port;
                }
            }
        } catch (Throwable t) {
            System.err.println("[WebBrowser] JBCefApp.getRemoteDebuggingPort failed: " + t.getMessage());
        }

        // 尝试通过 DevToolsActivePort 文件查找端口
        try {
            List<Path> candidates = new ArrayList<>();
            Path systemDir = PathManager.getSystemDir();
            if (systemDir != null) {
                candidates.add(systemDir.resolve("jcef_cache"));
            }
            try {
                String userHome = System.getProperty("user.home");
                if (userHome != null) {
                    candidates.add(Path.of(userHome, "Library", "Caches", "JetBrains"));
                }
            } catch (Exception e) {
                // 忽略，继续尝试其他候选路径
            }

            for (Path root : candidates) {
                if (!Files.isDirectory(root)) continue;
                java.util.Optional<Path> optionalPath = Files.walk(root, 6)
                        .filter(p -> "DevToolsActivePort".equals(p.getFileName().toString()))
                        .findFirst();
                if (optionalPath.isPresent()) {
                    Path foundPath = optionalPath.get();
                    List<String> lines = Files.readAllLines(foundPath);
                    if (!lines.isEmpty()) {
                        try {
                            Integer port = Integer.parseInt(lines.get(0).trim());
                            if (port > 0) {
                                System.err.println("[WebBrowser] DevTools port via file: " + port + " (" + foundPath + ")");
                                return port;
                            }
                        } catch (NumberFormatException e) {
                            // 忽略无效端口
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[WebBrowser] DevToolsActivePort file search failed: " + t.getMessage());
        }

        return null;
    }

    // 连接 CDP 并加载嵌入式 DevTools
    private void connectDevTools(int port, Consumer<JBCefBrowser> callback) {
        try {
            URL url = new URI("http://127.0.0.1:" + port + "/json").toURL();
            String json;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                json = reader.lines().collect(Collectors.joining("\n"));
            }
            String truncatedJson = json.length() > 500 ? json.substring(0, 500) : json;
            System.err.println("[WebBrowser] CDP /json (port=" + port + "): " + truncatedJson);

            JsonArray pages = JsonParser.parseString(json).getAsJsonArray();

            // 按当前 URL 匹配 page 目标
            JsonObject matchedPage = null;
            for (JsonElement page : pages) {
                JsonObject obj = page.getAsJsonObject();
                JsonElement typeElem = obj.get("type");
                if (typeElem != null && "page".equals(typeElem.getAsString())) {
                    String pageUrl = obj.get("url") != null ? obj.get("url").getAsString() : "";
                    if (pageUrl.equals(currentUrl) ||
                            (!"about:blank".equals(currentUrl) && pageUrl.contains(currentUrl)) ||
                            ("about:blank".equals(currentUrl) && pages.size() == 1)) {
                        matchedPage = obj;
                        break;
                    }
                }
            }

            // 如果没匹配到，则使用第一个可用的 page 目标
            if (matchedPage == null) {
                System.err.println("[WebBrowser] No page matched '" + currentUrl + "', using first available");
                for (JsonElement page : pages) {
                    JsonObject obj = page.getAsJsonObject();
                    JsonElement typeElem = obj.get("type");
                    if (typeElem != null && "page".equals(typeElem.getAsString())) {
                        matchedPage = obj;
                        break;
                    }
                }
            }

            if (matchedPage != null) {
                JsonElement devtoolsUrlElem = matchedPage.get("devtoolsFrontendUrl");
                String devtoolsUrl = devtoolsUrlElem != null ? devtoolsUrlElem.getAsString() : null;
                if (devtoolsUrl != null && !devtoolsUrl.isBlank()) {
                    System.err.println("[WebBrowser] Loading DevTools frontend: " + devtoolsUrl);
                    final String finalDevtoolsUrl = devtoolsUrl;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            JBCefBrowser devBrowser = new JBCefBrowser(finalDevtoolsUrl);
                            embeddedDevTools = devBrowser;
                            callback.accept(devBrowser);
                        } catch (Exception e) {
                            System.err.println("[WebBrowser] Failed to create DevTools browser: " + e.getMessage());
                            callback.accept(null);
                        }
                    });
                } else {
                    System.err.println("[WebBrowser] No devtoolsFrontendUrl in /json response: " + matchedPage);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
                }
            } else {
                System.err.println("[WebBrowser] No page found in /json list");
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
            }
        } catch (Exception e) {
            System.err.println("[WebBrowser] DevTools connection failed: " + e.getMessage());
            ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
        }
    }

    // 关闭嵌入式 DevTools
    public void closeEmbeddedDevTools() {
        if (embeddedDevTools != null) {
            JBCefBrowser devTools = embeddedDevTools;
            Runnable disposeRunnable = () -> {
                devTools.dispose();
                embeddedDevTools = null;
            };
            if (ApplicationManager.getApplication().isDispatchThread()) {
                disposeRunnable.run();
            } else {
                ApplicationManager.getApplication().invokeLater(disposeRunnable);
            }
        }
    }

    // 获取嵌入式 DevTools 的 UI 组件
    public JComponent getEmbeddedDevToolsComponent() {
        return embeddedDevTools != null ? embeddedDevTools.getComponent() : null;
    }

    // 释放资源
    public void dispose() {
        closeEmbeddedDevTools();
        Runnable disposeRunnable = () -> {
            browser.dispose();
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            disposeRunnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(disposeRunnable);
        }
    }

    // 将 URL 加入导航历史，并清理当前位置之后的记录
    private void pushHistory(String url) {
        while (navigationHistory.size() > currentHistoryIndex + 1) {
            navigationHistory.removeLast();
        }
        navigationHistory.addLast(url);
        currentHistoryIndex = navigationHistory.size() - 1;
    }
}
