package com.cpw.browser.editor;

import com.cpw.browser.BrowserProjectService;
import com.cpw.browser.settings.BrowserSettingsState;
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

    // 新建编辑器标签页时的初始 URL（由 ToggleBrowserAction.createNewEditorTab 写入）
    public static final Key<String> INITIAL_URL_KEY = Key.create("BrowserEditorInitialUrl");

    // 浏览器工具窗口面板实例
    private final BrowserToolWindowPanel browserPanel;
    // 是否独占面板（编辑区独立标签页模式，关闭时需释放资源）
    private final boolean ownPanel;
    // 关联的虚拟文件
    private final VirtualFile file;
    // 属性变更监听器列表
    private final List<PropertyChangeListener> listeners = new ArrayList<>();
    // 用户数据持有者
    private final UserDataHolderBase userDataHolder = new UserDataHolderBase();

    public BrowserFileEditor(Project project, VirtualFile file) {
        this.file = file;
        // 根据 editorNewTabOnClick 设置决定使用独立面板还是共享面板
        if (BrowserSettingsState.getInstance().isEditorNewTabOnClick()) {
            // 编辑区独立标签页模式：每个编辑器创建独立面板，关闭时释放资源
            // 读取创建时写入的初始 URL（如点击书签新建系统 tab），非空则首个标签页直接导航
            String initialUrl = file.getUserData(INITIAL_URL_KEY);
            this.browserPanel = new BrowserToolWindowPanel(project, true, initialUrl);
            this.ownPanel = true;
        } else { // 维持现状：从项目级服务获取共享面板，确保关闭再打开编辑器时标签页不丢失
            this.browserPanel = BrowserProjectService.getInstance(project).getEditorPanel();
            this.ownPanel = false;
        }

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

    // 释放编辑器资源（独占面板模式下释放；共享面板由 BrowserProjectService 在项目关闭时统一清理）
    @Override
    public void dispose() {
        // 独占面板模式下释放浏览器资源
        if (ownPanel) {
            browserPanel.dispose();
        }
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
