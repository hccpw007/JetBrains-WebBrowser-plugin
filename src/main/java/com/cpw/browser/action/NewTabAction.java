package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// 创建新浏览器标签页的 Action
public class NewTabAction extends AnAction {

    private final BrowserTabManager tabManager;

    public NewTabAction(BrowserTabManager tabManager) {
        super("新建标签页", "打开一个新的浏览器标签页", WebBrowserIcons.NEW_TAB);
        this.tabManager = tabManager;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        tabManager.createTab();
    }
}
