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

    // 编辑器标题的用户数据 Key
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
            // 标题变更时才更新 VirtualFile 并通知刷新展示
            if (!title.equals(current)) {
                file.putUserData(TITLE_KEY, title);
                FileEditorManager.getInstance(project).updateFilePresentation(file);
            }
        });
    }

    // 获取关联的虚拟文件
    @Override
    public VirtualFile getFile() {
        return file;
    }

    // 获取浏览器面板的 Swing 组件
    @Override
    public JComponent getComponent() {
        return browserPanel.getContent();
    }

    // 获取首选焦点组件
    @Override
    public JComponent getPreferredFocusedComponent() {
        return browserPanel.getContent();
    }

    // 获取编辑器名称
    @Override
    public @NotNull String getName() {
        return "WebBrowser";
    }

    // 编辑器内容从未被外部修改
    @Override
    public boolean isModified() {
        return false;
    }

    // 编辑器始终有效
    @Override
    public boolean isValid() {
        return true;
    }

    // 获取用户数据
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return userDataHolder.getUserData(key);
    }

    // 设置用户数据
    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        userDataHolder.putUserData(key, value);
    }

    // 释放浏览器面板资源
    @Override
    public void dispose() {
        browserPanel.dispose();
    }

    // 获取编辑器状态
    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return (state, lvl) -> true;
    }

    // 恢复编辑器状态（无状态需要恢复）
    @Override
    public void setState(@NotNull FileEditorState state) {
        // 无需恢复状态
    }

    // 添加属性变更监听器
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        listeners.add(listener);
    }

    // 移除属性变更监听器
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    // 编辑器被选中时回调
    @Override
    public void selectNotify() {
        // 选中时无需操作
    }

    // 编辑器取消选中时回调
    @Override
    public void deselectNotify() {
        // 取消选中时无需操作
    }
}
