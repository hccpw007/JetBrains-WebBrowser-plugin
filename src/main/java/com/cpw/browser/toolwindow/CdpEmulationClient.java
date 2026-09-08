// CDP 命令客户端：向指定页面 WebSocket 发送 Emulation 域的设备模拟命令（手机 User-Agent 与触摸模拟）
package com.cpw.browser.toolwindow;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// CDP Emulation 命令客户端，仅提供静态方法
final class CdpEmulationClient {

    // 手机模式 User-Agent（iPhone Safari，用于让站点返回移动端页面与样式）
    private static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15"
            + " (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

    // 工具类，禁止实例化
    private CdpEmulationClient() {
    }

    // 对指定页面 WebSocket 应用或清除手机设备模拟
    // wsUrl 为页面目标 WebSocket 地址
    // mobile 为 true 时设置手机 UA 与触摸模拟，false 时恢复默认并关闭触摸
    // 命令发送成功返回 true，失败返回 false
    static boolean applyMobileSimulation(String wsUrl, boolean mobile) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            // 建立到页面目标 WebSocket 的连接（连接超时 3 秒）
            WebSocket ws = client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    })
                    .get(4, TimeUnit.SECONDS);
            // 手机模式：覆盖为手机 UA 并开启触摸模拟
            if (mobile) {
                JsonObject uaParams = new JsonObject();
                uaParams.addProperty("userAgent", MOBILE_USER_AGENT);
                send(ws, 1, "Emulation.setUserAgentOverride", uaParams);

                JsonObject touchParams = new JsonObject();
                touchParams.addProperty("enabled", true);
                touchParams.addProperty("maxTouchPoints", 5);
                send(ws, 2, "Emulation.setTouchEmulationEnabled", touchParams);
            } else { // 桌面模式：恢复默认 UA 并关闭触摸模拟（userAgent 空串表示清除覆盖）
                JsonObject uaParams = new JsonObject();
                uaParams.addProperty("userAgent", "");
                send(ws, 1, "Emulation.setUserAgentOverride", uaParams);

                JsonObject touchParams = new JsonObject();
                touchParams.addProperty("enabled", false);
                send(ws, 2, "Emulation.setTouchEmulationEnabled", touchParams);
            }
            // 关闭连接，等待 2 秒避免连接泄漏
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(2, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            System.err.println("[WebBrowser] CDP emulation command failed: " + t.getMessage());
            return false;
        }
    }

    // 通过 WebSocket 发送一条 CDP 命令
    // ws 为目标 WebSocket 连接
    // id 为命令自增 id
    // method 为 CDP 方法名
    // params 为 CDP 方法参数
    // 网络等待可能被中断或超时，受检异常上抛给调用方统一处理
    private static void send(WebSocket ws, int id, String method, JsonObject params)
            throws InterruptedException, ExecutionException, TimeoutException {
        // 组装 JSON-RPC 格式的 CDP 命令
        JsonObject cmd = new JsonObject();
        cmd.addProperty("id", id);
        cmd.addProperty("method", method);
        cmd.add("params", params);
        // 发送文本帧并等待 3 秒，确保命令已写入
        ws.sendText(cmd.toString(), true).get(3, TimeUnit.SECONDS);
    }
}
