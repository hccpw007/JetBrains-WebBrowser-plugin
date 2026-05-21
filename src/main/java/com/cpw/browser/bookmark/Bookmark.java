// 书签数据模型
package com.cpw.browser.bookmark;

import java.util.Objects;

public class Bookmark {
    private final String title;
    private final String url;
    private final long createdAt;

    // 全参构造
    public Bookmark(String title, String url, long createdAt) {
        this.title = title;
        this.url = url;
        this.createdAt = createdAt;
    }

    // 便捷构造，自动设置创建时间
    public Bookmark(String title, String url) {
        this(title, url, System.currentTimeMillis());
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return createdAt == bookmark.createdAt &&
               Objects.equals(title, bookmark.title) &&
               Objects.equals(url, bookmark.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, url, createdAt);
    }

    @Override
    public String toString() {
        return "Bookmark{" +
               "title='" + title + '\'' +
               ", url='" + url + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
