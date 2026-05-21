// 浏览历史条目数据模型
package com.cpw.browser.history;

import java.util.Objects;

public class HistoryEntry {
    // 访问的 URL
    private final String url;
    // 页面标题
    private final String title;
    // 访问时间戳（毫秒）
    private final long timestamp;

    // 全参构造，创建历史记录条目
    public HistoryEntry(String url, String title, long timestamp) {
        this.url = url;
        this.title = title;
        this.timestamp = timestamp;
    }

    // 获取访问的 URL
    public String getUrl() {
        return url;
    }

    // 获取页面标题
    public String getTitle() {
        return title;
    }

    // 获取访问时间戳
    public long getTimestamp() {
        return timestamp;
    }

    // 判断两个历史条目是否相等
    @Override
    public boolean equals(Object o) {
        // 引用相同则相等
        if (this == o) return true;
        // 类型不匹配则不相等
        if (o == null || getClass() != o.getClass()) return false;
        HistoryEntry that = (HistoryEntry) o;
        return timestamp == that.timestamp &&
               Objects.equals(url, that.url) &&
               Objects.equals(title, that.title);
    }

    // 计算历史条目的哈希码
    @Override
    public int hashCode() {
        return Objects.hash(url, title, timestamp);
    }

    // 返回历史条目的字符串表示
    @Override
    public String toString() {
        return "HistoryEntry{" +
               "url='" + url + '\'' +
               ", title='" + title + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
