package com.cpw.browser.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class BrowserSettingsPage : Configurable {
    private var homePageField: JBTextField? = null
    private var openHomeCheckBox: JBCheckBox? = null
    private var maxHistoryDaysField: JBTextField? = null
    private var maxHistoryCountField: JBTextField? = null

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

        gbc.gridy = 2
        panel.add(JLabel("历史最多保存天数:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        maxHistoryDaysField = JBTextField().apply { columns = 6 }
        panel.add(maxHistoryDaysField!!, gbc)

        gbc.gridx = 0; gbc.gridy = 3
        panel.add(JLabel("历史最多记录条数:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        maxHistoryCountField = JBTextField().apply { columns = 6 }
        panel.add(maxHistoryCountField!!, gbc)

        // 右下角签名
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.LAST_LINE_END
        val footer = JLabel("开发者:陈彭伟", SwingConstants.RIGHT).apply {
            font = font.deriveFont(11f)
        }
        val footerPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
            add(footer)
        }
        panel.add(footerPanel, gbc)

        return panel
    }

    override fun isModified(): Boolean {
        val state = BrowserSettingsState.getInstance()
        return homePageField?.text != state.homePageUrl ||
                openHomeCheckBox?.isSelected != state.openHomeOnNewTab ||
                maxHistoryDaysField?.text?.toIntOrNull() != state.maxHistoryDays ||
                maxHistoryCountField?.text?.toIntOrNull() != state.maxHistoryCount
    }

    override fun apply() {
        val state = BrowserSettingsState.getInstance()
        state.homePageUrl = homePageField?.text ?: state.homePageUrl
        state.openHomeOnNewTab = openHomeCheckBox?.isSelected ?: state.openHomeOnNewTab
        state.maxHistoryDays = maxHistoryDaysField?.text?.toIntOrNull() ?: state.maxHistoryDays
        state.maxHistoryCount = maxHistoryCountField?.text?.toIntOrNull() ?: state.maxHistoryCount
    }

    override fun reset() {
        val state = BrowserSettingsState.getInstance()
        homePageField?.text = state.homePageUrl
        openHomeCheckBox?.isSelected = state.openHomeOnNewTab
        maxHistoryDaysField?.text = state.maxHistoryDays.toString()
        maxHistoryCountField?.text = state.maxHistoryCount.toString()
    }
}
