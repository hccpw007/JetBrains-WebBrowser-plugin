// URL 规范化工具类
package com.cpw.browser.util;

import com.cpw.browser.settings.BrowserSettingsState;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlUtils {

    private UrlUtils() {
    }

    // 搜索引擎 URL 模板：{0} 会被替换为编码后的搜索词
    private static final String[] SEARCH_URLS = {
            "https://www.google.com/search?q={0}",
            "https://www.bing.com/search?q={0}",
            "https://duckduckgo.com/?q={0}",
            "https://www.baidu.com/s?wd={0}"
    };
    // 搜索引擎代码列表，与 SEARCH_URLS 顺序对应
    private static final String[] SEARCH_ENGINE_CODES = {"google", "bing", "duckduckgo", "baidu"};

    // 根据当前配置获取搜索引擎 URL 模板
    private static String getSearchUrlTemplate() {
        String code = BrowserSettingsState.getInstance().getSearchEngine();
        for (int i = 0; i < SEARCH_ENGINE_CODES.length; i++) {
            // 匹配搜索引擎代码
            if (SEARCH_ENGINE_CODES[i].equals(code)) {
                return SEARCH_URLS[i];
            }
        }
        return SEARCH_URLS[0];
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
            String template = getSearchUrlTemplate();
            return template.replace("{0}", URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name()));
        } catch (java.io.UnsupportedEncodingException e) {
            return "about:blank";
        }
    }
}
