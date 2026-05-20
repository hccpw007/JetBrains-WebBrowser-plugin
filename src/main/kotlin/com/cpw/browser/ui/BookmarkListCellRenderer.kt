package com.cpw.browser.ui

import com.cpw.browser.bookmark.Bookmark
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class BookmarkListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (value is Bookmark) {
            text = value.title
            toolTipText = value.url
        }
        return renderer
    }
}
