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

        public String getHomePageUrl() {
            return homePageUrl;
        }

        public void setHomePageUrl(String homePageUrl) {
            this.homePageUrl = homePageUrl;
        }

        public boolean isOpenHomeOnNewTab() {
            return openHomeOnNewTab;
        }

        public void setOpenHomeOnNewTab(boolean openHomeOnNewTab) {
            this.openHomeOnNewTab = openHomeOnNewTab;
        }

        public int getMaxHistoryDays() {
            return maxHistoryDays;
        }

        public void setMaxHistoryDays(int maxHistoryDays) {
            this.maxHistoryDays = maxHistoryDays;
        }

        public int getMaxHistoryCount() {
            return maxHistoryCount;
        }

        public void setMaxHistoryCount(int maxHistoryCount) {
            this.maxHistoryCount = maxHistoryCount;
        }

        public String getDisplayPosition() {
            return displayPosition;
        }

        public void setDisplayPosition(String displayPosition) {
            this.displayPosition = displayPosition;
        }
    }

    private State state = new State();

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public String getHomePageUrl() {
        return state.getHomePageUrl();
    }

    public void setHomePageUrl(String homePageUrl) {
        state.setHomePageUrl(homePageUrl);
    }

    public boolean isOpenHomeOnNewTab() {
        return state.isOpenHomeOnNewTab();
    }

    public void setOpenHomeOnNewTab(boolean openHomeOnNewTab) {
        state.setOpenHomeOnNewTab(openHomeOnNewTab);
    }

    public int getMaxHistoryDays() {
        return state.getMaxHistoryDays();
    }

    public void setMaxHistoryDays(int maxHistoryDays) {
        state.setMaxHistoryDays(maxHistoryDays);
    }

    public int getMaxHistoryCount() {
        return state.getMaxHistoryCount();
    }

    public void setMaxHistoryCount(int maxHistoryCount) {
        state.setMaxHistoryCount(maxHistoryCount);
    }

    public String getDisplayPosition() {
        return state.getDisplayPosition();
    }

    public void setDisplayPosition(String displayPosition) {
        state.setDisplayPosition(displayPosition);
    }

    public static BrowserSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(BrowserSettingsState.class);
    }
}
