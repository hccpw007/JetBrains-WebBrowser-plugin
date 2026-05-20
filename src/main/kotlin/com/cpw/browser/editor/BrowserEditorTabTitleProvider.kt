package com.cpw.browser.editor

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class BrowserEditorTabTitleProvider : EditorTabTitleProvider {

    override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
        if (file.fileType == BrowserFileType) {
            val title = file.getUserData(BrowserFileEditor.TITLE_KEY)
                ?: return "Web Browser"
            return if (title.length > 15) title.take(15) + "..." else title
        }
        return null
    }
}
