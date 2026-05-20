package com.cpw.browser.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class BrowserSettingsPage : Configurable {
    private var homePageField: JBTextField? = null
    private var openHomeCheckBox: JBCheckBox? = null

    override fun getDisplayName() = "WebBrowser"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0; gridy = 0
        }
        panel.add(JLabel("主页 URL:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        homePageField = JBTextField().apply { columns = 30 }
        panel.add(homePageField!!, gbc)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 0.0
        openHomeCheckBox = JBCheckBox("新标签页时打开主页")
        panel.add(openHomeCheckBox!!, gbc)
        return panel
    }

    override fun isModified(): Boolean {
        val state = BrowserSettingsState.getInstance()
        return homePageField?.text != state.homePageUrl ||
                openHomeCheckBox?.isSelected != state.openHomeOnNewTab
    }

    override fun apply() {
        val state = BrowserSettingsState.getInstance()
        state.homePageUrl = homePageField?.text ?: state.homePageUrl
        state.openHomeOnNewTab = openHomeCheckBox?.isSelected ?: state.openHomeOnNewTab
    }

    override fun reset() {
        val state = BrowserSettingsState.getInstance()
        homePageField?.text = state.homePageUrl
        openHomeCheckBox?.isSelected = state.openHomeOnNewTab
    }
}
