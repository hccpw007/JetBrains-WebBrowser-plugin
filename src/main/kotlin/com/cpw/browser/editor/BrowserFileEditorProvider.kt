package com.cpw.browser.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * 标记虚拟文件，用于在编辑区打开浏览器。
 */
class BrowserVirtualFile : VirtualFile() {
    override fun getName(): String = "WebBrowser"
    override fun getPath(): String = "webbrowser://WebBrowser"
    override fun getUrl(): String = "webbrowser://WebBrowser"
    override fun getParent(): VirtualFile? = null
    override fun getChildren(): Array<VirtualFile> = EMPTY_ARRAY
    override fun getFileSystem(): VirtualFileSystem = BrowserFileSystem
    override fun isValid(): Boolean = true
    override fun isDirectory(): Boolean = false
    override fun isWritable(): Boolean = true
    override fun getLength(): Long = 0L
    override fun getTimeStamp(): Long = -1L
    override fun contentsToByteArray(): ByteArray = ByteArray(0)
    override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

    override fun getOutputStream(
        requestor: Any?,
        newModificationStamp: Long,
        newTimeStamp: Long
    ): OutputStream = throw IOException("BrowserVirtualFile is read-only")

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    private object BrowserFileSystem : VirtualFileSystem() {
        override fun getProtocol(): String = "webbrowser"
        override fun findFileByPath(path: String): VirtualFile? = null
        override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
        override fun refresh(asynchronous: Boolean) {}
        override fun isReadOnly(): Boolean = false

        override fun addVirtualFileListener(listener: VirtualFileListener) {}
        override fun removeVirtualFileListener(listener: VirtualFileListener) {}

        override fun deleteFile(requestor: Any?, file: VirtualFile) {}
        override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {}
        override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) {}
        override fun createChildFile(requestor: Any?, dir: VirtualFile, fileName: String): VirtualFile =
            throw IOException("Not supported")
        override fun createChildDirectory(requestor: Any?, dir: VirtualFile, dirName: String): VirtualFile =
            throw IOException("Not supported")
        override fun copyFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile =
            throw IOException("Not supported")
    }
}

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
