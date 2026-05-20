package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RemoveBookmarkAction(
    private val getSelectedBookmarkUrl: () -> String?,
    private val onBookmarkChanged: () -> Unit
) : AnAction("删除书签", "删除选中的书签", WebBrowserIcons.BookmarkRemove) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getSelectedBookmarkUrl() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val url = getSelectedBookmarkUrl() ?: return
        BookmarkPersistentState.getInstance().removeBookmark(url)
        onBookmarkChanged()
    }
}
