package com.cpw.browser.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NavigateToAction(
    private val onFocusAddressBar: () -> Unit
) : AnAction("导航到网址", "聚焦地址栏输入网址", null) {

    override fun actionPerformed(e: AnActionEvent) {
        onFocusAddressBar()
    }
}
