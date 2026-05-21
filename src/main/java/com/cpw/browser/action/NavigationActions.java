// 浏览器导航相关 Action 合集：后退、前进、主页、刷新、新标签页、DevTools
package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.settings.BrowserSettingsState;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public final class NavigationActions {

    private NavigationActions() {
    }

    // 后退导航
    public static class GoBack extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public GoBack(BrowserTabManager tabManager) {
            super("后退", "返回上一页", WebBrowserIcons.BACK);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            e.getPresentation().setEnabled(activeTab != null && activeTab.canGoBack());
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            if (activeTab != null) {
                activeTab.goBack();
            }
        }
    }

    // 前进导航
    public static class GoForward extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public GoForward(BrowserTabManager tabManager) {
            super("前进", "前进到下一页", WebBrowserIcons.FORWARD);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            e.getPresentation().setEnabled(activeTab != null && activeTab.canGoForward());
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            if (activeTab != null) {
                activeTab.goForward();
            }
        }
    }

    // 主页导航
    public static class GoHome extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public GoHome(BrowserTabManager tabManager) {
            super("主页", "回到主页", WebBrowserIcons.HOME);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserSettingsState settings = BrowserSettingsState.getInstance();
            String homeUrl = settings.getHomePageUrl().isBlank() ? "about:blank" : settings.getHomePageUrl();
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            if (activeTab != null) {
                activeTab.navigate(homeUrl);
            }
        }
    }

    // 刷新页面
    public static class Refresh extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public Refresh(BrowserTabManager tabManager) {
            super("刷新", "重新加载当前页面", WebBrowserIcons.REFRESH);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            if (activeTab != null) {
                activeTab.refresh();
            }
        }
    }

    // 新建标签页
    public static class NewTab extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public NewTab(BrowserTabManager tabManager) {
            super("新建标签页", "打开一个新的浏览器标签页", WebBrowserIcons.NEW_TAB);
            this.tabManager = tabManager;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.createTab();
        }
    }

    // 打开开发者工具
    public static class OpenDevTools extends AnAction {

        // 标签页管理器
        private final BrowserTabManager tabManager;
        // 开发者工具打开回调
        private final Runnable onOpenDevTools;

        public OpenDevTools(BrowserTabManager tabManager, Runnable onOpenDevTools) {
            super("开发者工具", "打开 Chrome DevTools", WebBrowserIcons.DEV_TOOLS);
            this.tabManager = tabManager;
            this.onOpenDevTools = onOpenDevTools;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            onOpenDevTools.run();
        }
    }
}
