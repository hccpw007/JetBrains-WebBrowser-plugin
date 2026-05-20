package com.cpw.browser.editor

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * 浏览器编辑器标签页的文件类型，用于在编辑区打开浏览器面板。
 */
object BrowserFileType : FileType {
    override fun getName() = "WebBrowser"
    override fun getDescription() = "Web Browser"
    override fun getDefaultExtension() = "webbrowser"
    override fun isBinary() = false
    override fun getIcon(): Icon? = null
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}
