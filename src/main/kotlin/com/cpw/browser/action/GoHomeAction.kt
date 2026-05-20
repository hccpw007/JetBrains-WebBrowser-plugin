package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GoHomeAction(private val tabManager: BrowserTabManager) : AnAction("主页", "回到主页", WebBrowserIcons.Home) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = tabManager.activeTab != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val homeUrl = PropertiesComponent.getInstance().getValue(HOME_URL_KEY, DEFAULT_HOME_URL)
        tabManager.activeTab?.navigate(homeUrl ?: DEFAULT_HOME_URL)
    }

    companion object {
        const val HOME_URL_KEY = "webbrowser.home.url"
        const val DEFAULT_HOME_URL = "about:blank"
    }
}
