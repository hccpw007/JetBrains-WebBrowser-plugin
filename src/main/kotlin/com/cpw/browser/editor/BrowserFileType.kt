package com.cpw.browser.editor

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * 浏览器编辑器标签页的文件类型，用于在编辑区打开浏览器面板。
 */
object BrowserFileType : FileType {
    private val TAB_ICON = IconLoader.getIcon("/icons/webbrowser_tab.svg", BrowserFileType::class.java)

    override fun getName() = "WebBrowser"
    override fun getDescription() = "Web Browser"
    override fun getDefaultExtension() = "webbrowser"
    override fun isBinary() = false
    override fun getIcon(): Icon? = TAB_ICON
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}
