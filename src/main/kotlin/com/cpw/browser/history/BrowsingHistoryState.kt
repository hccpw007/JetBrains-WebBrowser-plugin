package com.cpw.browser.history

import com.cpw.browser.settings.BrowserSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.time.LocalDate
import java.time.ZoneId

data class HistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long  // epoch millis
)

@State(name = "BrowsingHistoryState", storages = [Storage("WebBrowser.xml")])
class BrowsingHistoryState : PersistentStateComponent<BrowsingHistoryState.State> {

    data class State(
        val entries: MutableList<HistoryEntry> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun addEntry(url: String, title: String) {
        if (url.isBlank() || url == "about:blank" || title.isBlank()) return
        val now = System.currentTimeMillis()
        val todayStart = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // 同一天相同 URL 只保留最后一条
        val existingIndex = state.entries.indexOfLast {
            it.url == url && it.timestamp >= todayStart
        }
        if (existingIndex >= 0) state.entries.removeAt(existingIndex)
        state.entries.add(HistoryEntry(url, title, now))
        trimEntries()
    }

    private fun trimEntries() {
        val settings = BrowserSettingsState.getInstance()
        if (settings.maxHistoryDays > 0) {
            val cutoff = System.currentTimeMillis() - settings.maxHistoryDays * 86400000L
            state.entries.removeAll { it.timestamp < cutoff }
        }
        if (settings.maxHistoryCount > 0 && state.entries.size > settings.maxHistoryCount) {
            state.entries.subList(0, state.entries.size - settings.maxHistoryCount).clear()
        }
    }

    fun updateLastEntryTitle(title: String) {
        val last = state.entries.lastOrNull() ?: return
        state.entries[state.entries.size - 1] = last.copy(title = title)
    }

    fun getEntries(): List<HistoryEntry> = state.entries.toList().reversed()

    fun removeEntry(url: String, timestamp: Long) {
        state.entries.removeAll { it.url == url && it.timestamp == timestamp }
    }

    fun clearEntries(hours: Long? = null) {
        if (hours == null) {
            state.entries.clear()
        } else {
            val cutoff = System.currentTimeMillis() - hours * 3600 * 1000
            state.entries.removeAll { it.timestamp >= cutoff }
        }
    }

    companion object {
        fun getInstance(): BrowsingHistoryState =
            ApplicationManager.getApplication().getService(BrowsingHistoryState::class.java)
    }
}
