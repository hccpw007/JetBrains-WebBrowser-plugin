package com.cpw.browser.ui

import com.cpw.browser.WebBrowserIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLayeredPane
import javax.swing.JPanel

class AddressBar(
    private val onNavigate: (String) -> Unit,
    private val isUrlBookmarked: (String) -> Boolean,
    private val onToggleBookmark: (String) -> Unit
) : JPanel(BorderLayout()) {

    val urlField = JBTextField()
    private val starLabel = JBLabel(WebBrowserIcons.Star).apply {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val url = urlField.text.trim()
                if (url.isNotEmpty() && url != "about:blank") {
                    onToggleBookmark(url)
                }
            }
        })
    }

    init {
        // 自定义边框：聚焦时不用蓝色
        val normalBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(0xC0C0C0, 0x4A4A4A), 1),
            BorderFactory.createEmptyBorder(0, 4, 0, 22)
        )
        val focusBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(0x909090, 0x888888), 1),
            BorderFactory.createEmptyBorder(0, 4, 0, 22)
        )
        urlField.border = normalBorder
        urlField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                urlField.border = focusBorder
            }
            override fun focusLost(e: FocusEvent) {
                urlField.border = normalBorder
            }
        })

        urlField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    navigate()
                }
            }
        })

        // 使用 JLayeredPane 将星标叠加在文本字段右侧内部
        val fieldLayer = object : JLayeredPane() {
            override fun doLayout() {
                val w = width
                val h = height
                urlField.setBounds(0, 0, w, h)
                val starSize = 16
                starLabel.setBounds(w - starSize - 4, (h - starSize) / 2, starSize, starSize)
                setLayer(starLabel, PALETTE_LAYER)
            }

            override fun getPreferredSize() = urlField.preferredSize
        }

        fieldLayer.add(urlField, JLayeredPane.DEFAULT_LAYER)
        fieldLayer.add(starLabel, JLayeredPane.PALETTE_LAYER)

        add(fieldLayer, BorderLayout.CENTER)
    }

    fun setUrl(url: String) {
        if (url != urlField.text) {
            urlField.text = url
        }
        updateStarIcon(url)
    }

    fun updateStarIcon(url: String) {
        if (url.isNotEmpty() && url != "about:blank") {
            starLabel.icon = if (isUrlBookmarked(url)) WebBrowserIcons.StarFilled else WebBrowserIcons.Star
            starLabel.toolTipText = if (isUrlBookmarked(url)) "从书签中移除" else "添加书签"
        } else {
            starLabel.icon = WebBrowserIcons.Star
            starLabel.toolTipText = "添加书签"
        }
    }

    fun getUrl(): String = urlField.text.trim()

    fun requestFocusOnField() {
        urlField.requestFocus()
        urlField.selectAll()
    }

    private fun navigate() {
        val text = getUrl()
        if (text.isNotEmpty()) {
            onNavigate(text)
        }
    }
}
