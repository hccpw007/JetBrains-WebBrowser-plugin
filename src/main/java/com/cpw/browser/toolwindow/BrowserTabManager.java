package com.cpw.browser.toolwindow;

import com.cpw.browser.settings.BrowserSettingsState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// 多标签页管理器，支持创建/关闭/切换标签页，管理回调事件
public class BrowserTabManager {

    private final List<BrowserTabPanel> tabs = new ArrayList<>();
    private int activeTabIndex = -1;

    // 分离的回调：添加、移除、切换
    private Consumer<BrowserTabPanel> onTabAdded = null;
    private Consumer<BrowserTabPanel> onTabRemoved = null;
    private Consumer<BrowserTabPanel> onActiveTabChanged = null;

    // 获取当前活跃标签页
    public BrowserTabPanel getActiveTab() {
        // 索引在有效范围内则返回对应标签页
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    // 获取标签页数量
    public int getTabCount() {
        return tabs.size();
    }

    // 获取当前活跃标签页的索引
    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    // 设置标签页添加回调
    public void setOnTabAdded(Consumer<BrowserTabPanel> onTabAdded) {
        this.onTabAdded = onTabAdded;
    }

    // 设置标签页移除回调
    public void setOnTabRemoved(Consumer<BrowserTabPanel> onTabRemoved) {
        this.onTabRemoved = onTabRemoved;
    }

    // 设置活跃标签页变更回调
    public void setOnActiveTabChanged(Consumer<BrowserTabPanel> onActiveTabChanged) {
        this.onActiveTabChanged = onActiveTabChanged;
    }

    // 创建新标签页，默认打开空白页
    public BrowserTabPanel createTab() {
        return createTab("about:blank");
    }

    // 创建新标签页并导航到指定 URL
    public BrowserTabPanel createTab(String initialUrl) {
        BrowserTabPanel tab = new BrowserTabPanel(initialUrl);

        // 标签内部状态变更 -> 仅当是当前活动标签时才通知
        tab.setOnUrlChanged(url -> {
            // 仅当本标签是当前活跃标签时通知外部
            if (activeTabIndex >= 0 && activeTabIndex < tabs.size() && tabs.get(activeTabIndex) == tab) {
                // 已设置变更回调则调用
                if (onActiveTabChanged != null) {
                    onActiveTabChanged.accept(tab);
                }
            }
        });
        tab.setOnTitleChanged(title -> {
            // 仅当本标签是当前活跃标签时通知外部
            if (activeTabIndex >= 0 && activeTabIndex < tabs.size() && tabs.get(activeTabIndex) == tab) {
                // 已设置变更回调则调用
                if (onActiveTabChanged != null) {
                    onActiveTabChanged.accept(tab);
                }
            }
        });
        tab.setOnLoadingStateChanged(loading -> {
            // 仅当本标签是当前活跃标签时通知外部
            if (activeTabIndex >= 0 && activeTabIndex < tabs.size() && tabs.get(activeTabIndex) == tab) {
                // 已设置变更回调则调用
                if (onActiveTabChanged != null) {
                    onActiveTabChanged.accept(tab);
                }
            }
        });
        // 网页弹窗/新窗口 -> 在当前插件中新建标签页
        tab.setOnPopupUrl(url -> createTab(url));

        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;
        // 已设置添加回调则调用
        if (onTabAdded != null) {
            onTabAdded.accept(tab);
        }
        // 已设置活跃变更回调则调用
        if (onActiveTabChanged != null) {
            onActiveTabChanged.accept(tab);
        }

        // 如果新建的空白标签且用户开启了"新标签打开主页"，则导航到主页
        if ("about:blank".equals(initialUrl)) {
            BrowserSettingsState settings = BrowserSettingsState.getInstance();
            // 设置了主页 URL 则导航到主页
            if (settings.isOpenHomeOnNewTab() && !settings.getHomePageUrl().isBlank()) {
                tab.navigate(settings.getHomePageUrl());
            }
        }

        return tab;
    }

    // 关闭指定索引的标签页
    public boolean closeTab(int index) {
        // 索引越界时，如果仍有标签页则返回 true
        if (index < 0 || index >= tabs.size()) {
            return !tabs.isEmpty();
        }

        // 如果是最后一个标签页，先创建一个空白标签页，确保 tab 栏不消失
        if (tabs.size() == 1) {
            createTab();
        }

        BrowserTabPanel tab = tabs.get(index);
        tab.dispose();
        tabs.remove(index);
        // 已设置移除回调则调用
        if (onTabRemoved != null) {
            onTabRemoved.accept(tab);
        }

        // 如果标签页已空
        if (tabs.isEmpty()) {
            activeTabIndex = -1;
            // 通知外部无活跃标签页
            if (onActiveTabChanged != null) {
                onActiveTabChanged.accept(null);
            }
            return false;
        }

        // 修正活跃索引
        if (activeTabIndex >= tabs.size()) {
            activeTabIndex = tabs.size() - 1;
        } else if (activeTabIndex > index) { // 关闭的标签页在当前活跃标签之前
            activeTabIndex--;
        }
        // 通知外部活跃标签页变更
        if (onActiveTabChanged != null) {
            onActiveTabChanged.accept(getActiveTab());
        }
        return true;
    }

    // 切换到指定索引的标签页
    public BrowserTabPanel switchToTab(int index) {
        // 索引越界则返回 null
        if (index < 0 || index >= tabs.size()) {
            return null;
        }
        activeTabIndex = index;
        // 已设置活跃变更回调则调用
        if (onActiveTabChanged != null) {
            onActiveTabChanged.accept(getActiveTab());
        }
        return getActiveTab();
    }

    // 获取所有标签页（防御性拷贝）
    public List<BrowserTabPanel> getTabs() {
        return new ArrayList<>(tabs);
    }

    // 放大当前标签页
    public void zoomIn() {
        BrowserTabPanel tab = getActiveTab();
        // 有活跃标签页则放大
        if (tab != null) {
            tab.zoomIn();
        }
    }

    // 缩小当前标签页
    public void zoomOut() {
        BrowserTabPanel tab = getActiveTab();
        // 有活跃标签页则缩小
        if (tab != null) {
            tab.zoomOut();
        }
    }

    // 重置当前标签页缩放
    public void zoomReset() {
        BrowserTabPanel tab = getActiveTab();
        // 有活跃标签页则重置缩放
        if (tab != null) {
            tab.zoomReset();
        }
    }

    // 释放所有标签页资源
    public void disposeAll() {
        // 遍历所有标签页释放资源
        for (BrowserTabPanel tab : tabs) {
            tab.dispose();
        }
        tabs.clear();
        activeTabIndex = -1;
    }
}
