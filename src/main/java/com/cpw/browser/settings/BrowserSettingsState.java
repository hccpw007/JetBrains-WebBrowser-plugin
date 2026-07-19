// 浏览器设置持久化状态
package com.cpw.browser.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "BrowserSettingsState", storages = @Storage("WebBrowser.xml"))
public class BrowserSettingsState implements PersistentStateComponent<BrowserSettingsState.State> {

    // 设置状态内部数据类
    public static class State {
        // 主页 URL 默认值
        private String homePageUrl = "https://www.google.com";
        // 新标签页是否打开主页
        private boolean openHomeOnNewTab = false;
        // 历史记录最多保存天数
        private int maxHistoryDays = 30;
        // 历史记录最多保存条数
        private int maxHistoryCount = 200;
        // 显示位置（toolbar/editor）
        private String displayPosition = "editor";
        // 编辑区模式下点击图标的行为（true=每次新建独立 IDEA 标签页；false=维持 toggle 单个共享 tab）
        private boolean editorNewTabOnClick = false;
        // 开发者工具打开方式（split=当前页面下方/window=独立窗口）
        private String devToolsMode = "window";
        // 界面语言（default=跟随系统/zh=中文/en=英语/ja=日语/ko=韩语/fr=法语/de=德语）
        private String language = "default";
        // 默认搜索引擎
        private String searchEngine = "google";

        // 获取主页 URL
        public String getHomePageUrl() {
            return homePageUrl;
        }

        // 设置主页 URL
        public void setHomePageUrl(String homePageUrl) {
            this.homePageUrl = homePageUrl;
        }

        // 是否在新标签页打开主页
        public boolean isOpenHomeOnNewTab() {
            return openHomeOnNewTab;
        }

        // 设置新标签页是否打开主页
        public void setOpenHomeOnNewTab(boolean openHomeOnNewTab) {
            this.openHomeOnNewTab = openHomeOnNewTab;
        }

        // 获取历史记录最多保存天数
        public int getMaxHistoryDays() {
            return maxHistoryDays;
        }

        // 设置历史记录最多保存天数
        public void setMaxHistoryDays(int maxHistoryDays) {
            this.maxHistoryDays = maxHistoryDays;
        }

        // 获取历史记录最多保存条数
        public int getMaxHistoryCount() {
            return maxHistoryCount;
        }

        // 设置历史记录最多保存条数
        public void setMaxHistoryCount(int maxHistoryCount) {
            this.maxHistoryCount = maxHistoryCount;
        }

        // 获取显示位置
        public String getDisplayPosition() {
            return displayPosition;
        }

        // 设置显示位置
        public void setDisplayPosition(String displayPosition) {
            this.displayPosition = displayPosition;
        }

        // 是否每次点击图标新建独立编辑区标签页
        public boolean isEditorNewTabOnClick() {
            return editorNewTabOnClick;
        }

        // 设置是否每次点击图标新建独立编辑区标签页
        public void setEditorNewTabOnClick(boolean editorNewTabOnClick) {
            this.editorNewTabOnClick = editorNewTabOnClick;
        }

        // 获取开发者工具打开方式
        public String getDevToolsMode() {
            return devToolsMode;
        }

        // 设置开发者工具打开方式
        public void setDevToolsMode(String devToolsMode) {
            this.devToolsMode = devToolsMode;
        }

        // 获取界面语言设置
        public String getLanguage() {
            return language;
        }

        // 设置界面语言
        public void setLanguage(String language) {
            this.language = language;
        }

        // 获取默认搜索引擎
        public String getSearchEngine() {
            return searchEngine;
        }

        // 设置默认搜索引擎
        public void setSearchEngine(String searchEngine) {
            this.searchEngine = searchEngine;
        }
    }

    // 浏览器设置持久化状态对象
    private State state = new State();

    // 获取持久化状态
    @Override
    public @Nullable State getState() {
        return state;
    }

    // 加载持久化状态
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // 获取主页 URL
    public String getHomePageUrl() {
        return state.getHomePageUrl();
    }

    // 设置主页 URL
    public void setHomePageUrl(String homePageUrl) {
        state.setHomePageUrl(homePageUrl);
    }

    // 是否在新标签页打开主页
    public boolean isOpenHomeOnNewTab() {
        return state.isOpenHomeOnNewTab();
    }

    // 设置新标签页是否打开主页
    public void setOpenHomeOnNewTab(boolean openHomeOnNewTab) {
        state.setOpenHomeOnNewTab(openHomeOnNewTab);
    }

    // 获取历史记录最多保存天数
    public int getMaxHistoryDays() {
        return state.getMaxHistoryDays();
    }

    // 设置历史记录最多保存天数
    public void setMaxHistoryDays(int maxHistoryDays) {
        state.setMaxHistoryDays(maxHistoryDays);
    }

    // 获取历史记录最多保存条数
    public int getMaxHistoryCount() {
        return state.getMaxHistoryCount();
    }

    // 设置历史记录最多保存条数
    public void setMaxHistoryCount(int maxHistoryCount) {
        state.setMaxHistoryCount(maxHistoryCount);
    }

    // 获取显示位置
    public String getDisplayPosition() {
        return state.getDisplayPosition();
    }

    // 设置显示位置
    public void setDisplayPosition(String displayPosition) {
        state.setDisplayPosition(displayPosition);
    }

    // 是否每次点击图标新建独立编辑区标签页
    public boolean isEditorNewTabOnClick() {
        return state.isEditorNewTabOnClick();
    }

    // 设置是否每次点击图标新建独立编辑区标签页
    public void setEditorNewTabOnClick(boolean editorNewTabOnClick) {
        state.setEditorNewTabOnClick(editorNewTabOnClick);
    }

    // 获取开发者工具打开方式
    public String getDevToolsMode() {
        return state.getDevToolsMode();
    }

    // 设置开发者工具打开方式
    public void setDevToolsMode(String devToolsMode) {
        state.setDevToolsMode(devToolsMode);
    }

    // 获取界面语言设置
    public String getLanguage() {
        return state.getLanguage();
    }

    // 设置界面语言
    public void setLanguage(String language) {
        state.setLanguage(language);
    }

    // 获取默认搜索引擎
    public String getSearchEngine() {
        return state.getSearchEngine();
    }

    // 设置默认搜索引擎
    public void setSearchEngine(String searchEngine) {
        state.setSearchEngine(searchEngine);
    }

    // 获取 BrowserSettingsState 单例
    public static BrowserSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(BrowserSettingsState.class);
    }
}
