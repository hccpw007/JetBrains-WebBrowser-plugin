package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class AddBookmarkAction(
    private val tabManager: com.cpw.browser.browser.BrowserTabManager,
    private val onBookmarkChanged: () -> Unit
) : AnAction("添加书签", "将当前页面添加到书签", WebBrowserIcons.BookmarkAdd) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val tab = tabManager.activeTab ?: return
        val bookmarkState = BookmarkPersistentState.getInstance()
        if (bookmarkState.contains(tab.currentUrl)) {
            Messages.showInfoMessage("该书签已存在", "书签")
            return
        }
        bookmarkState.addBookmark(Bookmark(tab.pageTitle.ifBlank { tab.currentUrl }, tab.currentUrl))
        onBookmarkChanged()
    }
}
