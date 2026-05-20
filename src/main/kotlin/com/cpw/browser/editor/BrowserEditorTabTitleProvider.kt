package com.cpw.browser.editor

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class BrowserEditorTabTitleProvider : EditorTabTitleProvider {

    override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
        if (file.fileType == BrowserFileType) {
            return file.getUserData(BrowserFileEditor.TITLE_KEY)
                ?: "Web Browser"
        }
        return null
    }
}
