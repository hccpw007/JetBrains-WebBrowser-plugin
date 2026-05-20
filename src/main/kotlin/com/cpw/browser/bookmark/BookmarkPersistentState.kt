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

    fun updateTitle(url: String, title: String) {
        state.bookmarks.find { it.url == url }?.let {
            val index = state.bookmarks.indexOf(it)
            state.bookmarks[index] = it.copy(title = title)
        }
    }

    companion object {
        fun getInstance(): BookmarkPersistentState =
            ApplicationManager.getApplication().getService(BookmarkPersistentState::class.java)
    }
}
