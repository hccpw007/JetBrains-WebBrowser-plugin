package com.cpw.browser.editor;

import com.cpw.browser.toolwindow.BrowserToolWindowPanel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

// 文件编辑器集成，支持 .webbrowser 文件类型的内嵌浏览器编辑器
public class BrowserFileEditor implements FileEditor {

    public static final Key<String> TITLE_KEY = Key.create("BrowserEditorTitle");

    private final BrowserToolWindowPanel browserPanel;
    private final VirtualFile file;
    private final List<PropertyChangeListener> listeners = new ArrayList<>();
    private final UserDataHolderBase userDataHolder = new UserDataHolderBase();

    public BrowserFileEditor(Project project, VirtualFile file) {
        this.file = file;
        this.browserPanel = new BrowserToolWindowPanel(project);

        // 标题变更时同步到 VirtualFile 的 UserData，并通知 FileEditorManager 更新展示
        browserPanel.setOnTitleChanged(title -> {
            String current = file.getUserData(TITLE_KEY);
            if (!title.equals(current)) {
                file.putUserData(TITLE_KEY, title);
                FileEditorManager.getInstance(project).updateFilePresentation(file);
            }
        });
    }

    @Override
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public JComponent getComponent() {
        return browserPanel.getContent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return browserPanel.getContent();
    }

    @Override
    public @NotNull String getName() {
        return "WebBrowser";
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return userDataHolder.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        userDataHolder.putUserData(key, value);
    }

    @Override
    public void dispose() {
        browserPanel.dispose();
    }

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return (state, lvl) -> true;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // 无需恢复状态
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void selectNotify() {
        // 选中时无需操作
    }

    @Override
    public void deselectNotify() {
        // 取消选中时无需操作
    }
}
