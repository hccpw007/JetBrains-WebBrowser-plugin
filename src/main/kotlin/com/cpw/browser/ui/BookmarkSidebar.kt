package com.cpw.browser.ui

import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.cpw.browser.history.BrowsingHistoryState
import com.cpw.browser.history.HistoryEntry
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.swing.AbstractListModel
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class BookmarkSidebar(
    private val onBookmarkSelected: (Bookmark) -> Unit,
    private val onHistoryEntrySelected: (String) -> Unit = {}
) : JBPanel<BookmarkSidebar>(BorderLayout()) {

    // ---- 书签 ----
    private val bookmarkListModel = com.intellij.ui.CollectionListModel<Bookmark>(
        BookmarkPersistentState.getInstance().getBookmarks()
    )
    private val bookmarkList = JBList(bookmarkListModel).apply {
        addListSelectionListener {
            if (!it.valueIsAdjusting && selectedValue != null) {
                onBookmarkSelected(selectedValue)
            }
        }
        cellRenderer = BookmarkListCellRenderer()
    }
    private val bookmarkScroll = JBScrollPane(bookmarkList)

    // ---- 历史 ----
    private val historyListModel = HistoryEntriesModel()
    private val historyList = JBList(historyListModel).apply {
        cellRenderer = HistoryListRenderer()
        addListSelectionListener {
            if (!it.valueIsAdjusting && selectedValue is HistoryEntry) {
                onHistoryEntrySelected((selectedValue as HistoryEntry).url)
            }
        }
    }
    private val historyScroll = JBScrollPane(historyList)

    // ---- 标签切换按钮 ----
    private val bookmarkBtn = JButton("书签").apply {
        isFocusPainted = false
        font = font.deriveFont(Font.BOLD, 11f)
        preferredSize = Dimension(70, 22)
        addActionListener { showBookmarks() }
    }
    private val historyBtn = JButton("历史").apply {
        isFocusPainted = false
        font = font.deriveFont(Font.PLAIN, 11f)
        preferredSize = Dimension(70, 22)
        addActionListener { showHistory() }
    }

    // ---- 布局 ----
    private val contentPanel = JPanel(CardLayout())
    private val historyPanel = JPanel(BorderLayout())

    init {
        ButtonGroup().apply {
            add(bookmarkBtn)
            add(historyBtn)
            setSelected(bookmarkBtn.model, true)
        }

        val tabBar = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(0xC0C0C0, 0x4A4A4A)),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)
            )
            add(bookmarkBtn)
            add(historyBtn)
        }

        // 历史面板 — 清空链接
        val clearLabel = JBLabel("清空记录").apply {
            font = font.deriveFont(11f)
            foreground = JBColor(0x3366CC, 0x7799DD)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    showClearPopup()
                }
            })
        }
        val clearPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)).apply {
            add(clearLabel)
        }
        historyPanel.add(clearPanel, BorderLayout.NORTH)
        historyPanel.add(historyScroll, BorderLayout.CENTER)

        contentPanel.add(bookmarkScroll, "bookmarks")
        contentPanel.add(historyPanel, "history")

        add(tabBar, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
    }

    fun refreshBookmarks() {
        bookmarkListModel.replaceAll(BookmarkPersistentState.getInstance().getBookmarks())
    }

    fun refreshHistory() {
        historyListModel.refresh()
    }

    fun getSelectedBookmarkUrl(): String? = bookmarkList.selectedValue?.url

    private fun showBookmarks() {
        bookmarkBtn.font = bookmarkBtn.font.deriveFont(Font.BOLD)
        historyBtn.font = historyBtn.font.deriveFont(Font.PLAIN)
        (contentPanel.layout as CardLayout).show(contentPanel, "bookmarks")
    }

    private fun showHistory() {
        historyBtn.font = historyBtn.font.deriveFont(Font.BOLD)
        bookmarkBtn.font = bookmarkBtn.font.deriveFont(Font.PLAIN)
        refreshHistory()
        (contentPanel.layout as CardLayout).show(contentPanel, "history")
    }

    private fun showClearPopup() {
        val popup = JPopupMenu()
        popup.add(createClearItem("清空一小时内记录", 1L))
        popup.add(createClearItem("清空24小时内记录", 24L))
        popup.add(createClearItem("清空所有记录", null))
        popup.show(this, 8, historyBtn.y + historyBtn.height + 4)
    }

    private fun createClearItem(text: String, hours: Long?) = JButton(text).apply {
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        font = font.deriveFont(12f)
        horizontalAlignment = SwingConstants.LEFT
        border = EmptyBorder(4, 12, 4, 12)
        addActionListener {
            BrowsingHistoryState.getInstance().clearEntries(hours)
            refreshHistory()
        }
    }

    // ---- 历史列表模型 ----
    private class HistoryEntriesModel : AbstractListModel<Any>() {
        private var items: List<Any> = buildItems()

        fun refresh() {
            items = buildItems()
            fireContentsChanged(this, 0, items.size.coerceAtLeast(0))
        }

        override fun getSize(): Int = items.size
        override fun getElementAt(index: Int): Any = items[index]

        private fun buildItems(): List<Any> {
            val entries = BrowsingHistoryState.getInstance().getEntries()
            if (entries.isEmpty()) return emptyList()

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val mondayStart = monday.atStartOfDay(zone).toInstant().toEpochMilli()

            val todayGroup = mutableListOf<HistoryEntry>()
            val weekGroups = mutableMapOf<DayOfWeek, MutableList<HistoryEntry>>()
            val earlierGroup = mutableListOf<HistoryEntry>()

            for (e in entries) {
                when {
                    e.timestamp >= todayStart -> todayGroup.add(e)
                    e.timestamp >= mondayStart -> {
                        val dow = Instant.ofEpochMilli(e.timestamp).atZone(zone).dayOfWeek
                        weekGroups.getOrPut(dow) { mutableListOf() }.add(e)
                    }
                    else -> earlierGroup.add(e)
                }
            }

            val result = mutableListOf<Any>()
            if (todayGroup.isNotEmpty()) {
                result.add(HEADER_TODAY)
                result.addAll(todayGroup)
            }
            for (dow in listOf(DayOfWeek.MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)) {
                weekGroups[dow]?.let {
                    if (it.isNotEmpty()) {
                        result.add(dow)
                        result.addAll(it)
                    }
                }
            }
            if (earlierGroup.isNotEmpty()) {
                result.add(HEADER_OLDER)
                result.addAll(earlierGroup)
            }
            return result
        }

        companion object {
            const val HEADER_TODAY = "今天"
            const val HEADER_OLDER = "以前"
            private val TUESDAY = DayOfWeek.TUESDAY
            private val WEDNESDAY = DayOfWeek.WEDNESDAY
            private val THURSDAY = DayOfWeek.THURSDAY
            private val FRIDAY = DayOfWeek.FRIDAY
            private val SATURDAY = DayOfWeek.SATURDAY
            private val SUNDAY = DayOfWeek.SUNDAY
        }
    }

    // ---- 历史列表渲染器 ----
    private class HistoryListRenderer : JPanel(), ListCellRenderer<Any> {
        private val titleLabel = JBLabel().apply {
            font = font.deriveFont(12f)
        }
        private val timeLabel = JBLabel().apply {
            font = font.deriveFont(10f)
            foreground = JBColor(0x888888, 0x999999)
        }

        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(2, 8, 2, 8)
            add(titleLabel)
            add(timeLabel)
        }

        override fun getListCellRendererComponent(
            list: JList<out Any>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            background = if (isSelected) JBColor(0xD0E4F6, 0x3A4A5A) else JBColor.WHITE
            when (value) {
                is String -> {
                    titleLabel.text = value
                    titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 11f)
                    titleLabel.foreground = JBColor(0x666666, 0xAAAAAA)
                    timeLabel.isVisible = false
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(0xE0E0E0, 0x555555)),
                        EmptyBorder(4, 8, 2, 8)
                    )
                }
                is DayOfWeek -> {
                    titleLabel.text = when (value) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                    }
                    titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 11f)
                    titleLabel.foreground = JBColor(0x666666, 0xAAAAAA)
                    timeLabel.isVisible = false
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(0xE0E0E0, 0x555555)),
                        EmptyBorder(4, 8, 2, 8)
                    )
                }
                is HistoryEntry -> {
                    titleLabel.text = value.title
                    titleLabel.font = titleLabel.font.deriveFont(Font.PLAIN, 12f)
                    titleLabel.foreground = JBColor(0x000000, 0xDDDDDD)
                    timeLabel.text = formatTimestamp(value.timestamp)
                    timeLabel.isVisible = true
                    border = EmptyBorder(2, 8, 2, 8)
                }
            }
            return this
        }
    }

    companion object {
        private fun formatTimestamp(millis: Long): String {
            val zone = ZoneId.systemDefault()
            val zdt = Instant.ofEpochMilli(millis).atZone(zone)
            val today = LocalDate.now(zone)
            val entryDate = zdt.toLocalDate()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            return when {
                entryDate == today -> zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
                !entryDate.isBefore(monday) -> {
                    val wd = when (zdt.dayOfWeek) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                    }
                    "$wd ${zdt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                }
                else -> zdt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            }
        }
    }
}
