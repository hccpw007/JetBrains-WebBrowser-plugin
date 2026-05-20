package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CloseTabAction(private val tabManager: BrowserTabManager) : AnAction("关闭标签页", "关闭当前浏览器标签页", WebBrowserIcons.CloseTab) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.tabCount > 0
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (tabManager.tabCount > 0) {
            tabManager.closeTab(tabManager.activeTabIndex)
        }
    }
}
