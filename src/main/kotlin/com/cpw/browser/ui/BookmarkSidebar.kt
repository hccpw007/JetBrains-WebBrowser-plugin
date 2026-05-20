package com.cpw.browser.ui

import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.DefaultListSelectionModel
import javax.swing.event.ListSelectionListener

class BookmarkSidebar(
    private val onBookmarkSelected: (Bookmark) -> Unit
) : JBPanel<BookmarkSidebar>(BorderLayout()) {

    private val listModel = com.intellij.ui.CollectionListModel<Bookmark>(
        BookmarkPersistentState.getInstance().getBookmarks()
    )
    val bookmarkList = JBList(listModel).apply {
        selectionModel = DefaultListSelectionModel()
        // 单击跳转
        addListSelectionListener(ListSelectionListener {
            if (!it.valueIsAdjusting && selectedValue != null) {
                onBookmarkSelected(selectedValue)
            }
        })
        cellRenderer = BookmarkListCellRenderer()
    }

    fun refreshBookmarks() {
        listModel.replaceAll(BookmarkPersistentState.getInstance().getBookmarks())
    }

    fun getSelectedBookmarkUrl(): String? = bookmarkList.selectedValue?.url

    init {
        val scrollPane = JBScrollPane(bookmarkList)
        add(scrollPane, BorderLayout.CENTER)
    }
}
