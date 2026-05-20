package com.cpw.browser.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "BrowserSettingsState", storages = [Storage("WebBrowser.xml")])
class BrowserSettingsState : PersistentStateComponent<BrowserSettingsState.State> {

    data class State(
        var homePageUrl: String = "https://www.google.com",
        var openHomeOnNewTab: Boolean = false
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    var homePageUrl: String
        get() = state.homePageUrl
        set(value) { state.homePageUrl = value }

    var openHomeOnNewTab: Boolean
        get() = state.openHomeOnNewTab
        set(value) { state.openHomeOnNewTab = value }

    companion object {
        fun getInstance(): BrowserSettingsState =
            ApplicationManager.getApplication().getService(BrowserSettingsState::class.java)
    }
}
