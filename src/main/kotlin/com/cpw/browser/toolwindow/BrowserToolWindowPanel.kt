package com.cpw.browser.toolwindow

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.action.GoBackAction
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.cpw.browser.action.GoForwardAction
import com.cpw.browser.action.GoHomeAction
import com.cpw.browser.action.NewTabAction
import com.cpw.browser.action.OpenDevToolsAction
import com.cpw.browser.action.RefreshAction
import com.cpw.browser.browser.BrowserTabManager
import com.cpw.browser.ui.AddressBar
import com.cpw.browser.ui.BookmarkSidebar
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URLEncoder
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class BrowserToolWindowPanel(private val project: Project) {

    private val tabManager = BrowserTabManager()
    private val addressBar = AddressBar(
        onNavigate = { rawUrl -> onNavigateRequested(rawUrl) },
        isUrlBookmarked = { url -> BookmarkPersistentState.getInstance().contains(url) },
        onToggleBookmark = { url -> toggleBookmark(url) }
    )
    private lateinit var bookmarkSidebar: BookmarkSidebar
    private val tabStripPanel = JPanel() // 自定义标签页栏
    private val browserContentPanel = JPanel(BorderLayout()) // 浏览器内容区域
    private val centerPanel = JPanel(BorderLayout()) // 居中区域：书签(可隐藏) + 浏览器内容
    private val statusLabel = JBLabel("就绪", SwingConstants.LEFT)
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val tabTitleLabels = mutableMapOf<BrowserTabPanel, JBLabel>() // 标题标签缓存
    private val tabStripItems = mutableMapOf<BrowserTabPanel, JPanel>() // 标签页栏条目
    private val tabCloseButtons = mutableMapOf<BrowserTabPanel, JButton>() // 关闭按钮

    init {
        bookmarkSidebar = BookmarkSidebar { bookmark -> onBookmarkSelected(bookmark) }
        bookmarkSidebar.isVisible = false // 默认隐藏书签侧边栏

        // 居中区域：[书签侧边栏(可隐藏)] [浏览器内容]
        centerPanel.add(bookmarkSidebar, BorderLayout.WEST)
        centerPanel.add(browserContentPanel, BorderLayout.CENTER)

        // 标签页栏
        tabStripPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        tabStripPanel.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)

        tabManager.onTabAdded = { tab -> addTabToStrip(tab) }
        tabManager.onTabRemoved = { tab ->
            tabTitleLabels.remove(tab)
            tabCloseButtons.remove(tab)
            removeTabFromStrip(tab)
        }
        tabManager.onActiveTabChanged = { tab -> onActiveTabChanged(tab) }

        // 后退/前进/刷新/主页 mini 工具栏
        val navGroup = DefaultActionGroup().apply {
            add(GoBackAction(tabManager))
            add(GoForwardAction(tabManager))
            add(RefreshAction(tabManager))
            add(GoHomeAction(tabManager))
        }
        val navToolbar = ActionManager.getInstance()
            .createActionToolbar("WebBrowser.NavBar", navGroup, true)
        navToolbar.setTargetComponent(navToolbar.component)

        // url 右侧工具栏：开发者工具、书签侧边栏切换、新建标签页
        val rightGroup = DefaultActionGroup().apply {
            add(OpenDevToolsAction(tabManager))
            addSeparator()
            add(ToggleBookmarkSidebarAction())
            addSeparator()
            add(NewTabAction(tabManager))
        }
        val rightToolbar = ActionManager.getInstance()
            .createActionToolbar("WebBrowser.RightActions", rightGroup, true)
        rightToolbar.setTargetComponent(rightToolbar.component)

        // 地址栏行：[导航按钮] [地址栏] [开发者工具/书签/新标签页]
        val navAddressBar = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(navToolbar.component, BorderLayout.WEST)
            add(addressBar, BorderLayout.CENTER)
            add(rightToolbar.component, BorderLayout.EAST)
        }

        // 顶部区域：[标签页栏] [地址栏]
        val topSection = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(tabStripPanel)
            add(navAddressBar)
        }

        mainPanel.add(topSection, BorderLayout.NORTH)
        mainPanel.add(centerPanel, BorderLayout.CENTER)
        mainPanel.add(statusLabel, BorderLayout.SOUTH)

        tabManager.createTab()
    }

    fun getContent(): JBPanel<JBPanel<*>> = mainPanel

    fun dispose() {
        ApplicationManager.getApplication().invokeLater {
            tabManager.disposeAll()
        }
    }

    fun focusAddressBar() {
        addressBar.requestFocusOnField()
    }

    fun openDevTools() {
        tabManager.activeTab?.openDevTools()
    }

    // 书签侧边栏切换 Action
    private inner class ToggleBookmarkSidebarAction : AnAction(
        "显示书签", "显示或隐藏书签侧边栏", WebBrowserIcons.BookmarkAdd
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            bookmarkSidebar.isVisible = !bookmarkSidebar.isVisible
            centerPanel.revalidate()
            centerPanel.repaint()
            // 更新按钮描述
            e.presentation.description = if (bookmarkSidebar.isVisible) "隐藏书签侧边栏" else "显示书签侧边栏"
        }
    }

    private fun addTabToStrip(tab: BrowserTabPanel) {
        val titleLabel = JBLabel(tab.getTabTitle())
        tabTitleLabels[tab] = titleLabel

        val closeBtn = JButton("×").apply {
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            preferredSize = Dimension(16, 16)
            maximumSize = Dimension(16, 16)
            minimumSize = Dimension(16, 16)
            font = font.deriveFont(12f)
            toolTipText = "关闭标签页"
            addActionListener {
                val index = tabManager.getTabs().indexOf(tab)
                if (index >= 0) {
                    tabManager.closeTab(index)
                }
            }
        }
        tabCloseButtons[tab] = closeBtn

        val tabStripItem = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = true
            border = BorderFactory.createEmptyBorder(3, 8, 3, 8)
            add(titleLabel)
            add(Box.createRigidArea(Dimension(6, 0)))
            add(closeBtn)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val index = tabManager.getTabs().indexOf(tab)
                    if (index >= 0) {
                        tabManager.switchToTab(index)
                    }
                }
            })
        }

        tabStripItems[tab] = tabStripItem
        tabStripPanel.add(tabStripItem)
        tabStripPanel.revalidate()
        tabStripPanel.repaint()
        updateTabStripHighlight()
    }

    private fun removeTabFromStrip(tab: BrowserTabPanel) {
        val item = tabStripItems.remove(tab) ?: return
        tabStripPanel.remove(item)
        tabStripPanel.revalidate()
        tabStripPanel.repaint()
        updateTabStripHighlight()
    }

    private fun updateTabStripHighlight() {
        val activeTab = tabManager.activeTab
        for ((tab, item) in tabStripItems) {
            item.background = if (tab == activeTab) tabStripPanel.background else tabStripPanel.background.darker()
        }
    }

    private fun onActiveTabChanged(tab: BrowserTabPanel?) {
        if (tab != null) {
            addressBar.setUrl(tab.currentUrl)
            statusLabel.text = if (tab.isLoading) "加载中..." else "就绪"
            updateBrowserContent(tab)
            updateTabStripHighlight()
            updateTabTitle(tab)
        } else {
            addressBar.setUrl("")
            statusLabel.text = "就绪"
            updateBrowserContent(null)
        }
    }

    private fun updateBrowserContent(tab: BrowserTabPanel?) {
        browserContentPanel.removeAll()
        if (tab != null) {
            browserContentPanel.add(tab.component, BorderLayout.CENTER)
        }
        browserContentPanel.revalidate()
        browserContentPanel.repaint()
    }

    private fun toggleBookmark(url: String) {
        val bookmarkState = BookmarkPersistentState.getInstance()
        if (bookmarkState.contains(url)) {
            bookmarkState.removeBookmark(url)
        } else {
            val tab = tabManager.activeTab
            val title = tab?.pageTitle?.ifBlank { url } ?: url
            bookmarkState.addBookmark(com.cpw.browser.bookmark.Bookmark(title, url))
        }
        bookmarkSidebar.refreshBookmarks()
        addressBar.updateStarIcon(url)
    }

    private fun onNavigateRequested(rawUrl: String) {
        val url = normalizeUrl(rawUrl)
        val tab = tabManager.activeTab
        if (tab != null) {
            tab.navigate(url)
        } else {
            tabManager.createTab(url)
        }
    }

    private fun onBookmarkSelected(bookmark: com.cpw.browser.bookmark.Bookmark) {
        val tab = tabManager.activeTab
        if (tab != null) {
            tab.navigate(bookmark.url)
        } else {
            tabManager.createTab(bookmark.url)
        }
    }

    private fun updateTabTitle(tab: BrowserTabPanel) {
        tabTitleLabels[tab]?.text = tab.getTabTitle()
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
        if (trimmed.contains('.') && !trimmed.contains(' ')) return "https://$trimmed"
        return "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
