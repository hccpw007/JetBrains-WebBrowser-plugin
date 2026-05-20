package com.cpw.browser.history

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

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
        if (url.isBlank() || url == "about:blank") return
        val now = System.currentTimeMillis()
        val last = state.entries.lastOrNull()
        // 如果最后一条记录同 URL，更新时间戳（避免连续重复）
        if (last?.url == url) {
            state.entries[state.entries.size - 1] = last.copy(timestamp = now)
        } else {
            state.entries.add(HistoryEntry(url, title.ifBlank { url }, now))
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
