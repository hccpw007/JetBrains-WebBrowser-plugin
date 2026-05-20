package com.cpw.browser.action

import com.cpw.browser.WebBrowserIcons
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NewTabAction(private val tabManager: BrowserTabManager) : AnAction("新建标签页", "打开一个新的浏览器标签页", WebBrowserIcons.NewTab) {

    override fun actionPerformed(e: AnActionEvent) {
        tabManager.createTab()
    }
}
