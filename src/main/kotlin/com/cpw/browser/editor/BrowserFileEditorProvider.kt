package com.cpw.browser.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 浏览器面板的 FileEditorProvider，支持在编辑区显示浏览器。
 */
class BrowserFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == BrowserFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return BrowserFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "webbrowser-editor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
