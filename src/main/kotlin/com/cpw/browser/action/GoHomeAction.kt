package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GoHomeAction(private val tabManager: BrowserTabManager) : AnAction("主页", "回到主页", WebBrowserIcons.Home) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val settings = BrowserSettingsState.getInstance()
        val homeUrl = settings.homePageUrl.ifBlank { "about:blank" }
        tabManager.activeTab?.navigate(homeUrl)
    }
}
