package com.cpw.browser.action;

import com.cpw.browser.editor.BrowserFileEditor;
import com.cpw.browser.settings.BrowserSettingsState;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.WeakHashMap;

// 切换浏览器显示模式：编辑区标签页 或 工具窗口侧边栏
public class ToggleBrowserAction extends AnAction implements DumbAware {

    // 存储每个项目对应的浏览器编辑器临时文件
    private static final Map<Project, VirtualFile> projectFiles = new WeakHashMap<>();

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅在有打开的项目时可用
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    // 执行浏览器显示模式的切换操作
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // 项目为空时直接返回
        if (project == null) return;

        BrowserSettingsState settings = BrowserSettingsState.getInstance();

        // 根据设置中的显示位置决定切换模式
        if ("editor".equals(settings.getDisplayPosition())) {
            toggleEditorMode(project);
        } else { // 使用工具窗口侧边栏模式
            toggleToolbarMode(project);
        }
    }

    // 切换到编辑区标签页模式
    private void toggleEditorMode(Project project) {
        // 确保侧边栏按钮隐藏
        hideToolWindowStrip(project);

        FileEditorManager manager = FileEditorManager.getInstance(project);

        // 查找已打开的浏览器编辑器
        BrowserFileEditor existingEditor = null;
        for (FileEditor fe : manager.getAllEditors()) {
            // 找到已打开的浏览器编辑器实例
            if (fe instanceof BrowserFileEditor) {
                existingEditor = (BrowserFileEditor) fe;
                break;
            }
        }

        // 已存在浏览器编辑器时关闭并清理
        if (existingEditor != null) {
            // 直接从编辑器获取 VirtualFile 关闭（兼容 IDE 重启恢复会话后 projectFiles 为空的场景）
            VirtualFile vf = existingEditor.getFile();
            // 成功获取到有效文件时关闭编辑器
            if (vf != null && vf.isValid()) {
                manager.closeFile(vf);
                File localFile = vf.toNioPath().toFile();
                // 临时文件存在则删除
                if (localFile.exists()) localFile.delete();
            }
            projectFiles.remove(project);
            return;
        }

        try {
            // 创建临时文件作为标记，用于在编辑区打开浏览器
            File tempFile = new File(System.getProperty("java.io.tmpdir"), "Web Browser.webbrowser");
            // 临时文件已存在则删除重建
            if (tempFile.exists()) tempFile.delete();
            tempFile.createNewFile();
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), "WebBrowser");

            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
            // 成功获取到有效文件时在编辑区打开
            if (vf != null && vf.isValid()) {
                projectFiles.put(project, vf);
                manager.openFile(vf, true);
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    // 切换到工具窗口侧边栏模式
    private void toggleToolbarMode(Project project) {
        // 如果有浏览器编辑器标签页，先关闭它
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor fe : manager.getAllEditors()) {
            // 找到已打开的浏览器编辑器实例并关闭
            if (fe instanceof BrowserFileEditor) {
                VirtualFile vf = fe.getFile();
                // 成功获取到有效文件时关闭并删除临时文件
                if (vf != null && vf.isValid()) {
                    manager.closeFile(vf);
                    vf.toNioPath().toFile().delete();
                }
                projectFiles.remove(project);
                break;
            }
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser");

        // 工具窗口未初始化时直接返回
        if (toolWindow == null) return;

        // 根据工具窗口当前可见状态切换显隐
        if (toolWindow.isVisible()) {
            toolWindow.hide();
            toolWindow.setAvailable(false, null);
        } else { // 显示并激活工具窗口
            toolWindow.setAvailable(true, null);
            toolWindow.show();
        }
    }

    // 确保侧边栏按钮隐藏（编辑区模式切换时调用）
    private void hideToolWindowStrip(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser");

        // 工具窗口未初始化时直接返回
        if (toolWindow == null) return;
        // 工具窗口可见时将其隐藏
        if (toolWindow.isVisible()) toolWindow.hide();
        toolWindow.setAvailable(false, null);
    }
}
