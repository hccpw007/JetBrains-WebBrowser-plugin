package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RefreshAction(private val tabManager: BrowserTabManager) : AnAction("刷新", "重新加载当前页面", WebBrowserIcons.Refresh) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        tabManager.zoomReset()
        tabManager.activeTab?.refresh()
    }
}
