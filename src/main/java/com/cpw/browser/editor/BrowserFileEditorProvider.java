package com.cpw.browser.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

// 浏览器面板的 FileEditorProvider，支持在编辑区显示浏览器
public class BrowserFileEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        // 仅接受浏览器文件类型
        return file.getFileType() == BrowserFileType.INSTANCE;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new BrowserFileEditor(project, file);
    }

    // 获取编辑器类型唯一标识
    @Override
    public @NotNull String getEditorTypeId() {
        return "webbrowser-editor";
    }

    // 获取编辑器策略：隐藏默认编辑器
    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
