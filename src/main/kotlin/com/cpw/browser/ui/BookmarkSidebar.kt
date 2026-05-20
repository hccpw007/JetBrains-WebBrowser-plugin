package com.cpw.browser.ui

import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.cpw.browser.history.BrowsingHistoryState
import com.cpw.browser.history.HistoryEntry
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
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
import javax.swing.JButton
import javax.swing.JLabel
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
        cellRenderer = BookmarkListCellRenderer()
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleBookmarkClick(e)
            }
        })
    }
    private val bookmarkScroll = JBScrollPane(bookmarkList)

    // ---- 历史 ----
    private val historyListModel = HistoryEntriesModel()
    private val historyList = JBList(historyListModel).apply {
        cellRenderer = HistoryListRenderer()
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleHistoryClick(e)
            }
        })
    }
    private val historyScroll = JBScrollPane(historyList)

    // ---- 布局 ----
    private val contentPanel = JPanel(CardLayout())
    private val historyPanel = JPanel(BorderLayout())
    private val segmentWidth = 140
    private val segmentHeight = 24

    // ---- 分段切换器 (Element Plus 风格) ----
    private var currentTab = TAB_BOOKMARKS
    private val segmentBar = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = JBColor(0xE8E8E8, 0x3C3C3C)
            g2.fillRoundRect(0, 0, width - 1, height - 1, 12, 12)
            g2.dispose()
        }
    }
    private val bookmarkLabel = JLabel("书签", SwingConstants.CENTER).apply {
        font = font.deriveFont(Font.BOLD, 11f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { showBookmarks() }
        })
    }
    private val historyLabel = JLabel("历史", SwingConstants.CENTER).apply {
        font = font.deriveFont(Font.PLAIN, 11f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { showHistory() }
        })
    }
    /** 选中态白色滑块 */
    private val activeBg = object : JLabel() {
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = JBColor(0xFFFFFF, 0x585A5C)
            g2.fillRoundRect(0, 0, width - 1, height - 1, 10, 10)
            g2.dispose()
        }
    }.apply {
        isOpaque = false
        setBounds(0, 0, segmentWidth / 2, segmentHeight)
    }

    init {
        segmentBar.layout = null
        segmentBar.preferredSize = Dimension(segmentWidth, segmentHeight)
        segmentBar.minimumSize = Dimension(segmentWidth, segmentHeight)
        segmentBar.maximumSize = Dimension(segmentWidth, segmentHeight)
        segmentBar.add(activeBg)
        segmentBar.add(bookmarkLabel)
        segmentBar.add(historyLabel)
        segmentBar.setComponentZOrder(activeBg, segmentBar.componentCount - 1)
        segmentBar.setComponentZOrder(bookmarkLabel, 0)
        segmentBar.setComponentZOrder(historyLabel, 1)
        bookmarkLabel.setBounds(0, 0, segmentWidth / 2, segmentHeight)
        historyLabel.setBounds(segmentWidth / 2, 0, segmentWidth / 2, segmentHeight)

        val tabBar = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(0xC0C0C0, 0x4A4A4A)),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)
            )
            add(segmentBar)
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
        if (currentTab == TAB_BOOKMARKS) return
        currentTab = TAB_BOOKMARKS
        bookmarkLabel.font = bookmarkLabel.font.deriveFont(Font.BOLD)
        historyLabel.font = historyLabel.font.deriveFont(Font.PLAIN)
        // 移动选中背景到左侧
        segmentBar.getComponent(segmentBar.componentCount - 1)?.setBounds(0, 0, segmentBar.width / 2, segmentBar.height)
        segmentBar.repaint()
        (contentPanel.layout as CardLayout).show(contentPanel, "bookmarks")
    }

    private fun showHistory() {
        if (currentTab == TAB_HISTORY) return
        currentTab = TAB_HISTORY
        historyLabel.font = historyLabel.font.deriveFont(Font.BOLD)
        bookmarkLabel.font = bookmarkLabel.font.deriveFont(Font.PLAIN)
        // 移动选中背景到右侧
        segmentBar.getComponent(segmentBar.componentCount - 1)?.setBounds(segmentBar.width / 2, 0, segmentBar.width / 2, segmentBar.height)
        segmentBar.repaint()
        refreshHistory()
        (contentPanel.layout as CardLayout).show(contentPanel, "history")
    }

    private fun handleBookmarkClick(e: MouseEvent) {
        val index = bookmarkList.locationToIndex(e.point)
        if (index < 0) return
        val bounds = bookmarkList.getCellBounds(index, index) ?: return
        // 点击右侧删除区域 (~22px)
        if (e.point.x >= bounds.x + bounds.width - 22) {
            val bookmark = bookmarkListModel.getElementAt(index)
            val result = Messages.showYesNoDialog(
                "确定删除书签 \"${bookmark.title}\" 吗？",
                "删除书签",
                null
            )
            if (result == Messages.YES) {
                BookmarkPersistentState.getInstance().removeBookmark(bookmark.url)
                refreshBookmarks()
            }
        } else {
            val bookmark = bookmarkListModel.getElementAt(index)
            onBookmarkSelected(bookmark)
        }
    }

    private fun handleHistoryClick(e: MouseEvent) {
        val index = historyList.locationToIndex(e.point)
        if (index < 0) return
        val item = historyListModel.getElementAt(index)
        if (item !is HistoryEntry) return
        val bounds = historyList.getCellBounds(index, index) ?: return
        // 点击右侧删除区域 (~22px)
        if (e.point.x >= bounds.x + bounds.width - 22) {
            val result = Messages.showYesNoDialog(
                "确定删除该条历史记录吗？",
                "删除历史记录",
                null
            )
            if (result == Messages.YES) {
                BrowsingHistoryState.getInstance().removeEntry(item.url, item.timestamp)
                refreshHistory()
            }
        } else {
            onHistoryEntrySelected(item.url)
        }
    }

    private fun showClearPopup() {
        val popup = JPopupMenu()
        popup.add(createClearItem("清空一小时内记录", 1L))
        popup.add(createClearItem("清空24小时内记录", 24L))
        popup.add(createClearItem("清空所有记录", null))
        popup.show(this, 8, segmentBar.y + segmentBar.height + 4)
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
    private class HistoryListRenderer : JPanel(BorderLayout()), ListCellRenderer<Any> {
        private val titleLabel = JBLabel().apply {
            font = font.deriveFont(12f)
        }
        private val timeLabel = JBLabel().apply {
            font = font.deriveFont(10f)
            foreground = JBColor(0x888888, 0x999999)
        }
        private val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(titleLabel)
            add(timeLabel)
        }
        private val deleteLabel = JLabel("×").apply {
            font = font.deriveFont(Font.PLAIN, 13f)
            foreground = JBColor(0xAAAAAA, 0x777777)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = EmptyBorder(0, 4, 0, 2)
        }

        init {
            add(contentPanel, BorderLayout.CENTER)
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
            background = if (isSelected) JBColor(0xD0E4F6, 0x3A4A5A) else JBColor.WHITE
            when (value) {
                is String -> {
                    titleLabel.text = value
                    titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 11f)
                    titleLabel.foreground = JBColor(0x666666, 0xAAAAAA)
                    timeLabel.isVisible = false
                    contentPanel.border = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(0xE0E0E0, 0x555555)),
                        EmptyBorder(4, 0, 2, 0)
                    )
                    deleteLabel.isVisible = false
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
                    contentPanel.border = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(0xE0E0E0, 0x555555)),
                        EmptyBorder(4, 0, 2, 0)
                    )
                    deleteLabel.isVisible = false
                }
                is HistoryEntry -> {
                    titleLabel.text = value.title
                    titleLabel.font = titleLabel.font.deriveFont(Font.PLAIN, 12f)
                    titleLabel.foreground = JBColor(0x000000, 0xDDDDDD)
                    timeLabel.text = formatTimestamp(value.timestamp)
                    timeLabel.isVisible = true
                    contentPanel.border = EmptyBorder(0, 0, 0, 0)
                    deleteLabel.isVisible = true
                    deleteLabel.toolTipText = "删除"
                }
            }
            return this
        }
    }

    companion object {
        private const val TAB_BOOKMARKS = 0
        private const val TAB_HISTORY = 1

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
