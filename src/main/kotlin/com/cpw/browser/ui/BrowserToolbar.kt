package com.cpw.browser.ui

import com.cpw.browser.action.*
import com.cpw.browser.browser.BrowserTabManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent

class BrowserToolbar(
    private val tabManager: BrowserTabManager,
    private val onBookmarkChanged: () -> Unit,
    private val getSelectedBookmarkUrl: () -> String?
) {

    val component: JComponent

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(OpenDevToolsAction(tabManager))
            add(AddBookmarkAction(tabManager, onBookmarkChanged))
            addSeparator()
            add(NewTabAction(tabManager))
            add(CloseTabAction(tabManager))
        }

        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("WebBrowser.Toolbar", actionGroup, true)
        component = actionToolbar.component
    }
}
