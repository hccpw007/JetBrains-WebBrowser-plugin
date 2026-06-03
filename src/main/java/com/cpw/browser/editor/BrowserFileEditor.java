package com.cpw.browser.editor;

import com.cpw.browser.BrowserProjectService;
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

    // 浏览器工具窗口面板实例
    private final BrowserToolWindowPanel browserPanel;
    // 关联的虚拟文件
    private final VirtualFile file;
    // 属性变更监听器列表
    private final List<PropertyChangeListener> listeners = new ArrayList<>();
    // 用户数据持有者
    private final UserDataHolderBase userDataHolder = new UserDataHolderBase();

    public BrowserFileEditor(Project project, VirtualFile file) {
        this.file = file;
        // 从项目级服务获取共享的浏览器面板，确保关闭再打开编辑器时标签页不丢失
        this.browserPanel = BrowserProjectService.getInstance(project).getEditorPanel();

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
        return "Web Browser";
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

    // 释放编辑器资源（共享面板由 BrowserProjectService 在项目关闭时统一清理，此处不做释放）
    @Override
    public void dispose() {
        // 不移除共享面板中的标签页，保留状态供下次编辑器打开时复用
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
