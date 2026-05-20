package com.cpw.browser.editor

import com.cpw.browser.toolwindow.BrowserToolWindowPanel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class BrowserFileEditor(project: Project, private val file: VirtualFile) : FileEditor {

    companion object {
        val TITLE_KEY = Key<String>("BrowserEditorTitle")
    }

    private val browserPanel = BrowserToolWindowPanel(project)
    private val listeners = mutableListOf<PropertyChangeListener>()
    private val userDataHolder = UserDataHolderBase()

    init {
        browserPanel.onTitleChanged = { title ->
            val current = file.getUserData(TITLE_KEY)
            if (title != current) {
                file.putUserData(TITLE_KEY, title)
                FileEditorManager.getInstance(project).updateFilePresentation(file)
            }
        }
    }

    override fun getComponent(): JComponent = browserPanel.getContent()
    override fun getPreferredFocusedComponent(): JComponent? = browserPanel.getContent()
    override fun getName(): String = "WebBrowser"
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true

    override fun <T> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)
    override fun <T> putUserData(key: Key<T>, value: T?) { userDataHolder.putUserData(key, value) }

    override fun dispose() {
        browserPanel.dispose()
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState { _, _ -> true }
    override fun setState(state: FileEditorState) {}

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        listeners.add(listener)
    }
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        listeners.remove(listener)
    }

    override fun selectNotify() {}
    override fun deselectNotify() {}
}
