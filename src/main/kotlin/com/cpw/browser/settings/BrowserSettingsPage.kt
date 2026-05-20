package com.cpw.browser.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.TitledSeparator
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
        val c = GridBagConstraints().apply {
            insets = Insets(0, 0, 0, 0)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0; gridy = 0
        }

        // 主页 URL — 标签
        panel.add(JLabel("主页 URL:"), c)
        // 主页 URL — 输入框
        c.gridy = 1; c.weightx = 1.0; c.insets = Insets(2, 0, 6, 0)
        homePageField = JBTextField()
        panel.add(homePageField!!, c)

        // 新标签页时打开主页
        c.gridy = 2; c.weightx = 0.0; c.insets = Insets(0, 0, 0, 0)
        openHomeCheckBox = JBCheckBox("新标签页时打开主页")
        panel.add(openHomeCheckBox!!, c)

        // 分隔线 — 历史设置区域
        c.gridy = 3; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(12, 0, 8, 0)
        panel.add(TitledSeparator("历史记录"), c)

        // 历史天数 — 标签和输入框
        c.gridy = 4; c.gridwidth = 1; c.fill = GridBagConstraints.NONE
        c.insets = Insets(0, 0, 2, 16); c.weightx = 0.5
        val daysLabel = JLabel("最多保存天数:")
        panel.add(daysLabel, c)
        c.gridx = 1
        val countLabel = JLabel("最多记录条数:")
        panel.add(countLabel, c)

        c.gridy = 5; c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(0, 0, 0, 8); c.weightx = 0.5
        maxHistoryDaysField = JBTextField().apply { columns = 4 }
        panel.add(maxHistoryDaysField!!, c)
        c.gridx = 1; c.insets = Insets(0, 8, 0, 0)
        maxHistoryCountField = JBTextField().apply { columns = 4 }
        panel.add(maxHistoryCountField!!, c)

        // 右下角签名
        c.gridy = 6; c.gridx = 1; c.weightx = 1.0; c.anchor = GridBagConstraints.LAST_LINE_END
        c.fill = GridBagConstraints.NONE; c.insets = Insets(20, 0, 0, 0)
        val footerPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
            add(JLabel("开发者:陈彭伟", SwingConstants.RIGHT).apply {
                font = font.deriveFont(11f)
            })
        }
        panel.add(footerPanel, c)

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
