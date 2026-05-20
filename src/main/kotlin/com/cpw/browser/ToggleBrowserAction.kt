package com.cpw.browser

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class ToggleBrowserAction : AnAction(), DumbAware {

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser")
            e.presentation.isEnabled = toolWindow != null
        } else {
            e.presentation.isEnabled = false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser") ?: return
        if (toolWindow.isVisible) {
            toolWindow.hide()
        } else {
            toolWindow.show()
        }
    }
}
