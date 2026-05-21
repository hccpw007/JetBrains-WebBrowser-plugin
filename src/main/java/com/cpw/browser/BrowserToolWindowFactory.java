package com.cpw.browser;

import com.cpw.browser.toolwindow.BrowserToolWindowPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.jetbrains.annotations.NotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

// 浏览器工具窗口工厂，负责创建工具窗口内容并注册键盘快捷键
public class BrowserToolWindowFactory implements ToolWindowFactory {

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        BrowserToolWindowPanel panel = new BrowserToolWindowPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);

        // 初始隐藏侧边栏按钮，由 ToggleBrowserAction 控制
        toolWindow.setAvailable(false, null);

        // 注册键盘快捷键
        registerShortcuts(panel, toolWindow);

        // 关闭工具窗口时释放所有 JCEF 资源
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                panel.dispose();
            }
        });
    }

    private void registerShortcuts(BrowserToolWindowPanel panel, ToolWindow toolWindow) {
        // 获取主机组件用于注册快捷键
        JComponent component = panel.getContent();

        // Ctrl+L — 聚焦地址栏
        AnAction focusAddressBarAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                panel.focusAddressBar();
            }
        };
        focusAddressBarAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK)),
                component
        );

        // Ctrl+Shift+I — 打开开发者工具
        AnAction openDevToolsAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                panel.openDevTools();
            }
        };
        openDevToolsAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
                component
        );
    }
}
