// 浏览器工具面板的操作 Action 合集：缩放、书签侧边栏、系统打开、设置
package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.settings.BrowserSettingsPage;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.cpw.browser.ui.BookmarkSidebar;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

import javax.swing.JPanel;
import java.util.function.Consumer;

public final class PanelActions {

    private PanelActions() {
    }

    // 放大网页
    public static class ZoomIn extends AnAction implements DumbAware {

        // 标签页管理器
        private final BrowserTabManager tabManager;
        // 缩放提示回调（接收提示文本）
        private final Consumer<String> showZoomToast;

        public ZoomIn(BrowserTabManager tabManager, Consumer<String> showZoomToast) {
            super("放大", "放大网页 5%", WebBrowserIcons.ZOOM_IN);
            this.tabManager = tabManager;
            this.showZoomToast = showZoomToast;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomIn();
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            int pct = (int) ((activeTab != null ? activeTab.getZoomLevel() : 1.0) * 100);
            showZoomToast.accept("放大至" + pct + "%");
        }
    }

    // 缩小网页
    public static class ZoomOut extends AnAction implements DumbAware {

        // 标签页管理器
        private final BrowserTabManager tabManager;
        // 缩放提示回调
        private final Consumer<String> showZoomToast;

        public ZoomOut(BrowserTabManager tabManager, Consumer<String> showZoomToast) {
            super("缩小", "缩小网页 5%", WebBrowserIcons.ZOOM_OUT);
            this.tabManager = tabManager;
            this.showZoomToast = showZoomToast;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomOut();
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            int pct = (int) ((activeTab != null ? activeTab.getZoomLevel() : 1.0) * 100);
            showZoomToast.accept("缩小至" + pct + "%");
        }
    }

    // 书签侧边栏显示/隐藏切换
    public static class ToggleBookmarkSidebar extends AnAction implements DumbAware {

        // 书签侧边栏
        private final BookmarkSidebar bookmarkSidebar;
        // 居中面板
        private final JPanel centerPanel;

        public ToggleBookmarkSidebar(BookmarkSidebar bookmarkSidebar, JPanel centerPanel) {
            super("显示书签", "显示或隐藏书签侧边栏", WebBrowserIcons.SHOW_BOOKMARK);
            this.bookmarkSidebar = bookmarkSidebar;
            this.centerPanel = centerPanel;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            bookmarkSidebar.setVisible(!bookmarkSidebar.isVisible());
            centerPanel.revalidate();
            centerPanel.repaint();
            e.getPresentation().setDescription(
                    bookmarkSidebar.isVisible() ? "隐藏书签侧边栏" : "显示书签侧边栏"
            );
        }
    }

    // 在系统默认浏览器中打开
    public static class OpenInSystemBrowser extends AnAction implements DumbAware {

        // 标签页管理器
        private final BrowserTabManager tabManager;

        public OpenInSystemBrowser(BrowserTabManager tabManager) {
            super("系统浏览器打开", "在系统浏览器中打开当前网页", WebBrowserIcons.GOOGLE);
            this.tabManager = tabManager;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            if (url != null && !url.isBlank() && !"about:blank".equals(url)) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    // 静默失败，无状态栏可写
                }
            }
        }

        @Override
        public void update(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            e.getPresentation().setEnabled(url != null && !url.isBlank() && !"about:blank".equals(url));
        }
    }

    // 打开 WebBrowser 设置页
    public static class Settings extends AnAction implements DumbAware {

        // 当前项目
        private final Project project;

        public Settings(Project project) {
            super("设置", "打开 WebBrowser 设置", AllIcons.General.Settings);
            this.project = project;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, BrowserSettingsPage.class);
        }
    }
}
