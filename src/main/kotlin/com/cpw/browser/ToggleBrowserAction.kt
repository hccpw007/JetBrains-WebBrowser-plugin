package com.cpw.browser

import com.cpw.browser.editor.BrowserFileEditor
import com.cpw.browser.editor.BrowserVirtualFile
import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.util.WeakHashMap

class ToggleBrowserAction : AnAction(), DumbAware {

    companion object {
        private val virtualFiles = WeakHashMap<Project, BrowserVirtualFile>()
    }

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
        val settings = BrowserSettingsState.getInstance()

        if (settings.displayPosition == "editor") {
            toggleEditorMode(project)
        } else {
            toggleToolbarMode(project)
        }
    }

    private fun toggleEditorMode(project: Project) {
        val manager = FileEditorManager.getInstance(project)

        // 检查是否已有浏览器编辑器打开
        val existing = manager.allEditors.find { it is BrowserFileEditor }
        if (existing != null) {
            // 已打开，关闭它
            val vf = virtualFiles.remove(project)
            if (vf != null) manager.closeFile(vf)
        } else {
            // 未打开，新建
            val vf = BrowserVirtualFile()
            virtualFiles[project] = vf
            manager.openFile(vf, /* focusEditor = */ true)
        }
    }

    private fun toggleToolbarMode(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser") ?: return
        if (toolWindow.isVisible) {
            toolWindow.hide()
        } else {
            toolWindow.show()
        }
    }
}
