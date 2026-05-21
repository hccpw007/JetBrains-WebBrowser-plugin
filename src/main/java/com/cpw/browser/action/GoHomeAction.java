package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.settings.BrowserSettingsState;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 导航到主页的 Action
public class GoHomeAction extends AnAction {

    private final BrowserTabManager tabManager;

    public GoHomeAction(BrowserTabManager tabManager) {
        super("主页", "回到主页", WebBrowserIcons.HOME);
        this.tabManager = tabManager;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当存在活动标签页时启用
        e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        BrowserSettingsState settings = BrowserSettingsState.getInstance();
        // 如果主页 URL 为空则使用 about:blank
        String homeUrl = settings.getHomePageUrl().isBlank() ? "about:blank" : settings.getHomePageUrl();
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.navigate(homeUrl);
        }
    }
}
