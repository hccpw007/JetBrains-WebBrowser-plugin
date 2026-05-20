package com.cpw.browser.toolwindow

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.action.GoBackAction
import com.cpw.browser.bookmark.BookmarkPersistentState
import com.cpw.browser.action.GoForwardAction
import com.cpw.browser.action.GoHomeAction
import com.cpw.browser.action.OpenDevToolsAction
import com.cpw.browser.action.RefreshAction
import com.cpw.browser.browser.BrowserTabManager
import com.cpw.browser.settings.BrowserSettingsPage
import com.cpw.browser.settings.BrowserSettingsState
import com.cpw.browser.ui.AddressBar
import com.cpw.browser.ui.BookmarkSidebar
import com.cpw.browser.ui.ChromeTab
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.net.URLEncoder
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

class BrowserToolWindowPanel(private val project: Project) {

    private val tabManager = BrowserTabManager()
    private val addressBar = AddressBar(
        onNavigate = { rawUrl -> onNavigateRequested(rawUrl) },
        isUrlBookmarked = { url -> BookmarkPersistentState.getInstance().contains(url) },
        onToggleBookmark = { url -> toggleBookmark(url) }
    )
    private lateinit var bookmarkSidebar: BookmarkSidebar
    private val tabStripPanel = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = ChromeTab.STRIP_BG
            g2.fillRect(0, 0, width, height)
            g2.dispose()
        }
    }
    private val browserContentPanel = JPanel(BorderLayout()) // 浏览器内容区域
    private val zoomToast = object : JBLabel() {
        private val bgColor = JBColor(0x444444, 0xCCCCCC)

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            g2.composite = AlphaComposite.SrcOver.derive(0.65f)
            g2.color = bgColor
            g2.fillRoundRect(0, 0, width, height, 10, 10)
            g2.dispose()
            super.paintComponent(g)
        }
    }.apply {
        isOpaque = false
        foreground = JBColor(0xFFFFFF, 0x333333)
        font = font.deriveFont(Font.BOLD, 13f)
        border = BorderFactory.createEmptyBorder(6, 16, 6, 16)
        isVisible = false
    }
    private val browserLayer = object : JLayeredPane() {
        override fun doLayout() {
            val w = width
            val h = height
            browserContentPanel.setBounds(0, 0, w, h)
            if (zoomToast.isVisible) {
                val pref = zoomToast.preferredSize
                zoomToast.setBounds(
                    maxOf(0, (w - pref.width) / 2),
                    50,
                    pref.width,
                    pref.height
                )
            }
        }
    }
    private var zoomToastTimer: Timer? = null
    private val centerPanel = JPanel(BorderLayout()) // 居中区域：书签(可隐藏) + 浏览器内容
    private val statusLabel = JBLabel("就绪", SwingConstants.LEFT)
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val chromeTabs = mutableMapOf<BrowserTabPanel, ChromeTab>()
    private val addTabButton = JButton("+").apply {
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        font = font.deriveFont(16f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        toolTipText = "新建标签页"
        preferredSize = Dimension(28, ChromeTab.TAB_HEIGHT)
        maximumSize = Dimension(28, ChromeTab.TAB_HEIGHT)
        minimumSize = Dimension(28, ChromeTab.TAB_HEIGHT)
        addActionListener { tabManager.createTab() }
    }

    init {
        bookmarkSidebar = BookmarkSidebar { bookmark -> onBookmarkSelected(bookmark) }.apply {
            isVisible = false // 默认隐藏书签侧边栏
            val sideW = 200
            preferredSize = Dimension(sideW, 0)
            minimumSize = Dimension(sideW, 0)
            maximumSize = Dimension(sideW, Int.MAX_VALUE)
        }

        // 居中区域：[书签侧边栏(可隐藏)] [浏览器内容层(含 toast 叠加)]
        browserLayer.setLayer(browserContentPanel, JLayeredPane.DEFAULT_LAYER)
        browserLayer.setLayer(zoomToast, JLayeredPane.PALETTE_LAYER)
        browserLayer.add(browserContentPanel, JLayeredPane.DEFAULT_LAYER)
        browserLayer.add(zoomToast, JLayeredPane.PALETTE_LAYER)
        centerPanel.add(bookmarkSidebar, BorderLayout.WEST)
        centerPanel.add(browserLayer, BorderLayout.CENTER)

        // 标签页栏
        tabStripPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        tabStripPanel.border = BorderFactory.createEmptyBorder(4, 4, 0, 4)

        tabManager.onTabAdded = { tab -> addTabToStrip(tab) }
        tabManager.onTabRemoved = { tab ->
            removeTabFromStrip(tab)
            chromeTabs.remove(tab)
        }
        tabManager.onActiveTabChanged = { tab -> onActiveTabChanged(tab) }

        // 后退/前进/刷新/主页 mini 工具栏
        val navGroup = DefaultActionGroup().apply {
            add(GoBackAction(tabManager))
            add(GoForwardAction(tabManager))
            add(RefreshAction(tabManager) { showZoomToastForActiveTab("恢复至") })
            add(GoHomeAction(tabManager))
        }
        val navToolbar = ActionManager.getInstance()
            .createActionToolbar("WebBrowser.NavBar", navGroup, true)
        navToolbar.setTargetComponent(navToolbar.component)

        // url 右侧工具栏：放大、缩小、开发者工具、书签侧边栏切换
        val rightGroup = DefaultActionGroup().apply {
            add(ZoomInAction(tabManager))
            add(ZoomOutAction(tabManager))
            add(OpenDevToolsAction(tabManager))
            addSeparator()
            add(ToggleBookmarkSidebarAction())
            add(OpenInSystemBrowserAction())
            add(SettingsAction())
        }
        val rightToolbar = ActionManager.getInstance()
            .createActionToolbar("WebBrowser.RightActions", rightGroup, true)
        rightToolbar.setTargetComponent(rightToolbar.component)

        // 地址栏行：[导航按钮] [地址栏] [开发者工具/书签/新标签页]
        val navAddressBar = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(4, 0, 2, 0)
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

        tabStripPanel.add(addTabButton)
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

    // 放大 Action
    private inner class ZoomInAction(tabManager: BrowserTabManager) : AnAction(
        "放大", "放大网页 5%", WebBrowserIcons.ZoomIn
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            tabManager.zoomIn()
            showZoomToastForActiveTab("放大至")
        }
    }

    // 缩小 Action
    private inner class ZoomOutAction(tabManager: BrowserTabManager) : AnAction(
        "缩小", "缩小网页 5%", WebBrowserIcons.ZoomOut
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            tabManager.zoomOut()
            showZoomToastForActiveTab("缩小至")
        }
    }

    // 书签侧边栏切换 Action
    private inner class ToggleBookmarkSidebarAction : AnAction(
        "显示书签", "显示或隐藏书签侧边栏", WebBrowserIcons.ShowBookmark
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            bookmarkSidebar.isVisible = !bookmarkSidebar.isVisible
            centerPanel.revalidate()
            centerPanel.repaint()
            e.presentation.description = if (bookmarkSidebar.isVisible) "隐藏书签侧边栏" else "显示书签侧边栏"
        }
    }

    // 系统浏览器打开 Action
    private inner class OpenInSystemBrowserAction : AnAction(
        "系统浏览器打开", "在系统浏览器中打开当前网页", WebBrowserIcons.Google
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            val url = tabManager.activeTab?.currentUrl
            if (!url.isNullOrBlank() && url != "about:blank") {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                } catch (ex: Exception) {
                    statusLabel.text = "打开系统浏览器失败: ${ex.message}"
                }
            }
        }

        override fun update(e: AnActionEvent) {
            val url = tabManager.activeTab?.currentUrl
            e.presentation.isEnabled = !url.isNullOrBlank() && url != "about:blank"
        }
    }

    // 设置 Action
    private inner class SettingsAction : AnAction(
        "设置", "打开 WebBrowser 设置", com.intellij.icons.AllIcons.General.Settings
    ), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, BrowserSettingsPage::class.java)
        }
    }

    private fun addTabToStrip(tab: BrowserTabPanel) {
        val chromeTab = ChromeTab(
            browserTab = tab,
            onSelect = {
                val index = tabManager.getTabs().indexOf(tab)
                if (index >= 0) tabManager.switchToTab(index)
            },
            onClose = {
                val index = tabManager.getTabs().indexOf(tab)
                if (index >= 0) tabManager.closeTab(index)
            }
        )

        chromeTabs[tab] = chromeTab
        tabStripPanel.add(chromeTab, tabStripPanel.componentCount - 1)
        tabStripPanel.revalidate()
        tabStripPanel.repaint()
        updateTabStripHighlight()

        // 监听标题变更（即便非活跃标签也更新标题）
        val origTitleCb = tab.onTitleChanged
        tab.onTitleChanged = { title ->
            chromeTab.titleLabel.text = tab.getTabTitle()
            origTitleCb?.invoke(title)
        }
    }

    private fun removeTabFromStrip(tab: BrowserTabPanel) {
        val chromeTab = chromeTabs[tab] ?: return
        tabStripPanel.remove(chromeTab)
        tabStripPanel.revalidate()
        tabStripPanel.repaint()
        updateTabStripHighlight()
    }

    private fun updateTabStripHighlight() {
        val activeTab = tabManager.activeTab
        for ((tab, chromeTab) in chromeTabs) {
            chromeTab.isActive = tab == activeTab
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
        chromeTabs[tab]?.titleLabel?.text = tab.getTabTitle()
    }

    private fun showZoomToast(text: String) {
        zoomToast.text = text
        zoomToast.isVisible = true
        zoomToastTimer?.stop()
        browserLayer.revalidate()
        browserLayer.repaint()
        zoomToastTimer = Timer(1000) {
            zoomToast.isVisible = false
            browserLayer.repaint()
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun showZoomToastForActiveTab(action: String) {
        val pct = ((tabManager.activeTab?.zoomLevel ?: 1.0) * 100).toInt()
        showZoomToast("${action}${pct}%")
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
        if (trimmed.contains('.') && !trimmed.contains(' ')) return "https://$trimmed"
        return "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
