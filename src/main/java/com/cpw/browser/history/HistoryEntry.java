// 浏览历史条目数据模型
package com.cpw.browser.history;

import java.util.Objects;

public class HistoryEntry {
    private final String url;
    private final String title;
    private final long timestamp; // epoch millis

    public HistoryEntry(String url, String title, long timestamp) {
        this.url = url;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryEntry that = (HistoryEntry) o;
        return timestamp == that.timestamp &&
               Objects.equals(url, that.url) &&
               Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, title, timestamp);
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
               "url='" + url + '\'' +
               ", title='" + title + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
