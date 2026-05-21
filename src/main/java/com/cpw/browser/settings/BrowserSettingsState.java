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
        private String homePageUrl = "https://www.google.com";
        private boolean openHomeOnNewTab = false;
        private int maxHistoryDays = 30;
        private int maxHistoryCount = 200;
        private String displayPosition = "toolbar";

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
    }

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

    // 获取 BrowserSettingsState 单例
    public static BrowserSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(BrowserSettingsState.class);
    }
}
