package com.cpw.browser.toolwindow

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.action.GoBackAction
import com.cpw.browser.action.GoForwardAction
import com.cpw.browser.action.GoHomeAction
import com.cpw.browser.action.RefreshAction
import com.cpw.browser.bookmark.Bookmark
import com.cpw.browser.browser.BrowserTabManager
import com.cpw.browser.ui.AddressBar
import com.cpw.browser.ui.BookmarkSidebar
import com.cpw.browser.ui.BrowserToolbar
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.net.URLEncoder
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants

class BrowserToolWindowPanel(private val project: Project) {

    private val tabManager = BrowserTabManager()
    private val addressBar = AddressBar { rawUrl -> onNavigateRequested(rawUrl) }
    private lateinit var bookmarkSidebar: BookmarkSidebar
    private lateinit var toolbar: BrowserToolbar
    private val tabPane = JTabbedPane()
    private val statusLabel = JBLabel("就绪", SwingConstants.LEFT)
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    // 标题标签缓存，key 为 BrowserTabPanel 引用
    private val tabTitleLabels = mutableMapOf<BrowserTabPanel, JBLabel>()

    init {
        bookmarkSidebar = BookmarkSidebar { bookmark -> onBookmarkSelected(bookmark) }

        toolbar = BrowserToolbar(
            tabManager = tabManager,
            onBookmarkChanged = { bookmarkSidebar.refreshBookmarks() },
            getSelectedBookmarkUrl = { bookmarkSidebar.getSelectedBookmarkUrl() }
        )

        tabManager.onTabAdded = { tab -> addTabToPane(tab) }
        tabManager.onTabRemoved = { tab ->
            tabTitleLabels.remove(tab)
            val index = getTabIndex(tab)
            if (index >= 0) tabPane.removeTabAt(index)
        }
        tabManager.onActiveTabChanged = { tab -> onActiveTabChanged(tab) }

        tabPane.addChangeListener {
            val index = tabPane.selectedIndex
            if (index >= 0 && index != tabManager.activeTabIndex) {
                tabManager.switchToTab(index)
            }
        }

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

        // 地址栏行
        val navAddressBar = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(navToolbar.component, BorderLayout.WEST)
            add(addressBar, BorderLayout.CENTER)
        }

        // 布局
        val topPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(navAddressBar, BorderLayout.SOUTH)
        }

        val sidebarSplitter = JBSplitter(false, 0.2f)
        sidebarSplitter.firstComponent = bookmarkSidebar
        sidebarSplitter.secondComponent = tabPane

        mainPanel.add(topPanel, BorderLayout.NORTH)
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

    private fun addTabToPane(tab: BrowserTabPanel) {
        val titleLabel = JBLabel(tab.getTabTitle())
        tabTitleLabels[tab] = titleLabel

        val closeBtn = JButton(WebBrowserIcons.CloseTab).apply {
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            toolTipText = "关闭标签页"
            addActionListener {
                for (i in 0 until tabPane.tabCount) {
                    if (tabPane.getComponentAt(i) == tab.component) {
                        tabManager.closeTab(i)
                        return@addActionListener
                    }
                }
            }
        }

        val tabComponent = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = javax.swing.border.EmptyBorder(0, 0, 0, 0)
            add(titleLabel)
            add(Box.createHorizontalGlue())
            add(closeBtn)
        }

        tabPane.addTab(null, tab.component)
        tabPane.setTabComponentAt(tabPane.tabCount - 1, tabComponent)
        tabPane.selectedIndex = tabPane.tabCount - 1
    }

    private fun onActiveTabChanged(tab: BrowserTabPanel?) {
        if (tab != null) {
            addressBar.setUrl(tab.currentUrl)
            statusLabel.text = if (tab.isLoading) "加载中..." else "就绪"
            updateTabPaneSelection(tab)
            updateTabTitle(tab)
        } else {
            addressBar.setUrl("")
            statusLabel.text = "就绪"
        }
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

    private fun updateTabPaneSelection(tab: BrowserTabPanel) {
        for (i in 0 until tabPane.tabCount) {
            if (tabPane.getComponentAt(i) == tab.component) {
                if (tabPane.selectedIndex != i) {
                    tabPane.selectedIndex = i
                }
                return
            }
        }
    }

    private fun updateTabTitle(tab: BrowserTabPanel) {
        tabTitleLabels[tab]?.text = tab.getTabTitle()
    }

    private fun getTabIndex(tab: BrowserTabPanel): Int {
        for (i in 0 until tabPane.tabCount) {
            if (tabPane.getComponentAt(i) == tab.component) return i
        }
        return -1
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
        if (trimmed.contains('.') && !trimmed.contains(' ')) return "https://$trimmed"
        return "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
