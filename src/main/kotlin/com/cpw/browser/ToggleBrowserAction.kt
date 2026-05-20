package com.cpw.browser

import com.cpw.browser.editor.BrowserFileEditor
import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
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
        private val LOG = Logger.getInstance(ToggleBrowserAction::class.java)
        private val projectFiles = WeakHashMap<Project, VirtualFile>()
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

        LOG.info("ToggleBrowserAction: displayPosition=${settings.displayPosition}")

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
        LOG.info("toggleEditorMode: existing editors count=${manager.allEditors.size}, found=${existing != null}")

        if (existing != null) {
            // 已打开，关闭它
            val vf = projectFiles.remove(project)
            if (vf != null && vf.isValid) {
                LOG.info("toggleEditorMode: closing file ${vf.path}")
                manager.closeFile(vf)
                val localFile = vf.toNioPath().toFile()
                if (localFile.exists()) localFile.delete()
            }
            return
        }

        try {
            // 创建临时文件作为标记，用于在编辑区打开浏览器
            val tempFile = File.createTempFile("webbrowser", ".webbrowser")
            tempFile.deleteOnExit()
            tempFile.writeText("WebBrowser")
            LOG.info("toggleEditorMode: created temp file ${tempFile.absolutePath}")

            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile)
            if (vf != null && vf.isValid) {
                projectFiles[project] = vf
                LOG.info("toggleEditorMode: opening file ${vf.path}, extension=${vf.extension}")
                val editors = manager.openFile(vf, true)
                LOG.info("toggleEditorMode: openFile returned ${editors.size} editors")
            } else {
                LOG.error("toggleEditorMode: refreshAndFindFileByIoFile returned null for ${tempFile.absolutePath}")
            }
        } catch (ex: Exception) {
            LOG.error("toggleEditorMode: exception", ex)
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
