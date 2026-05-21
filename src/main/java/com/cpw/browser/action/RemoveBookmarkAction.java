package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.util.function.Supplier;

// 删除选中书签的 Action
public class RemoveBookmarkAction extends AnAction {

    private final Supplier<String> getSelectedBookmarkUrl;
    private final Runnable onBookmarkChanged;

    public RemoveBookmarkAction(Supplier<String> getSelectedBookmarkUrl, Runnable onBookmarkChanged) {
        super("删除书签", "删除选中的书签", WebBrowserIcons.BOOKMARK_REMOVE);
        this.getSelectedBookmarkUrl = getSelectedBookmarkUrl;
        this.onBookmarkChanged = onBookmarkChanged;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当选中书签 URL 不为 null 时启用
        e.getPresentation().setEnabled(getSelectedBookmarkUrl.get() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        String url = getSelectedBookmarkUrl.get();
        if (url == null) {
            return;
        }
        BookmarkPersistentState.getInstance().removeBookmark(url);
        onBookmarkChanged.run();
    }
}
