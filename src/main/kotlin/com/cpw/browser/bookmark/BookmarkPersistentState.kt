package com.cpw.browser.bookmark

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "BookmarkPersistentState", storages = [Storage("WebBrowser.xml")])
class BookmarkPersistentState : PersistentStateComponent<BookmarkPersistentState.State> {

    data class State(
        val bookmarks: MutableList<Bookmark> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun addBookmark(bookmark: Bookmark) {
        if (state.bookmarks.none { it.url == bookmark.url }) {
            state.bookmarks.add(bookmark)
        }
    }

    fun removeBookmark(url: String): Boolean {
        return state.bookmarks.removeAll { it.url == url }
    }

    fun getBookmarks(): List<Bookmark> = state.bookmarks.toList()

    fun contains(url: String): Boolean = state.bookmarks.any { it.url == url }

    fun updateBookmark(oldUrl: String, newTitle: String, newUrl: String) {
        val bookmark = state.bookmarks.find { it.url == oldUrl } ?: return
        val index = state.bookmarks.indexOf(bookmark)
        if (oldUrl != newUrl) {
            state.bookmarks.removeAt(index)
            if (state.bookmarks.none { it.url == newUrl }) {
                state.bookmarks.add(bookmark.copy(title = newTitle, url = newUrl))
            }
        } else {
            state.bookmarks[index] = bookmark.copy(title = newTitle)
        }
    }

    companion object {
        fun getInstance(): BookmarkPersistentState =
            ApplicationManager.getApplication().getService(BookmarkPersistentState::class.java)
    }
}
