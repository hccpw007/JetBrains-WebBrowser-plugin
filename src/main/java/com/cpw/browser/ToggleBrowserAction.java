package com.cpw.browser;

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

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        BrowserSettingsState settings = BrowserSettingsState.getInstance();

        // 根据设置中的显示位置决定切换模式
        if ("editor".equals(settings.getDisplayPosition())) {
            toggleEditorMode(project);
        } else {
            toggleToolbarMode(project);
        }
    }

    // 切换到编辑区标签页模式
    private void toggleEditorMode(Project project) {
        // 确保侧边栏按钮隐藏
        hideToolWindowStrip(project);

        FileEditorManager manager = FileEditorManager.getInstance(project);

        // 检查是否已有浏览器编辑器打开
        boolean hasBrowserEditor = false;
        for (FileEditor fe : manager.getAllEditors()) {
            if (fe instanceof BrowserFileEditor) {
                hasBrowserEditor = true;
                break;
            }
        }

        if (hasBrowserEditor) {
            // 已打开，关闭它
            VirtualFile vf = projectFiles.remove(project);
            if (vf != null && vf.isValid()) {
                manager.closeFile(vf);
                File localFile = vf.toNioPath().toFile();
                if (localFile.exists()) localFile.delete();
            }
            return;
        }

        try {
            // 确保工具窗口已隐藏
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser");
            if (toolWindow != null && toolWindow.isVisible()) {
                toolWindow.hide();
            }

            // 创建临时文件作为标记，用于在编辑区打开浏览器
            File tempFile = new File(System.getProperty("java.io.tmpdir"), "Web Browser.webbrowser");
            if (tempFile.exists()) tempFile.delete();
            tempFile.createNewFile();
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), "WebBrowser");

            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
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
            if (fe instanceof BrowserFileEditor) {
                VirtualFile vf = projectFiles.remove(project);
                if (vf != null && vf.isValid()) {
                    manager.closeFile(vf);
                    vf.toNioPath().toFile().delete();
                }
                break;
            }
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser");
        if (toolWindow == null) return;

        if (toolWindow.isVisible()) {
            toolWindow.hide();
            toolWindow.setAvailable(false, null);
        } else {
            toolWindow.setAvailable(true, null);
            toolWindow.show();
        }
    }

    // 确保侧边栏按钮隐藏（编辑区模式切换时调用）
    private void hideToolWindowStrip(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebBrowser");
        if (toolWindow == null) return;
        if (toolWindow.isVisible()) toolWindow.hide();
        toolWindow.setAvailable(false, null);
    }
}
