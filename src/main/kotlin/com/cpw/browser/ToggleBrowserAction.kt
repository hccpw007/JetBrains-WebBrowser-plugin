package com.cpw.browser

import com.cpw.browser.editor.BrowserFileEditor
import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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

        Messages.showInfoMessage(
            "actionPerformed 被调用!\ndisplayPosition=${settings.displayPosition}",
            "调试 入口"
        )

        if (settings.displayPosition == "editor") {
            toggleEditorMode(project)
        } else {
            toggleToolbarMode(project)
        }
    }

    private fun toggleEditorMode(project: Project) {
        val manager = FileEditorManager.getInstance(project)

        Messages.showInfoMessage(
            "开始 toggleEditorMode\nallEditors.size=${manager.allEditors.size}",
            "调试 Step 1"
        )

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
            Messages.showInfoMessage("已关闭现有浏览器编辑器", "调试")
            return
        }

        try {
            // 创建临时文件作为标记，用于在编辑区打开浏览器
            val tempFile = File.createTempFile("webbrowser", ".webbrowser")
            tempFile.deleteOnExit()
            tempFile.writeText("WebBrowser")

            Messages.showInfoMessage(
                "临时文件已创建: ${tempFile.absolutePath}\n即将调用 refreshAndFindFileByIoFile",
                "调试 Step 2"
            )

            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile)
            if (vf != null && vf.isValid) {
                Messages.showInfoMessage(
                    "VirtualFile 创建成功: ${vf.path}\nextension=${vf.extension}\nfileType=${vf.fileType.name}",
                    "调试 Step 3"
                )
                projectFiles[project] = vf
                val editors = manager.openFile(vf, true)
                Messages.showInfoMessage(
                    "openFile 返回 ${editors.size} 个编辑器",
                    "调试 Step 4"
                )
            } else {
                Messages.showInfoMessage(
                    "refreshAndFindFileByIoFile 返回 null!\n文件路径: ${tempFile.absolutePath}",
                    "调试 错误"
                )
            }
        } catch (ex: Exception) {
            Messages.showInfoMessage("异常: ${ex.message}", "调试 异常")
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
