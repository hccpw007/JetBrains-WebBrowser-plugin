// 嵌入式 DevTools 管理器，负责端口发现、CDP 连接、打开/关闭/状态管理
package com.cpw.browser.toolwindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import javax.swing.JComponent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EmbeddedDevToolsManager {

    // 嵌入式 DevTools 浏览器实例
    private JBCefBrowser embeddedDevTools = null;
    // 所属标签页，用于获取当前 URL 以匹配 CDP 页面目标
    private final BrowserTabPanel tab;

    public EmbeddedDevToolsManager(BrowserTabPanel tab) {
        this.tab = tab;
    }

    // 嵌入式 DevTools 是否已打开
    public boolean isOpen() {
        return embeddedDevTools != null;
    }

    // 获取嵌入式 DevTools 的 UI 组件
    public JComponent getComponent() {
        return embeddedDevTools != null ? embeddedDevTools.getComponent() : null;
    }

    // 打开嵌入式 DevTools：通过 CDP 远程调试端口连接并加载本地 DevTools 前端
    public void open(Consumer<JBCefBrowser> callback) {
        // 如果已经打开，直接返回
        if (embeddedDevTools != null) {
            callback.accept(embeddedDevTools);
            return;
        }

        // 在后台线程查找 DevTools 端口
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Integer port = CdpSupport.findDevToolsPort();
                // 如果找到有效端口，则连接 DevTools
                if (port != null && port > 0) {
                    connectDevTools(port, callback);
                } else { // 未找到端口，通知回调失败
                    System.err.println("[WebBrowser] DevTools port not found");
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            callback.accept(null);
                        } catch (Throwable t) {
                            System.err.println("[WebBrowser] DevTools port callback error: " + t.getMessage());
                        }
                    });
                }
            } catch (Throwable t) {
                System.err.println("[WebBrowser] DevTools open error: " + t.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        callback.accept(null);
                    } catch (Throwable t2) {
                        System.err.println("[WebBrowser] DevTools fallback callback error: " + t2.getMessage());
                    }
                });
            }
        });
    }

    // 关闭嵌入式 DevTools
    public void close() {
        // 只有嵌入式 DevTools 已打开时才需要关闭
        if (embeddedDevTools != null) {
            JBCefBrowser devTools = embeddedDevTools;
            embeddedDevTools = null;
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    devTools.dispose();
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] DevTools dispose error: " + t.getMessage());
                }
            });
        }
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

            // 按当前 URL 匹配 page 目标（未匹配时回退第一个可用的 page 目标）
            JsonObject matchedPage = CdpSupport.matchPageTarget(pages, tab.getCurrentUrl());
            // 无任何可用 page 目标时打印提示
            if (matchedPage == null) {
                System.err.println("[WebBrowser] No page found in /json list");
            }

            // 如果匹配到了目标页面
            if (matchedPage != null) {
                // 使用本地 DevTools 地址（避免访问 chrome-devtools-frontend.appspot.com）
                JsonElement idElem = matchedPage.get("id");
                String pageId = idElem != null ? idElem.getAsString() : "";
                String devtoolsUrl = "http://127.0.0.1:" + port + "/devtools/inspector.html?ws=127.0.0.1:" + port + "/devtools/page/" + pageId;
                // 如果 devtoolsUrl 有效，则加载嵌入式 DevTools
                if (devtoolsUrl != null && !devtoolsUrl.isBlank()) {
                    System.err.println("[WebBrowser] Loading DevTools frontend: " + devtoolsUrl);
                    final String finalDevtoolsUrl = devtoolsUrl;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            JBCefBrowser devBrowser = new JBCefBrowser(finalDevtoolsUrl);
                            // DevTools 加载完成后：关闭 screencast 模式，然后删除 screencast 工具栏按钮
                            devBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                                @Override
                                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                                    // 仅处理主框架
                                    if (frame.isMain()) {
                                        /*
                                         * 解决开发者工具打开默认会显示页面iframe的问题
                                         * 如果class="icon only-icon primary-toggle"的元素的有个属性 aria-pressed="true",
                                         * 则点击一下这个元素,
                                         * 然后200毫秒后删除class="toolbar-button" aria-label="Toggle screencast"的元素
                                         */
                                        browser.executeJavaScript(
                                            "setTimeout(function(){" +
                                            "var w=function r(n){try{n.querySelectorAll('.icon.only-icon.primary-toggle').forEach(function(e){" +
                                            "if(e.getAttribute('aria-pressed')==='true')e.click()})}catch(e){}" +
                                            "try{n.querySelectorAll('*').forEach(function(e){if(e.shadowRoot)r(e.shadowRoot)})}catch(e){}};" +
                                            "w(document);" +
                                            "setTimeout(function(){!function r(n){try{n.querySelectorAll('.toolbar-button[aria-label=\"Toggle screencast\"]').forEach(function(e){e.remove()})}catch(e){}" +
                                            "try{n.querySelectorAll('*').forEach(function(e){if(e.shadowRoot)r(e.shadowRoot)})}catch(e){}}(document)},200)" +
                                            "},1000)",
                                            "", 0);
                                    }
                                }
                            }, devBrowser.getCefBrowser());
                            embeddedDevTools = devBrowser;
                            callback.accept(devBrowser);
                        } catch (Throwable e) {
                            System.err.println("[WebBrowser] Failed to create DevTools browser: " + e.getMessage());
                            try {
                                callback.accept(null);
                            } catch (Throwable t2) {
                                System.err.println("[WebBrowser] DevTools fallback callback error: " + t2.getMessage());
                            }
                        }
                    });
                } else { // devtoolsUrl 为空，通知回调失败
                    System.err.println("[WebBrowser] No devtools id in /json response: " + matchedPage);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            callback.accept(null);
                        } catch (Throwable t) {
                            System.err.println("[WebBrowser] DevTools callback error: " + t.getMessage());
                        }
                    });
                }
            } else { // 未找到可用的 page 目标，通知回调失败
                System.err.println("[WebBrowser] No page found in /json list");
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        callback.accept(null);
                    } catch (Throwable t) {
                        System.err.println("[WebBrowser] DevTools callback error: " + t.getMessage());
                    }
                });
            }
        } catch (Throwable e) {
            System.err.println("[WebBrowser] DevTools connection failed: " + e.getMessage());
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    callback.accept(null);
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] DevTools fallback callback error: " + t.getMessage());
                }
            });
        }
    }
}
