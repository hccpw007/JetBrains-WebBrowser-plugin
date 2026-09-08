// CDP 公共支撑工具：JCEF 远程调试端口发现与页面目标匹配，供嵌入式 DevTools 与设备模拟共用
package com.cpw.browser.toolwindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.PathManager;
import com.intellij.ui.jcef.JBCefApp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

// CDP 支撑工具类，仅提供静态方法
public final class CdpSupport {

    // 工具类，禁止实例化
    private CdpSupport() {
    }

    // 查找 JCEF 远程调试端口
    public static Integer findDevToolsPort() {
        // 尝试通过 JBCefApp API 获取端口
        try {
            JBCefApp app = JBCefApp.getInstance();
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            // 异步查询远程调试端口
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

        // 尝试通过 DevToolsActivePort 文件查找端口（限制搜索深度为 2，防止在大缓存目录中长时间遍历）
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

            // 遍历所有候选路径，查找 DevToolsActivePort 文件（限制搜索深度为 2 层）
            for (Path root : candidates) {
                // 跳过非目录的候选路径
                if (!Files.isDirectory(root)) continue;
                try {
                    Optional<Path> optionalPath = Files.walk(root, 2)
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
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] DevToolsActivePort search in " + root + " failed: " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            System.err.println("[WebBrowser] DevToolsActivePort file search failed: " + t.getMessage());
        }

        return null;
    }

    // 从 CDP /json 页面列表中按当前 URL 匹配类型为 page 的目标，未匹配到则回退第一个 page 目标
    // pages 为 /json 接口返回的页面列表
    // currentUrl 为当前页面 URL，用于精确或包含匹配
    // 返回匹配到的页面目标，列表为空或无 page 类型目标时返回 null
    public static JsonObject matchPageTarget(JsonArray pages, String currentUrl) {
        JsonObject matchedPage = null;
        // 遍历 CDP 返回的所有页面列表
        for (JsonElement page : pages) {
            JsonObject obj = page.getAsJsonObject();
            JsonElement typeElem = obj.get("type");
            // 只筛选类型为 "page" 的页面
            if (typeElem != null && "page".equals(typeElem.getAsString())) {
                String pageUrl = obj.get("url") != null ? obj.get("url").getAsString() : "";
                // 按 URL 精确匹配或包含匹配，空白页时（仅一个页面）直接用
                if (pageUrl.equals(currentUrl)
                        || (!"about:blank".equals(currentUrl) && pageUrl.contains(currentUrl))
                        || ("about:blank".equals(currentUrl) && pages.size() == 1)) {
                    matchedPage = obj;
                    break;
                }
            }
        }
        // 如果没匹配到，则回退使用第一个可用的 page 目标
        if (matchedPage == null) {
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
        return matchedPage;
    }

    // 查询 CDP /json 接口并返回匹配当前 URL 的页面 WebSocket 地址，供设备模拟等 CDP 命令使用
    // port 为远程调试端口
    // currentUrl 为当前页面 URL
    // 返回形如 ws://127.0.0.1:port/devtools/page/{id} 的地址，查找失败返回 null
    public static String findPageWebSocketUrl(int port, String currentUrl) {
        try {
            URL url = new URI("http://127.0.0.1:" + port + "/json").toURL();
            String json;
            // 读取 /json 返回的页面目标列表
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                json = reader.lines().collect(Collectors.joining("\n"));
            }
            JsonArray pages = JsonParser.parseString(json).getAsJsonArray();
            JsonObject matched = matchPageTarget(pages, currentUrl);
            // 未匹配到任何 page 目标则返回 null
            if (matched == null) return null;
            JsonElement idElem = matched.get("id");
            String pageId = idElem != null ? idElem.getAsString() : null;
            // 页面 id 为空则返回 null
            if (pageId == null || pageId.isBlank()) return null;
            return "ws://127.0.0.1:" + port + "/devtools/page/" + pageId;
        } catch (Throwable t) {
            System.err.println("[WebBrowser] CDP page target lookup failed: " + t.getMessage());
            return null;
        }
    }
}
