// 浏览历史持久化状态管理
package com.cpw.browser.history;

import com.cpw.browser.settings.BrowserSettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@State(name = "BrowsingHistoryState", storages = @Storage("WebBrowser.xml"))
public class BrowsingHistoryState implements PersistentStateComponent<BrowsingHistoryState.State> {

    // 历史记录硬上限，防止用户设置不限时内存无限增长导致 OOM
    private static final int MAX_HISTORY_HARD_LIMIT = 10000;

    // 浏览历史状态内部数据类
    public static class State {
        // 历史记录条目集合
        private List<HistoryEntry> entries = new ArrayList<>();

        public List<HistoryEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<HistoryEntry> entries) {
            this.entries = entries;
        }
    }

    // 浏览历史状态对象
    private State state = new State();


    // 获取持久化状态
    @Override
    public @Nullable State getState() {
        return state;
    }

    // 加载持久化状态（HistoryEntry 的无参构造和 setter 已确保字段不出现 null）
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // 添加浏览记录，同一天相同 URL 只保留最后一条
    public void addEntry(String url, String title) {
        if (url.isBlank() || url.equals("about:blank") || title.isBlank()) return;

        long now = System.currentTimeMillis();
        long todayStart = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 查找同一天相同 URL 的最后一条记录
        int existingIndex = -1;
        for (int i = state.getEntries().size() - 1; i >= 0; i--) {
            HistoryEntry entry = state.getEntries().get(i);
            if (Objects.equals(entry.getUrl(), url) && entry.getTimestamp() >= todayStart) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            state.getEntries().remove(existingIndex);
        }

        state.getEntries().add(new HistoryEntry(url, title, now));
        trimEntries();
    }

    // 根据设置裁剪历史记录（按天数/条数限制），并强制应用硬上限
    private void trimEntries() {
        BrowserSettingsState settings = BrowserSettingsState.getInstance();
        if (settings.getMaxHistoryDays() > 0) {
            long cutoff = System.currentTimeMillis() - settings.getMaxHistoryDays() * 86400000L;
            state.getEntries().removeIf(entry -> entry.getTimestamp() < cutoff);
        }
        if (settings.getMaxHistoryCount() > 0 && state.getEntries().size() > settings.getMaxHistoryCount()) {
            state.getEntries().subList(0, state.getEntries().size() - settings.getMaxHistoryCount()).clear();
        }
        // 强制硬上限保护：即使用户设置 unlimited，也不超过 MAX_HISTORY_HARD_LIMIT 条，防止 OOM
        if (state.getEntries().size() > MAX_HISTORY_HARD_LIMIT) {
            state.getEntries().subList(0, state.getEntries().size() - MAX_HISTORY_HARD_LIMIT).clear();
        }
    }

    // 更新最后一条记录的标题
    public void updateLastEntryTitle(String title) {
        if (state.getEntries().isEmpty()) return;
        HistoryEntry last = state.getEntries().get(state.getEntries().size() - 1);
        state.getEntries().set(state.getEntries().size() - 1,
                new HistoryEntry(last.getUrl(), title, last.getTimestamp()));
    }

    // 获取所有历史记录（按时间倒序）
    public List<HistoryEntry> getEntries() {
        List<HistoryEntry> reversed = new ArrayList<>(state.getEntries());
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    // 根据 URL 和时间戳移除指定记录
    public void removeEntry(String url, long timestamp) {
        state.getEntries().removeIf(entry -> Objects.equals(entry.getUrl(), url) && entry.getTimestamp() == timestamp);
    }

    // 清除全部历史记录
    public void clearEntries() {
        state.getEntries().clear();
    }

    // 清除指定小时数内的历史记录
    public void clearEntries(long hours) {
        long cutoff = System.currentTimeMillis() - hours * 3600 * 1000;
        state.getEntries().removeIf(entry -> entry.getTimestamp() >= cutoff);
    }

    // 获取 BrowsingHistoryState 单例
    public static BrowsingHistoryState getInstance() {
        return ApplicationManager.getApplication().getService(BrowsingHistoryState.class);
    }
}
