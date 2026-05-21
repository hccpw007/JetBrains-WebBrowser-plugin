package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 打开 Chrome DevTools 开发者工具的 Action
public class OpenDevToolsAction extends AnAction {

    private final BrowserTabManager tabManager;
    private final Runnable onOpenDevTools;

    // 使用默认 DevTools 打开逻辑
    public OpenDevToolsAction(BrowserTabManager tabManager) {
        this(tabManager, () -> {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            if (activeTab != null) {
                activeTab.openDevTools();
            }
        });
    }

    // 使用自定义 DevTools 打开逻辑
    public OpenDevToolsAction(BrowserTabManager tabManager, Runnable onOpenDevTools) {
        super("开发者工具", "打开 Chrome DevTools", WebBrowserIcons.DEV_TOOLS);
        this.tabManager = tabManager;
        this.onOpenDevTools = onOpenDevTools;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当存在活动标签页时启用
        e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        onOpenDevTools.run();
    }
}
