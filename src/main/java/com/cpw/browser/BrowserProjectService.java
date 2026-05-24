package com.cpw.browser;

import com.cpw.browser.toolwindow.BrowserToolWindowPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

// 项目级服务，持有一个编辑器模式共享的 BrowserToolWindowPanel 实例
// 编辑器模式下关闭再打开 .webbrowser 文件时，复用同一个面板，保留所有标签页
public class BrowserProjectService implements Disposable {

    // 编辑器模式共享的浏览器面板
    private BrowserToolWindowPanel editorPanel;
    // 关联的项目
    private final Project project;

    public BrowserProjectService(Project project) {
        this.project = project;
    }

    // 获取当前项目的 BrowserProjectService 实例
    public static BrowserProjectService getInstance(@NotNull Project project) {
        return project.getService(BrowserProjectService.class);
    }

    // 获取编辑器模式共享的浏览器面板，同一项目中跨编辑器实例复用
    public synchronized BrowserToolWindowPanel getEditorPanel() {
        // 首次访问时创建面板
        if (editorPanel == null) {
            editorPanel = new BrowserToolWindowPanel(project);
        }
        return editorPanel;
    }

    // 项目关闭时释放浏览器资源
    @Override
    public void dispose() {
        if (editorPanel != null) {
            editorPanel.dispose();
            editorPanel = null;
        }
    }
}
