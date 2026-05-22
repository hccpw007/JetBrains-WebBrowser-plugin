// 浏览器工具面板的操作 Action 合集：缩放、书签侧边栏、系统打开、设置
package com.cpw.browser.action;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.settings.BrowserSettingsPage;
import com.cpw.browser.toolwindow.BrowserTabManager;
import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.cpw.browser.ui.BookmarkSidebar;
import com.cpw.browser.util.TranslationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
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

        private final BrowserTabManager tabManager;
        private final Consumer<String> showZoomToast;

        public ZoomIn(BrowserTabManager tabManager, Consumer<String> showZoomToast) {
            super(TranslationUtil.getText("action.zoom.in"), TranslationUtil.getText("action.zoom.in.desc"), WebBrowserIcons.ZOOM_IN);
            this.tabManager = tabManager;
            this.showZoomToast = showZoomToast;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.zoom.in"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.zoom.in.desc"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomIn();
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            int pct = (int) Math.round((activeTab != null ? activeTab.getZoomLevel() : 1.0) * 100);
            showZoomToast.accept(TranslationUtil.getText("zoom.toast.in", String.valueOf(pct)));
        }
    }

    // 缩小网页
    public static class ZoomOut extends AnAction implements DumbAware {

        private final BrowserTabManager tabManager;
        private final Consumer<String> showZoomToast;

        public ZoomOut(BrowserTabManager tabManager, Consumer<String> showZoomToast) {
            super(TranslationUtil.getText("action.zoom.out"), TranslationUtil.getText("action.zoom.out.desc"), WebBrowserIcons.ZOOM_OUT);
            this.tabManager = tabManager;
            this.showZoomToast = showZoomToast;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.zoom.out"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.zoom.out.desc"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomOut();
            BrowserTabPanel activeTab = tabManager.getActiveTab();
            int pct = (int) Math.round((activeTab != null ? activeTab.getZoomLevel() : 1.0) * 100);
            showZoomToast.accept(TranslationUtil.getText("zoom.toast.out", String.valueOf(pct)));
        }
    }

    // 重置缩放
    public static class ZoomReset extends AnAction implements DumbAware {

        private final BrowserTabManager tabManager;
        private final Consumer<String> showZoomToast;

        public ZoomReset(BrowserTabManager tabManager, Consumer<String> showZoomToast) {
            super(TranslationUtil.getText("action.zoom.reset"), TranslationUtil.getText("action.zoom.reset.desc"), WebBrowserIcons.ZOOM_RESET);
            this.tabManager = tabManager;
            this.showZoomToast = showZoomToast;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.zoom.reset"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.zoom.reset.desc"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            tabManager.zoomReset();
            showZoomToast.accept(TranslationUtil.getText("zoom.toast.reset"));
        }
    }

    // 书签侧边栏显示/隐藏切换
    public static class ToggleBookmarkSidebar extends AnAction implements DumbAware {

        private final BookmarkSidebar bookmarkSidebar;
        private final JPanel centerPanel;

        public ToggleBookmarkSidebar(BookmarkSidebar bookmarkSidebar, JPanel centerPanel) {
            super(TranslationUtil.getText("action.show.bookmark"), TranslationUtil.getText("action.show.bookmark.desc"), WebBrowserIcons.SHOW_BOOKMARK);
            this.bookmarkSidebar = bookmarkSidebar;
            this.centerPanel = centerPanel;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.show.bookmark"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.show.bookmark.desc"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            bookmarkSidebar.setVisible(!bookmarkSidebar.isVisible());
            centerPanel.revalidate();
            centerPanel.repaint();
        }
    }

    // "更多"弹出菜单
    public static class MoreMenu extends DefaultActionGroup {

        public MoreMenu(Project project, BrowserTabManager tabManager, Runnable openDevTools) {
            super(TranslationUtil.getText("action.more"), true);
            getTemplatePresentation().setIcon(WebBrowserIcons.MORE);
            getTemplatePresentation().setDescription(TranslationUtil.getText("action.more.desc"));

            add(new Settings(project));
            addSeparator();
            add(new NavigationActions.OpenDevTools(tabManager, openDevTools));
            add(new AutoRefresh(tabManager));
            add(new ClearCache(tabManager));
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.more"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.more.desc"));
        }
    }

    // 定时刷新当前页面
    public static class AutoRefresh extends AnAction implements DumbAware {

        private final BrowserTabManager tabManager;

        public AutoRefresh(BrowserTabManager tabManager) {
            super(TranslationUtil.getText("action.auto.refresh"), TranslationUtil.getText("action.auto.refresh.desc"), AllIcons.Actions.Refresh);
            this.tabManager = tabManager;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            if (tab == null) return;
            boolean enabled = !tab.isAutoRefreshEnabled();
            if (enabled) {
                String input = Messages.showInputDialog(
                        TranslationUtil.getText("auto.refresh.prompt"),
                        TranslationUtil.getText("auto.refresh.title"), null, "30", null);
                if (input == null) return;
                try {
                    int seconds = Integer.parseInt(input.trim());
                    if (seconds < 1) return;
                    tab.setAutoRefreshInterval(seconds);
                } catch (NumberFormatException ex) {
                    return;
                }
                tab.setAutoRefresh(true);
                e.getPresentation().setText(TranslationUtil.getText("action.stop.refresh"));
                e.getPresentation().setDescription(
                        TranslationUtil.getText("action.stop.refresh.desc") + "(" + tab.getAutoRefreshInterval() + "s)");
            } else {
                int result = Messages.showYesNoDialog(
                        TranslationUtil.getText("auto.refresh.stop.confirm"),
                        TranslationUtil.getText("auto.refresh.stop.title"), null);
                if (result != Messages.YES) return;
                tab.setAutoRefresh(false);
                e.getPresentation().setText(TranslationUtil.getText("action.auto.refresh"));
                e.getPresentation().setDescription(TranslationUtil.getText("action.auto.refresh.desc"));
            }
        }

        @Override
        public void update(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            boolean enabled = tab != null;
            e.getPresentation().setEnabled(enabled);
            if (enabled) {
                boolean running = tab.isAutoRefreshEnabled();
                e.getPresentation().setText(running
                        ? TranslationUtil.getText("action.stop.refresh")
                        : TranslationUtil.getText("action.auto.refresh"));
                e.getPresentation().setDescription(running
                        ? TranslationUtil.getText("action.stop.refresh.desc") + "(" + tab.getAutoRefreshInterval() + "s)"
                        : TranslationUtil.getText("action.auto.refresh.desc"));
            } else {
                e.getPresentation().setText(TranslationUtil.getText("action.auto.refresh"));
                e.getPresentation().setDescription(TranslationUtil.getText("action.auto.refresh.desc"));
            }
        }
    }

    // 清空浏览器缓存和 Cookie
    public static class ClearCache extends AnAction implements DumbAware {

        private final BrowserTabManager tabManager;

        public ClearCache(BrowserTabManager tabManager) {
            super(TranslationUtil.getText("action.clear.cache"), TranslationUtil.getText("action.clear.cache.desc"), AllIcons.Actions.GC);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.clear.cache"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.clear.cache.desc"));
            e.getPresentation().setEnabled(tabManager.getActiveTab() != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            if (tab == null) return;
            tab.clearCache();
            tab.refresh();
        }
    }

    // 在系统默认浏览器中打开
    public static class OpenInSystemBrowser extends AnAction implements DumbAware {

        private final BrowserTabManager tabManager;

        public OpenInSystemBrowser(BrowserTabManager tabManager) {
            super(TranslationUtil.getText("action.open.system"), TranslationUtil.getText("action.open.system.desc"), WebBrowserIcons.SYSTEM_BROWSER);
            this.tabManager = tabManager;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.open.system"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.open.system.desc"));
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            e.getPresentation().setEnabled(url != null && !url.isBlank() && !"about:blank".equals(url));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            BrowserTabPanel tab = tabManager.getActiveTab();
            String url = tab != null ? tab.getCurrentUrl() : null;
            if (url != null && !url.isBlank() && !"about:blank".equals(url)) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                }
            }
        }
    }

    // 打开 WebBrowser 设置页
    public static class Settings extends AnAction implements DumbAware {

        private final Project project;

        public Settings(Project project) {
            super(TranslationUtil.getText("action.settings"), TranslationUtil.getText("action.settings.desc"), AllIcons.General.Settings);
            this.project = project;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.settings"));
            e.getPresentation().setDescription(TranslationUtil.getText("action.settings.desc"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, BrowserSettingsPage.class);
        }
    }
}
