package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GoBackAction(private val tabManager: BrowserTabManager) : AnAction("后退", "返回上一页", WebBrowserIcons.Back) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab?.canGoBack() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        tabManager.activeTab?.goBack()
    }
}
