package com.cpw.browser.ui

import com.cpw.browser.bookmark.Bookmark
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.border.EmptyBorder

class BookmarkListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<Any> {
    private val titleLabel = JBLabel().apply {
        font = font.deriveFont(12f)
    }
    private val deleteLabel = JLabel("×").apply {
        font = font.deriveFont(Font.PLAIN, 13f)
        foreground = JBColor(0xAAAAAA, 0x777777)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = EmptyBorder(0, 4, 0, 6)
    }

    init {
        add(titleLabel, BorderLayout.CENTER)
        add(deleteLabel, BorderLayout.EAST)
        border = EmptyBorder(2, 8, 2, 2)
    }

    override fun getListCellRendererComponent(
        list: JList<out Any>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        background = JBColor.WHITE
        if (value is Bookmark) {
            titleLabel.text = value.title
            titleLabel.toolTipText = value.url
            deleteLabel.toolTipText = "删除书签"
        }
        return this
    }
}
