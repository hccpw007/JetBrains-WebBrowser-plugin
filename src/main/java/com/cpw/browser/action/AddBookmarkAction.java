package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

// 将当前页面添加到书签的 Action
public class AddBookmarkAction extends AnAction {

    private final BrowserTabManager tabManager;
    private final Runnable onBookmarkChanged;

    public AddBookmarkAction(BrowserTabManager tabManager, Runnable onBookmarkChanged) {
        super("添加书签", "将当前页面添加到书签", WebBrowserIcons.BOOKMARK_ADD);
        this.tabManager = tabManager;
        this.onBookmarkChanged = onBookmarkChanged;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当存在活动标签页时启用
        e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        BrowserTabPanel tab = tabManager.getActiveTab();
        if (tab == null) {
            return;
        }
        BookmarkPersistentState bookmarkState = BookmarkPersistentState.getInstance();
        // 检查书签是否已存在
        if (bookmarkState.contains(tab.getCurrentUrl())) {
            Messages.showInfoMessage("该书签已存在", "书签");
            return;
        }
        String title = tab.getPageTitle().isBlank() ? tab.getCurrentUrl() : tab.getPageTitle();
        bookmarkState.addBookmark(new Bookmark(title, tab.getCurrentUrl()));
        onBookmarkChanged.run();
    }
}
