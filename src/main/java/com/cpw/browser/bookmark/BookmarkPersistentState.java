// 书签持久化状态管理
package com.cpw.browser.bookmark;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

@State(name = "BookmarkPersistentState", storages = @Storage("WebBrowser.xml"))
public class BookmarkPersistentState implements PersistentStateComponent<BookmarkPersistentState.State> {

    // 书签持久化状态内部数据类
    public static class State {
        private List<Bookmark> bookmarks = new ArrayList<>();

        public List<Bookmark> getBookmarks() {
            return bookmarks;
        }

        public void setBookmarks(List<Bookmark> bookmarks) {
            this.bookmarks = bookmarks;
        }
    }

    private State state = new State();

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // 添加书签，如果 URL 已存在则不添加
    public void addBookmark(Bookmark bookmark) {
        boolean exists = state.getBookmarks().stream()
                .anyMatch(b -> b.getUrl().equals(bookmark.getUrl()));
        if (!exists) {
            state.getBookmarks().add(bookmark);
        }
    }

    // 根据 URL 移除书签
    public boolean removeBookmark(String url) {
        return state.getBookmarks().removeIf(b -> b.getUrl().equals(url));
    }

    // 获取所有书签的副本
    public List<Bookmark> getBookmarks() {
        return new ArrayList<>(state.getBookmarks());
    }

    // 判断是否已包含指定 URL 的书签
    public boolean contains(String url) {
        return state.getBookmarks().stream().anyMatch(b -> b.getUrl().equals(url));
    }

    // 更新书签的标题和 URL
    public void updateBookmark(String oldUrl, String newTitle, String newUrl) {
        Bookmark bookmark = state.getBookmarks().stream()
                .filter(b -> b.getUrl().equals(oldUrl))
                .findFirst()
                .orElse(null);
        if (bookmark == null) return;

        int index = state.getBookmarks().indexOf(bookmark);
        if (!oldUrl.equals(newUrl)) {
            // URL 变更时移除旧条目，若新 URL 不存在则添加
            state.getBookmarks().remove(index);
            boolean exists = state.getBookmarks().stream()
                    .anyMatch(b -> b.getUrl().equals(newUrl));
            if (!exists) {
                state.getBookmarks().add(new Bookmark(newTitle, newUrl, bookmark.getCreatedAt()));
            }
        } else {
            // URL 不变时仅更新标题
            state.getBookmarks().set(index, new Bookmark(newTitle, bookmark.getUrl(), bookmark.getCreatedAt()));
        }
    }

    public static BookmarkPersistentState getInstance() {
        return ApplicationManager.getApplication().getService(BookmarkPersistentState.class);
    }
}
