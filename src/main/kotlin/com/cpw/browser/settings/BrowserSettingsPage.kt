package com.cpw.browser.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
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
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        // 主页 URL
        panel.add(JLabel("主页 URL:"))
        panel.add(Box.createVerticalStrut(4))
        homePageField = JBTextField().apply { columns = 30; maximumSize = preferredSize }
        panel.add(homePageField!!)
        panel.add(Box.createVerticalStrut(8))

        // 新标签页打开主页
        openHomeCheckBox = JBCheckBox("新标签页时打开主页")
        panel.add(openHomeCheckBox!!)
        panel.add(Box.createVerticalStrut(12))

        // 历史字段 — 同一行，左右各一个
        val historyRow = JPanel(FlowLayout(FlowLayout.LEFT, 16, 0))
        val daysPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel("历史最多保存天数:"))
            add(Box.createVerticalStrut(4))
            maxHistoryDaysField = JBTextField().apply { columns = 4; maximumSize = preferredSize }
            add(maxHistoryDaysField!!)
        }
        val countPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel("历史最多记录条数:"))
            add(Box.createVerticalStrut(4))
            maxHistoryCountField = JBTextField().apply { columns = 4; maximumSize = preferredSize }
            add(maxHistoryCountField!!)
        }
        historyRow.add(daysPanel)
        historyRow.add(countPanel)
        panel.add(historyRow)

        // 右下角签名
        val footerPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
            add(JLabel("开发者:陈彭伟", SwingConstants.RIGHT).apply {
                font = font.deriveFont(11f)
            })
        }
        panel.add(Box.createVerticalGlue())
        panel.add(footerPanel)

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
