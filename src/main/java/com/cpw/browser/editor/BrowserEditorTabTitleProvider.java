package com.cpw.browser.editor;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

// 浏览器编辑器标签页标题提供者，截断过长标题并追加省略号
public class BrowserEditorTabTitleProvider implements EditorTabTitleProvider {

    // 获取编辑器标签页标题，超过 15 个字符时截断
    @Override
    public String getEditorTabTitle(Project project, VirtualFile file) {
        // 仅处理浏览器文件类型
        if (file.getFileType() == BrowserFileType.INSTANCE) {
            String title = file.getUserData(BrowserFileEditor.TITLE_KEY);
            // 无标题时返回默认值
            if (title == null) {
                return "Web Browser";
            }
            // 标题超过 15 个字符时截断并追加"..."
            return title.length() > 15 ? title.substring(0, 15) + "..." : title;
        }
        return null;
    }
}
