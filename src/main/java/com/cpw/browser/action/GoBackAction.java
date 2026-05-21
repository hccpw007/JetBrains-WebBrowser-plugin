package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 浏览器后退导航的 Action
public class GoBackAction extends AnAction {

    private final BrowserTabManager tabManager;

    // 构造后退导航 Action
    public GoBackAction(BrowserTabManager tabManager) {
        super("后退", "返回上一页", WebBrowserIcons.BACK);
        this.tabManager = tabManager;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当活动标签页可以后退时启用
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        e.getPresentation().setEnabled(activeTab != null && activeTab.canGoBack());
    }

    // 执行后退导航
    @Override
    public void actionPerformed(AnActionEvent e) {
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.goBack();
        }
    }
}
