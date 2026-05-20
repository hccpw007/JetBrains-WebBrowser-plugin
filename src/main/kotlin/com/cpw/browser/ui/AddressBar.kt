package com.cpw.browser.ui

import com.cpw.browser.WebBrowserIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
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
        // 给文本字段右侧留出空间给内嵌星标
        urlField.border = BorderFactory.createCompoundBorder(
            urlField.border,
            BorderFactory.createEmptyBorder(0, 0, 0, 22)
        )

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
                val starSize = 20
                starLabel.setBounds(w - starSize - 4, (h - starSize) / 2, starSize, starSize)
                setLayer(starLabel, PALETTE_LAYER)
            }

            override fun getPreferredSize() = urlField.preferredSize
        }

        fieldLayer.add(urlField, JLayeredPane.DEFAULT_LAYER)
        fieldLayer.add(starLabel, JLayeredPane.PALETTE_LAYER)

        val goButton = JButton("前往").apply {
            addActionListener { navigate() }
        }

        add(fieldLayer, BorderLayout.CENTER)
        add(goButton, BorderLayout.EAST)
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
