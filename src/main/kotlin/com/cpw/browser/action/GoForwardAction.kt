package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GoForwardAction(private val tabManager: BrowserTabManager) : AnAction("前进", "前进到下一页", WebBrowserIcons.Forward) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab?.canGoForward() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        tabManager.activeTab?.goForward()
    }
}
