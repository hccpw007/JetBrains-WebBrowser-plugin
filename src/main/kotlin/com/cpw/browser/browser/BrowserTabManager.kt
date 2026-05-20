package com.cpw.browser.browser

import com.cpw.browser.toolwindow.BrowserTabPanel

class BrowserTabManager {

    private val tabs = mutableListOf<BrowserTabPanel>()
    var activeTabIndex: Int = -1
        private set

    val activeTab: BrowserTabPanel?
        get() = tabs.getOrNull(activeTabIndex)

    val tabCount: Int get() = tabs.size

    // 分离的回调：添加、移除、切换
    var onTabAdded: ((BrowserTabPanel) -> Unit)? = null
    var onTabRemoved: ((BrowserTabPanel) -> Unit)? = null
    var onActiveTabChanged: ((BrowserTabPanel?) -> Unit)? = null

    fun createTab(initialUrl: String = "about:blank"): BrowserTabPanel {
        val tab = BrowserTabPanel(initialUrl)

        // 标签内部状态变更 → 仅当是当前活动标签时才通知
        tab.onUrlChanged = { _ ->
            if (tabs.getOrNull(activeTabIndex) == tab) {
                onActiveTabChanged?.invoke(tab)
            }
        }
        tab.onTitleChanged = { _ ->
            if (tabs.getOrNull(activeTabIndex) == tab) {
                onActiveTabChanged?.invoke(tab)
            }
        }
        tab.onLoadingStateChanged = { _ ->
            if (tabs.getOrNull(activeTabIndex) == tab) {
                onActiveTabChanged?.invoke(tab)
            }
        }
        // 网页弹窗/新窗口 → 在当前插件中新建标签页
        tab.onPopupUrl = { url ->
            createTab(url)
        }

        tabs.add(tab)
        if (activeTabIndex == -1) {
            activeTabIndex = 0
        }
        onTabAdded?.invoke(tab)
        onActiveTabChanged?.invoke(tab)
        return tab
    }

    fun closeTab(index: Int): Boolean {
        if (index < 0 || index >= tabs.size) return tabs.isNotEmpty()

        // 如果是最后一个标签页，先创建一个空白标签页，确保 tab 栏不消失
        if (tabs.size == 1) {
            createTab()
        }

        val tab = tabs[index]
        tab.dispose()
        tabs.removeAt(index)
        onTabRemoved?.invoke(tab)

        if (tabs.isEmpty()) {
            activeTabIndex = -1
            onActiveTabChanged?.invoke(null)
            return false
        }

        if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        } else if (activeTabIndex > index) {
            activeTabIndex--
        }
        onActiveTabChanged?.invoke(activeTab)
        return true
    }

    fun switchToTab(index: Int): BrowserTabPanel? {
        if (index < 0 || index >= tabs.size) return null
        activeTabIndex = index
        onActiveTabChanged?.invoke(activeTab)
        return activeTab
    }

    fun getTabs(): List<BrowserTabPanel> = tabs.toList()

    fun disposeAll() {
        tabs.forEach { it.dispose() }
        tabs.clear()
        activeTabIndex = -1
    }
}
