package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 浏览器前进导航的 Action
public class GoForwardAction extends AnAction {

    private final BrowserTabManager tabManager;

    // 构造前进导航 Action
    public GoForwardAction(BrowserTabManager tabManager) {
        super("前进", "前进到下一页", WebBrowserIcons.FORWARD);
        this.tabManager = tabManager;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当活动标签页可以前进时启用
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        e.getPresentation().setEnabled(activeTab != null && activeTab.canGoForward());
    }

    // 执行前进导航
    @Override
    public void actionPerformed(AnActionEvent e) {
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.goForward();
        }
    }
}
