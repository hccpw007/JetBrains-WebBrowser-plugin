package com.cpw.browser.toolwindow

import com.cpw.browser.action.AddBookmarkAction
import com.cpw.browser.action.GoBackAction
import com.cpw.browser.action.GoForwardAction
import com.cpw.browser.action.GoHomeAction
import com.cpw.browser.action.NewTabAction
import com.cpw.browser.action.OpenDevToolsAction
import com.cpw.browser.action.RefreshAction
import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.browser.BrowserTabManager
import com.cpw.browser.ui.AddressBar
import com.cpw.browser.ui.BookmarkSidebar
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
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
import javax.swing.JPanel
import javax.swing.SwingConstants

class BrowserToolWindowPanel(private val project: Project) {

    private val tabManager = BrowserTabManager()
    private val addressBar = AddressBar { rawUrl -> onNavigateRequested(rawUrl) }
    private lateinit var bookmarkSidebar: BookmarkSidebar
    private val tabStripPanel = JPanel() // 自定义标签页栏
    private val browserContentPanel = JPanel(BorderLayout()) // 浏览器内容区域
    private val statusLabel = JBLabel("就绪", SwingConstants.LEFT)
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val tabTitleLabels = mutableMapOf<BrowserTabPanel, JBLabel>() // 标题标签缓存
    private val tabStripItems = mutableMapOf<BrowserTabPanel, JPanel>() // 标签页栏条目

    init {
        bookmarkSidebar = BookmarkSidebar { bookmark -> onBookmarkSelected(bookmark) }

        // 标签页栏
        tabStripPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        tabStripPanel.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)

        tabManager.onTabAdded = { tab -> addTabToStrip(tab) }
        tabManager.onTabRemoved = { tab ->
            tabTitleLabels.remove(tab)
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

        // url 右侧工具栏：开发者工具、添加书签、新建标签页
        val rightGroup = DefaultActionGroup().apply {
            add(OpenDevToolsAction(tabManager))
            add(AddBookmarkAction(tabManager) { bookmarkSidebar.refreshBookmarks() })
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

        // 侧边栏分割器：[书签] [浏览器内容]
        val sidebarSplitter = JBSplitter(false, 0.2f)
        sidebarSplitter.firstComponent = bookmarkSidebar
        sidebarSplitter.secondComponent = browserContentPanel

        mainPanel.add(topSection, BorderLayout.NORTH)
        mainPanel.add(sidebarSplitter, BorderLayout.CENTER)
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

    private fun addTabToStrip(tab: BrowserTabPanel) {
        val titleLabel = JBLabel(tab.getTabTitle())
        tabTitleLabels[tab] = titleLabel

        val tabStripItem = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = true
            border = BorderFactory.createEmptyBorder(3, 8, 3, 8)
            add(titleLabel)
            add(Box.createRigidArea(Dimension(4, 0)))

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

    private fun onNavigateRequested(rawUrl: String) {
        val url = normalizeUrl(rawUrl)
        val tab = tabManager.activeTab
        if (tab != null) {
            tab.navigate(url)
        } else {
            tabManager.createTab(url)
        }
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
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
