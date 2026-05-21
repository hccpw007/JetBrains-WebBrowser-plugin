package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 刷新当前页面的 Action，刷新前会重置缩放
public class RefreshAction extends AnAction {

    // 标签页管理器，用于刷新当前标签页
    private final BrowserTabManager tabManager;
    // 缩放重置完成后的回调
    private final Runnable onAfterZoomReset;

    // 没有刷新后回调的构造
    public RefreshAction(BrowserTabManager tabManager) {
        this(tabManager, () -> {});
    }

    // 带刷新后回调的构造
    public RefreshAction(BrowserTabManager tabManager, Runnable onAfterZoomReset) {
        super("刷新", "重新加载当前页面", WebBrowserIcons.REFRESH);
        this.tabManager = tabManager;
        this.onAfterZoomReset = onAfterZoomReset;
    }

    @Override
    public void update(AnActionEvent e) {
        // 当存在活动标签页时启用
        e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 刷新前先重置缩放
        tabManager.zoomReset();
        onAfterZoomReset.run();
        BrowserTabPanel activeTab = tabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.refresh();
        }
    }
}
