// 书签数据模型
package com.cpw.browser.bookmark;

import java.util.Objects;

public class Bookmark {
    // 书签标题
    private String title;
    // 书签 URL
    private String url;
    // 书签创建时间戳
    private long createdAt;

    // 无参构造，用于 XML 反序列化（初始化字段防止 null）
    private Bookmark() {
        this.url = "";
        this.title = "";
    }

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

    // 获取书签标题
    public String getTitle() {
        return title != null ? title : "";
    }

    // 设置书签标题（XmlSerializer 反序列化时使用）
    private void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    // 获取书签 URL
    public String getUrl() {
        return url != null ? url : "";
    }

    // 设置书签 URL（XmlSerializer 反序列化时使用）
    private void setUrl(String url) {
        this.url = url != null ? url : "";
    }

    // 获取书签创建时间戳
    public long getCreatedAt() {
        return createdAt;
    }

    // 设置书签创建时间戳（XmlSerializer 反序列化时使用）
    private void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // 判断两个书签是否相等
    @Override
    public boolean equals(Object o) {
        // 引用相同则相等
        if (this == o) return true;
        // 类型不匹配则不相等
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return createdAt == bookmark.createdAt &&
               Objects.equals(title, bookmark.title) &&
               Objects.equals(url, bookmark.url);
    }

    // 计算书签的哈希码
    @Override
    public int hashCode() {
        return Objects.hash(title, url, createdAt);
    }

    // 返回书签的字符串表示
    @Override
    public String toString() {
        return "Bookmark{" +
               "title='" + title + '\'' +
               ", url='" + url + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
