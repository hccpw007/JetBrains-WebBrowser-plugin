package com.cpw.browser

import com.cpw.browser.editor.BrowserFileEditor
import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File
import java.util.WeakHashMap

class ToggleBrowserAction : AnAction(), DumbAware {

    companion object {
        private val projectFiles = WeakHashMap<Project, VirtualFile>()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
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
        // 确保侧边栏按钮隐藏
        hideToolWindowStrip(project)

        val manager = FileEditorManager.getInstance(project)

        // 检查是否已有浏览器编辑器打开
        val existing = manager.allEditors.find { it is BrowserFileEditor }

        if (existing != null) {
            // 已打开，关闭它
            val vf = projectFiles.remove(project)
            if (vf != null && vf.isValid) {
                manager.closeFile(vf)
                val localFile = vf.toNioPath().toFile()
                if (localFile.exists()) localFile.delete()
            }
            return
        }

        try {
            // 确保工具窗口已隐藏
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser")
            if (toolWindow != null && toolWindow.isVisible) {
                toolWindow.hide()
            }

            // 创建临时文件作为标记，用于在编辑区打开浏览器
            val tempFile = File.createTempFile("webbrowser", ".webbrowser")
            tempFile.deleteOnExit()
            tempFile.writeText("WebBrowser")

            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile)
            if (vf != null && vf.isValid) {
                projectFiles[project] = vf
                manager.openFile(vf, true)
            }
        } catch (ex: Exception) {
            // ignore
        }
    }

    private fun toggleToolbarMode(project: Project) {
        // 如果有浏览器编辑器标签页，先关闭它
        val manager = FileEditorManager.getInstance(project)
        val existingEditor = manager.allEditors.find { it is BrowserFileEditor }
        if (existingEditor != null) {
            projectFiles.remove(project)?.let { vf ->
                if (vf.isValid) {
                    manager.closeFile(vf)
                    vf.toNioPath().toFile().delete()
                }
            }
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser") ?: return
        if (toolWindow.isVisible) {
            toolWindow.hide()
            toolWindow.setAvailable(false, null)
        } else {
            toolWindow.setAvailable(true, null)
            toolWindow.show()
        }
    }

    // 工具方法: 确保侧边栏按钮隐藏（编辑区模式切换时调用）
    private fun hideToolWindowStrip(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser") ?: return
        if (toolWindow.isVisible) toolWindow.hide()
        toolWindow.setAvailable(false, null)
    }
}
