// URL 规范化工具类
package com.cpw.browser.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlUtils {

    private UrlUtils() {
    }

    // 规范化 URL 输入（无协议头自动补充 https://，含空格视为搜索）
    public static String normalize(String input) {
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
