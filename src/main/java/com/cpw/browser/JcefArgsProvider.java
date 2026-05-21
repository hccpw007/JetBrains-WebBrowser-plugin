package com.cpw.browser;

import com.intellij.ui.jcef.JBCefAppRequiredArgumentsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// 在 JCEF 初始化时传递 --remote-debugging-port=0 等参数，
// 使 JCEF 启用远程调试端口并生成 DevToolsActivePort 文件，
// 供嵌入式 DevTools 通过 CDP 连接使用。
class JcefArgsProvider implements JBCefAppRequiredArgumentsProvider {

    @Override
    public @NotNull List<String> getOptions() {
        return Arrays.asList(
                "--remote-debugging-port=0",
                "--remote-allow-origins=*",         // 允许 CDP 接受来自 CDN 等任意来源的 WebSocket 连接
                "--allow-running-insecure-content" // 允许 HTTPS 页面加载 WS 等非安全内容（DevTools CDN 到 CDP）
        );
    }
}
