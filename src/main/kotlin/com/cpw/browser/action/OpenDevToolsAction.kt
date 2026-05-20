package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenDevToolsAction(
    private val tabManager: BrowserTabManager,
    private val onOpenDevTools: () -> Unit = { tabManager.activeTab?.openDevTools() }
) : AnAction("开发者工具", "打开 Chrome DevTools", WebBrowserIcons.DevTools) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        onOpenDevTools()
    }
}
