// 嵌入式 DevTools 管理器，负责端口发现、CDP 连接、打开/关闭/状态管理
package com.cpw.browser.toolwindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.JComponent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

    // 打开嵌入式 DevTools：通过 CDP 远程调试端口连接并加载 devtoolsFrontendUrl
    public void open(Consumer<JBCefBrowser> callback) {
        // 如果已经打开，直接返回
        if (embeddedDevTools != null) {
            callback.accept(embeddedDevTools);
            return;
        }

        // 在后台线程查找 DevTools 端口
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Integer port = findDevToolsPort();
            // 如果找到有效端口，则连接 DevTools
            if (port != null && port > 0) {
                connectDevTools(port, callback);
            } else { // 未找到端口，通知回调失败
                System.err.println("[WebBrowser] DevTools port not found");
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
            }
        });
    }

    // 关闭嵌入式 DevTools
    public void close() {
        // 只有嵌入式 DevTools 已打开时才需要关闭
        if (embeddedDevTools != null) {
            JBCefBrowser devTools = embeddedDevTools;
            Runnable disposeRunnable = () -> {
                devTools.dispose();
                embeddedDevTools = null;
            };
            // 如果在调度线程中，直接执行；否则在调度线程中执行
            if (ApplicationManager.getApplication().isDispatchThread()) {
                disposeRunnable.run();
            } else { // 不在调度线程中，通过 invokeLater 调度
                ApplicationManager.getApplication().invokeLater(disposeRunnable);
            }
        }
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
            // 等待端口查询结果，超时 3 秒
            if (latch.await(3, TimeUnit.SECONDS)) {
                Integer port = result.get();
                // 如果端口有效则直接返回
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
            // 如果系统目录不为空，添加 jcef_cache 作为候选路径
            if (systemDir != null) {
                candidates.add(systemDir.resolve("jcef_cache"));
            }
            try {
                String userHome = System.getProperty("user.home");
                // 如果用户主目录不为空，添加 JetBrains 缓存目录作为候选路径
                if (userHome != null) {
                    candidates.add(Path.of(userHome, "Library", "Caches", "JetBrains"));
                }
            } catch (Exception e) {
                // 忽略，继续尝试其他候选路径
            }

            // 遍历所有候选路径，查找 DevToolsActivePort 文件
            for (Path root : candidates) {
                // 跳过非目录的候选路径
                if (!Files.isDirectory(root)) continue;
                java.util.Optional<Path> optionalPath = Files.walk(root, 6)
                        .filter(p -> "DevToolsActivePort".equals(p.getFileName().toString()))
                        .findFirst();
                // 如果找到 DevToolsActivePort 文件，读取其中的端口号
                if (optionalPath.isPresent()) {
                    Path foundPath = optionalPath.get();
                    List<String> lines = Files.readAllLines(foundPath);
                    // 如果文件内容不为空，解析端口号
                    if (!lines.isEmpty()) {
                        try {
                            Integer port = Integer.parseInt(lines.get(0).trim());
                            // 如果端口有效则返回
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
            String currentUrl = tab.getCurrentUrl();
            // 遍历 CDP 返回的所有页面列表
            for (JsonElement page : pages) {
                JsonObject obj = page.getAsJsonObject();
                JsonElement typeElem = obj.get("type");
                // 只筛选类型为 "page" 的页面
                if (typeElem != null && "page".equals(typeElem.getAsString())) {
                    String pageUrl = obj.get("url") != null ? obj.get("url").getAsString() : "";
                    // 按 URL 精确匹配或包含匹配，空白页时直接用第一个
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
                // 遍历页面列表，取第一个类型为 "page" 的页面
                for (JsonElement page : pages) {
                    JsonObject obj = page.getAsJsonObject();
                    JsonElement typeElem = obj.get("type");
                    // 只筛选类型为 "page" 的页面
                    if (typeElem != null && "page".equals(typeElem.getAsString())) {
                        matchedPage = obj;
                        break;
                    }
                }
            }

            // 如果匹配到了目标页面
            if (matchedPage != null) {
                JsonElement devtoolsUrlElem = matchedPage.get("devtoolsFrontendUrl");
                String devtoolsUrl = devtoolsUrlElem != null ? devtoolsUrlElem.getAsString() : null;
                // 如果 devtoolsFrontendUrl 有效，则加载嵌入式 DevTools
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
                } else { // devtoolsFrontendUrl 为空，通知回调失败
                    System.err.println("[WebBrowser] No devtoolsFrontendUrl in /json response: " + matchedPage);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
                }
            } else { // 未找到可用的 page 目标，通知回调失败
                System.err.println("[WebBrowser] No page found in /json list");
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
            }
        } catch (Exception e) {
            System.err.println("[WebBrowser] DevTools connection failed: " + e.getMessage());
            ApplicationManager.getApplication().invokeLater(() -> callback.accept(null));
        }
    }
}
