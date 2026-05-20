package com.cpw.browser.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * 标记虚拟文件，用于在编辑区打开浏览器。
 */
class BrowserVirtualFile : LightVirtualFile("WebBrowser")

/**
 * 浏览器面板的 FileEditorProvider，支持在编辑区显示浏览器。
 */
class BrowserFileEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file is BrowserVirtualFile
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return BrowserFileEditor(project)
    }

    override fun getEditorTypeId(): String = "webbrowser-editor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
